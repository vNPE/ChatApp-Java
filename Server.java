import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class Server {
    private static final int PORT = 3000;
    private static final int HISTORY_LIMIT = 200;

    private static final Logger log = new Logger();

    private static final Db db = new Db();

    private static final List<ClientConnection> connections = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, ClientConnection> connectionsByName =
            new ConcurrentHashMap<>();

    private static final Moderation moderation;
    static {
        try {
            moderation = new Moderation(Path.of("banned-names.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load banned names file", e);
        }
    }

    public static void main(String[] args) {
        ServerArgs a = ServerArgs.parse(args);
        log.setConsole(a.verbose, a.color);

        if (a.help) {
            ServerArgs.printHelp();
            return;
        }

        log.info("Server started");

        ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                pool.submit(() -> handleClient(client));
            }
        } catch (IOException e) {
            log.error("Server error: " + e.getMessage());
        } finally {
            pool.shutdownNow();
            log.info("Server closed");
        }
    }

    private static void handleClient(Socket client) {
        ClientConnection connection = null;
        String addr = client.getInetAddress().getHostAddress() + ":" + client.getPort();

        try (Socket c = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(c.getOutputStream(), true)) {

            String name = in.readLine();
            if (name == null) return;

            name = name.trim();
            if (name.isEmpty()) name = "Anonymous";

            if (moderation.isBannedName(name)) {
                out.println("Name not allowed.");
                return;
            }

            ClientConnection existing = connectionsByName.putIfAbsent(name, new ClientConnection(out, name));
            if (existing != null) {
                out.println("Name '" + name + "' already in use.");
                return;
            }

            connection = connectionsByName.get(name);
            connections.add(connection);

            log.info("Client: " + addr + " connected as: " + name);

            out.println("Hello " + name + ", welcome to the server.");

            // Send last 200 messages by default
            out.println("---- History ----");
            try {
                List<Db.Msg> history = db.lastMessages(HISTORY_LIMIT); // newest->oldest unless you sort
                for (Db.Msg m : history) {
                    out.println(m.sender() + ": " + m.body());
                }
            } catch (SQLException e) {
                out.println("Failed to load history.");
                log.error("History load error (" + name + "): " + e.getMessage());
            }
            out.println("---- End history ----");

            broadcastExcept(connection, name + " has joined");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/exit")) break;

                if (msg.equalsIgnoreCase("/users")) {
                    out.println("Users: " + String.join(", ", connectionsByName.keySet()));
                    continue;
                }

                if (msg.equalsIgnoreCase("/history")) {
                    out.println("---- History ----");
                    try {
                        List<Db.Msg> history = db.lastMessages(HISTORY_LIMIT);
                        for (Db.Msg m : history) {
                            out.println(m.sender() + ": " + m.body());
                        }
                    } catch (SQLException e) {
                        out.println("Failed to load history.");
                        log.error("History load error (" + name + "): " + e.getMessage());
                    }
                    out.println("---- End history ----");
                    continue;
                }

                log.info("Client: " + addr + " (" + name + ") said: " + msg);

                try {
                    db.insertMessage(name, msg);
                } catch (SQLException e) {
                    out.println("Failed to save message.");
                    log.error("DB insert error (" + name + "): " + e.getMessage());
                    continue;
                }

                broadcastExcept(connection, name + ": " + msg);
            }

        } catch (IOException e) {
            log.error("Client handler error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connections.remove(connection);
                connectionsByName.remove(connection.name, connection);

                broadcastExcept(connection, connection.name + " has left");
                log.info("Client disconnected: " + connection.name);
            }
        }
    }

    private static void broadcastExcept(ClientConnection sender, String msg) {
        for (ClientConnection ch : connections) {
            if (ch == sender) continue;
            ch.send(msg);
        }
    }

    private static class ClientConnection {
        private final PrintWriter out;
        private final String name;

        ClientConnection(PrintWriter out, String name) {
            this.out = out;
            this.name = name;
        }

        void send(String msg) {
            out.println(msg);
        }
    }
}

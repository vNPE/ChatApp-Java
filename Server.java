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

    private static final String HISTORY_HEADER = "---- History ----";
    private static final String HISTORY_FOOTER = "---- End history ----";

    // Central logger for server events (startup, disconnects, errors, etc.)
    private static final Logger log = new Logger();

    // Database access for chat history + message persistence
    private static final Db db = new Db();

    // =======================
    // Connected clients (thread-safe)
    // =======================
    private static final List<ClientSession> clients = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, ClientSession> clientsByName =
            new ConcurrentHashMap<>();

    // Moderation rules (banned names) loaded once at startup
    private static final Moderation moderation;
    static {
        try {
            moderation = new Moderation(Path.of("banned-names.txt"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load banned names file", e);
        }
    }

    public static void main(String[] args) {
        ServerArgs serverArgs = ServerArgs.parse(args);
        log.setConsole(serverArgs.verbose, serverArgs.color);

        if (serverArgs.help) {
            ServerArgs.printHelp();
            return;
        }

        log.info("Server started");

        ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool();

        // =======================
        // Accept loop
        // =======================
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.submit(() -> handleClientSession(clientSocket));
            }
        } catch (IOException e) {
            log.error("Server error: " + e.getMessage());
        } finally {
            pool.shutdownNow();
            log.info("Server closed");
        }
    }

    // =======================
    // Client lifecycle (runs per connection in a thread)
    // =======================
    private static void handleClientSession(Socket clientSocket) {
        ClientSession currentClient = null;
        String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

        try (
                Socket client = clientSocket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
                );
                PrintWriter out = new PrintWriter(client.getOutputStream(), true)
        ) {
            // 1) Read username (first line from the client)
            String rawName = in.readLine();
            if (rawName == null) return;

            // 2) Validate username + moderation rules
            String name = normalizeName(rawName);
            if (moderation.isBannedName(name)) {
                out.println("Name not allowed.");
                return;
            }

            // 3) Enforce unique usernames
            ClientSession existing = clientsByName.putIfAbsent(name, new ClientSession(out, name));
            if (existing != null) {
                out.println("Name '" + name + "' already in use.");
                return;
            }

            // 4) Register client so broadcasts can include it
            currentClient = clientsByName.get(name);
            clients.add(currentClient);

            log.info("Client: " + clientId + " connected as: " + name);

            // 5) Welcome + initial history snapshot
            out.println("Hello " + name + ", welcome to the server.");
            sendHistoryBlock(out);

            // 6) Notify others
            broadcastToOthers(currentClient, name + " has joined");

            // 7) Main message loop
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/exit")) break;

                // Commands
                if (msg.equalsIgnoreCase("/users")) {
                    out.println("Users: " + String.join(", ", clientsByName.keySet()));
                    continue;
                }

                if (msg.equalsIgnoreCase("/history")) {
                    sendHistoryBlock(out);
                    continue;
                }

                // Normal message: persist + broadcast
                log.info("Client: " + clientId + " (" + name + ") said: " + msg);

                try {
                    db.insertMessage(name, msg);
                } catch (SQLException e) {
                    out.println("Failed to save message.");
                    log.error("DB insert error (" + name + "): " + e.getMessage());
                    continue;
                }

                broadcastToOthers(currentClient, name + ": " + msg);
            }

        } catch (IOException e) {
            log.error("Client handler error: " + e.getMessage());
        } finally {
            // =======================
            // Cleanup + broadcast leave
            // =======================
            if (currentClient != null) {
                clients.remove(currentClient);
                clientsByName.remove(currentClient.name, currentClient);

                broadcastToOthers(currentClient, currentClient.name + " has left");
                log.info("Client disconnected: " + currentClient.name);
            }
        }
    }

    // Convert raw username input into a usable display name
    private static String normalizeName(String raw) {
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? "Anonymous" : trimmed;
    }

    // Send last HISTORY_LIMIT messages to the given client
    // Used both at connect time and when someone requests /history
    private static void sendHistoryBlock(PrintWriter out) {
        out.println(HISTORY_HEADER);
        try {
            List<Db.Msg> history = db.lastMessages(HISTORY_LIMIT);
            for (Db.Msg m : history) {
                out.println(m.sender() + ": " + m.body());
            }
        } catch (SQLException e) {
            out.println("Failed to load history.");
            log.error("History load error: " + e.getMessage());
        }
        out.println(HISTORY_FOOTER);
    }

    // Broadcast a message to everyone except the sender
    private static void broadcastToOthers(ClientSession sender, String message) {
        for (ClientSession client : clients) {
            if (client == sender) continue;
            client.send(message);
        }
    }

    // Represents one connected client and the stream to write messages to
    private static class ClientSession {
        private final PrintWriter out;
        private final String name;

        private ClientSession(PrintWriter out, String name) {
            this.out = out;
            this.name = name;
        }

        void send(String message) {
            out.println(message);
        }
    }
}

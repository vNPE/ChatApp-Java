import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class Server {
    private static final int PORT = 3000;
    private static final Logger log = new Logger();
    private static boolean shouldExitAfterHelp = false;

    private static final List<ClientConnection> connections = new CopyOnWriteArrayList<>();

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

            log.info("Client: " + addr + " connected as: " + name);

            connection = new ClientConnection(out, name);
            connections.add(connection);

            out.println("Hello " + name + ", welcome to the server.");
            broadcastExcept(connection, name + " has joined");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/exit")) break;

                log.info("Client: " + addr + " (" + name + ") said: " + msg);
                broadcastExcept(connection, name + ": " + msg);
            }
        } catch (IOException e) {
            log.error("Client handler error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connections.remove(connection);
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

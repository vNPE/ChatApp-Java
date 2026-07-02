import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Path;

public class Server {
    enum LogLevel { INFO, WARNING, ERROR }

    private static final int PORT = 3000;
    private static final Logger log = new Logger();

    private static boolean consoleLogEnabled = false;
    private static boolean ansiColorEnabled = false;
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
        argumentParser(args);
        if (shouldExitAfterHelp) return;

        logWithLevel(LogLevel.INFO, "Server started");

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                pool.submit(() -> handleClient(client));
            }
        } catch (IOException e) {
            logWithLevel(LogLevel.ERROR, "Server error: " + e.getMessage());
        } finally {
            pool.shutdownNow();
            logWithLevel(LogLevel.INFO, "Server closed");
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

            logWithLevel(LogLevel.INFO, "Client: " + addr + " connected as: " + name);

            connection = new ClientConnection(out, name);
            connections.add(connection);

            out.println("Hello " + name + ", welcome to the server.");
            broadcastExcept(connection, name + " has joined");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/exit")) break;

                logWithLevel(LogLevel.INFO, "Client: " + addr + " (" + name + ") said: " + msg);
                broadcastExcept(connection, name + ": " + msg);
            }
        } catch (IOException e) {
            logWithLevel(LogLevel.ERROR, "Client handler error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connections.remove(connection);
                broadcastExcept(connection, connection.name + " has left");
                logWithLevel(LogLevel.INFO, "Client disconnected: " + connection.name);
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

    private static void argumentParser(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "-v", "--verbose" -> consoleLogEnabled = true;
                case "-c", "--color" -> ansiColorEnabled = true;
                case "-h", "--help" -> {
                    shouldExitAfterHelp = true;
                    printHelp();
                    return;
                }
                default -> throw new IllegalArgumentException(
                        "Unknown argument. Do -h for a list of valid arguments"
                );
            }
        }
    }

    private static void printHelp() {
        System.out.println(
                "Available flags:\n"
                        + "  -v, --verbose   Prints everything that gets added to the log to the console.\n"
                        + "  -c, --color     Uses ANSI colors for console log output.\n"
                        + "  -h, --help      Prints this help message."
        );
    }

    private static void logWithLevel(LogLevel severity, String msg) {
        if (consoleLogEnabled) {
            String label = severity + ": ";
            String code = switch (severity) {
                case INFO -> Color.blue();
                case WARNING -> Color.yellow();
                case ERROR -> Color.red();
            };
            System.out.println(Color.wrap(label, code, ansiColorEnabled) + msg);
        }
        switch (severity) {
            case INFO -> log.info(msg);
            case WARNING -> log.warning(msg);
            case ERROR -> log.error(msg);
        }
    }
}

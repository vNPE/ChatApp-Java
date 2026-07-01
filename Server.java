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

public class Server {
    enum LogLevel { INFO, WARNING, ERROR }

    private static final int listenPort = 3000;
    private static final Logger log = new Logger();

    private static boolean enableConsoleLog = false;
    private static boolean enableAnsiColor = false;
    private static boolean helpRequested = false;

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        argumentParser(args);
        if (helpRequested) return;

        logWithLevel(LogLevel.INFO, "Server started");

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(listenPort)) {
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
        ClientHandler handler = null;

        try (Socket c = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(c.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null) return;

            String addr = c.getInetAddress().getHostAddress() + ":" + c.getPort();
            logWithLevel(LogLevel.INFO, "Client: " + addr + " said: " + line);

            handler = new ClientHandler(out);
            clients.add(handler);

            out.println("Hello client, I heard: " + line);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/exit")) break;

                logWithLevel(LogLevel.INFO, "Client: " + addr + " said: " + msg);
                broadcastExcept(handler, addr + " -> " + msg);
            }

        } catch (IOException e) {
            logWithLevel(LogLevel.ERROR, "Client handler error: " + e.getMessage());
        } finally {
            if (handler != null) {
                clients.remove(handler);
                logWithLevel(LogLevel.INFO, "Client disconnected");
            }
        }
    }

    private static void broadcastExcept(ClientHandler sender, String msg) {
        for (ClientHandler ch : clients) {
            if (ch == sender) continue;
            ch.send(msg);
        }
    }

    private static class ClientHandler {
        private final PrintWriter out;

        ClientHandler(PrintWriter out) {
            this.out = out;
        }

        void send(String msg) {
            out.println(msg);
        }
    }

    private static void argumentParser(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "-v":
                case "--verbose":
                    enableConsoleLog = true;
                    break;
                case "-c":
                case "--color":
                    enableAnsiColor = true;
                    break;
                case "-h":
                case "--help":
                    helpRequested = true;
                    printHelp();
                    return;
                default:
                    throw new IllegalArgumentException("Unknown argument. Do -h for a list of valid arguments");
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
        switch (severity) {
            case INFO -> log.info(msg);
            case WARNING -> log.warning(msg);
            case ERROR -> log.error(msg);
        }
        if (!enableConsoleLog) return;

        String label = switch (severity) {
            case INFO -> "INFO: ";
            case WARNING -> "WARNING: ";
            case ERROR -> "ERROR: ";
        };

        String code = switch (severity) {
            case INFO -> Color.blue();
            case WARNING -> Color.yellow();
            case ERROR -> Color.red();
        };

        System.out.println(Color.wrap(label, code, enableAnsiColor) + msg);
    }
}

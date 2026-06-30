import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server{
    private static final String ERROR_COLOR_CODE = "\u001B[31m";
    private static final String WARNING_COLOR_CODE = "\u001B[33m";
    private static final String INFO_COLOR_CODE = "\u001B[34m";

    private static final int listenPort = 3000;
    private static final Logger log = new Logger();

    private static boolean enableConsoleLog=false;
    private static boolean enableAnsiColor=false;
    private static boolean helpRequested = false;

    public static void main(String[] args){
        argumentParser(args);
        if(helpRequested) return;

        logWithLevel("INFO", "Server started");

        ExecutorService pool = Executors.newCachedThreadPool();

        try(ServerSocket server = new ServerSocket(listenPort)){
            while (true){
                Socket client = server.accept();
                pool.submit(() -> handleClient(client));
            }
        }
        catch(IOException e){
            logWithLevel("ERROR", "Server error: "+e.getMessage());
        }
        finally{
            pool.shutdownNow();
            logWithLevel("INFO", "Server closed");
        }
    }

    private static void handleClient(Socket client){
        try(Socket c = client;
            BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(c.getOutputStream(), true)){
            
            String line = in.readLine();
            if(line==null)
                return;
            logWithLevel("INFO", "Client: "+c.getInetAddress().getHostAddress()+":"+c.getPort()+" said: "+line);
            out.println("Hello client, I heard: "+line);
        }
        catch(IOException e){
            logWithLevel("ERROR", "Client handler error: "+e.getMessage());
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


    private static void logWithLevel(String severity, String msg){
        switch(severity){
            case "INFO":
                log.info(msg);
                if(enableConsoleLog)
                    System.out.println(Color.wrap("INFO: ",INFO_COLOR_CODE, enableAnsiColor)+msg);
                return;
            case "WARNING":
                log.warning(msg);
                if(enableConsoleLog)
                    System.out.println(Color.wrap("WARNING: ",WARNING_COLOR_CODE, enableAnsiColor)+msg);
                return;
            case "ERROR":
                log.error(msg);
                if(enableConsoleLog)
                    System.out.println(Color.wrap("ERROR: ",ERROR_COLOR_CODE, enableAnsiColor)+msg);
                return;
            default:
                throw new IllegalArgumentException("Unknown log level: " + severity);
        }
    }
}

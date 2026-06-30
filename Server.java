import java.io.*;
import java.net.*;

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
        try(ServerSocket server = new ServerSocket(listenPort)){
            Socket client = server.accept();
        
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            String line = in.readLine();
            logWithLevel("INFO", "Client: "+client.getInetAddress().getHostAddress()+":"+client.getPort()+" said: "+line);
            out.println("Hello client, I heard: "+line);
            client.close();
            server.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        logWithLevel("INFO", "Server Closed");
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
        if("INFO".equals(severity)){
            log.info(msg);
            if(enableConsoleLog)
                System.out.println(Color.wrap("INFO: ",INFO_COLOR_CODE, enableAnsiColor)+msg);
        }
        else if("WARNING".equals(severity)){
            log.warning(msg);
            if(enableConsoleLog)
                System.out.println(Color.wrap("WARNING: ",WARNING_COLOR_CODE, enableAnsiColor)+msg);
        }
        else if("ERROR".equals(severity)){
            log.error(msg);
            if(enableConsoleLog)
                System.out.println(Color.wrap("ERROR: ",ERROR_COLOR_CODE, enableAnsiColor)+msg);
        }
        else
            throw new IllegalArgumentException("Unknown log level: " + severity);
    }
}

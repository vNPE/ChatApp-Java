import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

public class Server{
    private static final String ERROR_COLOR_CODE = "\u001B[31m";
    private static final String WARNING_COLOR_CODE = "\u001B[33m";
    private static final String INFO_COLOR_CODE = "\u001B[34m";

    private static final int listenPort = 3000;
    private static final Logger log = new Logger();
    private static boolean enableConsoleLog=false;
    private static boolean enableAnsiColor=false;

    public static void main(String[] args){
        if(args.length>0){
            for(int i=0;i<args.length;i++)
                if(args[i].equals("-v"))
                    enableConsoleLog=true;
                else if(args[i].equals("--color"))
                    enableAnsiColor=true;
                else
                    System.out.println("Unknow argument: "+args[i]);
        }

        log.info("Server started");
        try(ServerSocket server = new ServerSocket(listenPort)){
            Socket client = server.accept();
        
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            String line = in.readLine();
            log("INFO", "Client: "+client.getInetAddress().getHostAddress()+":"+client.getPort()+" said: "+line);
            out.println("Hello client, I heard: "+line);
            client.close();
            server.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        log.info("Server Closed");
    }

    public static void log(String severity, String msg){
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

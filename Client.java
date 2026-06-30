import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Scanner;

public class Client{
    private static final int destPort = 3000;
    private static final String destIp = "localhost";

    public static void main(String[] args){
        try (Socket socket = new Socket(destIp, destPort)){
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner input = new Scanner(System.in);
            out.println(input.nextLine());

            String reply = in.readLine();
            System.out.println("Server replied: " +reply);
            socket.close();
            input.close();
        }
        catch(IOException e){
            e.printStackTrace(); 
        }
    }
}

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final int destPort = 3000;
    private static final String destIp = "localhost";

    public static void main(String[] args) {
        try (Socket socket = new Socket(destIp, destPort);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            System.out.println("Enter your first message:");
            String firstMsg = userIn.readLine();
            if (firstMsg == null) return;

            out.println(firstMsg);

            Thread receiver = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException ignored) {
                }
                System.out.println("Disconnected from server.");
            });
            receiver.setDaemon(true);
            receiver.start();

            while (true) {
                String msg = userIn.readLine();
                if (msg == null) break;

                out.println(msg);
                if (msg.trim().equalsIgnoreCase("/exit")) break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

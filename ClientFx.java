import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientFx extends Application {
    // Defaults (user can override)
    private static final String defaultIp = "localhost";
    private static final int defaultPort = 3000;

    private TextArea chatArea;
    private TextField input;
    private TextField nameField;
    private Button sendBtn;

    private TextField ipField;
    private TextField portField;

    private Socket socket;
    private BufferedReader serverIn;
    private PrintWriter out;

    @Override
    public void start(Stage stage) {
        chatArea = new TextArea();
        chatArea.setEditable(false);

        nameField = new TextField();
        nameField.setPromptText("Your name");

        ipField = new TextField(defaultIp);
        ipField.setPromptText("Server IP");

        portField = new TextField(String.valueOf(defaultPort));
        portField.setPromptText("Server port");

        input = new TextField();
        input.setPromptText("Message");

        sendBtn = new Button("Send");
        sendBtn.setDisable(true);

        Button connectBtn = new Button("Connect");
        connectBtn.setOnAction(e -> connect());

        sendBtn.setOnAction(e -> send());
        input.setOnAction(e -> send());

        VBox root = new VBox(
                8,
                new Label("Name:"), nameField,
                new Label("Server IP:"), ipField,
                new Label("Server port:"), portField,
                connectBtn,
                chatArea,
                input,
                sendBtn
        );

        stage.setTitle("Chat Client");
        stage.setScene(new Scene(root, 520, 420));
        stage.show();
    }

    private void connect() {
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            alert("Enter a name first.");
            return;
        }

        String ip = ipField.getText();
        if (ip == null || ip.isBlank()) {
            alert("Enter a server IP.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (Exception ex) {
            alert("Enter a valid port (1-65535).");
            return;
        }

        new Thread(() -> {
            try {
                socket = new Socket(ip, port);
                serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println(name);

                Platform.runLater(() -> {
                    sendBtn.setDisable(false);
                    input.requestFocus();
                    chatArea.appendText("Connected to " + ip + ":" + port + "\n");
                });

                String line;
                while ((line = serverIn.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> chatArea.appendText(msg + "\n"));
                }

                Platform.runLater(() -> chatArea.appendText("Disconnected from server.\n"));
            } catch (IOException ex) {
                Platform.runLater(() -> alert("Connection failed: " + ex.getMessage()));
            }
        }, "client-receiver").start();
    }

    private void send() {
        if (out == null) return;
        String msg = input.getText();
        if (msg == null) return;

        out.println(msg);
        chatArea.appendText("Me: " + msg + "\n");

        input.clear();

        if (msg.trim().equalsIgnoreCase("/exit")) {
            close();
        }
    }

    private void close() {
        new Thread(() -> {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
            Platform.runLater(() -> sendBtn.setDisable(true));
        }).start();
    }

    private void alert(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

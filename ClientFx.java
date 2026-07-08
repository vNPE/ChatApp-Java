import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientFx extends Application {
    private static final String DEFAULT_IP = "localhost";
    private static final int DEFAULT_PORT = 3000;

    // =======================
    // UI fields
    // =======================
    private TextArea chatArea;
    private TextField usernameField;
    private TextField serverIpField;
    private TextField serverPortField;
    private TextField messageField;
    private Button sendButton;

    // =======================
    // Networking fields
    // =======================
    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;

    @Override
    public void start(final Stage stage) {
        // =======================
        // Build UI
        // =======================
        chatArea = new TextArea();
        chatArea.setEditable(false);

        usernameField = new TextField();
        usernameField.setPromptText("Your name");

        serverIpField = new TextField(DEFAULT_IP);
        serverIpField.setPromptText("Server IP");

        serverPortField = new TextField(String.valueOf(DEFAULT_PORT));
        serverPortField.setPromptText("Server port");

        messageField = new TextField();
        messageField.setPromptText("Message");

        sendButton = new Button("Send");
        sendButton.setDisable(true);

        final Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> onConnectClicked());
        sendButton.setOnAction(e -> onSendClicked());
        messageField.setOnAction(e -> onSendClicked());

        final VBox root = new VBox(
                8,
                new Label("Name:"), usernameField,
                new Label("Server IP:"), serverIpField,
                new Label("Server port:"), serverPortField,
                connectButton,
                chatArea,
                messageField,
                sendButton
        );

        stage.setTitle("Chat Client");
        stage.setScene(new Scene(root, 520, 420));
        stage.show();
    }

    // =======================
    // UI event handlers
    // =======================
    private void onConnectClicked() {
        final String username = usernameField.getText();
        if (username == null || username.isBlank()) {
            alert("Enter a name first.");
            return;
        }

        final String ip = serverIpField.getText();
        if (ip == null || ip.isBlank()) {
            alert("Enter a server IP.");
            return;
        }

        final int port;
        try {
            port = Integer.parseInt(serverPortField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (Exception ex) {
            alert("Enter a valid port (1-65535).");
            return;
        }

        // Disable until connection succeeds (re-enabled in connectAndListen)
        sendButton.setDisable(true);

        new Thread(() -> connectAndListen(username, ip, port), "client-receiver").start();
    }

    private void onSendClicked() {
        if (serverWriter == null) return;

        final String msg = messageField.getText();
        if (msg == null || msg.isBlank()) return;

        serverWriter.println(msg);
        chatArea.appendText("Me: " + msg + "\n");
        messageField.clear();

        if (msg.trim().equalsIgnoreCase("/exit")) {
            closeConnectionAsync();
        }
    }

    // =======================
    // Networking
    // =======================
    private void connectAndListen(final String username, final String ip, final int port) {
        try {
            socket = new Socket(ip, port);
            serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            serverWriter = new PrintWriter(socket.getOutputStream(), true);

            serverWriter.println(username);

            Platform.runLater(() -> {
                sendButton.setDisable(false);
                messageField.requestFocus();
                chatArea.appendText("Connected to " + ip + ":" + port + "\n");
            });

            String line;
            while ((line = serverReader.readLine()) != null) {
                final String message = line;
                Platform.runLater(() -> chatArea.appendText(message + "\n"));
            }

            Platform.runLater(() -> chatArea.appendText("Disconnected from server.\n"));
        } catch (IOException ex) {
            Platform.runLater(() -> alert("Connection failed: " + ex.getMessage()));
        } finally {
            cleanup();
            Platform.runLater(() -> sendButton.setDisable(true));
        }
    }

    private void closeConnectionAsync() {
        new Thread(() -> {
            cleanup();
            Platform.runLater(() -> sendButton.setDisable(true));
        }, "client-closer").start();
    }

    private void cleanup() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        } finally {
            socket = null;
            serverReader = null;
            serverWriter = null;
        }
    }

    // =======================
    // Dialog
    // =======================
    private void alert(final String text) {
        new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK).showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

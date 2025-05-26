import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class client{
    private JFrame frame;
    private JTextArea textArea;
    private JTextField inputField;
    private JButton sendButton;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public client(String serverAddress, int port) {
        setupGUI();
        connectToServer(serverAddress, port);
    }

    private void setupGUI() {
        frame = new JFrame("Word Chain Game");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        inputField = new JTextField();
        inputField.setEnabled(false);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);

        sendButton.addActionListener(e -> sendWord());
        inputField.addActionListener(e -> sendWord());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connectToServer(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleServerMessage(line);
                    }
                } catch (IOException e) {
                    appendMessage("Disconnected from server.");
                }
            }).start();

        } catch (IOException e) {
            appendMessage("Cannot connect to server: " + e.getMessage());
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("YOUR_TURN ")) {
            String lastWord = message.substring("YOUR_TURN ".length());
            appendMessage("🔔 Your turn! Last word: " + lastWord);
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();
        } else {
            appendMessage(message);
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
            inputField.setText("");
        }
    }

    private void sendWord() {
        String word = inputField.getText().trim();
        if (!word.isEmpty()) {
            out.println(word);
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
            inputField.setText("");
        }
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new client("localhost", 10000));
    }
}

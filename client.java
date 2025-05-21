import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class client {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public client(String serverAddress, int port) {
        setupGUI();
        connectToServer(serverAddress, port);
        listenForMessages();
    }

    private void setupGUI() {
        frame = new JFrame("Word Chain Game - Client");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        sendButton.addActionListener(e -> sendWord());
        inputField.addActionListener(e -> sendWord());
    }

    private void connectToServer(String address, int port) {
        try {
            socket = new Socket(address, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            chatArea.append("Error connecting to server\n");
        }
    }

    private void listenForMessages() {
        Thread thread = new Thread(() -> {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    chatArea.append("Server: " + message + "\n");
                }
            } catch (IOException e) {
                chatArea.append("Connection closed\n");
            }
        });
        thread.start();
    }

    private void sendWord() {
        String word = inputField.getText().trim();
        if (!word.isEmpty()) {
            chatArea.append("You: " + word + "\n");
            out.println(word);
            inputField.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new client("localhost", 10000));
    }
}


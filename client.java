// client.java (เพิ่มระบบตั้งชื่อผู้เล่น)

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class client {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton resetButton;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean myTurn = false;
    private String playerName = "";

    public client(String serverAddress, int port) {
        askForName();
        setupGUI();
        connectToServer(serverAddress, port);
        listenForMessages();
    }

    private void askForName() {
        playerName = JOptionPane.showInputDialog(null, "Enter your name:", "Player Name", JOptionPane.QUESTION_MESSAGE);
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Player" + (int) (Math.random() * 1000);
        }
    }

    private void setupGUI() {
        frame = new JFrame("Word Chain Game - " + playerName);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setEnabled(false);

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendButton.setEnabled(false);

        resetButton = new JButton("Reset");
        resetButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        resetButton.setEnabled(false);

        sendButton.addActionListener(e -> sendWord());
        inputField.addActionListener(e -> sendWord());
        resetButton.addActionListener(e -> {
            out.println("RESET");
            messageArea.setText("");
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(sendButton);
        buttonPanel.add(resetButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void connectToServer(String address, int port) {
        try {
            socket = new Socket(address, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("NAME " + playerName);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "❌ Could not connect to the server.\nPlease make sure the server is running.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenForMessages() {
        Thread listener = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("YOUR_TURN")) {
                        String[] parts = message.split(" ", 2);
                        if (parts.length > 1) {
                            appendMessage("🔔 Your turn! Last word: " + parts[1]);
                        }
                        myTurn = true;
                        inputField.setEnabled(true);
                        sendButton.setEnabled(true);
                        resetButton.setEnabled(true);
                    } else {
                        appendMessage(message);
                        if (message.contains("You win") || message.contains("You lose")) {
                            inputField.setEnabled(false);
                            sendButton.setEnabled(false);
                            myTurn = false;
                        }
                    }
                }
            } catch (IOException e) {
                appendMessage("❌ Connection closed.");
            }
        });
        listener.start();
    }

    private void sendWord() {
        if (!myTurn)
            return;

        String word = inputField.getText().trim();
        if (!word.isEmpty()) {
            out.println(word);
            inputField.setText("");
            myTurn = false;
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

    private void appendMessage(String message) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new client("localhost", 10000));
    }
}


// server.java
import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int PORT = 10000;
    private static final int MAX_PLAYERS = 2;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static String lastWord;
    private static int currentTurn = 0;
    private static Set<String> usedWords = new HashSet<>();
    private static List<String> word = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        loadword("word.txt");
        lastWord = getRandomWord();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        while (clients.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket, clients.size());
            clients.add(client);
            new Thread(client).start();
        }

        usedWords.add(lastWord.toLowerCase());
        broadcast("Game started! First word: " + lastWord);
        clients.get(currentTurn).yourTurn();
    }

    private static void loadword(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    word.add(line.trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load word. Using default word.");
            word.add("start");
        }
    }

    private static String getRandomWord() {
        Random rand = new Random();
        return word.get(rand.nextInt(word.size()));
    }

    public static synchronized void handleWord(String word, int playerId) {
        if (playerId != currentTurn) {
            clients.get(playerId).send("Not your turn.");
            return;
        }

        word = word.toLowerCase();

        if (usedWords.contains(word)) {
            clients.get(playerId).send("Word already used! You lost. Game over.");
            clients.get(1 - playerId).send("You win!");
            return;
        }

        if (word.charAt(0) != lastWord.toLowerCase().charAt(lastWord.length() - 1)) {
            clients.get(playerId).send("Incorrect! You lost. Game over.");
            clients.get(1 - playerId).send("You win!");
            return;
        }

        lastWord = word;
        usedWords.add(word);
        broadcast("Player " + (playerId + 1) + " played: " + word);
        currentTurn = 1 - currentTurn;
        clients.get(currentTurn).yourTurn();
    }

    public static void broadcast(String msg) {
        for (ClientHandler client : clients) {
            client.send(msg);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int playerId;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        public void send(String msg) {
            out.println(msg);
        }

        public void yourTurn() {
            out.println("YOUR_TURN " + lastWord);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("You is PLAYER" + (playerId + 1));

                String input;
                while ((input = in.readLine()) != null) {
                    handleWord(input.trim(), playerId);
                }
            } catch (IOException e) {
                System.out.println("Player " + (playerId + 1) + " disconnected.");
            }
        }
    }
}

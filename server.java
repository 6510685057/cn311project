import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    private static final int PORT = 10000;
    private static final int MAX_PLAYERS = 2;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static List<String> playerNames = new ArrayList<>(Arrays.asList("Player1", "Player2"));
    private static String lastWord;
    private static int currentTurn = new Random().nextInt(2);
    private static Set<String> usedWords = new HashSet<>();
    private static Set<String> dictionary = new HashSet<>();
    private static List<String> wordList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        loadWordFile("word.txt");
        lastWord = getRandomWord();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("✅ Loaded " + dictionary.size() + " words from word.txt");
        System.out.println("Server started on port " + PORT);

        while (clients.size() < MAX_PLAYERS) {
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket, clients.size());
            clients.add(client);
            new Thread(client).start();
        }

        usedWords.add(lastWord.toLowerCase());
        broadcast("Game started! First word: " + lastWord);
        broadcast(playerNames.get(currentTurn) + " will start.");
        clients.get(currentTurn).yourTurn();
    }

    private static void loadWordFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String word = line.trim().toLowerCase();
                if (!word.isEmpty()) {
                    dictionary.add(word);
                    wordList.add(word);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to load word file.");
            dictionary.add("start");
            wordList.add("start");
        }
    }

    private static String getRandomWord() {
        Random rand = new Random();
        return wordList.get(rand.nextInt(wordList.size()));
    }

    public static synchronized void handleWord(String word, int playerId) {
        if (playerId != currentTurn) {
            clients.get(playerId).send("Not your turn.");
            return;
        }

        word = word.toLowerCase();

        if (!dictionary.contains(word)) {
            clients.get(playerId).send("❌ '" + word + "' is not in the dictionary. You lose.");
            clients.get(1 - playerId).send("🎉 You win!");
            return;
        }

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
        broadcast(playerNames.get(playerId) + " played: " + word);
        currentTurn = 1 - currentTurn;
        clients.get(currentTurn).yourTurn();
    }

    public static void broadcast(String msg) {
        for (ClientHandler client : clients) {
            client.send(msg);
        }
    }

    private static synchronized void resetGame() {
        lastWord = getRandomWord();
        usedWords.clear();
        usedWords.add(lastWord.toLowerCase());
        currentTurn = new Random().nextInt(2);
        broadcast("🔁 Game restarted! First word: " + lastWord);
        broadcast(playerNames.get(currentTurn) + " will start.");
        clients.get(currentTurn).yourTurn();
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

                // Wait for name from client before greeting
                String input;
                while ((input = in.readLine()) != null) {
                    input = input.trim();
                    if (input.toUpperCase().startsWith("NAME ")) {
                        String name = input.substring(5).trim();
                        if (!name.isEmpty()) {
                            playerNames.set(playerId, name);
                            System.out.println("✅ Player " + (playerId + 1) + " set name to: " + name);
                            break;
                        }
                    }
                }

                out.println("You are " + playerNames.get(playerId));

                while ((input = in.readLine()) != null) {
                    input = input.trim();
                    if (input.equalsIgnoreCase("RESET")) {
                        resetGame();
                    } else {
                        handleWord(input, playerId);
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Error handling client " + (playerId + 1));
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class server {
    private static final int PORT = 10000;
    private static Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private static String lastWord = null;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Word Chain Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                clients.add(out);
                out.println("Welcome to Word Chain Game!");

                String word;
                while ((word = in.readLine()) != null) {
                    synchronized (server.class) {
                        if (isValidWord(word)) {
                            lastWord = word;
                            broadcast("[✔] Accepted: " + word);
                        } else {
                            out.println("[✘] Invalid word. Try again.");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(out);
            }
        }

        private boolean isValidWord(String word) {
            if (lastWord == null) return true;
            if (word.isEmpty()) return false;
            return word.toLowerCase().charAt(0) == lastWord.toLowerCase().charAt(lastWord.length() - 1);
        }

        private void broadcast(String message) {
            for (PrintWriter client : clients) {
                client.println(message);
            }
        }
    }
}

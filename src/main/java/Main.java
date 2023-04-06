import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final Map<String, String> STORE = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        final int port = 6379;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                new RedisConnection(clientSocket).start();
            }
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    public static class RedisConnection extends Thread {

        private Socket clientSocket;

        public RedisConnection(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                Scanner scanner = new Scanner(clientSocket.getInputStream());
                OutputStream outputStream = clientSocket.getOutputStream();
                boolean isEchoCommand = false;
                boolean isSetKey = false;
                boolean isSetValue = false;
                boolean isGetCommand = false;
                String key = null;

                while (scanner.hasNext()) {
                    String next = scanner.nextLine();
                    System.out.println("next = " + next);

                    if (next.equalsIgnoreCase("PING")) {
                        outputStream.write("+PONG\r\n".getBytes());
                    } else if (next.equalsIgnoreCase("DOCS")) {
                        outputStream.write("+\r\n".getBytes());
                    } else if (next.equalsIgnoreCase("ECHO")) {
                        isEchoCommand = true;
                    } else if (next.equalsIgnoreCase("SET")) {
                        isSetKey = true;
                    } else if (next.equalsIgnoreCase("GET")) {
                        isGetCommand = true;
                    } else if (next.matches("^(?![+\\-:$*]).+")) {
                        if (isEchoCommand) {
                            outputStream.write(("+" + next + "\r\n").getBytes());
                        } else if (isSetKey) {
                            key = next;
                            isSetValue = true;
                            isSetKey = false;
                        } else if (isSetValue && key != null) {
                            STORE.put(key, next);
                            outputStream.write("+OK\r\n".getBytes());
                        } else if (isGetCommand) {
                            String value = STORE.getOrDefault(next, "nil");
                            outputStream.write(("+" + value + "\r\n").getBytes());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            } finally {
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            }
        }
    }
}

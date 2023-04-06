import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final Map<String, String> VALUE_STORE = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> EXPIRY_STORE = new ConcurrentHashMap<>();

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
                boolean isExpiry = false;
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
                    } else if (next.equalsIgnoreCase("PX")) {
                        isExpiry = true;
                    } else if (next.equalsIgnoreCase("GET")) {
                        isGetCommand = true;
                    } else if (next.matches("^(?![+\\-:$*]).+")) {
                        if (isEchoCommand) {
                            outputStream.write(("+" + next + "\r\n").getBytes());
                        } else if (isSetKey) {
                            outputStream.write("+OK\r\n".getBytes());
                            key = next;
                            isSetValue = true;
                            isSetKey = false;
                        } else if (isSetValue && key != null) {
                            VALUE_STORE.put(key, next);
                            isSetValue = false;
                        } else if (isExpiry) {
                            EXPIRY_STORE.put(key, LocalDateTime.now().plus(Long.parseLong(next), ChronoUnit.MILLIS));
                        } else if (isGetCommand) {
                            if (EXPIRY_STORE.containsKey(next)) {
                                if (LocalDateTime.now().isBefore(EXPIRY_STORE.get(next))) {
                                    outputStream.write(("+" + VALUE_STORE.get(next) + "\r\n").getBytes());
                                } else {
                                    outputStream.write("$-1\r\n".getBytes());
                                }
                            } else {
                                String value = VALUE_STORE.get(next);
                                if (value != null) {
                                    outputStream.write(("+" + value + "\r\n").getBytes());
                                } else {
                                    outputStream.write("$-1\r\n".getBytes());
                                }
                            }
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

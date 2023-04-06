import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final Map<String, String> VALUE_STORE = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> EXPIRY_STORE = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        final int port = 6379;

        try (final Selector selector = Selector.open();
             final ServerSocketChannel serverChannel = ServerSocketChannel.open()
        ) {
            serverChannel.bind(new InetSocketAddress("localhost", port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server channel listening");

            while (true) {
                selector.select();
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();

                    if (key.isValid() == false) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptConnection(selector, serverChannel);
                    } else if (key.isReadable()) {
                        readFromChannel(buffer, key);
                    }

                    selectedKeys.remove();
                }
            }
        } catch (Exception e) {
            System.out.println("error: " + Arrays.toString(e.getStackTrace()));
        }
    }

    private static void acceptConnection(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientSocket = serverSocket.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        System.out.println("client connected");
    }

    private static void readFromChannel(ByteBuffer buffer, SelectionKey key) throws IOException {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        clientSocket.read(buffer);
        printBuffer(buffer);

        if (buffer.position() == 0) {
            clientSocket.close();
            return;
        }

        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        RedisCommand redisCommand = new RedisCommand(new String(data));
        redisCommand.response(clientSocket);

        buffer.clear();
    }

    private static void printBuffer(ByteBuffer buffer) {
        int limit = buffer.limit();
        int position = buffer.position();
        System.out.println("buffer = " + new String(buffer.array()));
        System.out.println("position = " + position);
        System.out.println("limit = " + limit);
    }

    public static class RedisCommand {

        private String command;
        private String commandKey;
        private String commandValue;

        private String option;
        private String optionValue;

        public RedisCommand(String input) {
            String[] commands = input.trim().split("\r\n");
            for (int i = 0; i < commands.length; i++) {
                if (commands[i].matches("^(?![+\\-:$*]).+")) {
                    String command = commands[i];

                    switch (command.toUpperCase()) {
                        case "PING":
                            this.command = "PING";
                            break;
                        case "ECHO":
                            this.command = "ECHO";
                            this.commandValue = commands[i + 2];
                            break;
                        case "DOCS":
                            this.command = "DOCS";
                            break;
                        case "GET":
                            this.command = "GET";
                            this.commandKey = commands[i + 2];
                            break;
                        case "SET":
                            this.command = "SET";
                            this.commandKey = commands[i + 2];
                            this.commandValue = commands[i + 4];
                            break;
                        case "PX":
                            this.option = "PX";
                            this.optionValue = commands[i + 2];
                            break;
                    }
                }
            }

            System.out.println("command = " + command);
            System.out.println("commandKey = " + commandKey);
            System.out.println("commandValue = " + commandValue);
            System.out.println("option = " + option);
            System.out.println("optionValue = " + optionValue);
        }

        public void response(SocketChannel clientSocket) throws IOException {
            switch (command) {
                case "PING":
                    clientSocket.write(ByteBuffer.wrap("+PONG\r\n".getBytes()));
                    break;
                case "DOCS":
                    clientSocket.write(ByteBuffer.wrap("+\r\n".getBytes()));
                    break;
                case "ECHO":
                    clientSocket.write(ByteBuffer.wrap(("+" + commandValue + "\r\n").getBytes()));
                    break;
                case "GET":
                    if (EXPIRY_STORE.containsKey(commandKey)) {
                        if (LocalDateTime.now().isBefore(EXPIRY_STORE.get(commandKey))) {
                            clientSocket.write(ByteBuffer.wrap(("+" + VALUE_STORE.get(commandKey) + "\r\n").getBytes()));
                        } else {
                            clientSocket.write(ByteBuffer.wrap("$-1\r\n".getBytes()));
                        }
                    } else {
                        String value = VALUE_STORE.get(commandKey);
                        if (value != null) {
                            clientSocket.write(ByteBuffer.wrap(("+" + VALUE_STORE.get(commandKey) + "\r\n").getBytes()));

                        } else {
                            clientSocket.write(ByteBuffer.wrap("$-1\r\n".getBytes()));
                        }
                    }

                    break;
                case "SET":
                    if (Objects.equals(option, "PX")) {
                        EXPIRY_STORE.put(commandKey, LocalDateTime.now().plus(Long.parseLong(optionValue), ChronoUnit.MILLIS));
                    }

                    VALUE_STORE.put(commandKey, commandValue);
                    clientSocket.write(ByteBuffer.wrap("+OK\r\n".getBytes()));
                    break;
            }
        }
    }
}

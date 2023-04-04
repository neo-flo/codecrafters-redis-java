import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        final int port = 6379;

        Selector selector = Selector.open();

        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", port));
        serverSocket.configureBlocking(false);

        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("server listening");

        ByteBuffer buffer = ByteBuffer.allocate(256);
        printBufferStatus("init buffer", buffer);

        while (true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            System.out.println("selectedKeys = " + selectedKeys);
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    register(selector, serverSocket);
                }

                if (key.isReadable()) {
                    read(buffer, key);
                }

                iterator.remove();
            }
        }
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientSocket = serverSocket.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        System.out.println("client connected");
    }

    private static void read(ByteBuffer buffer, SelectionKey key) throws IOException, InterruptedException {
        SocketChannel clientSocket = (SocketChannel) key.channel();
        clientSocket.read(buffer);

        System.out.println("read buffer = " + new String(buffer.array()));
        printBufferStatus("read buffer", buffer);

        List<String> lines = Arrays.stream(new String(buffer.array()).trim().split("\r\n"))
                .collect(Collectors.toList());

        for (String next : lines) {
            if (next.startsWith("ping")) {
                String pong = "+PONG\r\n";
                clientSocket.write(ByteBuffer.wrap(pong.getBytes()));
            } else if (next.startsWith("DOCS")) {
                clientSocket.write(ByteBuffer.wrap("+\r\n".getBytes()));
            }
        }

        buffer.clear();
        System.out.println("client connected");
        clientSocket.close();
    }

    private static void printBufferStatus(String name, ByteBuffer buffer) {
        System.out.println(name + " : position[" + buffer.position() + "] Limit[" + buffer.limit() + "] Capacity[" + buffer.capacity() + "]");
    }
}

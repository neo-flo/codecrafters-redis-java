import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");
        final int port = 6379;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            clientSocket = serverSocket.accept();

            Scanner scanner = new Scanner(clientSocket.getInputStream());
            OutputStream outputStream = clientSocket.getOutputStream();
            StringBuilder input = new StringBuilder();

            while (scanner.hasNext()) {
                String next = scanner.nextLine();
                System.out.println("next = " + next);
                input.append(next);

                if (next.startsWith("ping")) {
                    String pong = "+PONG\r\n";
                    outputStream.write(pong.getBytes());
                } else if (next.startsWith("DOCS")) {
                    outputStream.write("+\r\n".getBytes());
                }
            }

            System.out.println("input = " + input);

//            if (input.toString().contains("DOCS")) {
//                out.println();
//            } else {
//                out.print("+PONG\r\n");
//                out.flush();
//            }
//            out.flush();
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

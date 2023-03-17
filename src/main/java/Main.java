import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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

//            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

//            StringBuilder result = new StringBuilder();
//            for (int data = in.read(); data != -1; data = in.read()) {
//                System.out.println("data = " + data);
//                result.append((char) data);
//            }

//            String input = result.toString();
//            System.out.println("input = " + input);

            out.print("+PONG\r\n");
            out.flush();
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

        //  Uncomment this block to pass the first stage
        //    ServerSocket serverSocket = null;
        //    Socket clientSocket = null;
        //    int port = 6379;
        //    try {
        //      serverSocket = new ServerSocket(port);
        //      serverSocket.setReuseAddress(true);
        //      // Wait for connection from client.
        //      clientSocket = serverSocket.accept();
        //    } catch (IOException e) {
        //      System.out.println("IOException: " + e.getMessage());
        //    } finally {
        //      try {
        //        if (clientSocket != null) {
        //          clientSocket.close();
        //        }
        //      } catch (IOException e) {
        //        System.out.println("IOException: " + e.getMessage());
        //      }
        //    }
    }
}

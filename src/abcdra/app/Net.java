package abcdra.app;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Net {
    public static void runServer() {
        try {
            ServerSocket server = new ServerSocket(31350);
            System.out.println("Running");
            Socket client = server.accept();
            System.out.println("Catch");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

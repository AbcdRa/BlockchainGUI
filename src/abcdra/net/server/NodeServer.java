package abcdra.net.server;

import abcdra.blockchain.Blockchain;
import abcdra.util.JLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class NodeServer implements Runnable{
    public int defaultPort = 3735;
    ServerSocket serverSocket;
    private boolean isEnable = true;
    private final Blockchain blockchain;
    private final JLogger logger;


    public NodeServer(Blockchain blockchain, JLogger logger) {
        this.logger = logger;
        serverSocket = initSocket();
        this.blockchain = blockchain;

    }

    private ServerSocket initSocket() {
        try {
            return new ServerSocket(defaultPort);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERR Не удалось инициализировать сокет");
        }
        return null;
    }

    @Override
    public void run() {
        while (isEnable) {
            try {
                logger.write("Жду клиентов");
                Socket client = serverSocket.accept();
                logger.write("Подключился" + client.getInetAddress());
                NodeClientThread nodeClientThread = new NodeClientThread(client, blockchain, logger);
                nodeClientThread.start();
            } catch (IOException e) {
                isEnable = false;
                logger.write("Не удалось принять клиента");
                e.printStackTrace();
            }
        }
    }
}

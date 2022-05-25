package abcdra.net.server;

import abcdra.blockchain.Blockchain;
import abcdra.net.JLogger;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class NodeServer implements Runnable{
    public int defaultPort = 3735;
    ServerSocket serverSocket;
    private static ArrayList<NodeClientThread> nodes;
    private Blockchain blockchain;
    private JLogger logger;


    public NodeServer(Blockchain blockchain, JLogger logger) {
        this.logger = logger;
        serverSocket = initSocket();
        this.blockchain = blockchain;
        nodes = new ArrayList<>();
    }

    private ServerSocket initSocket() {
        try {
            ServerSocket socket = new ServerSocket(defaultPort);

            return socket;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERR Не удалось инициализировать сокет");
        }
        return null;
    }

    @Override
    public void run() {
        while (true) {
            try {
                logger.write("Жду клиентов");
                Socket client = serverSocket.accept();
                logger.write("Подключился" + client.getInetAddress());
                NodeClientThread nodeClientThread = new NodeClientThread(client, blockchain, logger);
                nodes.add(nodeClientThread);
                nodeClientThread.start();
            } catch (IOException e) {

                logger.write("Не удалось принять клиента");
                e.printStackTrace();
            }
        }
    }
}

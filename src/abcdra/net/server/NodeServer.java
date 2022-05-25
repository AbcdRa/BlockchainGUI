package abcdra.net.server;

import abcdra.blockchain.Blockchain;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class NodeServer implements Runnable{
    public int defaultPort = 3735;
    ServerSocket serverSocket;
    private static ArrayList<NodeThread> nodes;
    private Blockchain blockchain;
    private JLabel infoLabel;


    public NodeServer(Blockchain blockchain, JLabel infoLabel) {
        this.infoLabel = infoLabel;
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
                infoLabel.setText("Жду клиентов");
                Socket client = serverSocket.accept();
                infoLabel.setText("Подключился" + client.getInetAddress());
                NodeThread nodeThread = new NodeThread(client, blockchain, infoLabel);
                nodes.add(nodeThread);
                nodeThread.start();
            } catch (IOException e) {

                infoLabel.setText("Не удалось принять клиента");
                e.printStackTrace();
            }
        }
    }
}

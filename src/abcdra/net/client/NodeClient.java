package abcdra.net.client;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.net.ComplexData;
import abcdra.net.JLogger;
import abcdra.net.NodeThread;
import abcdra.net.server.NodeServer;
import abcdra.transaction.Transaction;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Exchanger;

//TODO Добавить задержку между запросами на синхронизацию
//TODO Добавить запрос на добавление в мемпул
//TODO Добавить запрос на блок
//TODO Добавить информацию о том синхронизирован ли узел
//TODO Добавить возможность форка
public class NodeClient implements Runnable {
    private Blockchain blockchain;
    private ArrayList<NodeServerThread> nodes;
    private JLogger logger;
    private Exchanger<ComplexData> exchanger;


    public NodeClient(Blockchain blockchain, JLogger logger,
                      Exchanger<ComplexData> exchanger) {
        this.blockchain = blockchain;
        this.logger = logger;
        nodes = new ArrayList<>();
        this.exchanger = exchanger;

    }

    private Socket getSocket(String ipAndPort) {
        int lim = ipAndPort.indexOf(":");
        String ip = ipAndPort.substring(0, lim);
        int port = Integer.parseInt(ipAndPort.substring(lim+1));
        try {
            Socket newSocket = new Socket(ip, port);
            return newSocket;
        } catch (IOException e) {
        }
        return null;
    }

    public void exchangeAll() {
        try {
            ComplexData data = exchanger.exchange(null);
            for(NodeServerThread node: nodes) {
                node.exchanger.exchange(data);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        nodes = new ArrayList<>();
        for (String ip : blockchain.getNodesIp()) {
            Socket socket = getSocket(ip);

            if (socket != null) {
                NodeServerThread thread = new NodeServerThread(socket, blockchain, logger, new Exchanger<ComplexData>());
                if (!nodes.contains(thread)) {
                    nodes.add(thread);
                    thread.start();
                }
            }
        }
        if(nodes.size() == 0) return;
        while (true) exchangeAll();
    }
}

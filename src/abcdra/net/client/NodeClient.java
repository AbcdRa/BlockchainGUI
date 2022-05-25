package abcdra.net.client;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.net.JLogger;
import abcdra.net.NodeThread;
import abcdra.net.server.NodeServer;
import abcdra.transaction.Transaction;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
//TODO Добавить задержку между запросами на синхронизацию
//TODO Добавить запрос на добавление в мемпул
//TODO Добавить запрос на блок
//TODO Добавить информацию о том синхронизирован ли узел
//TODO Добавить возможность форка
public class NodeClient implements Runnable {
    private Blockchain blockchain;
    private ArrayList<NodeServerThread> nodes;
    private JLogger logger;
    private Block bufferBlock;
    private Transaction bufferTx;

    public void setBufferBlock(Block block) {
        if(block != null) {
            bufferBlock = block;
        }
    }

    public void setBufferTx(Transaction tx) {
        if(tx!=null){
            bufferTx = tx;
        }
    }

    public NodeClient(Blockchain blockchain, JLogger logger) {
        this.blockchain = blockchain;
        this.logger = logger;
        nodes = new ArrayList<>();
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

    public void sendBufferToAll() {
        for (NodeServerThread node: nodes) {
            node.setBufferTx(bufferTx);
            node.setBufferBlock(bufferBlock);
            bufferTx = null;
            bufferBlock = null;
        }
    }

    @Override
    public void run() {
        for(String ip : blockchain.getNodesIp()) {
            Socket socket = getSocket(ip);
            if(socket != null) {
                NodeServerThread thread = new NodeServerThread(socket, blockchain, logger);
                if(!nodes.contains(thread)) {
                    nodes.add(thread);
                    thread.start();
                }
            }
        }
        sendBufferToAll();

    }
}

package abcdra.net.client;

import abcdra.blockchain.Blockchain;
import abcdra.net.ComplexData;
import abcdra.util.JLogger;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Exchanger;

//TODO Возможность перезапуска если все клиенты не в сети
//TODO Добавить информацию о том синхронизирован ли узел

public class NodeClient implements Runnable {
    private final Blockchain blockchain;
    private ArrayList<NodeServerThread> nodes;
    private final JLogger logger;
    private final Exchanger<ComplexData> exchanger;
    public  boolean isEnable = true;


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
            return new Socket(ip, port);
        } catch (IOException ignored) {
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
                NodeServerThread thread = new NodeServerThread(socket, blockchain, logger, new Exchanger<>());
                if (!nodes.contains(thread)) {
                    nodes.add(thread);
                    thread.start();
                }
            }
        }
        if(nodes.size() == 0) return;
        while (isEnable) exchangeAll();
    }
}

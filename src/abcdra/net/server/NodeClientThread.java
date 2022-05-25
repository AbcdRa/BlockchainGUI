package abcdra.net.server;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.net.JLogger;
import abcdra.net.NodeThread;
import abcdra.transaction.Transaction;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class NodeClientThread extends NodeThread {

    private Blockchain blockchain;
    private JLogger logger;
    private int delay = 1000;

    public NodeClientThread(Socket socket, Blockchain blockchain, JLogger logger) {
        super(socket);
        this.logger = logger;
        this.blockchain = blockchain;


    }

    @Override
    public void run() {
        while (isOnline) {
            try {
                String command = inBR.readLine();
                String response = responseCommand(command);
                Thread.sleep(delay);
                if(response != null) {
                    send(response);
                }
            } catch (IOException e) {
                isOnline = false;
                logger.write(socket.getInetAddress()+" отключился");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String responseCommand(String command) {
        if(command.equals("GET HEIGHT")) {
            return String.valueOf(blockchain.maxHeight);
        }
        if(command.startsWith("GET BLOCK ")) {
            long i = Long.parseLong(command.substring(10));
            return blockchain.getRawBlock(i);
        }
        if(command.equals("GET MEMPOOL")) {
            String rawMempool = blockchain.getRawMempool();
            try { Thread.sleep(delay*2); } catch (Exception ignored) {};
            return rawMempool == null ? "EMPTY" : rawMempool;
        }
        if(command.startsWith("POST TX ")) {
            String rawTx = command.substring(8);
            logger.write("Получена транзакция " + rawTx);
            blockchain.addTransactionToMempool(Transaction.fromJSON(rawTx));
            return "OK";
        }
        if(command.startsWith("POST BLOCK ")) {
            String rawBlock = command.substring(10);
            logger.write("Получен блок " + rawBlock);
            blockchain.addBlock(Block.fromJSON(rawBlock));
            return "OK";
        }
        return null;
    }
}

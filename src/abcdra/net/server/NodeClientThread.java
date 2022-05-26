package abcdra.net.server;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.net.JLogger;
import abcdra.net.NodeThread;
import abcdra.transaction.Transaction;

import java.io.*;
import java.net.Socket;

public class NodeClientThread extends NodeThread {

    private final Blockchain blockchain;
    private final JLogger logger;

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
                //Thread.sleep(delay);
                if(response != null && !response.equals("NULL")) {
                    send(response);
                }
            } catch (IOException e) {
                isOnline = false;
                logger.write(socket.getInetAddress() + " отключился");
            }
        }
    }

    private String responseCommand(String command) {
        if(command==null) {
            isOnline = false;
            return "NULL";
        }
        if(command.equals("GET HEIGHT")) {
            return String.valueOf(blockchain.maxHeight);
        }
        if(command.startsWith("GET BLOCK ")) {
            long i = Long.parseLong(command.substring(10));
            return blockchain.getRawBlock(i);
        }
        if(command.equals("GET MEMPOOL")) {
            String rawMempool = blockchain.getRawMempool();
            try {
                int delay = 1000;
                Thread.sleep(delay ); } catch (Exception ignored) {}
            return rawMempool == null ? "EMPTY" : rawMempool;
        }
        if(command.startsWith("POST TX ")) {
            String rawTx = command.substring(8);
            logger.write("Получена транзакция " + rawTx.substring(8,20));
            String response = blockchain.addTransactionToMempool(Transaction.fromJSON(rawTx));
            logger.write(response);
            return "OK";
        }
        if(command.startsWith("POST BLOCK ")) {
            String rawBlock = command.substring(10);
            logger.write("Получен блок " + rawBlock.substring(10, 22));
            String response = blockchain.addBlock(Block.fromJSON(rawBlock));
            logger.write(response);
            return "OK";
        }
        if(command.startsWith("GET HASH ")) {
            long i = Long.parseLong(command.substring(9));
            String hash = blockchain.getBlockHash(i);
            return hash;
        }
        return null;
    }
}

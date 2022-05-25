package abcdra.net.client;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.net.JLogger;
import abcdra.net.NodeThread;
import abcdra.transaction.Transaction;
import org.codehaus.jackson.map.ObjectMapper;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

//TODO NodeServerThread и NodeThread похожи можно их наследовать от одного предка
public class NodeServerThread extends NodeThread {
    private Blockchain blockchain;
    private boolean isInitSynchronized = false;
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

    JLogger logger;

    public NodeServerThread(Socket socket, Blockchain blockchain, JLogger logger) {
        super(socket);
        this.logger = logger;
        this.blockchain = blockchain;
    }


    private void syncBlockchain(String heightResponse) {
        long serverHeight;
        try {
            serverHeight = Long.parseLong(heightResponse);
        } catch (NumberFormatException e) {
            return;
        }
        if(serverHeight <= blockchain.maxHeight) {
            return;
        }
        if(serverHeight > blockchain.maxHeight) {
            try {
                Block request = requestBlock(blockchain.maxHeight);
                String responseBlockchain = blockchain.addBlock(request);
                if(responseBlockchain.equals("Added")) syncBlockchain(heightResponse);
            }catch (IOException exception) {
                return;
            }

        }
    }

    private void syncMempool(String rawMempool) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String[] rawTxs = mapper.readValue(rawMempool, String[].class);
            for(String rawTx : rawTxs) {
                blockchain.addTransactionToMempool(Transaction.fromJSON(rawTx));
            }
        } catch (IOException e) {
            logger.write("Ошибка распознавания мемпула");
            return;
        }
    }

    private Block requestBlock(long i) throws IOException{
        send("GET BLOCK " + i);
        String response = inBR.readLine();
        Block requested = Block.fromJSON(response);
        return requested;
    }

    private String sendBlock(Block block) {
        try {
            send("POST BLOCK "+block.toJSON());
            String response = inBR.readLine();
            return response;
        } catch (IOException e) {
            isOnline = false;
            return "ERROR";
        }
    }

    private String sendTx(Transaction tx) {
        try {
            send("POST TX "+tx.toJSON());
            String response = inBR.readLine();
            return response;
        } catch (IOException e) {
            isOnline = false;
            return "ERROR";
        }
    }

    private void initSync() throws IOException {
        send("GET HEIGHT");
        String response = inBR.readLine();
        logger.write("Получен ответ: " + response);
        syncBlockchain(response);
        send("GET MEMPOOL");
        response = inBR.readLine();
        logger.write("Получен ответ: " + response);
        syncMempool(response);
        isInitSynchronized = true;
    }

    public void sendBuffers() {
        if(bufferBlock!=null) {
            String response = sendBlock(bufferBlock);
            if(response.equals("OK")) bufferBlock = null;
        }
        if(bufferTx!=null) {
            String response = sendTx(bufferTx);
            if(response.equals("OK")) bufferTx = null;
        }
    }

    @Override
    public void run() {
        while (isOnline) {
            try {
                if(!isInitSynchronized) initSync();
                sendBuffers();
            } catch (IOException e) {
                isOnline = false;
            }
        }
    }

}

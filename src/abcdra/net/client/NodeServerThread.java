package abcdra.net.client;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.blockchain.Configuration;
import abcdra.net.ComplexData;
import abcdra.util.JLogger;
import abcdra.net.NodeThread;
import abcdra.transaction.Transaction;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Exchanger;

public class NodeServerThread extends NodeThread {
    private final Blockchain blockchain;
    private boolean isInitSynchronized = false;

    public Exchanger<ComplexData> exchanger;




    JLogger logger;

    public NodeServerThread(Socket socket, Blockchain blockchain, JLogger logger,
                            Exchanger<ComplexData> exchanger) {
        super(socket);
        this.logger = logger;
        this.blockchain = blockchain;
        this.exchanger = exchanger;
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
        try {
            Block request = requestBlock(blockchain.maxHeight);
            String responseBlockchain = blockchain.addBlock(request);
            if(responseBlockchain.equals("Added")) syncBlockchain(heightResponse);
        }catch (IOException exception) {
            isOnline = false;
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
        }
    }

    private Block requestBlock(long i) throws IOException{
        send("GET BLOCK " + i);
        String response = inBR.readLine();
        return Block.fromJSON(response);
    }

    private void sendBlock(Block block) {
        try {
            send("POST BLOCK "+block.toJSON());
            logger.write("POST TX " + block);
            String response = inBR.readLine();
            if(response.equals("OK")) logger.write("Ошибка отправки блока");
        } catch (IOException e) {
            isOnline = false;
        }
    }

    private void sendTx(Transaction tx) {
        try {
            send("POST TX "+tx.toJSON());
            logger.write("POST TX " + tx.toJSON());
            String response = inBR.readLine();
            if(!response.equals("OK")) logger.write("Ошибка отправки транзакции :" + response);
        } catch (IOException e) {
            isOnline = false;
        }
    }

    private String getResponse(String command) throws IOException {
        send(command);
        return inBR.readLine();
    }

    private void initSync() throws IOException {
        String responseHeight = getResponse("GET HEIGHT");
        logger.write("Получен ответ: " + responseHeight);
        syncBlockchain(responseHeight);
        String response = getResponse("GET MEMPOOL");
        logger.write("Получен ответ: " + response);
        syncMempool(response);
        tryFork(responseHeight);
        isInitSynchronized = true;
    }

    private void tryFork(String heightRaw) throws IOException {
        long height = Long.parseLong(heightRaw);
        if(height > blockchain.maxHeight + Configuration.FORCE_FORK_LENGTH) {
            logger.write("Начинаю процесс форка");
            long forkPoint = blockchain.maxHeight - 1;
            for(; forkPoint >= -1; forkPoint--) {
                String serverHash = getResponse("GET HASH " + forkPoint);
                if(serverHash.equals(blockchain.getBlockHash(forkPoint))) break;
            }
            if(forkPoint <= -2) isOnline = false;
            logger.write("FORK POINT = " + forkPoint);
            String forkName = socket.getInetAddress().getHostName();
            blockchain.createFork(forkName, forkPoint);
            for(long i = forkPoint+1; i < blockchain.maxHeight + Configuration.FORCE_FORK_LENGTH; i++) {
                Block block = requestBlock(i);
                String response = blockchain.addToFork(forkName, block);
                if(!response.equals("Added")) {isOnline =false; blockchain.removeFork(forkName); return;}
            }
            String response = blockchain.mergeFork(forkName);
            logger.write(response);
            initSync();
        }
    }

    public void sendBuffers() {
        try {
            ComplexData data = exchanger.exchange(null);
            if(data.isBlock) sendBlock(data.block);
            else sendTx(data.tx);
        } catch (InterruptedException ignored) {

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

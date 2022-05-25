package abcdra.net.client;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

//TODO NodeServerThread и NodeThread похожи можно их наследовать от одного предка
public class NodeServerThread extends Thread{
    private Blockchain blockchain;
    private boolean isOnline;
    private Socket socket;
    private BufferedWriter outBW;
    private BufferedReader inBR;
    JLabel infoLabel;

    public NodeServerThread(Socket socket, Blockchain blockchain, JLabel infoLabel) {
        this.infoLabel = infoLabel;
        this.blockchain = blockchain;
        isOnline = false;
        this.socket = socket;
        try {
            outBW = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            inBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isOnline = true;
        } catch (IOException ignored) {

        }
    }

    public void send(String message) throws IOException {
        outBW.write(message+"\n");
        outBW.flush();
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

    private Block requestBlock(long i) throws IOException{
        send("GET BLOCK " + i);
        String response = inBR.readLine();
        Block requested = Block.fromJSON(response);
        return requested;
    }

    @Override
    public void run() {
        while (isOnline) {
            try {
                infoLabel.setText("Отправляю команду");
                send("GET HEIGHT");
                String response = inBR.readLine();
                infoLabel.setText("Получен ответ: " + response);
                syncBlockchain(response);

            } catch (IOException e) {
                isOnline = false;
            }
        }
    }

}

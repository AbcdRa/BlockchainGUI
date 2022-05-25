package abcdra.net.server;

import abcdra.blockchain.Blockchain;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class NodeThread extends Thread{

    private Socket socket;
    private BufferedWriter outBW;
    private BufferedReader inBR;
    public boolean isOnline;
    private Blockchain blockchain;
    private JLabel infoLabel;

    public NodeThread(Socket socket, Blockchain blockchain, JLabel infoLabel) {
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

    @Override
    public void run() {
        while (isOnline) {
            try {
                String command = inBR.readLine();
                String response = responseCommand(command);
                if(response != null) {
                    send(response);
                }
            } catch (IOException e) {
                isOnline = false;
                infoLabel.setText(socket.getInetAddress()+" отключился");
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
        return null;
    }
}

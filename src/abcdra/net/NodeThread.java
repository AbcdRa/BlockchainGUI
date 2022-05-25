package abcdra.net;

import java.io.*;
import java.net.Socket;

public class NodeThread extends Thread{
    protected boolean isOnline;
    protected final Socket socket;
    protected BufferedWriter outBW;
    protected BufferedReader inBR;

    public NodeThread(Socket socket) {
        this.socket = socket;
        try {
            outBW = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            inBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isOnline = true;
        } catch (IOException ignored) {
            isOnline = false;
            closeSocketForce();
        }
    }

    public void closeSocketForce() {
        try {
            socket.close();
        } catch (IOException e) {
            return;
        }
    }

    public void send(String message) {
        try {
            outBW.write(message + "\n");
            outBW.flush();
        } catch (IOException e) {
            isOnline = false;
        }
    }
}

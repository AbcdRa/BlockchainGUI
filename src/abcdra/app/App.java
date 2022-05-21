package abcdra.app;

import javax.swing.*;

public class App {
    private JPanel mainJPanel;
    private JTabbedPane tpMain;
    private JLabel lWalletInfo;

    public static void main(String[] args) {
        JFrame jFrame = new JFrame("Blockchain");
        jFrame.setContentPane(new App().mainJPanel);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setSize(500,500);
        jFrame.setVisible(true);
    }
}

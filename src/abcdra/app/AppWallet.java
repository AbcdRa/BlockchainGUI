package abcdra.app;

import abcdra.wallet.Wallet;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class AppWallet {
    protected Wallet wallet;
    private App app;

    public AppWallet(App app) {
        this.app =app;
        app.bCreateWallet.addActionListener(e -> createNewWallet());
        app.bRestoreWallet.addActionListener(e -> restoreWallet());
        app.bUpdateUTXO.addActionListener(e -> updateWalletInfo());
        app.bSaveWallet.addActionListener(e -> saveWallet());


    }

    public void saveWallet(){
        if(wallet == null) {
            JOptionPane.showMessageDialog(null, "Кошелек не открыт!");
            return;
        }
        String app_dir = System.getProperty("user.dir");
        JFileChooser fileChooser = new JFileChooser(app_dir);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Wallet", "wallet", "w");
        fileChooser.setFileFilter(filter);
        int response = fileChooser.showSaveDialog(null);
        if(JFileChooser.APPROVE_OPTION == response) {
            String pass = JOptionPane.showInputDialog("Введите пароль от кошелька");
            wallet.save(fileChooser.getSelectedFile(), pass);
        }
    }

    private void createNewWallet() {
        wallet = new Wallet();
        updateWalletInfo();
    }

    private void restoreWallet() {
        String app_dir = System.getProperty("user.dir");
        JFileChooser fileChooser = new JFileChooser(app_dir);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Wallet", "wallet", "w");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(null);
        if(result != JFileChooser.APPROVE_OPTION) return;
        File file = fileChooser.getSelectedFile();
        String pass = JOptionPane.showInputDialog("Введите пароль от кошелька");
        Wallet wallet = Wallet.restore(file, pass);
        if(wallet == null) {
            JOptionPane.showMessageDialog(null, "Не удалось восстановить кошелек");
            return;
        }
        this.wallet = wallet;
        updateWalletInfo();
    }

    protected void updateWalletInfo() {
        app.tfAddress.setText(wallet.address);
        long utxo = app.blockchain.getUTXO(wallet.address);
        app.tfUTXO.setText(String.valueOf(utxo));
    }
}

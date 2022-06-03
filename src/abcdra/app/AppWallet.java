package abcdra.app;

import abcdra.blockchain.CoinSUConvert;
import abcdra.wallet.Wallet;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class AppWallet {
    //TODO Красивый вывод счета
    protected Wallet wallet;
    private final App app;
    private boolean balancePShowMode = true;

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
            JPasswordField pf = new JPasswordField();
            int okCxl = JOptionPane.showConfirmDialog(null, pf, "Введите пароль от кошелька", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (okCxl == JOptionPane.OK_OPTION) {
                String password = new String(pf.getPassword());
                wallet.save(fileChooser.getSelectedFile(), password);
                JOptionPane.showMessageDialog(null, "Кошелек успешно сохранен!");
            }
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
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pf, "Введите пароль",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        Wallet wallet = null;
        if (okCxl == JOptionPane.OK_OPTION) {
            String password = new String(pf.getPassword());
            wallet = Wallet.restore(file, password);
        }
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
        if(balancePShowMode) app.tfUTXO.setText(CoinSUConvert.present(utxo));
        else app.tfUTXO.setText(String.valueOf(utxo));
    }

    protected void changeShowMode() {
        balancePShowMode = !balancePShowMode;
        updateWalletInfo();
    }
}

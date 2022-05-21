package abcdra.app;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.blockchain.MiningUtil;
import abcdra.transaction.Transaction;
import abcdra.wallet.Wallet;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.Vector;

public class App {
    private JPanel mainJPanel;
    private JTabbedPane tpMain;
    private JButton bCreateWallet;
    private JButton bRestoreWallet;
    private JButton bSaveWallet;
    private JLabel lAddress;
    private JTextField tfAddress;
    private JLabel lUTXO;
    private JTextField tfUTXO;
    private JButton bUpdateUTXO;
    private JPanel JPBlockExplorer;
    private JPanel JPWallet;
    private JTextField tfblockN;
    private JButton bFindBlock;
    private JLabel lHash;
    private JLabel lCurrentHeight;
    private JLabel lPvHash;
    private JLabel lDiff;
    private JLabel lDate;
    private JLabel lHeight;
    private JLabel lMerkleRoot;
    private JLabel lNonce;
    private JLabel lMinerAddress;
    private JList listTxs;
    private JList listInputs;
    private JList listOuts;
    private Wallet wallet;
    private Blockchain blockchain;
    private Transaction[] currentObserveTxs;

    public App() {
        blockchain = new Blockchain("src/blockchain_paths.json");
        lCurrentHeight.setText(String.valueOf(blockchain.maxHeight));
        bCreateWallet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewWallet();
            }
        });
        bRestoreWallet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restoreWallet();
            }
        });
        bUpdateUTXO.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateWalletInfo();
            }
        });
        bSaveWallet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveWallet();
            }
        });
        bFindBlock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showBlock();
            }
        });
        listTxs.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                System.out.println(e.getComponent());
                super.componentResized(e);
            }
        });
        listTxs.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                NamedTransaction selected = (NamedTransaction) listTxs.getSelectedValue();
                Transaction currentTx = selected.tx;

            }
        });
    }

    public void showBlock() {
        long i = 0;
        try {
            i = Long.parseLong(tfblockN.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, tfblockN.getText() + " - не число");
            return;
        }
        Block block = blockchain.getBlock(i);
        lHash.setText(MiningUtil.bytesToHex(block.hash));
        lPvHash.setText(MiningUtil.bytesToHex(block.pvHash));
        lPvHash.setForeground(Color.blue);
        lDiff.setText(String.valueOf(block.difficult));
        lMerkleRoot.setText(MiningUtil.bytesToHex(block.merkleRoot));
        lDate.setText(block.date.toString());
        lMinerAddress.setText(block.takeMinerAddress());
        lHeight.setText(String.valueOf(block.height));
        lNonce.setText(String.valueOf(block.nonce));
        NamedTransaction[] namedTransactions = new NamedTransaction[block.transactions.length];
        for(int j=0; j < block.transactions.length; j++) {
            namedTransactions[j] = new NamedTransaction(String.valueOf(j), block.transactions[j]);
            //listTxs.add(txNames[j], block.transactions[j])
        }
        listTxs.setListData(namedTransactions);

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
        int response = fileChooser.showSaveDialog(null);
        if(JFileChooser.APPROVE_OPTION == response) {
            String pass = JOptionPane.showInputDialog("Введите пароль от кошелька");
            wallet.save(fileChooser.getSelectedFile(), pass);
        }
    }

    public void createNewWallet() {
        wallet = new Wallet();
        updateWalletInfo();
    }

    public void restoreWallet() {
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

    public void updateWalletInfo() {
        tfAddress.setText(wallet.address);
        long utxo = blockchain.getUTXO(wallet.address);
        tfUTXO.setText(String.valueOf(utxo));
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame("Blockchain");

        JFrame.setDefaultLookAndFeelDecorated(true);
        jFrame.setContentPane(new App().mainJPanel);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setSize(600,500);
        jFrame.setVisible(true);
    }
}

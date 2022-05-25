package abcdra.app;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.blockchain.TransactionInfo;
import abcdra.net.client.NodeClient;
import abcdra.net.server.NodeServer;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;
import abcdra.transaction.TxOutput;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static abcdra.app.AppUtil.getArrayFromJList;

public class App {
    //TODO Допилить NET модуль
    //TODO Обновлять высоту блокчейна при синхронизации блокчейна
    //TODO Добавить Вкладку на изменение конфигов
    //TODO Исправить баг двойного добавления UTXO
    //TODO Добавить возможность по двойному клику на вход найти начальный выход
    //TODO Добавить возможность по двойному клику на выход найти потраченный вход

    private JPanel mainJPanel;
    protected JButton bCreateWallet;
    protected JButton bRestoreWallet;
    protected JButton bSaveWallet;
    protected JTextField tfAddress;
    protected JTextField tfUTXO;
    protected JButton bUpdateUTXO;
    JTextField tfBlockN;
    JButton bFindBlock;
    JLabel lHash;
    JLabel lCurrentHeight;
    JLabel lPvHash;
    JLabel lDiff;
    JLabel lDate;
    JLabel lHeight;
    JLabel lMerkleRoot;
    JLabel lNonce;
    JLabel lMinerAddress;
    JList<NamedTransaction> listTxs;
    JList<TxInput> listInputs;
    JList<TxOutput> listOuts;
    JButton bFindUTXO;
    JList<TransactionInfo> listFindUTXO;
    JList<TransactionInfo> listTxInputs;
    JList<TxOutput> listTxOutputs;
    JLabel lFee;
    JButton bAddOutput;
    JButton bDeleteOutput;
    JButton bAddInput;
    JButton bDeleteInput;
    JButton bCreateTx;
    JLabel lInputSum;
    JLabel lOutputSum;
    private JList<NamedTransaction> listMempool;
    private JList<NamedTransaction> listBlockTx;
    private JButton bAddTx;
    private JButton bMineBlock;
    private JButton bLoadMempool;
    private JButton bRemoveTx;
    private JLabel lBlockReward;
    JButton bCreateSimpleTx;
    private JLabel lResponse;
    private JButton bRunServer;
    private JButton bSyncronize;
    private JLabel lServerInfo;
    private JLabel lClientInfo;
    private JButton bAddAllMempool;
    final Blockchain blockchain;
    protected final AppWallet appWallet;
    protected final AppExplorer appExplorer;
    protected final AppTxCreator appTxCreator;

    public App() {
        blockchain = AppConfig.safeBlockchainInit();
        if(blockchain.maxHeight == 0) {
            Block genesis = new Block();
            genesis.mineBlock();
            blockchain.addBlock(genesis);
        }

        appWallet =  new AppWallet(this);
        appExplorer = new AppExplorer(this);
        appTxCreator = new AppTxCreator(this);
        updateBlockReward();
        bLoadMempool.addActionListener(e -> loadMempool());
        bAddTx.addActionListener(e -> {
            fromMempoolToBlock();
            updateBlockReward();
        });
        bRemoveTx.addActionListener(e -> {
            fromBlockToMempool();
            updateBlockReward();
        });
        bMineBlock.addActionListener(e -> {
            if(appWallet.wallet == null) {
                JOptionPane.showMessageDialog(null,"НЕ СОЗДАН КОШЕЛЕК");
                return;
            }
            ArrayList<NamedTransaction> txs = getArrayFromJList(listBlockTx);
            Transaction[] transactions = new Transaction[txs.size()];
            for(int i =0; i < txs.size(); i++) {
                transactions[i] = txs.get(i).tx;
            }
            Block newBlock = new Block(blockchain, appWallet.wallet.address, transactions);
            newBlock.mineBlock();
            String response = blockchain.addBlock(newBlock);
            lResponse.setText(response);
            if(response.equals("Added")) {
                JOptionPane.showMessageDialog(null, "Блок добавлен в блокчейн");
                appWallet.updateWalletInfo();
            }
            lCurrentHeight.setText(String.valueOf(blockchain.maxHeight));
        });


        bRunServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new NodeServer(blockchain, lServerInfo)).start();
            }
        });
        bSyncronize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new NodeClient(blockchain, lClientInfo)).start();
            }
        });
        bAddAllMempool.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<NamedTransaction> allMempoolTx = AppUtil.getArrayFromJList(listMempool);
                for(NamedTransaction tx: allMempoolTx) {
                    AppUtil.removeToJList(listMempool, tx);
                    AppUtil.addToJList(listBlockTx, tx);
                }
            }
        });
    }



    private void updateBlockReward() {
        long reward = blockchain.getNextCoinBase();
        ArrayList<NamedTransaction> txs = getArrayFromJList(listBlockTx);
        for(NamedTransaction tx: txs) {
            reward += tx.tx.calculateFee();
        }
        lBlockReward.setText(String.valueOf(reward));
    }

    private void fromBlockToMempool() {
        if(listBlockTx.getSelectedIndex() == -1) return;
        NamedTransaction selected = listBlockTx.getSelectedValue();
        AppUtil.removeToJList(listBlockTx, selected);
        AppUtil.addToJList(listMempool, selected);
    }

    private void fromMempoolToBlock() {
        if(listMempool.getSelectedIndex() == -1) return;
        NamedTransaction selected = listMempool.getSelectedValue();
        AppUtil.removeToJList(listMempool, selected);
        AppUtil.addToJList(listBlockTx, selected);
    }

    private void loadMempool() {
        Transaction[] mempoolTxs = blockchain.loadMempool();
        NamedTransaction[] wrappedTxs = new NamedTransaction[mempoolTxs.length];
        for(int i = 0; i < wrappedTxs.length; i++) {
            wrappedTxs[i] = new NamedTransaction(mempoolTxs[i]);
        }
        listMempool.setListData(wrappedTxs);
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame("Blockchain");

        JFrame.setDefaultLookAndFeelDecorated(true);
        jFrame.setContentPane(new App().mainJPanel);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setSize(1200,500);
        jFrame.setVisible(true);
    }
}

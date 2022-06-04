package abcdra.app;

import abcdra.blockchain.Block;
import abcdra.net.ComplexData;
import abcdra.transaction.Transaction;
import abcdra.util.AppUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static abcdra.util.AppUtil.getArrayFromJList;

public class AppMiner {
    App app;
    AppAutoMiner autoMiner;
    public AppMiner(App app) {
        this.app =app;
        this.autoMiner = new AppAutoMiner(this);
        app.bLoadMempool.addActionListener(e -> loadMempool());
        app.bAddTx.addActionListener(e -> {
            fromMempoolToBlock();
            updateBlockReward();
        });
        app.bRemoveTx.addActionListener(e -> {
            fromBlockToMempool();
            updateBlockReward();
        });
        app.bMineBlock.addActionListener(e -> mineBlock(true));
        app.bAddAllMempool.addActionListener(e -> addAllFromMempool());
        app.bAutoMining.addActionListener(e -> {
            autoMiner.isWork = !autoMiner.isWork;
            autoMiner.start();
        });
    }

    protected void addAllFromMempool() {
        ArrayList<NamedTransaction> allMempoolTx = AppUtil.getArrayFromJList(app.listMempool);
        for(NamedTransaction tx: allMempoolTx) {
            AppUtil.removeToJList(app.listMempool, tx);
            AppUtil.addToJList(app.listBlockTx, tx);
        }
    }


    protected void mineBlock(boolean showMessage) {
        if(app.appWallet.wallet == null) {
            JOptionPane.showMessageDialog(null,"НЕ СОЗДАН КОШЕЛЕК");
            return;
        }
        ArrayList<NamedTransaction> txs = getArrayFromJList(app.listBlockTx);
        Transaction[] transactions = new Transaction[txs.size()];
        for(int i =0; i < txs.size(); i++) {
            transactions[i] = txs.get(i).tx;
        }
        Block newBlock = new Block(app.blockchain, app.appWallet.wallet.address, transactions);
        newBlock.mineBlock();
        String response = app.blockchain.addBlock(newBlock);
        app.lResponse.setText(response);
        if(response.equals("Added")) {
            if(showMessage) JOptionPane.showMessageDialog(null, "Блок добавлен в блокчейн");
            app.appWallet.updateWalletInfo();
            try {app.exchanger.exchange(new ComplexData(newBlock), 0, TimeUnit.SECONDS); } catch (InterruptedException |
                                                                                        TimeoutException ignored) {}
        }
        app.lCurrentHeight.setText(String.valueOf(app.blockchain.maxHeight));
    }

    void updateBlockReward() {
        long reward = app.blockchain.getNextCoinBase();
        ArrayList<NamedTransaction> txs = getArrayFromJList(app.listBlockTx);
        for(NamedTransaction tx: txs) {
            reward += tx.tx.calculateFee();
        }
        app.lBlockReward.setText(String.valueOf(reward));
    }

    private void fromBlockToMempool() {
        if(app.listBlockTx.getSelectedIndex() == -1) return;
        NamedTransaction selected = app.listBlockTx.getSelectedValue();
        AppUtil.removeToJList(app.listBlockTx, selected);
        AppUtil.addToJList(app.listMempool, selected);
    }

    private void fromMempoolToBlock() {
        if(app.listMempool.getSelectedIndex() == -1) return;
        NamedTransaction selected = app.listMempool.getSelectedValue();
        AppUtil.removeToJList(app.listMempool, selected);
        AppUtil.addToJList(app.listBlockTx, selected);
    }

    protected void loadMempool() {
        Transaction[] mempoolTxs = app.blockchain.loadMempool();
        NamedTransaction[] wrappedTxs = new NamedTransaction[mempoolTxs.length];
        for(int i = 0; i < wrappedTxs.length; i++) {
            wrappedTxs[i] = new NamedTransaction(mempoolTxs[i]);
        }
        app.listMempool.setListData(wrappedTxs);
    }
}

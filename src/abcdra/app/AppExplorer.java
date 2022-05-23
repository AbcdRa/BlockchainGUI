package abcdra.app;

import abcdra.blockchain.Block;
import abcdra.blockchain.MiningUtil;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AppExplorer {
    App app;
    public AppExplorer(App app) {
        this.app = app;
        app.lCurrentHeight.setText(String.valueOf(app.blockchain.maxHeight));
        app.bFindBlock.addActionListener(e -> showBlock());
        app.lPvHash.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                long i = Long.parseLong(app.tfBlockN.getText()) - 1;
                if(i < 0) return;
                app.tfBlockN.setText(String.valueOf(i));
                showBlock(i);
                super.mouseClicked(e);
            }
        });
        app.lHash.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                long i;
                try {
                    i = Long.parseLong(app.tfBlockN.getText()) + 1;
                } catch (Exception exception) {
                    i = 0;
                }

                if(i >= app.blockchain.maxHeight) return;
                app.tfBlockN.setText(String.valueOf(i));
                showBlock(i);
                super.mouseClicked(e);
            }
        });
        app.listTxs.addListSelectionListener(e -> {
            if(app.listTxs.getSelectedIndex() == -1) return;
            NamedTransaction selected = app.listTxs.getSelectedValue();
            Transaction currentTx = selected.tx;
            if(currentTx.isCoinBase()) {
                app.listInputs.setListData(new TxInput[]{new TxInput("Новые монеты",0,0)});
            } else {
                app.listInputs.setListData(currentTx.inputs);
            }
            app.listOuts.setListData(currentTx.outputs);
        });
    }

    private void showBlock(long i) {
        Block block = app.blockchain.getBlock(i);
        app.lHash.setText(MiningUtil.bytesToHex(block.hash));
        app.lPvHash.setText(MiningUtil.bytesToHex(block.pvHash));
        app.lPvHash.setForeground(Color.blue);
        app.lDiff.setText(String.valueOf(block.difficult));
        app.lMerkleRoot.setText(MiningUtil.bytesToHex(block.merkleRoot));
        app.lDate.setText(block.date.toString());
        app.lMinerAddress.setText(block.takeMinerAddress());
        app.lHeight.setText(String.valueOf(block.height));
        app.lNonce.setText(String.valueOf(block.nonce));
        NamedTransaction[] namedTransactions = new NamedTransaction[block.transactions.length];
        for(int j=0; j < block.transactions.length; j++) {
            namedTransactions[j] = new NamedTransaction(block.transactions[j]);
            //listTxs.add(txNames[j], block.transactions[j])
        }
        app.listTxs.setListData(namedTransactions);
    }

    private void showBlock() {
        long i;
        try {
            i = Long.parseLong(app.tfBlockN.getText());
            showBlock(i);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, app.tfBlockN.getText() + " - не число");
            return;
        }
    }
}

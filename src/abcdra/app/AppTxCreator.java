package abcdra.app;

import abcdra.blockchain.TransactionInfo;
import abcdra.net.ComplexData;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;
import abcdra.transaction.TxOutput;
import com.starkbank.ellipticcurve.PublicKey;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static abcdra.app.AppUtil.*;

public class AppTxCreator {
    App app;

    public AppTxCreator(App app) {
        this.app = app;
        app.bFindUTXO.addActionListener(e -> findUTXO());
        app.bAddInput.addActionListener(e -> moveUTXOtoInput());
        app.bDeleteInput.addActionListener(e -> moveInputToUTXO());
        app.bAddOutput.addActionListener(e -> addTxOutput());
        app.bDeleteOutput.addActionListener(e -> removeTxOutput());
        app.bCreateTx.addActionListener(e -> createTx());
        app.bCreateSimpleTx.addActionListener(e -> createSimpleTx());
    }

    private void createSimpleTx() {
        String address = JOptionPane.showInputDialog("Адрес: ");
        long amount = Long.parseLong(JOptionPane.showInputDialog("Сумма: "));
        long fee = Long.parseLong(JOptionPane.showInputDialog("Комиссия: "));
        long inputSum = Long.parseLong(app.lInputSum.getText());
        addToJList(app.listTxOutputs, new TxOutput(address, amount));
        addToJList(app.listTxOutputs, new TxOutput(app.appWallet.wallet.address, inputSum-amount-fee));
        updateTxInfo();
    }

    private void moveInputToUTXO() {
        if(app.listTxInputs.getSelectedIndex() == -1 ) return;
        TransactionInfo selected = app.listTxInputs.getSelectedValue();
        removeToJList(app.listTxInputs, selected);
        addToJList(app.listFindUTXO, selected);
        updateTxInfo();
    }

    private void removeTxOutput() {
        if(app.listTxOutputs.getSelectedIndex() == -1 ) return;
        TxOutput selected = app.listTxOutputs.getSelectedValue();
        removeToJList(app.listTxOutputs, selected);
        updateTxInfo();
    }

    private void addTxOutput() {
        try {
            String address = JOptionPane.showInputDialog("Адрес получателя: ");
            long amount = Long.parseLong(JOptionPane.showInputDialog("Кол-во: "));
            addToJList(app.listTxOutputs, new TxOutput(address, amount));
            updateTxInfo();
        } catch (Exception ignored) {

        }

    }

    private void findUTXO() {
        if(app.appWallet.wallet==null) {
            JOptionPane.showMessageDialog(null, "Кошелек не создан!");
            return;
        }
        java.util.List<TransactionInfo> utxo = app.blockchain.findUTXO(app.appWallet.wallet.address);
        if(utxo.size() == 0) {
            JOptionPane.showMessageDialog(null, "UTXO не найдены!");
            return;
        }
        TransactionInfo[] raw_utxo = new TransactionInfo[utxo.size()];
        utxo.toArray(raw_utxo);
        app.listFindUTXO.setListData(raw_utxo);
    }

    private void moveUTXOtoInput() {
        if(app.listFindUTXO.getSelectedIndex() == -1) return;
        TransactionInfo selected = app.listFindUTXO.getSelectedValue();
        removeToJList(app.listFindUTXO, selected);
        addToJList(app.listTxInputs, selected);
        updateTxInfo();
    }

    //TODO Добавить подсказки сколько можно ввести
    private void updateTxInfo() {
        ListModel<TransactionInfo> inputModel = app.listTxInputs.getModel();
        long sumInput = 0;
        for(int i =0; i < inputModel.getSize(); i++) {
            sumInput += inputModel.getElementAt(i).getOutput().amount;
        }
        long sumOutput = 0;
        ListModel<TxOutput> outputModel = app.listTxOutputs.getModel();
        for(int i =0; i < outputModel.getSize(); i++) {
            sumOutput += outputModel.getElementAt(i).amount;
        }
        app.lInputSum.setText(String.valueOf(sumInput));
        app.lOutputSum.setText(String.valueOf(sumOutput));
        long fee = sumInput - sumOutput;
        app.lFee.setText(String.valueOf(fee));
    }

    private void createTx() {
        PublicKey pk = app.appWallet.wallet.getPk();
        Transaction tx = new Transaction();
        tx.pk = pk;
        ArrayList<TransactionInfo> raw_inputs = getArrayFromJList(app.listTxInputs);
        TxInput[] inputs = new TxInput[raw_inputs.size()];
        for(int i=0; i < raw_inputs.size(); i++) {
            inputs[i] = new TxInput(raw_inputs.get(i).getTxHash(),
                    raw_inputs.get(i).outNum, raw_inputs.get(i).getOutput().amount);
        }
        ArrayList<TxOutput> raw_outputs = getArrayFromJList(app.listTxOutputs);
        TxOutput[] outputs = new TxOutput[raw_outputs.size()];
        raw_outputs.toArray(outputs);
        tx.inputs = inputs;
        tx.outputs = outputs;
        tx.date = new Date();
        tx.pvBlockHash = app.blockchain.getBlock(app.blockchain.getCurrentHeight()-1).hash;
        tx.sign(app.appWallet.wallet.getSk());
        String response = app.blockchain.addTransactionToMempool(tx);
        if(response.equals("Added")) try {app.exchanger.exchange(new ComplexData(tx), 0, TimeUnit.SECONDS);} catch (
                InterruptedException | TimeoutException ignored) {}
        JOptionPane.showMessageDialog(null, response);
    }

}

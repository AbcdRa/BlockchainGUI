package abcdra.app;

import abcdra.blockchain.Block;
import abcdra.blockchain.Blockchain;
import abcdra.blockchain.MiningUtil;
import abcdra.blockchain.TransactionOutInfo;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;
import abcdra.transaction.TxOutput;
import abcdra.wallet.Wallet;
import com.starkbank.ellipticcurve.PublicKey;

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
import java.util.ArrayList;
import java.util.Date;

public class App {
    private JPanel mainJPanel;
    private JButton bCreateWallet;
    private JButton bRestoreWallet;
    private JButton bSaveWallet;
    private JTextField tfAddress;
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
    private JButton bFindUTXO;
    private JList listFindUTXO;
    private JPanel jpTransaction;
    private JList listTxInputs;
    private JList listTxOulputs;
    private JLabel lFee;
    private JButton bAddOutput;
    private JButton bDeleteOutput;
    private JButton bAddInput;
    private JButton bDeleteInput;
    private JButton bCreateTx;
    private JLabel lInputSum;
    private JLabel lOutputSum;
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
                if(currentTx.isCoinBase()) {
                    listInputs.setListData(new String[]{"Созданы новые монеты"});
                } else {
                    listInputs.setListData(currentTx.inputs);
                }
                listOuts.setListData(currentTx.outputs);
            }
        });
        bFindUTXO.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findUTXO();
            }
        });
        bAddInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUTXOToInput();
            }
        });
        bDeleteInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeUTXOToInput();
            }
        });
        bAddOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTxOutput();
            }
        });
        bDeleteOutput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeTxOutput();
            }
        });
        bCreateTx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }

    private void CreateTx() {
        PublicKey pk = wallet.getPk();
        Transaction tx = new Transaction();
        tx.pk = pk;
        TransactionOutInfo[] raw_inputs = (TransactionOutInfo[]) getArrayFromJList(listTxInputs);
        TxInput[] inputs = new TxInput[raw_inputs.length];
        for(int i=0; i < raw_inputs.length; i++) {
            inputs[i] = new TxInput(raw_inputs[i].getTxHash(), raw_inputs[i].outNum, raw_inputs[i].getOutput().amount);
        }
        TxOutput[] outputs = (TxOutput[]) getArrayFromJList(listTxOulputs);
        tx.inputs = inputs;
        tx.outputs = outputs;
        tx.date = new Date();
        tx.pvBlockHash = blockchain.getBlock(blockchain.getCurrentHeight()-1).hash;
        tx.sign(wallet.getSk());
        blockchain.addTransactionToMempool(tx);

    }

    private static <T> Object[] getArrayFromJList(JList<T> jlist) {
        ListModel<T> listModel = jlist.getModel();
        ArrayList<T> arrayList = new ArrayList<>(listModel.getSize());
        for(int i =0; i < listModel.getSize(); i++) {
            arrayList.set(i, listModel.getElementAt(i));
        }
        return arrayList.toArray();
    }

    private void removeTxOutput() {
        if(listTxOulputs.getSelectedIndex() == -1 ) return;
        TxOutput selected = (TxOutput) listTxOulputs.getSelectedValue();
        removeToJList(listTxOulputs, selected);
        updateTxInfo();
    }

    private void addTxOutput() {
        String address = JOptionPane.showInputDialog("Адрес получателя: ");
        long amount = Long.parseLong(JOptionPane.showInputDialog("Кол-во: "));
        addToJList(listTxOulputs, new TxOutput(address, amount));
        updateTxInfo();
    }

    private static <T> void addToJList(JList<T> jlist, T value ) {
        ListModel<T> model = jlist.getModel();
        java.util.List<T> new_model = new ArrayList<>();
        for(int i=0; i < model.getSize(); i++) {
            new_model.add(model.getElementAt(i));
        }
        new_model.add(value);
        jlist.setListData((T[]) new_model.toArray());
    }


    private static <T> void removeToJList(JList<T> jlist, T value ) {
        ListModel<T> model = jlist.getModel();
        java.util.List<T> new_model = new ArrayList<>();
        for(int i=0; i < model.getSize(); i++) {
            T element = model.getElementAt(i);
            if(element != value) new_model.add(element);
        }
        jlist.setListData((T[]) new_model.toArray());
    }

    public void addUTXOToInput() {
        if(listFindUTXO.getSelectedIndex() == -1) return;
        ListModel<TransactionOutInfo> model = listFindUTXO.getModel();
        java.util.List<TransactionOutInfo> new_model = new ArrayList<>();
        for(int i =0; i < model.getSize(); i++) {
            if(i != listFindUTXO.getSelectedIndex()) {
                new_model.add(model.getElementAt(i));
            }
        }
        ListModel<TransactionOutInfo> input_model = listTxInputs.getModel();
        java.util.List<TransactionOutInfo> input_new_model = new ArrayList<>();
        for(int i=0; i < input_model.getSize(); i++) {
            input_new_model.add(input_model.getElementAt(i));
        }
        input_new_model.add(model.getElementAt(listFindUTXO.getSelectedIndex()));
        listFindUTXO.setListData(new_model.toArray());

        listTxInputs.setListData(input_new_model.toArray());
        updateTxInfo();
    }

    public void updateTxInfo() {
        ListModel<TransactionOutInfo> inputModel = listTxInputs.getModel();
        long sumInput = 0;
        for(int i =0; i < inputModel.getSize(); i++) {
            sumInput += inputModel.getElementAt(i).getOutput().amount;
        }
        long sumOutput = 0;
        ListModel<TxOutput> outputModel = listTxOulputs.getModel();
        for(int i =0; i < outputModel.getSize(); i++) {
            sumOutput += outputModel.getElementAt(i).amount;
        }
        lInputSum.setText(String.valueOf(sumInput));
        lOutputSum.setText(String.valueOf(sumOutput));
        long fee = sumInput - sumOutput;
        lFee.setText(String.valueOf(fee));
    }

    public void removeUTXOToInput() {
        if(listTxInputs.getSelectedIndex() == -1 ) return;
        ListModel<TransactionOutInfo> input_model = listTxInputs.getModel();
        java.util.List<TransactionOutInfo> new_input_model = new ArrayList<>();
        for(int i =0; i < input_model.getSize(); i++) {
            if(i != listTxInputs.getSelectedIndex()) {
                new_input_model.add(input_model.getElementAt(i));
            }
        }
        ListModel<TransactionOutInfo> model = listFindUTXO.getModel();
        java.util.List<TransactionOutInfo> new_model = new ArrayList<>();
        for(int i=0; i < model.getSize(); i++) {
            new_model.add(model.getElementAt(i));
        }
        new_model.add(input_model.getElementAt(listTxInputs.getSelectedIndex()));
        listFindUTXO.setListData(new_model.toArray());

        listTxInputs.setListData(new_input_model.toArray());
        updateTxInfo();
    }

    public void findUTXO() {
        if(wallet==null) {
            JOptionPane.showMessageDialog(null, "Кошелек не создан!");
            return;
        }
        java.util.List<TransactionOutInfo> utxo = blockchain.findUTXO(wallet.address);
        if(utxo.size() == 0) {
            JOptionPane.showMessageDialog(null, "UTXO не найдены!");
            return;
        }
        listFindUTXO.setListData(utxo.toArray());
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
        jFrame.setSize(900,500);
        jFrame.setVisible(true);
    }
}

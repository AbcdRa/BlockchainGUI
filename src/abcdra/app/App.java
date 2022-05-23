package abcdra.app;

import abcdra.blockchain.*;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;
import abcdra.transaction.TxOutput;
import abcdra.wallet.Wallet;
import com.starkbank.ellipticcurve.PublicKey;

import javax.swing.*;
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
    //TODO Добавить NET модуль
    //TODO Добавить Вкладку на изменение конфигов
    //TODO При неудачном поиске конфига предложить найти самому
    //TODO Вынести из этого класса побольше методов иначе уже перегружен
    //TODO Исправить баг двойного добавления UTXO
    //TODO Сделать удобную навигацию по блокам хотя бы вперед назад
    //TODO Добавить возможность по двойному клику на вход найти начальный выход
    //TODO Добавить возможность по двойному клику на выход найти потраченный вход
    private JPanel mainJPanel;
    private JButton bCreateWallet;
    private JButton bRestoreWallet;
    private JButton bSaveWallet;
    private JTextField tfAddress;
    private JTextField tfUTXO;
    private JButton bUpdateUTXO;
    private JTextField tfBlockN;
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
    private JList<NamedTransaction> listTxs;
    private JList<TxInput> listInputs;
    private JList<TxOutput> listOuts;
    private JButton bFindUTXO;
    private JList<TransactionInfo> listFindUTXO;
    private JList<TransactionInfo> listTxInputs;
    private JList<TxOutput> listTxOutputs;
    private JLabel lFee;
    private JButton bAddOutput;
    private JButton bDeleteOutput;
    private JButton bAddInput;
    private JButton bDeleteInput;
    private JButton bCreateTx;
    private JLabel lInputSum;
    private JLabel lOutputSum;
    private JList<NamedTransaction> listMempool;
    private JList<NamedTransaction> listBlockTx;
    private JButton bAddTx;
    private JButton bMineBlock;
    private JButton bLoadMempool;
    private JButton bRemoveTx;
    private JLabel lBlockReward;
    private JButton bCreateSimpleTx;
    private JLabel lResponse;
    private Wallet wallet;
    private final Blockchain blockchain;


    public App() {
        blockchain = new Blockchain("src/blockchain_paths.json");
        if(blockchain.maxHeight == 0) {
            Block genesis = new Block();
            genesis.mineBlock();
            blockchain.addBlock(genesis);
        }
        lCurrentHeight.setText(String.valueOf(blockchain.maxHeight));
        bCreateWallet.addActionListener(e -> createNewWallet());
        bRestoreWallet.addActionListener(e -> restoreWallet());
        bUpdateUTXO.addActionListener(e -> updateWalletInfo());
        bSaveWallet.addActionListener(e -> saveWallet());
        bFindBlock.addActionListener(e -> showBlock());
        listTxs.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                System.out.println(e.getComponent());
                super.componentResized(e);
            }
        });
        listTxs.addListSelectionListener(e -> {
            if(listTxs.getSelectedIndex() == -1) return;
            NamedTransaction selected = listTxs.getSelectedValue();
            Transaction currentTx = selected.tx;
            if(currentTx.isCoinBase()) {
                listInputs.setListData(new TxInput[]{new TxInput("Новые монеты",0,0)});
            } else {
                listInputs.setListData(currentTx.inputs);
            }
            listOuts.setListData(currentTx.outputs);
        });
        bFindUTXO.addActionListener(e -> findUTXO());
        bAddInput.addActionListener(e -> moveUTXOtoInput());
        bDeleteInput.addActionListener(e -> moveInputToUTXO());
        bAddOutput.addActionListener(e -> addTxOutput());
        bDeleteOutput.addActionListener(e -> removeTxOutput());
        bCreateTx.addActionListener(e -> createTx());
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
            if(wallet == null) {
                JOptionPane.showMessageDialog(null,"НЕ СОЗДАН КОШЕЛЕК");
                return;
            }
            ArrayList<NamedTransaction> txs = getArrayFromJList(listBlockTx);
            Transaction[] transactions = new Transaction[txs.size()];
            for(int i =0; i < txs.size(); i++) {
                transactions[i] = txs.get(i).tx;
            }
            Block newBlock = new Block(blockchain, wallet.address, transactions);
            newBlock.mineBlock();
            String response = blockchain.addBlock(newBlock);
            lResponse.setText(response);
            if(response.equals("Added")) {
                JOptionPane.showMessageDialog(null, "Блок добавлен в блокчейн");
                updateWalletInfo();
            }
            lCurrentHeight.setText(String.valueOf(blockchain.maxHeight));
        });
        bCreateSimpleTx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String address = JOptionPane.showInputDialog("Адрес: ");
                long amount = Long.parseLong(JOptionPane.showInputDialog("Сумма: "));
                long fee = Long.parseLong(JOptionPane.showInputDialog("Комиссия: "));
                long inputSum = Long.parseLong(lInputSum.getText());
                addToJList(listTxOutputs, new TxOutput(address, amount));
                addToJList(listTxOutputs, new TxOutput(wallet.address, inputSum-amount-fee));
                updateTxInfo();
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
        removeToJList(listBlockTx, selected);
        addToJList(listMempool, selected);
    }

    private void fromMempoolToBlock() {
        if(listMempool.getSelectedIndex() == -1) return;
        NamedTransaction selected = listMempool.getSelectedValue();
        removeToJList(listMempool, selected);
        addToJList(listBlockTx, selected);
    }

    private void loadMempool() {
        Transaction[] mempoolTxs = blockchain.loadMempool();
        NamedTransaction[] wrappedTxs = new NamedTransaction[mempoolTxs.length];
        for(int i = 0; i < wrappedTxs.length; i++) {
            wrappedTxs[i] = new NamedTransaction(mempoolTxs[i]);
        }
        listMempool.setListData(wrappedTxs);
    }

    private void createTx() {
        PublicKey pk = wallet.getPk();
        Transaction tx = new Transaction();
        tx.pk = pk;
        ArrayList<TransactionInfo> raw_inputs = getArrayFromJList(listTxInputs);
        TxInput[] inputs = new TxInput[raw_inputs.size()];
        for(int i=0; i < raw_inputs.size(); i++) {
            inputs[i] = new TxInput(raw_inputs.get(i).getTxHash(),
                    raw_inputs.get(i).outNum, raw_inputs.get(i).getOutput().amount);
        }
        ArrayList<TxOutput> raw_outputs = getArrayFromJList(listTxOutputs);
        TxOutput[] outputs = new TxOutput[raw_outputs.size()];
        raw_outputs.toArray(outputs);
        tx.inputs = inputs;
        tx.outputs = outputs;
        tx.date = new Date();
        tx.pvBlockHash = blockchain.getBlock(blockchain.getCurrentHeight()-1).hash;
        tx.sign(wallet.getSk());
        blockchain.addTransactionToMempool(tx);

    }

    private static <T> ArrayList<T> getArrayFromJList(JList<T> jList) {
        ListModel<T> listModel = jList.getModel();
        ArrayList<T> arrayList = new ArrayList<>(listModel.getSize());
        for(int i =0; i < listModel.getSize(); i++) {
            arrayList.add(listModel.getElementAt(i));
        }
        //T[] result = new T[arrayList.size()];
        return arrayList;
    }

    private void removeTxOutput() {
        if(listTxOutputs.getSelectedIndex() == -1 ) return;
        TxOutput selected = listTxOutputs.getSelectedValue();
        removeToJList(listTxOutputs, selected);
        updateTxInfo();
    }

    private void addTxOutput() {
        try {
            String address = JOptionPane.showInputDialog("Адрес получателя: ");
            long amount = Long.parseLong(JOptionPane.showInputDialog("Кол-во: "));
            addToJList(listTxOutputs, new TxOutput(address, amount));
            updateTxInfo();
        } catch (Exception ignored) {

        }

    }

    private static <T> void addToJList(JList<T> jList, T value ) {
        ListModel<T> model = jList.getModel();
        java.util.List<T> new_model = new ArrayList<>();
        for(int i=0; i < model.getSize(); i++) {
            new_model.add(model.getElementAt(i));
        }
        new_model.add(value);
        jList.setListData((T[]) new_model.toArray());
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

    public void moveUTXOtoInput() {
        if(listFindUTXO.getSelectedIndex() == -1) return;
        TransactionInfo selected = listFindUTXO.getSelectedValue();
        removeToJList(listFindUTXO, selected);
        addToJList(listTxInputs, selected);
        updateTxInfo();
    }

    public void updateTxInfo() {
        ListModel<TransactionInfo> inputModel = listTxInputs.getModel();
        long sumInput = 0;
        for(int i =0; i < inputModel.getSize(); i++) {
            sumInput += inputModel.getElementAt(i).getOutput().amount;
        }
        long sumOutput = 0;
        ListModel<TxOutput> outputModel = listTxOutputs.getModel();
        for(int i =0; i < outputModel.getSize(); i++) {
            sumOutput += outputModel.getElementAt(i).amount;
        }
        lInputSum.setText(String.valueOf(sumInput));
        lOutputSum.setText(String.valueOf(sumOutput));
        long fee = sumInput - sumOutput;
        lFee.setText(String.valueOf(fee));
    }

    public void moveInputToUTXO() {
        if(listTxInputs.getSelectedIndex() == -1 ) return;
        TransactionInfo selected = listTxInputs.getSelectedValue();
        removeToJList(listTxInputs, selected);
        addToJList(listFindUTXO, selected);
        updateTxInfo();
    }

    public void findUTXO() {
        if(wallet==null) {
            JOptionPane.showMessageDialog(null, "Кошелек не создан!");
            return;
        }
        java.util.List<TransactionInfo> utxo = blockchain.findUTXO(wallet.address);
        if(utxo.size() == 0) {
            JOptionPane.showMessageDialog(null, "UTXO не найдены!");
            return;
        }
        TransactionInfo[] raw_utxo = new TransactionInfo[utxo.size()];
        utxo.toArray(raw_utxo);
        listFindUTXO.setListData(raw_utxo);
    }

    public void showBlock() {
        long i;
        try {
            i = Long.parseLong(tfBlockN.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, tfBlockN.getText() + " - не число");
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
            namedTransactions[j] = new NamedTransaction(block.transactions[j]);
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
        fileChooser.setFileFilter(filter);
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
        jFrame.setSize(1200,500);
        jFrame.setVisible(true);
    }
}

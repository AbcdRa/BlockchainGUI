package abcdra.app;

import abcdra.blockchain.Blockchain;
import abcdra.blockchain.TransactionInfo;
import abcdra.net.ComplexData;
import abcdra.net.JLogger;
import abcdra.net.client.NodeClient;
import abcdra.net.server.NodeServer;
import abcdra.transaction.TxInput;
import abcdra.transaction.TxOutput;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Exchanger;


public class App {
    //TODO Добавить Вкладку на изменение конфигов
    //TODO Исправить баг двойного добавления UTXO
    //TODO Добавить возможность по двойному клику на вход найти начальный выход
    //TODO Добавить возможность по двойному клику на выход найти потраченный вход
    //TODO Ошибки поиска конфига


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
    JList<NamedTransaction> listMempool;
    JList<NamedTransaction> listBlockTx;
    JButton bAddTx;
    JButton bMineBlock;
    JButton bLoadMempool;
    JButton bRemoveTx;
    JLabel lBlockReward;
    JButton bCreateSimpleTx;
    JLabel lResponse;
    private JButton bRunServer;
    private JButton bSyncronize;

    JButton bAddAllMempool;
    private JScrollPane spServerLog;
    private JScrollPane spClientLog;
    private JTextPane tpServerLog;
    private JTextPane tpClientLog;
    private JTabbedPane pVlockExplorer;
    JButton bAutoMining;
    final Blockchain blockchain;
    protected final AppWallet appWallet;
    protected final AppExplorer appExplorer;
    protected final AppTxCreator appTxCreator;
    protected final NodeClient nodeClient;
    protected final NodeServer nodeServer;
    protected final AppMiner appMiner;

    protected Exchanger<ComplexData> exchanger;



    public App() {
        blockchain = AppConfig.safeBlockchainInit();
       exchanger = new Exchanger<>();

        nodeServer = new NodeServer(blockchain, new JLogger(tpServerLog));
        nodeClient = new NodeClient(blockchain, new JLogger(tpClientLog), exchanger);

        appWallet =  new AppWallet(this);
        appExplorer = new AppExplorer(this);
        appTxCreator = new AppTxCreator(this);
        appMiner = new AppMiner(this);

        appMiner.updateBlockReward();

        bRunServer.addActionListener(e -> new Thread(nodeServer).start());
        bSyncronize.addActionListener(e -> new Thread(nodeClient).start());

        lCurrentHeight.addMouseListener(new MouseAdapter() {
        });
        lCurrentHeight.addComponentListener(new ComponentAdapter() {
        });
        pVlockExplorer.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                lCurrentHeight.setText(String.valueOf(blockchain.maxHeight));
            }
        });
        tfUTXO.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                appWallet.changeShowMode();
            }
        });

    }



    public static void main(String[] args) {
        JFrame jFrame = new JFrame("Blockchain");

        JFrame.setDefaultLookAndFeelDecorated(true);
        jFrame.setContentPane(new App().mainJPanel);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        Image icon = Toolkit.getDefaultToolkit().getImage("image\\icon.png");
        jFrame.setIconImage(icon);
        jFrame.setSize(1000,500);
        jFrame.setVisible(true);
    }
}

package abcdra.blockchain;

import com.starkbank.ellipticcurve.utils.Base64;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxOutput;


public class TransactionOutInfo {
    public long blockHeight;
    public int txN;
    public Transaction tx;
    public int outNum;
    public TransactionOutInfo(Transaction tx, int outNum, long blockHeight, int txN) {
        this.tx = tx;
        this.blockHeight = blockHeight;
        this.txN = txN;
        this.outNum = outNum;
    }



    public TxOutput getOutput() {
        return tx.outputs[outNum];
    }
}

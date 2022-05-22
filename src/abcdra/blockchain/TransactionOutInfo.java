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

    public String getTxHash() {
        return Base64.encodeBytes(tx.hash);
    }

    @Override
    public String toString() {
        return Base64.encodeBytes(tx.hash).substring(0,10) + " : " + getOutput().amount;
    }

    public TxOutput getOutput() {
        return tx.outputs[outNum];
    }
}

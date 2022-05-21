package abcdra.transaction;

public class TxInput implements TxPut {
    public String prevTx;
    public int n;
    public long amount;

    public TxInput() {

    }

    public TxInput(String prevTx, int n, long amount) {
        this.prevTx = prevTx;
        this.n = n;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return prevTx + ":" + n + " " + amount;
    }
}

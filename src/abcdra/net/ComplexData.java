package abcdra.net;

import abcdra.blockchain.Block;
import abcdra.transaction.Transaction;

public class ComplexData {
    public Block block;
    public Transaction tx;
    public boolean isBlock;

    public ComplexData(Block block) {
        this.block = block;
        isBlock = true;
    }

    public ComplexData(Transaction tx) {
        this.tx = tx;
        isBlock = false;
    }
}

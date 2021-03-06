package abcdra.app;

import abcdra.transaction.Transaction;

public class NamedTransaction {
    public String name;
    public Transaction tx;

    public NamedTransaction(Transaction tx) {
        this.tx = tx;
        this.name = tx.base64Hash().substring(0,10);
    }

    @Override
    public String toString() {
        return name;
    }
}

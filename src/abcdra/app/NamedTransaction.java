package abcdra.app;

import abcdra.transaction.Transaction;

public class NamedTransaction {
    public String name;
    public Transaction tx;
    public NamedTransaction(String name, Transaction tx) {
        this.name = name;
        this.tx = tx;
    }

    @Override
    public String toString() {
        return name;
    }
}
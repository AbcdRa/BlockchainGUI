package abcdra.blockchain;

import abcdra.transaction.Transaction;

public interface IBlockchain {
    long getMaxHeight();
    int calculateDiff();
    boolean isAdded(Transaction tx);

    TransactionInfo findTransactionById(String txId);

    boolean isSpent(TransactionInfo found);

    Block getBlock(long i);
    Block getLastBlock();

}

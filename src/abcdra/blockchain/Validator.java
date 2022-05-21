package abcdra.blockchain;

import abcdra.transaction.Transaction;

public class Validator {
    public static void checkBlock(Block newBlock, Block pvBlock) {


    }
    public static boolean validCoinBaseTx(Transaction tx, long height) {
        if(tx.inputs != null && tx.inputs.length > 0) return false;
        if(tx.pk != null) return false;
        if(tx.outputs == null || tx.outputs.length != 1) return false;
        if(tx.outputs[0].amount != getCoinbase(height)) return false;
        return true;
    }

    public static long getCoinbase(long height) {
        int pow =(int) (height/Configuration.COINBASE_REDUCE_HEIGHT);
        if(pow > 46) return 0;
        long coinbase = Configuration.INIT_COINBASE;
        for(int i=0; i < pow; i++) {
            coinbase/=2;
        }
        return coinbase;
    }
}

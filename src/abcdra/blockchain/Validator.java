package abcdra.blockchain;

import abcdra.crypt.util.CryptUtil;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;
import com.starkbank.ellipticcurve.Ecdsa;

import java.nio.charset.StandardCharsets;
import com.starkbank.ellipticcurve.utils.Base64;


public class Validator {
    public static String validateBlock(Block newBlock, Blockchain blockchain) {
        if(newBlock.height != blockchain.maxHeight) return "Invalid height";
        if(!newBlock.isComplete()) return "Block not mined";
        if(newBlock.difficult != blockchain.calculateDiff()) return "Invalid difficult";
        byte[] target = MiningUtil.getTarget(newBlock.difficult);
        byte[] testHash = CryptUtil.getHash((newBlock.toPartString() + newBlock.nonce).getBytes(StandardCharsets.UTF_8));
        if(MiningUtil.isLower(target, testHash)) return "Hash is great than target";
        if(!Base64.encodeBytes(testHash).equals(Base64.encodeBytes(newBlock.hash))) return "Invalid nonce";

        if(newBlock.transactions == null || newBlock.transactions.length < 1) return "Block without txs";
        String responseValidTxs = validateTransaction(newBlock.transactions, blockchain);
        if(!responseValidTxs.equals("OK")) return responseValidTxs;
        return "OK";
    }

    public static String validateTransaction(Transaction[] transactions, Blockchain blockchain) {
        if(validCoinBaseTx(transactions[0], blockchain.maxHeight)) return "Invalid Coinbase Tx";
        long fee = Transaction.getFee(transactions);
        if(fee == 0) {
            if(transactions[0].outputs.length != 1) return "extra fee outputs";
        } else{
            if(transactions[0].outputs.length != 2) return "extra coinbase outputs";
            if(transactions[0].outputs[1].amount != fee) return "invalid fee outputs";
        }
        for(int i = 1; i < transactions.length; i++) {
            if(transactions[i].sign == null) return "Transaction without sign";
            if (transactions[i].calculateFee() < 0) return "Output sum less than Input sum";
            if(!transactions[i].verifySign()) return "Sign is not valid";
            for(int j=0; j < transactions[i].inputs.length; j++) {
                TxInput currIn = transactions[i].inputs[j];

                TransactionInfo found = blockchain.findTransactionById(currIn.prevTx);
                if(found == null) return "Inputs linked to not exist transaction";
                found.outNum = currIn.n;

                if(found.getOutput() == null) return "Input ref out num is not exist";
                if(!found.getOutput().address.equals(transactions[i].calculateInAddress()))
                    return "Public key is not equals input ref address";

                if(found.getOutput().amount != currIn.amount) return "Input amount non equal prevOutput amount";
                if(blockchain.isSpent(found)) return "Input ref is overspent";
            }
        }
        //TODO Добавить проверку на дупликаты

        return "OK";
    }

    //TODO Проверка на добавление транзакции в мепул

    public static String validateMempoolAdd(Transaction tx, Blockchain blockchain) {
        String txId = tx.base64Hash();
        TransactionInfo found = blockchain.findTransactionById(txId);
        if(found != null && found.tx.equals(tx)) return "Tx is exist in blockchain";
        return "OK";
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

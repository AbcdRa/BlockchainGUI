package abcdra.blockchain;

import abcdra.crypt.util.CryptUtil;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;
import com.starkbank.ellipticcurve.Ecdsa;

import java.nio.charset.StandardCharsets;
import com.starkbank.ellipticcurve.utils.Base64;


public class Validator {
    public static boolean equals(byte[] bytes1, byte[] bytes2) {
        if(bytes1.length != bytes2.length) return false;
        for(int i =0; i < bytes1.length; i++) {
            if(bytes1[i] != bytes2[i]) return false;
        }
        return true;
    }

    public static String validateBlock(Block newBlock, Blockchain blockchain) {
        if(newBlock.height != blockchain.maxHeight) return "Invalid height";
        if(!newBlock.isComplete()) return "Block not mined";
        if(newBlock.difficult != blockchain.calculateDiff()) return "Invalid difficult";
        byte[] target = MiningUtil.getTarget(newBlock.difficult);
        byte[] testHash = CryptUtil.getHash((newBlock.toPartString() + newBlock.nonce).getBytes(StandardCharsets.UTF_8));
        if(MiningUtil.isLower(target, testHash)) return "Hash is great than target";
        if(!Base64.encodeBytes(testHash).equals(Base64.encodeBytes(newBlock.hash))) return "Invalid nonce";
        if(!equals(newBlock.merkleRoot, Block.getMerkleRoot(newBlock.transactions))) return "Not valid Merkle root";
        if(newBlock.transactions == null || newBlock.transactions.length < 1) return "Block without txs";
        String responseValidTxs = validateTransactions(newBlock.transactions, blockchain);
        if(!responseValidTxs.equals("OK")) return responseValidTxs;
        return "OK";
    }

    public static String validateTransaction(Transaction transaction, Blockchain blockchain) {
        if(blockchain.isAdded(transaction)) return "Transaction is already in block";
        if(transaction.sign == null) return "Transaction without sign";
        if (transaction.calculateFee() < 0) return "Output sum less than Input sum";
        if(!transaction.verifySign()) return "Sign is not valid";
        if(!isUniqIputs(transaction.inputs)) return "Transaction inputs has duplicate";
        for(int j=0; j < transaction.inputs.length; j++) {
            TxInput currIn = transaction.inputs[j];

            TransactionInfo found = blockchain.findTransactionById(currIn.prevTx);
            if (found == null) return "Inputs linked to not exist transaction";
            found.outNum = currIn.n;

            if (found.getOutput() == null) return "Input ref out num is not exist";
            if (!found.getOutput().address.equals(transaction.calculateInAddress()))
                return "Public key is not equals input ref address";

            if (found.getOutput().amount != currIn.amount) return "Input amount non equal prevOutput amount";
            if (blockchain.isSpent(found)) return "Input ref is overspent";
        }
        return "OK";
    }

    public static boolean isUniqIputs(TxInput[] inputs) {
        if(inputs.length == 1) return true;
        for(int i = 0; i < inputs.length; i++) {
            for(int j = i+1; j < inputs.length; j++) {
                if(inputs[i].equals(inputs[j])) return false;
            }
        }
        return true;
    }

    public static String validateTransactions(Transaction[] transactions, Blockchain blockchain) {
        if(!validCoinBaseTx(transactions[0], blockchain.maxHeight)) return "Invalid Coinbase Tx";
        long fee = Transaction.getFee(transactions);
        if(fee == 0) {
            if(transactions[0].outputs.length != 1) return "extra fee outputs";
        } else{
            if(transactions[0].outputs.length != 2) return "extra coinbase outputs";
            if(transactions[0].outputs[1].amount != fee) return "invalid fee outputs";
        }
        for(int i = 1; i < transactions.length; i++) {
            String txResponse = validateTransaction(transactions[i], blockchain);
            if(!txResponse.equals("OK")) return txResponse;
        }
        return "OK";
    }


    public static String validateMempoolAdd(Transaction tx, Blockchain blockchain) {
        String txId = tx.base64Hash();
        Transaction[] currentMempool = blockchain.loadMempool();
        for(Transaction mempoolTx: currentMempool) {
            if(mempoolTx.equals(tx)) return "Transaction is already in mempool!";
            if(isIntersectInputs(tx.inputs, mempoolTx.inputs)) return "In mempool already has intersect inputs";
        }
        return validateTransaction(tx, blockchain);
    }

    public static  boolean isIntersectInputs(TxInput[] inputs1, TxInput[] inputs2) {
        for (TxInput input1 : inputs1)
            for (TxInput input2 : inputs2) if (input1.equals(input2)) return true;
        return false;
    }
    public static boolean validCoinBaseTx(Transaction tx, long height) {
        if(tx.inputs != null && tx.inputs.length > 0) return false;
        if(tx.pk != null) return false;
        if(tx.outputs == null || tx.outputs.length > 2) return false;
        return tx.outputs[0].amount == getCoinbase(height);
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

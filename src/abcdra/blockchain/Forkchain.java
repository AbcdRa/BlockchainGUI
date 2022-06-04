package abcdra.blockchain;

import abcdra.util.CryptUtil;
import abcdra.transaction.Transaction;
import abcdra.transaction.TxInput;

import java.io.File;

public class Forkchain implements IBlockchain{

    Blockchain blockchain;
    String forkName;
    long maxHeight;
    long forkPoint;
    private String FD = File.separator;

    public Forkchain(Blockchain blockchain, String forkName, long forkPoint) {
        this.blockchain = blockchain;
        this.forkName = forkName;
        this.forkPoint = forkPoint;
        new File(this.blockchain.forkPath + FD + forkName).mkdir();
        this.maxHeight = forkPoint+1;
    }

    public void cleanFork() {
        File[] txFork = new File(blockchain.forkPath+FD+forkName).listFiles();
        assert txFork != null;
        if(txFork.length == 0) return;
        for (File file : txFork) {
            if (file!=null) file.delete();
        }
    }

    @Override
    public long getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int calculateDiff() {
        if(maxHeight%Configuration.DIFF_RECALCULATE_HEIGHT != 0) {
            return getBlock(maxHeight-1).difficult;
        }
        long n = maxHeight/Configuration.DIFF_RECALCULATE_HEIGHT - 1;

        Block last = getBlock(maxHeight-1);
        Block before = getBlock(n*Configuration.DIFF_RECALCULATE_HEIGHT);
        long sumTimeS = (last.date.getTime() - before.date.getTime());
        long predictTime = Configuration.AVERAGE_TIME_PER_BLOCK*Configuration.DIFF_RECALCULATE_HEIGHT;
        if(sumTimeS < predictTime) {
            double fraction = (double) predictTime/sumTimeS;
            int dDiff = (int) (Math.log(fraction)/Math.log(2));
            return getBlock(maxHeight-1).difficult + dDiff;
        } else {
            double fraction = (double) sumTimeS/predictTime;
            int dDiff = (int) (Math.log(fraction)/Math.log(2));
            return getBlock(maxHeight-1).difficult - dDiff;
        }
    }



    @Override
    public boolean isAdded(Transaction tx) {
        TransactionInfo found = findTransactionById(tx.base64Hash());
        return found!=null && tx.equals(found.tx);
    }

    @Override
    public TransactionInfo findTransactionById(String txId) {
        for(long i = maxHeight-1; i >= 0; i--) {
            Block block = getBlock(i);
            TransactionInfo foundTx = block.getTxById(txId);
            if(foundTx != null) {
                foundTx.blockHeight = block.height;
                return foundTx;
            }
        }
        return null;
    }

    @Override
    public boolean isSpent(TransactionInfo found) {
        String txId = found.tx.base64Hash();
        for(int i=0; i < maxHeight; i++) {
            String address = found.getOutput().address;
            Block b = getBlock(i);
            for(int j=1; j < b.transactions.length; j++) {
                if(b.transactions[j].calculateInAddress().equals(address)) {
                    TxInput input = b.transactions[j].findInByTxHash(txId, found.outNum);
                    if(input != null) return true;
                }
            }
        }
        return false;
    }


    public String addBlock(Block block) {
        File newFile = new File(blockchain.forkPath + FD + forkName + FD +
                Blockchain.defaultBlockName + block.height);
        String response =Validator.validateBlock(block, this);
        if(!response.equals("OK")) return response;
        CryptUtil.writeStringToFile(newFile, block.toJSON());
        maxHeight++;
        return "Added";
    }

    public String merge() {
        if(blockchain.maxHeight + Configuration.FORCE_FORK_LENGTH > maxHeight) {
            return "UNSAFE MERGE";
        }
        for (long i = forkPoint+1; i < maxHeight; i++) {
            blockchain.rewriteBlock(getBlock(i));
        }
        cleanFork();
        return "Merged";
    }

    @Override
    public Block getBlock(long i) {
        if(i < forkPoint) return blockchain.getBlock(i);
        File blockFile = new File(blockchain.forkPath + FD+ forkName + FD + blockchain.defaultBlockName + i);
        return Block.fromJSON(CryptUtil.readStringFromFile(blockFile));
    }

    @Override
    public Block getLastBlock() {
        return getBlock(maxHeight-1);
    }
}

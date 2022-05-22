package abcdra.blockchain;

import abcdra.crypt.util.CryptUtil;
import abcdra.transaction.Transaction;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import abcdra.transaction.TxInput;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Blockchain {
    public String blockchainPath;
    public String memoryPoolPath;
    public String otherNodeIpFilePath;
    private static final String defaultBlockName = "block";

    public long maxHeight;

    //TODO Выгрузка из мемпула
    //TODO Выполнять быстрый поиск по файлам


    public Blockchain(String blockchainPath, String memoryPoolPath, String otherNodeIpFilePath) {
        this.blockchainPath = blockchainPath;
        this.memoryPoolPath = memoryPoolPath;
        this.otherNodeIpFilePath = otherNodeIpFilePath;
        maxHeight = getCurrentHeight();

    }

    public Blockchain(String jsonConfigPath) {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File(jsonConfigPath);
        try {
            JsonNode jsonNode = mapper.readValue(jsonFile, JsonNode.class);
            String blockchainPath = jsonNode.findValue("BLOCKCHAIN_PATH").asText();
            String memPoolPath = jsonNode.findValue("MEMPOOL_PATH").asText();
            String ipNodePath = jsonNode.findPath("NODES_IP").asText();
            this.blockchainPath = blockchainPath;
            this.memoryPoolPath = memPoolPath;
            this.otherNodeIpFilePath = ipNodePath;
            maxHeight = getCurrentHeight();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Transaction[] loadMempool() {
        File[] txFiles = new File(memoryPoolPath).listFiles();
        assert txFiles != null;
        Transaction[] result = new Transaction[txFiles.length];
        for(int i =0; i < txFiles.length; i++) {
            result[i] = Transaction.fromJSON(CryptUtil.readStringFromFile(txFiles[i]));
        }
        return result;
    }

    public long getNextCoinBase() {
        return Validator.getCoinbase(maxHeight);
    }

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

    public List<TransactionInfo> findUTXO(String address) {
        List<TransactionInfo> utxo = new ArrayList<>();
        for(long i=0; i < maxHeight; i++) {
            Block b = getBlock(i);
            for(int j=0; j < b.transactions.length; j++) {
                ArrayList<Integer> outs = b.transactions[j].findOutsByAddress(address);

                if (outs.size() > 0) {
                    for (Integer out : outs) utxo.add(new TransactionInfo(b.transactions[j], out, i, j));
                }
            }
        }
        utxo.removeIf(this::isSpent);
        return utxo;
    }

    public long getUTXO(String address) {
        List<TransactionInfo> utxo = findUTXO(address);
        long amount = 0;
        for(TransactionInfo txInfo : utxo) {
            amount += txInfo.getOutput().amount;
        }
        return amount;
    }


    public boolean isSpent(TransactionInfo txOutInfo){
        String txId = txOutInfo.tx.base64Hash();
        for(int i=0; i < maxHeight; i++) {
            String address = txOutInfo.getOutput().address;
            Block b = getBlock(i);
            for(int j=1; j < b.transactions.length; j++) {
                if(b.transactions[j].calculateInAddress().equals(address)) {
                    TxInput input = b.transactions[j].findInByTxHash(txId, txOutInfo.outNum);
                    if(input != null) return true;
                }
            }
        }
        return false;
    }

    public Block getLastBlock() {
        return getBlock(maxHeight-1);
    }

    public void addTransactionToMempool(Transaction tx) {
        CryptUtil.writeStringToFile(memoryPoolPath+"/tx_"+tx.toHEXHash().substring(0,20),tx.toJSON());
    }


//    //public Transaction getMempoolTxs() {
//        throw new RuntimeException("NOT IMPLEMENTED");
//    }

    public Block getBlock(long height) {
        if(height >= maxHeight) {
            return null;
        }

        String jsonBlock = CryptUtil.readStringFromFile(blockchainPath+"/"+defaultBlockName+height);
        return Block.fromJSON(jsonBlock);
    }

    public String addBlock(Block block) {
        String validResult = Validator.validateBlock(block, this);
        if(!validResult.equals("OK")) return validResult;
        CryptUtil.writeStringToFile(blockchainPath+"/"+defaultBlockName+maxHeight,block.toJSON());
        maxHeight++;
        return "Added";
    }

    public long getCurrentHeight() {
        return Objects.requireNonNull((new File(blockchainPath)).listFiles()).length;
    }

    public int calculateDiff() {
        if(maxHeight%Configuration.DIFF_RECALCULATE_HEIGHT != 0) {
            return getLastBlock().difficult;
        }
        long n = maxHeight/Configuration.DIFF_RECALCULATE_HEIGHT - 1;

        Block last = getLastBlock();
        Block before = getBlock(n*Configuration.DIFF_RECALCULATE_HEIGHT);
        long sumTimeS = (last.date.getTime() - before.date.getTime());
        long predictTime = Configuration.AVERAGE_TIME_PER_BLOCK*Configuration.DIFF_RECALCULATE_HEIGHT;
        if(sumTimeS < predictTime) {
            double fraction = (double) predictTime/sumTimeS;
            int dDiff = (int) (Math.log(fraction)/Math.log(2));
            return getLastBlock().difficult + dDiff;
        } else {
            double fraction = (double) sumTimeS/predictTime;
            int dDiff = (int) (Math.log(fraction)/Math.log(2));
            return getLastBlock().difficult - dDiff;
        }


    }

}

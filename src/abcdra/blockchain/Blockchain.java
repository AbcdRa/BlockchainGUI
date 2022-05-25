package abcdra.blockchain;

import abcdra.crypt.util.CryptUtil;
import abcdra.transaction.Transaction;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import abcdra.transaction.TxInput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Blockchain {
    public String blockchainPath;
    public String memoryPoolPath;
    public String otherNodeIpFilePath;
    private static final String defaultBlockName = "block";
    public String rawMempoolPath;

    public long maxHeight;

    //TODO Выполнять быстрый поиск по файлам

    public boolean isAdded(Transaction tx) {
        TransactionInfo found = findTransactionById(tx.base64Hash());
        return found!=null && tx.equals(found.tx);
    }

    public Blockchain(String blockchainPath, String memoryPoolPath, String otherNodeIpFilePath) {
        this.blockchainPath = blockchainPath;
        this.memoryPoolPath = memoryPoolPath;
        this.otherNodeIpFilePath = otherNodeIpFilePath;
        maxHeight = getCurrentHeight();
    }

    public List<String> getNodesIp() {
        List<String> ips = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader( new FileReader(otherNodeIpFilePath))) {
            String line = null;
            while( ( line = reader.readLine() ) != null ) {
                ips.add( line );
            }

        } catch (IOException e) {

        }
        return ips;
    }

    public Blockchain(String jsonConfigPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File(jsonConfigPath);

        JsonNode jsonNode = mapper.readValue(jsonFile, JsonNode.class);
        String blockchainPath = jsonNode.findValue("BLOCKCHAIN_PATH").asText();
        String memPoolPath = jsonNode.findValue("MEMPOOL_PATH").asText();
        String ipNodePath = jsonNode.findPath("NODES_IP").asText();
        this.blockchainPath = blockchainPath;
        this.memoryPoolPath = memPoolPath + "\\current";
        new File(this.memoryPoolPath).mkdir();
        this.rawMempoolPath = memPoolPath+"\\raw.json";
        new File(rawMempoolPath).createNewFile();
        this.otherNodeIpFilePath = ipNodePath;
        maxHeight = getCurrentHeight();
        if(maxHeight == 0) {
            Block genesis = new Block();
            genesis.mineBlock();
            CryptUtil.writeStringToFile(blockchainPath+"/"+defaultBlockName+maxHeight,genesis.toJSON());
            maxHeight = getCurrentHeight();
        }

    }

    public void cleanMempool() {
        File[] txFiles = new File(memoryPoolPath).listFiles();
        assert txFiles != null;
        if(txFiles.length == 0) return;
        for(int i =0; i < txFiles.length; i++) {
            Transaction tx = Transaction.fromJSON(CryptUtil.readStringFromFile(txFiles[i]));
            if(isAdded(tx)) txFiles[i].delete();
        }

    }

    public Transaction[] loadMempool() {
        cleanMempool();
        File[] txFiles = new File(memoryPoolPath).listFiles();
        assert txFiles != null;
        Transaction[] result = new Transaction[txFiles.length];
        for(int i =0; i < txFiles.length; i++) {
            result[i] = Transaction.fromJSON(CryptUtil.readStringFromFile(txFiles[i]));
        }
        return result;
    }

    public String getRawMempool() {
        updateRawMempool();
        return CryptUtil.readStringFromFile(new File(rawMempoolPath));
    }

    public void updateRawMempool() {
        cleanMempool();
        File[] txFiles = new File(memoryPoolPath).listFiles();
        assert txFiles != null;
        String[] result = new String[txFiles.length];
        for(int i =0; i < txFiles.length; i++) {
            result[i] = CryptUtil.readStringFromFile(txFiles[i]);
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            mapper.writeValue(new File(rawMempoolPath), result);
        } catch (IOException e) {
            throw new RuntimeException(e);

        }

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

    public String addTransactionToMempool(Transaction tx) {
        String response = Validator.validateMempoolAdd(tx, this);
        if(!response.equals("OK")) return response;
        CryptUtil.writeStringToFile(memoryPoolPath+"/tx_"+tx.toHEXHash().substring(0,20),tx.toJSON());
        return "Added";
    }


//    //public Transaction getMempoolTxs() {
//        throw new RuntimeException("NOT IMPLEMENTED");
//    }

    public String getRawBlock(long height) {
        if(height >= maxHeight) {
            return null;
        }

        return CryptUtil.readStringFromFile(blockchainPath+"/"+defaultBlockName+height);
    }

    public Block getBlock(long height) {
        return Block.fromJSON(getRawBlock(height));
    }

    public String addBlock(Block block) {
        String validResult = Validator.validateBlock(block, this);
        if(!validResult.equals("OK")) return validResult;
        CryptUtil.writeStringToFile(blockchainPath+"/"+defaultBlockName+maxHeight,block.toJSON());
        maxHeight++;
        return "Added";
    }

    public long getCurrentHeight() {
        File[] files = new File(blockchainPath).listFiles();
        if(files==null) return 0;
        return files.length;
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

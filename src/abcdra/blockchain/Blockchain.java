package abcdra.blockchain;

import abcdra.crypt.util.CryptUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import abcdra.transaction.TxInput;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    public String blockchainPath;
    public String memoryPoolPath;
    public String otherNodeIpFilePath;
    private static final String defaultBlockName = "block";

    public long maxHeight;

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

    public List<TransactionOutInfo> findUTXO(String address) {
        List<TransactionOutInfo> utxo = new ArrayList<>();
        for(long i=0; i < maxHeight; i++) {
            Block b = getBlock(i);
            for(int j=0; j < b.transactions.length; j++) {
                int outputN = b.transactions[j].findOutByAddress(address);
                if (outputN != -1) {
                    utxo.add(new TransactionOutInfo(b.transactions[j],outputN,i,j));
                }
            }
        }
        for(TransactionOutInfo info : utxo) {
            if(isSpent(info)) utxo.remove(info);
        }
        return utxo;
    }

    public long getUTXO(String address) {
        List<TransactionOutInfo> utxo = findUTXO(address);
        long amount = 0;
        for(TransactionOutInfo txInfo : utxo) {
            amount += txInfo.getOutput().amount;
        }
        return amount;
    }

    public boolean isSpent(TransactionOutInfo txOutInfo){
        String txId = txOutInfo.tx.base64Hash();
        for(int i=0; i < maxHeight; i++) {
            String address = txOutInfo.getOutput().address;
            Block b = getBlock(i);
            for(int j=0; j < b.transactions.length; j++) {
                if(b.transactions[j].calculateInAddress() == address) {
                    TxInput input = b.transactions[j].findInByTxHash(txId, txOutInfo.outNum);
                    if(input != null) return true;
                }
            }
        }
        return false;
    }

    public Block getBlock(long height) {
        if(height >= maxHeight) {
            throw null;
        }

        String jsonBlock = CryptUtil.readStringFromFile(blockchainPath+"/"+defaultBlockName+height);
        return Block.fromJSON(jsonBlock);
    }

    public void addBlock(Block block) {
        if(block.height < maxHeight) return;
        CryptUtil.writeStringToFile(blockchainPath+"/"+defaultBlockName+maxHeight,block.toJSON());
        maxHeight++;
    }

    public long getCurrentHeight() {
        return (new File(blockchainPath)).listFiles().length;
    }

    public int calculateDiff() {
        throw new RuntimeException("NOT IMPLEMENTED");
    }

}

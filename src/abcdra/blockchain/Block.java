package abcdra.blockchain;

import com.starkbank.ellipticcurve.utils.Base64;
import abcdra.crypt.util.CryptUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.JSONWrappedObject;
import org.codehaus.jackson.type.TypeReference;
import abcdra.transaction.Transaction;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class Block {
    public byte[] hash;
    public byte[] pvHash;
    public byte[] merkleRoot;
    public Transaction[] transactions;
    public int difficult;
    public long height;
    public long nonce;
    public Date date;

    public Block() {
        pvHash = new byte[32];
        difficult = Configuration.INIT_DIFF;
        height = 0;
        date = new Date();
        transactions = new Transaction[]{
                new Transaction("xht8ffw8KU2NSn1djJWaXdPSVSkXogLohvQNoMjsay4=",
                Configuration.INIT_COINBASE, pvHash)};
        merkleRoot = getMerkleRoot(transactions);
    }

    public Block(Blockchain blockchain) {
        Block lastBlock = blockchain.getBlock(blockchain.maxHeight-1);
        pvHash = lastBlock.hash;
        height = lastBlock.height + 1;
        if(height%Configuration.DIFF_RECALCULATE_HEIGHT==0) {
            difficult = blockchain.calculateDiff();
        }
    }


    public String takeMinerAddress() {
        return transactions[0].outputs[0].address;
    }

    public String toPartString() {
        String result = "";
        result += Base64.encodeBytes(pvHash) + "/";
        result += Base64.encodeBytes(merkleRoot) + "/";
        //result += transactionsToString() + "/";
        result += difficult + "/";
        result += height + "/";
        result += date.getTime() + "/";
        return  result;
    }

    public void mineBlock() {
        String currBlock = toPartString();
        byte[] target = MiningUtil.getTarget(difficult);
        nonce = MiningUtil.getNonce(currBlock, target);
        hash = CryptUtil.getHash((currBlock+nonce).getBytes(StandardCharsets.UTF_8));
    }

    public String transactionsToString() {
        StringBuilder s = new StringBuilder("[");
        for(Transaction tx: transactions) {
            s.append(tx.toString());
            s.append(", ");
        }
        return s.toString() + "]";
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(this);
            return json;
        } catch (IOException e) {
            System.err.println("BLOCK -> JSON ERROR");
            throw new RuntimeException(e);
        }
    }


    public void addFeeTransactions() {
        if(transactions.length > 1) {
            throw new RuntimeException("NOT IMPLEMENTED");
        }
    }

    public static Block fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readValue(json, JsonNode.class);
            Block restore = new Block();
            JsonNode txNode =  jsonNode.findValue("transactions");
            Transaction[] txs = mapper.readValue(txNode, Transaction[].class);
            restore.transactions = txs;
            restore.hash = Base64.decode(jsonNode.findValue("hash").asText());
            restore.pvHash = Base64.decode(jsonNode.findValue("pvHash").asText());
            restore.nonce = jsonNode.findValue("nonce").asLong();
            restore.merkleRoot = Base64.decode(jsonNode.findValue("merkleRoot").asText());
            restore.height = jsonNode.findValue("height").asLong();
            restore.difficult = jsonNode.findValue("difficult").asInt();
            restore.date = new Date(jsonNode.findValue("date").asLong());
            return  restore;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public static byte[][] partMerkleRoot(byte[][] transactions) {
        int N = (transactions.length+1)/2;
        byte[][] hashs = new byte[N][32];
        for(int i=0; i < N-1; i++) {
            byte[] summaryHash = new byte[64];
            for (int j = 0; j < 32; j++) summaryHash[j] = transactions[2 * i][j];
            for (int j = 32; j < 64; j++) summaryHash[j] = transactions[2 * i + 1][32 - j];
            hashs[i] = CryptUtil.getHash(summaryHash);
        }
        byte[] summaryHash = new byte[64];
        for (int j = 0; j < 32; j++) summaryHash[j] = transactions[2*(N-1)][j];
        if (transactions.length%2==1) for (int j = 0; j < 32; j++) summaryHash[j] = transactions[2*(N-1)][j];
        else for (int j = 0; j < 32; j++) summaryHash[j] = transactions[2*(N-1)+1][j];
        hashs[N-1] = CryptUtil.getHash(summaryHash);
        return hashs;
    }

    public static byte[][] transactionsHash(Transaction[] transactions) {
        byte[][] hashs = new byte[transactions.length][32];
        for(int i=0; i < transactions.length; i++) {
            hashs[i] = transactions[i].hash;
        }
        return hashs;
    }

    public static byte[] getMerkleRoot(Transaction[] transactions) {
        byte[][] hashs = transactionsHash(transactions);
        while (hashs.length != 1) {
            hashs = partMerkleRoot(hashs);
        }
        return hashs[0];
    }


}

package abcdra.blockchain;

import abcdra.transaction.TxInput;
import abcdra.transaction.TxOutput;
import com.starkbank.ellipticcurve.PublicKey;
import com.starkbank.ellipticcurve.Signature;
import com.starkbank.ellipticcurve.utils.Base64;
import abcdra.crypt.util.CryptUtil;
import com.starkbank.ellipticcurve.utils.ByteString;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import abcdra.transaction.Transaction;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


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

    public Block(Blockchain blockchain, String address, Transaction[] txs) {
        Transaction[] wcbtxs = new Transaction[txs.length+1];

        date = new Date();
        Block lastBlock = blockchain.getLastBlock();
        wcbtxs[0] = new Transaction(address, blockchain.getNextCoinBase(), txs, lastBlock.hash);
        for(int i=1; i < wcbtxs.length; i++) {
            wcbtxs[i] = txs[i-1];
        }
        transactions = wcbtxs;
        pvHash = lastBlock.hash;
        height = lastBlock.height + 1;
        difficult = blockchain.calculateDiff();
        merkleRoot = getMerkleRoot(wcbtxs);
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
        Map<String, String> txMap = new HashMap<>();
        txMap.put("hash", Base64.encodeBytes(hash));
        txMap.put("pvHash", Base64.encodeBytes(pvHash));
        txMap.put("date", String.valueOf(date.getTime()));
        txMap.put("merkleRoot", Base64.encodeBytes(merkleRoot));
        txMap.put("difficult", String.valueOf(difficult));
        txMap.put("nonce", String.valueOf(nonce));
        txMap.put("height", String.valueOf(height));
        for(int i =0; i< transactions.length; i++) {
            txMap.put("t"+i, transactions[i].toJSON());
        }
        try {
            return mapper.writeValueAsString(txMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Block fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readValue(json, JsonNode.class);
            Block restore = new Block();
            restore.hash = Base64.decode(jsonNode.findValue("hash").asText());
            restore.pvHash = Base64.decode(jsonNode.findValue("pvHash").asText());
            restore.merkleRoot = Base64.decode(jsonNode.findValue("merkleRoot").asText());
            ArrayList<Transaction> txs = new ArrayList<>();
            for(int i = 0; jsonNode.has("t"+i); i++) {
                txs.add(Transaction.fromJSON(jsonNode.findValue("t"+i).asText()));
            }
            Transaction[] transactions = new Transaction[txs.size()];
            txs.toArray(transactions);
            restore.transactions = transactions;
            restore.date = new Date(jsonNode.findValue("date").asLong());
            restore.difficult = jsonNode.findValue("difficult").asInt();
            restore.height = jsonNode.findValue("height").asLong();
            restore.nonce = jsonNode.findValue("nonce").asLong();
            return restore;
        } catch (IOException e ) {
            throw new RuntimeException(e);
        }
    }


    public void addFeeTransactions() {
        if(transactions.length > 1) {
            throw new RuntimeException("NOT IMPLEMENTED");
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

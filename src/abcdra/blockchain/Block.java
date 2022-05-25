package abcdra.blockchain;

import com.starkbank.ellipticcurve.utils.Base64;
import abcdra.crypt.util.CryptUtil;
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
                new Transaction(Configuration.GENESIS_ADDRESS,
                Configuration.INIT_COINBASE, pvHash)};
        merkleRoot = getMerkleRoot(transactions);
    }

    public Block(Blockchain blockchain, String address, Transaction[] txs) {
        Transaction[] transactionsWithCB = new Transaction[txs.length+1];

        date = new Date();
        Block lastBlock = blockchain.getLastBlock();
        transactionsWithCB[0] = new Transaction(address, blockchain.getNextCoinBase(), txs, lastBlock.hash);
        System.arraycopy(txs, 0, transactionsWithCB, 1, transactionsWithCB.length - 1);
        transactions = transactionsWithCB;
        pvHash = lastBlock.hash;
        height = lastBlock.height + 1;
        difficult = blockchain.calculateDiff();
        merkleRoot = getMerkleRoot(transactionsWithCB);
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

    public boolean isComplete() {
        return hash != null;
    }

    public TransactionInfo getTxById(String txId) {
        for(int i=0; i < transactions.length; i++) {
            Transaction tx = transactions[i];
            if(tx.base64Hash().equals(txId)) return new TransactionInfo(tx, 0, 0, i);
        }
        return null;
    }

    public void mineBlock() {
        String currBlock = toPartString();
        byte[] target = MiningUtil.getTarget(difficult);
        boolean isMined = false;
        while (!isMined) {
            try {
                nonce = MiningUtil.getNonce(currBlock, target);
                isMined = true;
            } catch (Exception e) {
                date = new Date();
                System.out.println("Перебраны все long");
            }
        }
        hash = CryptUtil.getHash((currBlock+nonce).getBytes(StandardCharsets.UTF_8));
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
            return null;

        }
    }


    public static byte[][] partMerkleRoot(byte[][] transactions) {
        int N = (transactions.length+1)/2;
        byte[][] hashs = new byte[N][32];
        for(int i=0; i < N-1; i++) {
            byte[] summaryHash = new byte[64];
            System.arraycopy(transactions[2 * i], 0, summaryHash, 0, 32);
            System.arraycopy(transactions[2 * i + 1], 0, summaryHash, 32, 32);
            hashs[i] = CryptUtil.getHash(summaryHash);
        }
        byte[] summaryHash = new byte[64];
        System.arraycopy(transactions[2 * (N - 1)], 0, summaryHash, 0, 32);
        if (transactions.length%2==1) System.arraycopy(transactions[2 * (N - 1)], 0, summaryHash, 0, 32);
        else System.arraycopy(transactions[2 * (N - 1) + 1], 0, summaryHash, 0, 32);
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

package abcdra.transaction;
import abcdra.blockchain.MiningUtil;
import com.starkbank.ellipticcurve.*;
import com.starkbank.ellipticcurve.utils.Base64;
import abcdra.crypt.util.CryptUtil;
import com.starkbank.ellipticcurve.utils.ByteString;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Transaction {
    public PublicKey pk;
    public TxInput[] inputs;
    public TxOutput[] outputs;
    public Signature sign;
    public byte[] hash;

    public byte[] pvBlockHash;

    public Date date;

    public static boolean equals(PublicKey pk1, PublicKey pk2) {
        return pk1.toPem().equals(pk2.toPem());
    }
    public boolean equals(Transaction oth) {
        updateHash();
        oth.updateHash();
        if(!base64Hash().equals(oth.base64Hash())) return false;
        if(pk==null && oth.pk != null) return false;
        if(oth.pk == null && pk != null) return false;
        if(pk != null & oth.pk != null)
            if(!equals(pk, oth.pk)) return false;
        if(inputs == null && oth.inputs != null) return false;
        if(oth.inputs == null && inputs != null) return false;
        if(inputs != null) {
            if (inputs.length != oth.inputs.length) return false;
            for(int i=0; i < inputs.length; i++) {
                if(!inputs[i].equals(oth.inputs[i])) return false;
            }
        }
        if(!Base64.encodeBytes(pvBlockHash).equals(Base64.encodeBytes(oth.pvBlockHash))) return false;
        if(outputs.length != oth.outputs.length) return false;

        for (int i=0; i < outputs.length; i++) {
            if(!outputs[i].equals(oth.outputs[i])) return false;
        }
        if(sign == null && oth.sign != null) return false;
        if(oth.sign == null && sign != null) return false;
        if(sign != null) {
            if(!sign.toBase64().equals(oth.sign.toBase64())) return false;
        }
        return date.compareTo(oth.date) == 0;
    }

    public static long getFee(Transaction[] txs) {
        long fee = 0;
        for(Transaction tx : txs) {
            if(!tx.isCoinBase()) fee += tx.calculateFee();
        }
        return fee;
    }

    public ArrayList<Integer> findOutsByAddress(String address) {
        ArrayList<Integer> outs = new ArrayList<>();
        for(int i = 0; i < outputs.length; i++) {
            if(outputs[i].address.equals(address)) outs.add(i);
        }
        return outs;
    }

    public TxInput findInByTxHash(String hash, int n) {
        if(inputs == null) return null;
        for (TxInput input : inputs) {
            if (input.prevTx.equals(hash) && input.n == n) {
                return input;
            }
        }
        return null;
    }

    public String calculateInAddress() {
        if(pk == null) return null;
        return  Base64.encodeBytes(CryptUtil.getHash(pk.toByteString().getBytes()));
    }

    public String base64Hash() {
        return Base64.encodeBytes(hash);
    }

    public  Transaction() {

    }
    //Создание COINBASE TRANSACTION
    public Transaction(String address, long currentCoinbase, byte[] pvBlockHash) {
        inputs = new TxInput[0];
        outputs = new TxOutput[]{new TxOutput(address, currentCoinbase)};
        this.pvBlockHash = pvBlockHash;
        date = new Date();
        hash = calculateHash();
    }

    public Transaction(String address, long currentCoinbase, Transaction[] txs, byte[] pvBlockHash) {
        inputs = new TxInput[0];
        long sumFee = 0;
        for(Transaction tx: txs) {
            sumFee += tx.calculateFee();
        }
        if(sumFee == 0) {
            outputs = new TxOutput[]{new TxOutput(address, currentCoinbase)};
        } else {
            outputs = new TxOutput[]{new TxOutput(address, currentCoinbase), new TxOutput(address, sumFee)};
        }

        this.pvBlockHash = pvBlockHash;
        date = new Date();
        hash = calculateHash();
    }

    public void updateHash() {
        hash = calculateHash();
    }

    public void sign(PrivateKey sk) {
        sign = Ecdsa.sign(toPartStringWithoutSign(), sk);
        updateHash();
    }

    public boolean verifySign() {
        return Ecdsa.verify(toPartStringWithoutSign(), sign, pk);
    }

    public String toHEXHash() {
        return MiningUtil.bytesToHex(hash);
    }

    public long calculateInputSum() {
        if(inputs==null || inputs.length == 0) return 0;
        long sum = 0;
        for (TxInput input : inputs) sum += input.amount;
        return sum;
    }

    public long calculateOutputSum() {
        if(outputs==null || outputs.length == 0) return 0;
        long sum = 0;
        for (TxOutput output : outputs) sum += output.amount;
        return sum;
    }

    public long calculateFee() {
        return calculateInputSum() - calculateOutputSum();
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> txMap = new HashMap<>();
        if(pk != null) {
            txMap.put("pk", Base64.encodeBytes(pk.toByteString().getBytes()));
        } else {
            txMap.put("pk", "null");
        }

        txMap.put("hash", base64Hash());
        txMap.put("pvBlockHash", Base64.encodeBytes(pvBlockHash));
        txMap.put("date", String.valueOf(date.getTime()));
        if(sign != null) {
        txMap.put("sign", Base64.encodeBytes(sign.toDer().getBytes()));
        } else {
            txMap.put("sign", "null");
        }
        try {

            txMap.put("inputs", mapper.writeValueAsString(inputs));
            txMap.put("outputs", mapper.writeValueAsString(outputs));
            return mapper.writeValueAsString(txMap);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Transaction fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readValue(json, JsonNode.class);
            Transaction restore = new Transaction();
            String pk = jsonNode.findValue("pk").asText();
            if(!pk.equals("null")) {
                restore.pk = PublicKey.fromString(new ByteString(Base64.decode(pk)));
            } else {
                restore.pk = null;
            }
            restore.date = new Date(jsonNode.findValue("date").asLong());
            String inputs = jsonNode.findValue("inputs").asText();
            String outputs = jsonNode.findValue("outputs").asText();
            restore.inputs = mapper.readValue(inputs, TxInput[].class);
            restore.outputs = mapper.readValue(outputs, TxOutput[].class);
            String signBase64 = jsonNode.findValue("sign").asText();
            if(!signBase64.equals("null")) {
            restore.sign = Signature.fromDer(new ByteString(Base64.decode(signBase64)));
            } else {
                restore.sign = null;
            }
            restore.pvBlockHash = Base64.decode(jsonNode.findValue("pvBlockHash").asText());

            restore.updateHash();
            if(!jsonNode.findValue("hash").asText().equals(Base64.encodeBytes(restore.hash))) {
                return null;
            }
            return restore;
        } catch (IOException e ) {
            return null;
        }
    }




    public boolean isCoinBase() {
        return inputs == null || inputs.length == 0;
    }

    private String putsToString(TxPut[] puts) {
        if(puts == null || puts.length==0) return "[]";


        StringBuilder result = new StringBuilder("[{");
        for(int i=0; i < puts.length-1; i++) {
            result.append(puts[i].toString()).append("}, {");
        }
        return  result + puts[puts.length-1].toString() + "}]";

    }

    private byte[] calculateHash() {
        String s = toPartString();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("НЕ НАЙДЕН АЛГОРИТМ SHA256");
        }
        return null;
    }

    @Override
    public  String toString() {
        return toPartString() + "/" + Base64.encodeBytes(hash);
    }

    private String toPartString() {
        String partStr = pk == null ? "" : Base64.encodeBytes( pk.toByteString().getBytes());
        partStr += "/";
        partStr += putsToString(inputs) + "/";
        partStr += putsToString(outputs) + "/";
        partStr += sign == null ? "" : sign.toBase64();
        partStr += "/";
        partStr += date.getTime();
        partStr += "/";
        partStr += Base64.encodeBytes(pvBlockHash);
        partStr += "/";
        return  partStr;
    }
    private String toPartStringWithoutSign() {
        String partStr = pk == null ? "" : Base64.encodeBytes( pk.toByteString().getBytes());
        partStr += "/";
        partStr += putsToString(inputs) + "/";
        partStr += putsToString(outputs) + "//";
        partStr += date.getTime();
        partStr += "/";
        partStr += Base64.encodeBytes(pvBlockHash);
        partStr += "/";
        return  partStr;
    }

}




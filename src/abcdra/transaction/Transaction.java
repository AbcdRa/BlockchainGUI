package abcdra.transaction;
import com.starkbank.ellipticcurve.*;
import com.starkbank.ellipticcurve.utils.Base64;
import abcdra.crypt.util.CryptUtil;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Date;


public class Transaction {
    public PublicKey pk;
    public TxInput[] inputs;
    public TxOutput[] outputs;
    public Signature sign;
    public byte[] hash;

    public byte[] pvBlockHash;

    public Date date;

    public int findOutByAddress(String address) {
        for(int i = 0; i < outputs.length; i++) {
            if(outputs[i].address.equals(address)) return i;

        }
        return -1;
    }

    public TxInput findInByTxHash(String hash, int n) {
        if(inputs == null) return null;
        for (int i = 0; i < inputs.length; i++) {
            if(inputs[i].prevTx == hash && inputs[i].n == n) {
                return inputs[i];
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

    private String putsToString(TxPut[] puts) {
        if(puts == null || puts.length==0) return "[]";


        String result = "[{";
        for(int i=0; i < puts.length-1; i++) {
            result += puts[i].toString() + "}, {";
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
}


interface TxPut {
    String toString();
}



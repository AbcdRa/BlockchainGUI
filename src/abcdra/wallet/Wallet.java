package abcdra.wallet;

import com.starkbank.ellipticcurve.PrivateKey;
import com.starkbank.ellipticcurve.PublicKey;
import com.starkbank.ellipticcurve.utils.Base64;
import com.starkbank.ellipticcurve.utils.ByteString;
import abcdra.crypt.util.CryptUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Wallet {
    PrivateKey sk;
    PublicKey pk;
    public String address;

    public PrivateKey getSk() {
        return sk;
    }

    public PublicKey getPk() {
        return pk;
    }

    public Wallet(PrivateKey sk) {
        this.sk = sk;
        pk = sk.publicKey();
        address = Base64.encodeBytes(CryptUtil.getHash(pk.toByteString().getBytes()));
    }

    public Wallet() {
        this(new PrivateKey());
    }

    public static Wallet restore(File file, String pass) {
        String crypt = CryptUtil.readStringFromFile(file);
        SecretKeySpec key = CryptUtil.getKey(pass);
        String pem = CryptUtil.decryptString(crypt, key);
        if(pem == null) {
            return null;
        }
        PrivateKey sk = PrivateKey.fromPem(pem);
        return new Wallet(sk);
    }

    public static Wallet restore(String filepath, String pass) {
        return restore(new File(filepath), pass);
    }


    public void save(String filepath, String pass) {
        SecretKeySpec key= CryptUtil.getKey(pass);
        String crypt = CryptUtil.cryptString(sk.toPem(), key);
        CryptUtil.writeStringToFile(filepath, crypt);
    }

    public void save(File file, String pass) {
        SecretKeySpec key= CryptUtil.getKey(pass);
        String crypt = CryptUtil.cryptString(sk.toPem(), key);
        CryptUtil.writeStringToFile(file, crypt);
    }

}

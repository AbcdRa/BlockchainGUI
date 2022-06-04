package abcdra.util;

import com.starkbank.ellipticcurve.utils.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class CryptUtil {
    public static boolean isLog = false;
    public static SecretKeySpec getKey(String pass) {
        try {
            byte[] hash = getHash(pass.getBytes(StandardCharsets.UTF_8));
            if(hash != null) return new SecretKeySpec(hash, "AES");
        }
        catch (Exception e) {
            System.err.println("НЕ УДАЛОСЬ ПОЛУЧИТЬ КЛЮЧ ШИФРОВКИ");
            return null;
        }
        return null;
    }

    public static void writeStringToFile(File file, String text) {
        if(!file.exists()) try {
            file.getParentFile().mkdir();
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try(FileWriter writer = new FileWriter(file, false))
        {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void writeStringToFile(String filepath, String text) {
        writeStringToFile(new File(filepath), text);
    }

    public static String readStringFromFile(File file) {
        try(BufferedReader reader = new BufferedReader( new FileReader(file))) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            String ls = System.getProperty("line.separator");
            while( ( line = reader.readLine() ) != null ) {
                stringBuilder.append( line );
                stringBuilder.append( ls );
            }
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
            return stringBuilder.toString();
        } catch (IOException e) {
            if(isLog) System.err.println("Empty File: " + file.getPath());
            return null;
        }
    }

    public static String readStringFromFile(String filepath) {
        return readStringFromFile(new File(filepath));
    }


    public static String cryptString(String s, SecretKeySpec key){
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherBytes = cipher.doFinal(s.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBytes(cipherBytes);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("НЕ УДАЛОСЬ ЗАШИФРОВАТЬ СТРОКУ" + e.getMessage());
            return null;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("НЕ УДАЛОСЬ ЗАШИФРОВАТЬ СТРОКУ");
            throw new RuntimeException(e);
        }

    }

    public static String decryptString(String crypt, SecretKeySpec key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] base64dec = Base64.decode(crypt);
            byte[] byteStr = cipher.doFinal(base64dec);
            return new String(byteStr, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("НЕ УДАЛОСЬ РАСШИФРОВАТЬ СТРОКУ" + e.getMessage());
            return null;
        } catch (IllegalBlockSizeException e) {
            System.err.println("НЕ УДАЛОСЬ РАСШИФРОВАТЬ СТРОКУ");
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            System.err.println("НЕ УДАЛОСЬ РАСШИФРОВАТЬ СТРОКУ");
            return null;
            //throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getHash(byte[] s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("НЕ НАЙДЕН АЛГОРИТМ SHA256");
        }
        return null;
    }
}

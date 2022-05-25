package abcdra.blockchain;

import abcdra.crypt.util.CryptUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;

public class MiningUtil {
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static boolean isLower(byte[] arr1, byte[] arr2) {
        for(int i=0; i < arr1.length; i++) {
            if((arr1[i] & 0xFF) == (arr2[i] & 0xFF)) continue;
            return ((arr1[i] & 0xFF) < (arr2[i] & 0xFF));
        }
        return false;
    }

    public static long getNonce(String partStr, byte[] target) {
        long init_nonce = new Random().nextLong();
        long nonce = init_nonce+1;
        String StrWithNonce = partStr + nonce;
        while (!isLower(Objects.requireNonNull(CryptUtil.getHash(StrWithNonce.getBytes(StandardCharsets.UTF_8))), target) && nonce!=init_nonce) {
            nonce++;
            StrWithNonce = partStr + nonce;
        }
        if (nonce == init_nonce) throw new RuntimeException("НЕ УДАЛОСЬ ПОДОБРАТЬ NONCE");
        return nonce;
    }

    public static byte[] getTarget(int diff) {
        byte[] target = new byte[32];
        byte targetByte = (byte) (0b10000000 >> (diff%8));
        target[diff/8] = targetByte;
        return target;

    }

}

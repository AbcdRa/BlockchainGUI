package abcdra.blockchain;

public class CoinSUConvert {
    public static long parse(String s) {
        char lastChar = s.charAt(s.length()-1);
        if(lastChar >= '0' && lastChar <= '9') return Long.parseLong(s);
        int dotIndex = s.indexOf('.');
        long bDot, aDot;
        if(dotIndex == -1) {
            dotIndex = s.length() - 1;
            aDot = 0;
        } else {
            aDot = Long.parseLong(s.substring(dotIndex+1, s.length()-1));
        }
        bDot = Long.parseLong(s.substring(0, dotIndex));
        long pow=1;
        switch (lastChar) {
            case 'c':
                pow=1000000000000L;
                break;
            case 'm':
                pow=1000000000;
                break;
            case 'u':
                pow=1000000;
                break;
            case 'n':
                pow=1000;
                break;
        }
        return bDot*pow+(aDot*pow)/100;
    }

    public static String present(long l) {
        if(l>1000000000000L) {
            return l/1000000000000L+"."+l/10000000000L%100+"c";
        }
        if(l>1000000000) {
            return l/1000000000+"."+l/10000000%100+"m";
        }
        if(l>1000000) {
            return l/1000000+"."+l/10000%100+"u";
        }
        if(l>1000) {
            return l/1000+"."+l/10%100+"n";
        }
        return String.valueOf(l);
    }
}

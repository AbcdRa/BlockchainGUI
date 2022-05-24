package abcdra.blockchain;

public class Configuration {
    public static long INIT_COINBASE = 70368744177664l;
    public static long COINBASE_REDUCE_HEIGHT = 24000;
    public static int INIT_DIFF = 20;
    public static long DIFF_RECALCULATE_HEIGHT = 1000;
    public static long AVERAGE_TIME_PER_BLOCK = 5*60*1000;
    public static String GENESIS_ADDRESS = "xht8ffw8KU2NSn1djJWaXdPSVSkXogLohvQNoMjsay4=";

    public static int FORCE_FORK_LENGTH = 5;

}

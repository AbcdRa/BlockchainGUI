package abcdra.blockchain;

public class Configuration {
    public static long INIT_COINBASE = 70368744177664l;
    public static long COINBASE_REDUCE_HEIGHT = 24000;
    public static int INIT_DIFF = 20;
    public static long DIFF_RECALCULATE_HEIGHT = 1000;
    public static long AVERAGE_TIME_PER_BLOCK = 5*60*1000;
    public static String DEFAULT_BLOCKCHAIN_PATH = "D:\\Projects\\Java\\Blockchain\\src\\blockchain\\blockchain";
    public static String DEFAULT_MEMPOOL_PATH = "D:\\Projects\\Java\\Blockchain\\src\\blockchain\\mempool";
    public static String DEFAULT_NODES_IP = "D:\\Projects\\Java\\Blockchain\\src\\blockchain\\otherNodeIp.ini";


}

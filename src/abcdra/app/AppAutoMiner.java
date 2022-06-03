package abcdra.app;

public class AppAutoMiner extends Thread{
    AppMiner miner;
    boolean isWork;
    public AppAutoMiner(AppMiner miner) {
        this.miner = miner;
        isWork = false;
    }

    @Override
    public void run() {
        while (isWork) {
            miner.loadMempool();
            miner.addAllFromMempool();
            miner.mineBlock(false);
            if(!miner.app.lResponse.getText().equals("Added")) isWork = false;
        }
    }
}

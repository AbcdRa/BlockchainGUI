package abcdra.transaction;

public class TxOutput implements TxPut {
    public String address;
    public long amount;

    public TxOutput() {

    }

    public TxOutput(String address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return address + ":" + amount;
    }
}

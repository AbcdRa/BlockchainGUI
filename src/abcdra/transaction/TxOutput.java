package abcdra.transaction;

public class TxOutput implements TxPut {
    public String address;
    public long amount;

    public TxOutput() {

    }

    public boolean equals(TxOutput oth) {
        return address.equals(oth.address) && (amount == oth.amount);
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

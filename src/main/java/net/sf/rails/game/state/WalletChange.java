package net.sf.rails.game.state;

public final class WalletChange<T extends Countable> extends Change {

    private final Wallet<T> wallet;
    private final T item;
    private final int amount;
    
    WalletChange(Wallet<T> wallet, T item, int amount) {
        this.wallet = wallet;
        this.item = item;
        this.amount = amount;
        super.init(wallet);
    }
    
    @Override 
    void execute() {
        wallet.change(item, amount);
    }

    @Override 
    void undo() {
        wallet.change(item, -amount);
    }

    @Override
    public 
    Wallet<T> getState() {
        return wallet;
    }
    
    @Override
    public String toString() {
        return "Change for " + wallet + ": " + amount + " of " + item ;
    }
}

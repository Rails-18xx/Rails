package rails.game.state;

final class WalletChange<T extends CountableItem> extends Change {

    private final Wallet<T> in;
    private final Wallet<T> out;
    private final T item;
    private final int amount;
    
    WalletChange(Wallet<T> in, Wallet<T> out, T item, int amount) {
        this.in = in;
        this.out = out;
        this.item = item;
        this.amount = amount;
        super.init(in);
    }
    
    @Override void execute() {
        in.change(item, amount);
        if (out != null) {
            out.change(item, - amount);
        }
    }

    @Override void undo() {
        in.change(item, - amount);
        if (out != null) {
            out.change(item, amount);
        }
    }

    @Override Wallet<T> getState() {
        return in;
    }
    
    @Override
    public String toString() {
        if (out == null) {
            return "Change for " + in + ": Set " + item + " to " + amount;
        } else {
            return "Change for " + in + ": Add " + amount + " of " + item + " from " + out;
        }
    }
}

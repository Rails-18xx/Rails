package rails.game.state;

final class WalletChange<T extends CountableItem> extends Change {

    private final Wallet<T> in;
    private final Wallet<T> out;
    private final T item;
    private final int amount;
    
    WalletChange(Wallet<T> in, Wallet<T> out, T item, int amount) {
        super(in);
        this.in = in;
        this.out = out;
        this.item = item;
        this.amount = amount;
    }
    
    @Override
    public void execute() {
        in.change(item, amount);
        if (out != null) {
            out.change(item, - amount);
        }
    }

    @Override
    public void undo() {
        in.change(item, - amount);
        if (out != null) {
            out.change(item, amount);
        }
    }

    @Override
    public Wallet<T> getState() {
        return in;
    }
    
    @Override
    public String toString() {
        if (out == null) {
            return "WalletChange: Sets " + amount + " of " + item + " in wallet " + in.getId();
        } else {
            return "WalletChange: Moves " + amount + " of " + item + " from wallet " + out.getId() + " to wallet " + in.getId();
        }
    }
}

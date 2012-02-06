package rails.game.state;

final class WalletChange<T extends CountableItem> implements Change {

    private final Wallet<T> in;
    private final Wallet<T> out;
    private final T item;
    private final int amount;
    
    
    WalletChange(Wallet<T> in, Wallet<T> out, T item, int amount) {
        this.in = in;
        this.out = out;
        this.item = item;
        this.amount = amount;
        
        ChangeStack.add(this);
    }
    
    
    public void execute() {
        in.change(item, amount);
        if (out != null) {
            out.change(item, - amount);
        }
    }

    public void undo() {
        in.change(item, - amount);
        if (out != null) {
            out.change(item, amount);
        }
    }

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

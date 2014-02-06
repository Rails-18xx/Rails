package net.sf.rails.game.state;

public abstract class Wallet<T extends Countable> extends State {

    private final Class<T> type;
    
    /**
     * Creation of a wallet
     * @param parent owner of the wallet
     * @param id identifier of the wallet
     * @param type type of items stored in the wallet
     */
    protected Wallet(Owner parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        getWalletManager().addWallet(this);
    }

    protected WalletManager getWalletManager() {
        return getStateManager().getWalletManager();
    }

    /**
     * @return the owner of the wallet
     */
    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }
    
    /**
     * @param item for which the value is retrieved
     * @return the current amount of the item inside the wallet
     */
    public abstract int value(T item);

    /**
     * @return total value of all items
     */
    public abstract int value();
    
    
    protected Class<T> getType() {
        return type;
    }

    abstract void change(T item, int value);
    
}

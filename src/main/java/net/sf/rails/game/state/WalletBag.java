package net.sf.rails.game.state;

import com.google.common.base.Preconditions;

/**
 * A WalletBag allows the storage of only one item of the specified class
 */

public class WalletBag<T extends Countable> extends Wallet<T> {
    
    private final T item;
    private int amount = 0;
    
    private WalletBag(Owner parent, String id, Class<T> type, T item) {
        super(parent, id, type);
        this.item = item;
    }
    
    /**
     * Creates an empty WalletBag
     */
    public static <T extends Countable> WalletBag<T> create(Owner parent, String id, Class<T> type, T item){
        return new WalletBag<T>(parent, id, type, item);
    }
    
    /**
     * @param item for which the value is retrieved
     * @return the current amount of the item inside the wallet
     */
    @Override
    public int value(T item) {
        Preconditions.checkArgument(item == this.item, "WalletBag only accepts item " + this.item);
        return amount;
    }

    @Override
    public int value() {
        return amount;
    }
    
    @Override
    void change (T item, int value) {
        amount += value;
    }

    @Override
    public String toText() {
        return Integer.toString(amount);
    }

}

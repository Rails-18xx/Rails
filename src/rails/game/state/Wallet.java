package rails.game.state;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;

/**
 * A wallet allows the storage of CountableItem(s)
 * 
 * Remark: It is a wrapper around a Multiset, thus it does not support negative 
 * numbers.
 * 
 * @author freystef
 */

public final class Wallet<T extends CountableItem> extends State {

    private final HashMultiset<T> wallet = HashMultiset.create();

    private Wallet() {}
    
    /**
     * Creates an empty Wallet
     */
    public static <T extends CountableItem> Wallet<T> create(String id){
        return new Wallet<T>();
    }

    /**
     * Sets an item to the wallet to the amount given (only if there is no amount defined yet) 
     * @param item to set
     * @param amount initial (has to be positive)
     * @return false if portfolio already contains the item (=> no change), otherwise true

     * @exception IllegalArgumentException if amount is negative 
     */
    public boolean initialSet(T item, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Wallet amounts have to be positive");
        if (wallet.contains(item)) return false;
        
        new WalletChange<T>(this, null, item, amount);
        return true;
    }
    
    /**
     * Adds a specific amount to the specified item
     * @param item to change
     * @param amount initial (has to be positive)
     * @param source the wallet from which the amount is moved

     * @exception IllegalArgumentException if amount is negative 
     * @exception ArithmeticException if wallet which is used as source does not contain at least the amount 
     */
    
    public void moveInto(T item, int amount, Wallet<T> source) {
        if (amount < 0) throw new IllegalArgumentException("Wallet amounts have to be positive");
        
        if (amount > source.count(item)) throw new ArithmeticException("Source wallet does not contain required amount");
        
        new WalletChange<T>(this, source, item, amount);
    }
    
    /**
     * Adds one unit of the specified item
     * @param item to change
     * @param from the wallet from which the unit is taken
     */
    public void moveInto(T item, Wallet<T> from) {
        moveInto(item, 1, from);
    }
    
    
    /**
     * @param item to count
     * @return the current number of the specified amount in the wallet
     */
    public int count(T item) {
        return wallet.count(item);
    }

    /**
     * @return a Multiset view of the Wallet
     */
    public ImmutableMultiset<T> view() {
        return ImmutableMultiset.copyOf(wallet);
    }
    
    
    void change (T item, int value) {
        wallet.add(item, value);
    }
    
}

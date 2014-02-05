package rails.game.state;

import java.util.TreeMap;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

/**
 * A wallet allows the storage of CountableItem(s)
 * 
 * Advantage:
 * Allows the storage of several objects from the same CountableItems class
 */

public class WalletSet<T extends Countable> extends Wallet<T> {

    private final TreeMap<T, Integer> wallet = Maps.newTreeMap();

    private WalletSet(Owner parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    /**
     * Creates an empty WalletSet
     */
    public static <T extends Countable> WalletSet<T> create(Owner parent, String id, Class<T> type){
        return new WalletSet<T>(parent, id, type);
    }
    
    @Override
    public int value(T item) {
        if (wallet.containsKey(item)) {
            return wallet.get(item);
        } else {
            return 0;
        }
    }
    
    
    @Override
    public int value() {
        int sum = 0;
        for (int v:wallet.values()) {
            sum += v;
        }
        return sum;
    }

    /**
     * @return a view of the Wallet
     */
    public ImmutableSortedMap<T, Integer> view() {
        return ImmutableSortedMap.copyOf(wallet);
    }
    
    @Override
    void change (T item, int value) {
        wallet.put(item, value(item) + value);
    }
    
    @Override
    public String toText() {
        return wallet.toString();
    }
    
}

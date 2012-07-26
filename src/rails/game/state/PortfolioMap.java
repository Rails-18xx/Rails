package rails.game.state;

import java.util.Iterator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

/**
 * PortfolioMap is an implementation of a portfolio based on a HashMultimap
 *
 * @param <K> type of the keys that are used to structure the portfolio
 * @param <T> type of Ownable (items) stored inside the portfolio
 * Remark: T has to extend Typable<K> to inform the portfolio about its type 
 */

public final class PortfolioMap<K, T extends Ownable & Typable<K>> extends Portfolio<T> {

    private final HashMultimap<K, T> portfolio = HashMultimap.create();

    private PortfolioMap(PortfolioHolder parent, String id, Class<T> type) {
        super(parent, id, type);
    }

    private PortfolioMap(Owner parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    public static <K, T extends Ownable & Typable<K>> PortfolioMap<K, T> create(PortfolioHolder parent, String id, Class<T> type) {
        return new PortfolioMap<K,T>(parent, id, type);
    }

    public static <K, T extends Ownable & Typable<K>> PortfolioMap<K,T> create(Owner parent, String id, Class<T> type) {
        return new PortfolioMap<K,T>(parent, id, type);
    }

    public boolean moveInto(T item) {
        if (portfolio.containsValue(item)) return false;
        item.moveTo(getOwner());
        return true;
    }

    public boolean containsItem(T item) {
        return portfolio.containsValue(item);
    }

    public ImmutableSet<T> items() {
        return ImmutableSet.copyOf(portfolio.values());
    }

    public int size() {
        return portfolio.size();
    }
    
    public boolean isEmpty() {
        return portfolio.isEmpty();
    }
    
    /**
     * @param key that is checked if there are items stored for
     * @return true if there a items stored under that key, false otherwise
     */
    public boolean containsKey(K key) {
        return portfolio.containsKey(key);
    }

    /**
     * @param key that defines the specific for which the portfolio members get returned
     * @return all items for the key contained in the portfolio
     */
    public ImmutableSet<T> getItems(K key) {
        return ImmutableSet.copyOf(portfolio.get(key));
    }

    /**
     * @return a SetMultimap view of the Portfolio
     */
    public ImmutableSetMultimap<K, T> view() {
        return ImmutableSetMultimap.copyOf(portfolio);
    }

    void change(T item, boolean intoPortfolio) {
        if (intoPortfolio) {
            portfolio.put(item.getType(), item);
        } else {
            portfolio.remove(item.getType(), item);
        }
    }

    public Iterator<T> iterator() {
        return ImmutableSet.copyOf(portfolio.values()).iterator();
    }
    
    @Override
    public String observerText() {
        return portfolio.toString();
    }
}

    

package net.sf.rails.game.state;

import java.util.Iterator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.TreeMultimap;

/**
 * PortfolioMap is an implementation of a portfolio based on a SortedMultimap
 *
 * @param <K> type of the keys that are used to structure the portfolio
 * @param <T> type of Ownable (items) stored inside the portfolio
 * Remark: T has to extend Typable<K> to inform the portfolio about its type 
 */

public class PortfolioMap<K extends Comparable<K>, T extends Ownable & Typable<K>> extends Portfolio<T> {

    private final TreeMultimap<K, T> portfolio = TreeMultimap.create();

    private PortfolioMap(Owner parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    public static <K extends Comparable<K>, T extends Ownable & Typable<K>> PortfolioMap<K,T> create(Owner parent, String id, Class<T> type) {
        return new PortfolioMap<K,T>(parent, id, type);
    }

    @Override
    public boolean moveInto(T item) {
        if (portfolio.containsValue(item)) return false;
        item.moveTo(getParent());
        return true;
    }

    @Override
    public boolean containsItem(T item) {
        return portfolio.containsValue(item);
    }

    @Override
    public ImmutableSortedSet<T> items() {
        return ImmutableSortedSet.copyOf(portfolio.values());
    }
    
    @Override
    public int size() {
        return portfolio.size();
    }
    
    @Override
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
     * Returns the set of keys, each appearing once 
     * @return collection of distinct keys
     */
    public ImmutableSortedSet<K> keySet() {
        return ImmutableSortedSet.copyOf(portfolio.keySet());
    }

    /**
     * @param key that defines the specific for which the portfolio members get returned
     * @return all items for the key contained in the portfolio
     */
    public ImmutableSortedSet<T> items(K key) {
        return ImmutableSortedSet.copyOf(portfolio.get(key));
    }

    /**
     * @return a SetMultimap view of the Portfolio
     */
    public ImmutableSetMultimap<K, T> view() {
        return ImmutableSetMultimap.copyOf(portfolio);
    }

    @Override
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
    public String toText() {
        return portfolio.toString();
    }
}

    

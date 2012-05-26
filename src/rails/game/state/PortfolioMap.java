package rails.game.state;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

public final class PortfolioMap<T extends Ownable<T>> extends Portfolio<T> {

    private ArrayListMultimap<Item, T> portfolio = ArrayListMultimap.create();

    // default constructor
    
    public static <T extends Ownable<T>> PortfolioMap<T> create() {
        return new PortfolioMap<T>();
    }

    public boolean initialAdd(T item) {
        if (portfolio.containsValue(item)) return false;
        new PortfolioChange<T>(this, null, item);
        return true;
    }

    public boolean moveInto(T item) {
        if (portfolio.containsValue(item)) return false;
        new PortfolioChange<T>(this, item.getPortfolio(), item);
        return true;
    }

    public boolean containsItem(T item) {
        return portfolio.containsValue(item);
    }

    public ImmutableList<T> items() {
        return ImmutableList.copyOf(portfolio.values());
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
    public boolean containsKey(Item key) {
        return portfolio.containsKey(key);
    }

    /**
     * @param key that defines the specific for which the portfolio members get returned
     * @return all items for the key contained in the portfolio
     */
    public ImmutableList<T> getItems(Item key) {
        return ImmutableList.copyOf(portfolio.get(key));
    }

    /**
     * @return a ListMultimap view of the Portfolio
     */
    public ImmutableListMultimap<Item, T> view() {
        return ImmutableListMultimap.copyOf(portfolio);
    }

    /**
     * @return a Map view of the Portfolio
     */
    public ImmutableMap<Item, Collection<T>> viewAsMap() {
        return ImmutableMap.copyOf(portfolio.asMap());
    }

    void change(T item, boolean intoPortfolio) {
        if (intoPortfolio) {
            portfolio.put(item.getParent(), item);
            item.setPortfolio(this);
        } else {
            portfolio.remove(item.getParent(), item);
        }
    }

    public Iterator<T> iterator() {
        return portfolio.values().iterator();
    }
    
}

    

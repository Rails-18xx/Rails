package rails.game.state;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * PortfolioSet is an implementation of a Portfolio that is based on a (Hash)Set

 * @param <T> the type of Ownable (items) stored inside the portfolio
 */

public final class PortfolioSet<T extends Ownable> extends Portfolio<T> {

    private final Set<T> portfolio = Sets.newHashSet();
    
    private PortfolioSet(PortfolioHolder parent, String id, Class<T> type) {
        super(parent, id, type);
    }

    private PortfolioSet(Owner parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    public static <T extends Ownable> PortfolioSet<T> create(PortfolioHolder parent, String id, Class<T> type) {
        return new PortfolioSet<T>(parent, id, type);
    }

    public static <T extends Ownable> PortfolioSet<T> create(Owner parent, String id, Class<T> type) {
        return new PortfolioSet<T>(parent, id, type);
    }

    public boolean moveInto(T item) {
        if (portfolio.contains(item)) return false;
        item.moveTo(getOwner());
        return true;
    }

    public boolean containsItem(T item) {
        return portfolio.contains(item);
    }

    public ImmutableSet<T> items() {
        return ImmutableSet.copyOf(portfolio);
    }
    
    public int size() {
        return portfolio.size();
    }
    
    public boolean isEmpty() {
        return portfolio.isEmpty();
    }

    void change(T item, boolean intoPortfolio) {
        if (intoPortfolio) {
            portfolio.add(item);
        } else {
            portfolio.remove(item);
        }
    }
    
    public Iterator<T> iterator() {
        return ImmutableSet.copyOf(portfolio).iterator();
    }

    @Override
    public String observerText() {
        return portfolio.toString();
    }
    
}

    
    
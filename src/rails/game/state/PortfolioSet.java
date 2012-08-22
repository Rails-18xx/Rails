package rails.game.state;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

/**
 * PortfolioSet is an implementation of a Portfolio that is based on a SortedSet (TreeSet)

 * @param <T> the type of Ownable (items) stored inside the portfolio
 */

public final class PortfolioSet<T extends Ownable> extends Portfolio<T> {

    private final SortedSet<T> portfolio = Sets.newTreeSet();
    
    private PortfolioSet(Owner parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    public static <T extends Ownable> PortfolioSet<T> create(Owner parent, String id, Class<T> type) {
        return new PortfolioSet<T>(parent, id, type);
    }

    @Override
    public boolean moveInto(T item) {
        if (portfolio.contains(item)) return false;
        item.moveTo(getParent());
        return true;
    }

    @Override
    public boolean containsItem(T item) {
        return portfolio.contains(item);
    }

    @Override
    public ImmutableSet<T> items() {
        return ImmutableSet.copyOf(portfolio);
    }
    
    @Override
    public ImmutableSortedSet<T> items(Comparator<T> comparator) {
        return ImmutableSortedSet.copyOf(comparator, portfolio);
    }

    @Override
    public int size() {
        return portfolio.size();
    }
    
    @Override
    public boolean isEmpty() {
        return portfolio.isEmpty();
    }

    @Override
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
    public String toText() {
        return portfolio.toString();
    }
    
}

    
    
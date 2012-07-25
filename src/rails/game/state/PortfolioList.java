package rails.game.state;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public final class PortfolioList<T extends Ownable> extends Portfolio<T> {

    private final Set<T> portfolio = Sets.newHashSet();
    
    private PortfolioList(PortfolioHolder parent, String id, Class<T> type) {
        super(parent, id, type);
    }

    private PortfolioList(Owner parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    public static <T extends Ownable> PortfolioList<T> create(PortfolioHolder parent, String id, Class<T> type) {
        return new PortfolioList<T>(parent, id, type);
    }

    public static <T extends Ownable> PortfolioList<T> create(Owner parent, String id, Class<T> type) {
        return new PortfolioList<T>(parent, id, type);
    }

    public boolean moveInto(T item) {
        if (portfolio.contains(item)) return false;
        item.moveTo(getOwner());
        return true;
    }

    public boolean containsItem(T item) {
        return portfolio.contains(item);
    }

    public ImmutableList<T> items() {
        return ImmutableList.copyOf(portfolio);
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

    
    
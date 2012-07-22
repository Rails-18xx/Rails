package rails.game.state;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public final class PortfolioList<T extends Ownable> extends Portfolio<T> {

    private ArrayList<T> portfolio = Lists.newArrayList();
    
    private PortfolioList(PortfolioHolder parent, String id, Class<T> type) {
        super(parent, id, type);
    }
    
    public static <T extends Ownable> PortfolioList<T> create(PortfolioHolder parent, String id, Class<T> type) {
        return new PortfolioList<T>(parent, id, type);
    }

    public boolean initialAdd(T item) {
        if (portfolio.contains(item)) return false;
        new PortfolioChange<T>(this, null, item);
        return true;
    }

    public boolean moveInto(T item) {
        if (portfolio.contains(item)) return false;
        getPortfolioManager().moveItem(getType(), item, this);
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
        return portfolio.iterator();
    }

    @Override
    public String observerText() {
        return portfolio.toString();
    }
    
}

    
    
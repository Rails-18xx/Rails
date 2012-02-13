package rails.game.state;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public final class PortfolioList<T extends OwnableItem<T>> extends Portfolio<T> {

    private ArrayList<T> portfolio = Lists.newArrayList();
    
    private PortfolioList() {};
    
    public static <T extends OwnableItem<T>> PortfolioList<T> create() {
        return new PortfolioList<T>();
    }
    
    @Override
    public PortfolioList<T> init(Item parent, String id) {
        super.init(parent, id);
        return this;
    }

    public boolean initialAdd(T item) {
        if (portfolio.contains(item)) return false;
        new PortfolioChange<T>(this, null, item);
        return true;
    }

    public boolean moveInto(T item) {
        if (portfolio.contains(item)) return false;
        new PortfolioChange<T>(this, item.getPortfolio(), item);
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
            item.setPortfolio(this);
        } else {
            portfolio.remove(item);
        }
    }
    
    public Iterator<T> iterator() {
        return portfolio.iterator();
    }

    
    
}

    
    
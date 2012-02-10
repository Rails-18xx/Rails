package rails.game.state;

import com.google.common.collect.ImmutableList;

public abstract class Portfolio<T extends OwnableItem<T>> extends State implements Iterable<T> {

    Portfolio(String id){
        super(id);
    }
    
    /**
     * Creates an owned and empty PortfolioList
     */
    public static <T extends OwnableItem<T>> PortfolioList<T> createList(Item parent, String id) {
                return new PortfolioList<T>(id).init(parent);
    }

    /**
     * Creates an owned and empty PortfolioList
     */
    public static <T extends OwnableItem<T>> PortfolioMap<T> createMap(Item parent, String id) {
                return new PortfolioMap<T>(id).init(parent);
    }

    /**
     * Creates an unowned and empty PortfolioList
     * Remark: Still requires a call to the init-method
     */
    public static <T extends OwnableItem<T>> PortfolioList<T> createList(String id) {
        return new PortfolioList<T>(id);
    }

    /**
     * Creates an unowned and empty PortfolioMap
     * Remark: Still requires a call to the init-method
     */
    public static <T extends OwnableItem<T>> PortfolioMap<T> createMap(String id) {
        return new PortfolioMap<T>(id);
    }
    
    /**
     * Moves all items from one portfolio to the other
     */
    public static <T extends OwnableItem<T>> void moveAll(Portfolio<T> from, Portfolio<T> to) {
        for (T item: from.items()) {
            to.moveInto(item);
        }
    }
    
    @Override
    public Portfolio<T> init(Item parent) {
        super.init(parent);
        return this;
    }

    /**
     * Adds an item to the portfolio 
     * @param item to add
     * @return false if portfolio already contains the item, otherwise true
     */
    public abstract boolean initialAdd(T item);

    /**
     * Move a new item to the portfolio 
     * and removes the item from the previous portfolio
     * @param item to move
     * @return false if the portfolio already contains the item, otherwise true
     */
    public abstract boolean moveInto(T item);

    /**
     * @param item that is checked if it is in the portfolio
     * @return true if contained, false otherwise
     */
    public abstract boolean containsItem(T item);

    /** 
     * @return all items contained in the portfolio 
     */
    public abstract ImmutableList<T> items();

    /**
     * @return size of portfolio
     */
    public abstract int size();
    
    /** 
     * @return true if portfolio is empty
     */
    public abstract boolean isEmpty();
    
    abstract void change(T item, boolean intoPortfolio);

}
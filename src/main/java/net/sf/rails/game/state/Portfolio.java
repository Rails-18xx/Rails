package net.sf.rails.game.state;

import com.google.common.collect.ImmutableSortedSet;

public abstract class Portfolio<T extends Ownable> extends Model implements Iterable<T> {

    private final Class<T> type;

    /**
     * Creation of a portfolio
     * @param parent owner of the portfolio
     * @param id identifier of the portfolio
     * @param type type of items stored in the portfolio
     */
    protected Portfolio(Owner parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        getPortfolioManager().addPortfolio(this);
    }
    /**
     * @return the owner of the portfolio
     */
    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }

    protected Class<T> getType() {
        return type;
    }

    // delayed due to initialization issues
    // TODO: Check is this still true?
    protected PortfolioManager getPortfolioManager() {
        return getStateManager().getPortfolioManager();
    }
    
    /**
     * Add a new item to the portfolio and removes the item 
     * from the previous containing portfolio
     * 
     * @param item to add to the portfolio
     * @return false if the portfolio already contains the item, otherwise true
     */
    public abstract boolean add(T item);
    
    /**
     * @param item that is checked if it is in the portfolio
     * @return true if contained, false otherwise
     */
    public abstract boolean containsItem(T item);

    /**
     * @return all items contained in the portfolio
     */
    public abstract ImmutableSortedSet<T> items();

    /**
     * @return size of portfolio
     */
    public abstract int size();

    /**
     * @return true if portfolio is empty
     */
    public abstract boolean isEmpty();
    
    abstract void include(T item);

    abstract void exclude(T item);
    
    /**
     * Moves all items of the portfolio to the new owner
     * @param newOwner
     */
    public void moveAll(Owner newOwner) {
        for (T item : items()) {
            item.moveTo(newOwner);
        }
    }

    /**
     * Moves all items of an iterable object to a new owner
     * @param newOwner
     */
    public static <T extends Ownable> void moveAll(Iterable<T> items,
            Owner newOwner) {
        for (T item : items) {
            item.moveTo(newOwner);
        }
    }
    
    /**
     * Moves all items of a specific type from one owner to the other
     */
    public static <T extends Ownable> void moveAll(Class<T> type, Owner owner, Owner newOwner) {
        // get the portfolios
        //Portfolio<T> pf = owner.getRoot().getStateManager().getPortfolioManager().getPortfolio(type, newOwner);
        Portfolio<T> pf = owner.getRoot().getStateManager().getPortfolioManager().getPortfolio(type, owner);
        // and move items
        if (pf != null) pf.moveAll(newOwner);
    }

}
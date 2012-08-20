package rails.game.state;

import com.google.common.collect.ImmutableSet;

public abstract class Portfolio<T extends Ownable> extends State implements
        Iterable<T> {

    private final Class<T> type;
    private final Owner owner;

    /**
     * Constructor using a PortfolioHolder
     */
    protected Portfolio(PortfolioHolder parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.owner = parent.getParent();
        getPortfolioManager().addPortfolio(this);
    }

    /**
     * Constructor using an Owner
     */
    protected Portfolio(Owner parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.owner = parent;
        getPortfolioManager().addPortfolio(this);
    }

    protected Class<T> getType() {
        return type;
    }

    // delayed due to initialization issues
    protected PortfolioManager getPortfolioManager() {
        return getStateManager().getPortfolioManager();
    }

    public Owner getOwner() {
        return owner;
    }

    /**
     * Move a new item to the portfolio and removes the item from the previous
     * portfolio
     * 
     * @param item to move
     * @return false if the portfolio already contains the item, otherwise true
     */
    // FIXME: Rename that to add
    public abstract boolean moveInto(T item);
    
    /**
     * @param item that is checked if it is in the portfolio
     * @return true if contained, false otherwise
     */
    public abstract boolean containsItem(T item);

    /**
     * @return all items contained in the portfolio
     */
    public abstract ImmutableSet<T> items();

    /**
     * @return size of portfolio
     */
    public abstract int size();

    /**
     * @return true if portfolio is empty
     */
    public abstract boolean isEmpty();

    abstract void change(T item, boolean intoPortfolio);

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
        // get the portfolio
        Portfolio<T> pf = owner.getRoot().getStateManager().getPortfolioManager().getPortfolio(type, newOwner);
        // and move items
        pf.moveAll(newOwner);
    }

}
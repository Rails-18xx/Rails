package rails.game.state;
import com.google.common.collect.ImmutableList;

public abstract class Portfolio<T extends Ownable> extends State implements Iterable<T> {

    private final Class<T> type;
    private final Owner owner;
    
    /**
     * Constructor using a PortfolioHolder
     */
    protected Portfolio(PortfolioHolder parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.owner = parent.getParent();
    }

    /**
     * Constructor using an Owner
     */
    protected Portfolio(Owner parent, String id, Class<T> type) {
        super(parent, id);
        this.type = type;
        this.owner = parent;
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
    
    /**
     * Moves all items from one portfolio to the other
     */
    public static <T extends Ownable> void moveAll(Portfolio<T> from, Portfolio<T> to) {
        for (T item: from.items()) {
            to.moveInto(item);
        }
    }

}
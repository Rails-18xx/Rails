package rails.game.state;

import java.util.Collection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

public final class PortfolioNG<T extends OwnableItem<T>> extends State {

    private final ArrayListMultimap<ItemType, T> portfolio;
    
    private PortfolioNG(String id) {
        super(id);
        portfolio = ArrayListMultimap.create();
    }
    
    /**
     * Creates an owned and empty Portfolio
     */
    public static <T extends OwnableItem<T>> PortfolioNG<T> create(Item parent, String id){
        return new PortfolioNG<T>(id).init(parent);
    }

    /**
     * Creates an unowned and empty Portfolio
     * Remark: Still requires a call to the init-method
     */
    public static <T extends OwnableItem<T>> PortfolioNG<T> create( String id){
        return new PortfolioNG<T>(id);
    }
    
    @Override
    public PortfolioNG<T> init(Item parent){
        super.init(parent);
        return this;
    }
    
    /**
     * Adds an item to the portfolio 
     * @param item to add
     * @return false if portfolio already contains the item, otherwise true
     */
    public boolean initialAdd(T item) {
        if (portfolio.containsValue(item)) return false;
        new PortfolioChange<T>(this, null, item);
        return true;
    }
    
    /**
     * Move a new item to the portfolio 
     * and removes the item from the previous portfolio
     * @param item to move
     * @return false if the portfolio already contains the item, otherwise true
     */
    public boolean moveInto(T item) {
        if (portfolio.containsValue(item)) return false;
        new PortfolioChange<T>(this, item.getPortfolio(), item);
        return true;
    }

    /**
     * @param item that is checked if it is in the portfolio
     * @return true if contained, false otherwise
     */
    public boolean containsItem(T item) {
        return portfolio.containsValue(item);
    }
    
    /**
     * @param type that is checked if there are items stored for
     * @return true if there a items stored under that type, false otherwise
     */
    public boolean containsKey(ItemType type) {
        return portfolio.containsKey(type);
    }
    
    /** 
     * @return all items contained in the portfolio 
     */
    public ImmutableList<T> items() {
        return ImmutableList.copyOf(portfolio.values());
    }
    
    
    /**
     * @param key that defines the specific itemtype for which the portfolio members get returned
     * @return all items of type key contained in the portfolio
     */
    public ImmutableList<T> getItems(ItemType key) {
        return ImmutableList.copyOf(portfolio.get(key));
    }
    
    /**
     * @return a ListMultimap view of the Portfolio
     */
    public ImmutableListMultimap<ItemType, T> view() {
        return ImmutableListMultimap.copyOf(portfolio);
    }
    
    /**
     * @return a Map view of the Portfolio
     */
    public ImmutableMap<ItemType, Collection<T>> viewAsMap() {
        return ImmutableMap.copyOf(portfolio.asMap());
    }
    
    void change (T item, boolean intoPortfolio){
        if (intoPortfolio) {
            portfolio.put(item.getParent(), item);
        } else {
            portfolio.remove(item.getParent(), item);
        }
    }
    
}

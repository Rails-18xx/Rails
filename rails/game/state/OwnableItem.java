package rails.game.state;


public interface OwnableItem<T extends OwnableItem<T>> extends Item {

    /**
     * @return the parent of an ownableItem has to be an ItemType
     */
    public ItemType getParent();
     
    /**
     * @return the current portfolio
     */
    public PortfolioNG<T> getPortfolio();
    
}

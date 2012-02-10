package rails.game.state;

public interface OwnableItem<T extends OwnableItem<T>> extends Item {
    
    /**
     * @return the current portfolio
     */
    public Portfolio<T> getPortfolio();
    
    /**
     * @param p the new Portfolio to set
     */
    public void setPortfolio(Portfolio<T> p) ;
    
}

package rails.game.state;

public abstract class OwnableItem<T extends OwnableItem<T>> extends AbstractItem {
    
    private Portfolio<T> portfolio;
    
    /**
     * @return the current portfolio
     */
    public Portfolio<T> getPortfolio() {
        return portfolio;
    }
    
    /**
     * @param p the new Portfolio to set
     */
    public void setPortfolio(Portfolio<T> p) {
        portfolio = p;
    }
    
}

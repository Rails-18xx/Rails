package rails.game.state;

public interface Ownable<T extends Ownable<T>> extends Item {
    
    /**
     * @return the current portfolio
     */
    public Portfolio<T> getPortfolio();
    
    /**
     * @return the current owner
     */
    public Owner getOwner();
    
    /**
     * @param p the new Portfolio to set
     */
    public void setPortfolio(Portfolio<T> p);

}

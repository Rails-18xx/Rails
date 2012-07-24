package rails.game.state;

/**
 * A PortfolioHolder holds portfolios indirectly for an Owner
 * 
 * An Implementation is the PortfolioModel
 */
public interface PortfolioHolder extends Item {
    
    /**
     * @return the owner of the PortfolioHolder
     */
    public Owner getParent();
    

}

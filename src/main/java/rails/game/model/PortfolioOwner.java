package rails.game.model;

import rails.game.RailsOwner;

/**
 * PortfolioOwner does not hold Portfolios directly, but indirect via PortfolioModel
 */
public interface PortfolioOwner extends RailsOwner {

    public PortfolioModel getPortfolioModel();
    
}

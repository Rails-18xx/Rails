package net.sf.rails.game.model;

import net.sf.rails.game.RailsOwner;

/**
 * PortfolioOwner does not hold Portfolios directly, but indirect via PortfolioModel
 */
public interface PortfolioOwner extends RailsOwner {

    public PortfolioModel getPortfolioModel();
    
}

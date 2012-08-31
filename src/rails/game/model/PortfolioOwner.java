package rails.game.model;

import rails.game.RailsItem;
import rails.game.state.Owner;

/**
 * PortfolioOwner does not hold Portfolios directly, but indirect via PortfolioModel
 */
public interface PortfolioOwner extends Owner, RailsItem {

    public PortfolioModel getPortfolioModel();
    
}

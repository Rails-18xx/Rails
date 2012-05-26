package rails.game.model;

import rails.game.state.Owner;

/**
 * PortfolioOwner does not hold Portfolios directly, but indirect via PortfolioModel
 * @author freystef
 */
public interface PortfolioOwner extends Owner {

    public PortfolioModel getPortfolioModel();
    
}

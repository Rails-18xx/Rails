package net.sf.rails.game.financial;

import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.model.PortfolioOwner;

/**
 * BankPortfolios act as Owner of their owns
 * Used for implementation of the separate Bank identities (IPO, POOL, SCRAPHEAP)
 */
public final class BankPortfolio extends RailsAbstractItem implements PortfolioOwner {
    
    private final PortfolioModel portfolio = PortfolioModel.create(this);
    
    protected BankPortfolio(Bank parent, String id) {
        super (parent, id);
    }
    
    /**
     * @param parent restricted to bank
     */
    public static BankPortfolio create(Bank parent, String id) {
        return new BankPortfolio(parent, id);
    }
    
    public void finishConfiguration() {
        portfolio.finishConfiguration();
    }

    @Override
    public Bank getParent() {
        return (Bank)super.getParent();
    }
    
    // Owner methods
    public PortfolioModel getPortfolioModel() {
        return portfolio;
    }

}

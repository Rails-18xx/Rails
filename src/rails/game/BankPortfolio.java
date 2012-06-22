package rails.game;

import rails.game.model.PortfolioModel;
import rails.game.model.PortfolioOwner;
import rails.game.state.AbstractItem;
import rails.game.state.Item;

/**
 * BankPortfolios
 */

public class BankPortfolio extends AbstractItem implements PortfolioOwner {
    
    private final PortfolioModel portfolio = PortfolioModel.create(this);
    
    protected BankPortfolio(Item parent, String id) {
        super (parent, id);
    }
    
    /**
     * @param parent restricted to bank
     */
    public static BankPortfolio create(Bank parent, String id) {
        return new BankPortfolio(parent, id);
    }

    @Override
    public Bank getParent() {
        return (Bank)super.getParent();
    }
    
    // PortfolioOwner methods
    public PortfolioModel getPortfolioModel() {
        return portfolio;
    }

}

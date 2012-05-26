package rails.game;

import rails.game.model.PortfolioModel;
import rails.game.model.PortfolioOwner;
import rails.game.state.AbstractItem;
import rails.game.state.Item;

/**
 * BankPortfolios
 */

public class BankPortfolio extends AbstractItem implements PortfolioOwner {

    private final PortfolioModel portfolio = PortfolioModel.create();
    
    private BankPortfolio() {}
    
    public static BankPortfolio create() {
        BankPortfolio bp = new BankPortfolio();
        return bp;
    }
    
    /**
     * parent is restricted to Bank
     */
    @Override
    public void init(Item parent, String id) {
        super.checkedInit(parent, id, Bank.class);
        portfolio.init(this, "portfolio");
    }
    
    // PortfolioOwner methods
    public PortfolioModel getPortfolioModel() {
        return portfolio;
    }
    

}

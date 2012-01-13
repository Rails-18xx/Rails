package rails.game.model;

import rails.game.state.Item;

public abstract class PortfolioCashOwner extends PortfolioOwner implements CashOwner {

    private final CashModel cashModel = new CashModel();
    
    public PortfolioCashOwner(Item parent, String id) {
        super(parent, id);
    }
    
    public final CashModel getCashModel() {
       return cashModel;
    }

    public final int getCash() {
        return cashModel.value();
    }

}

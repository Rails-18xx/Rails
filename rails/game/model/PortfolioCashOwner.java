package rails.game.model;

import rails.game.state.Item;

public abstract class PortfolioCashOwner extends PortfolioOwner implements CashOwner {

    private final CashModel cashModel = new CashModel();
    
    public PortfolioCashOwner(String id) {
        super(id);
    }
    
    @Override
    public PortfolioCashOwner init(Item parent){
        super.init(parent);
        cashModel.init(this);
        return this;
    }
    
    public final CashModel getCashModel() {
       return cashModel;
    }

    public final int getCash() {
        return cashModel.value();
    }

}

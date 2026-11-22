package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;

public class CompanyBuyOpenMarketShare_1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    
    private final String companyId;
    private final int price;

public CompanyBuyOpenMarketShare_1817(net.sf.rails.game.RailsRoot root, String companyId, int price) {
super(root);
this.companyId = companyId;
this.price = price;
}

    public String getCompanyId() {
        return companyId;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return companyId + " buys 1 share from Open Market";
    }
}
package net.sf.rails.game.specific._1817.action;

import net.sf.rails.game.RailsRoot;
import rails.game.action.PossibleORAction;

public class LiquidateCompany_1817 extends PossibleORAction {
    private static final long serialVersionUID = 1L;
    private final String companyName;
    private final int shortfall;

    public LiquidateCompany_1817(RailsRoot root, String companyName, int shortfall) {
        super(root);
        this.companyName = companyName;
        this.shortfall = shortfall;
    }

    public String getCompanyName() { return companyName; }
    public int getShortfall() { return shortfall; }

    @Override
    public String getButtonLabel() {
        return "Liquidate (" + companyName + ")";
    }
}
package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class LayCoalToken_1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;
    private final String hexId;

    public LayCoalToken_1817(RailsRoot root, String companyId, String hexId) {
        super(root);
        this.companyId = companyId;
        this.hexId = hexId;
    }

    public String getCompanyId() { return companyId; }
    public String getHexId() { return hexId; }

    @Override
    public String toString() {
        return "Place Coal Mine token on " + hexId + " (Refunds $15)";
    }
}
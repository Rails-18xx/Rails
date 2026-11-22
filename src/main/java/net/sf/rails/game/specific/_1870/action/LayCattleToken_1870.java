// --- START FIX ---
package net.sf.rails.game.specific._1870.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class LayCattleToken_1870 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;
    private String hexId;

    public LayCattleToken_1870(RailsRoot root, String companyId, String hexId) {
        super(root);
        this.companyId = companyId;
        this.hexId = hexId;
    }

    public String getCompanyId() { return companyId; }
    public String getHexId() { return hexId; }
    public void setHexId(String hexId) { this.hexId = hexId; }

    @Override
    public String toString() {
        return "Place Cattle token" + (hexId != null ? " on " + hexId : "");
    }


    @Override
    public String getButtonLabel() {
        return toString();
    }
}

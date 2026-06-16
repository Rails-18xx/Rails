package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class LayBridgeToken_1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;
    private final String hexId;

    public LayBridgeToken_1817(RailsRoot root, String companyId, String hexId) {
        super(root);
        this.companyId = companyId;
        this.hexId = hexId;
    }

    public String getCompanyId() { return companyId; }
    public String getHexId() { return hexId; }

    // --- START FIX ---
    @Override
    public String getButtonLabel() {
        return "Lay Bridge (" + hexId + ")";
    }
    // --- END FIX ---
}
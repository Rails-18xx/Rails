package net.sf.rails.game.specific._1870.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class LayGulfToken_1870 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;
    private String hexId;
    private final boolean isOpen;

    public LayGulfToken_1870(RailsRoot root, String companyId, String hexId, boolean isOpen) {
        super(root);
        this.companyId = companyId;
        this.hexId = hexId;
        this.isOpen = isOpen;
    }

    public String getCompanyId() { return companyId; }
    public String getHexId() { return hexId; }
    public void setHexId(String hexId) { this.hexId = hexId; }
    public boolean isOpen() { return isOpen; }

public String toString() {
        return (isOpen ? "Lay open port" : "Lay closed port") + (hexId != null ? " (" + hexId + ")" : "");
    }

    @Override
    public String getButtonLabel() {
        return toString();
    }
}
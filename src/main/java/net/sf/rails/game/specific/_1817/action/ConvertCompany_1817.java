package net.sf.rails.game.specific._1817.action;

import java.awt.Color;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.Owner;
import rails.game.action.PossibleAction;
import rails.game.action.GuiTargetedAction;
import net.sf.rails.game.Player;

public class ConvertCompany_1817 extends PossibleAction implements GuiTargetedAction {

    private static final long serialVersionUID = 1L;
    private final String companyId;
    private transient PublicCompany companyCache;

    public ConvertCompany_1817(PublicCompany company) {
        super(company.getRoot());
        this.companyId = company.getId();
    }

    public PublicCompany getCompany() { 
        if (companyCache == null) {
            companyCache = getRoot().getCompanyManager().getPublicCompany(companyId);
        }
        return companyCache;
    }

    @Override
    public Color getButtonColor() { 
        return new Color(144, 238, 144); // LightGreen for Conversion
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return getButtonColor();
    }

    @Override
    public Color getHighlightBorderColor() {
        return new Color(34, 139, 34); // ForestGreen
    }

    @Override
    public Color getHighlightTextColor() {
        return Color.BLACK;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;
        ConvertCompany_1817 action = (ConvertCompany_1817) pa;
        return this.companyId.equals(action.companyId);
    }

    @Override
    public Owner getActor() { 
        return getCompany().getPresident(); 
    }

    @Override
    public String getGroupLabel() { 
        return getCompany().getId(); 
    }

    @Override
    public String getButtonLabel() { 
        return "Convert " + companyId; 
    }

    @Override
    public String toString() {
        Player president = getCompany().getPresident();
        String actorName = (president != null) ? president.getName() : "System";
        return actorName + ": convert " + companyId + "?";
    }
}
package net.sf.rails.game.specific._1817;

import java.awt.Color;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.Owner;
import rails.game.action.PossibleAction;
import rails.game.action.GuiTargetedAction;
import net.sf.rails.game.Player;

public class MergeCompanies_1817 extends PossibleAction implements GuiTargetedAction {

    private static final long serialVersionUID = 1L;
    private final String initiatingCompanyId;
    private final String targetCompanyId;
    private transient PublicCompany initiatingCompanyCache;
    private transient PublicCompany targetCompanyCache;

    public MergeCompanies_1817(PublicCompany initiatingCompany, PublicCompany targetCompany) {
        super(initiatingCompany.getRoot());
        this.initiatingCompanyId = initiatingCompany.getId();
        this.targetCompanyId = targetCompany.getId();
    }

    public PublicCompany getInitiatingCompany() { 
        if (initiatingCompanyCache == null) {
            initiatingCompanyCache = getRoot().getCompanyManager().getPublicCompany(initiatingCompanyId);
        }
        return initiatingCompanyCache;
    }

    public PublicCompany getTargetCompany() { 
        if (targetCompanyCache == null) {
            targetCompanyCache = getRoot().getCompanyManager().getPublicCompany(targetCompanyId);
        }
        return targetCompanyCache;
    }



    @Override
    public Color getButtonColor() { 
        return new Color(173, 216, 230); // LightBlue
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return getButtonColor();
    }

    @Override
    public Color getHighlightBorderColor() {
        return Color.BLUE;
    }

    @Override
    public Color getHighlightTextColor() {
        return Color.BLACK;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;
        MergeCompanies_1817 action = (MergeCompanies_1817) pa;
        return this.initiatingCompanyId.equals(action.initiatingCompanyId) &&
               this.targetCompanyId.equals(action.targetCompanyId);
    }


@Override
    public Owner getActor() { return getInitiatingCompany().getPresident(); }

    @Override
    public String getGroupLabel() { return getInitiatingCompany().getId(); }

    @Override
    public String getButtonLabel() { return "merge " + initiatingCompanyId + " with " + targetCompanyId; }

    @Override
    public String toString() {
        Player president = getInitiatingCompany().getPresident();
        String actorName = (president != null) ? president.getName() : "System";
        return actorName + ": merge " + initiatingCompanyId + " and " + targetCompanyId + "?";
    }

}
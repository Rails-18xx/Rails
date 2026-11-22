// File: ExchangeForPrussianShare.java
package rails.game.specific._1835;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyManager;
import com.google.common.base.Objects;
import rails.game.action.GuiTargetedAction;
import rails.game.action.PossibleAction;
import net.sf.rails.game.state.Owner;

public class ExchangeForPrussianShare extends PossibleAction implements GuiTargetedAction {

    private static final long serialVersionUID = 1L;
    private transient Company companyToExchange;
    private String companyToExchangeName;

    public ExchangeForPrussianShare(Company companyToExchange) {
        super(companyToExchange.getRoot());
        this.companyToExchange = companyToExchange;
        this.companyToExchangeName = companyToExchange.getId();
    }

    public Company getCompanyToExchange() {
        return companyToExchange;
    }


    @Override
    public Owner getActor() { 
        if (companyToExchange instanceof net.sf.rails.game.PrivateCompany) {
            return ((net.sf.rails.game.PrivateCompany) companyToExchange).getOwner();
        } else if (companyToExchange instanceof net.sf.rails.game.PublicCompany) {
            return ((net.sf.rails.game.PublicCompany) companyToExchange).getPresident();
        }
        return companyToExchange; 
    }

    @Override
    public Object getTarget() {
        return companyToExchange;
    }

    @Override
    public String getGroupLabel() { 
        return "Merge with Prussia?"; 
    }


    @Override
    public String getButtonLabel() { return "Exchange " + companyToExchange.getId(); }

    // --- START FIX ---
    // UNIFIED "FORMATION" SIGNATURE (Matches StartPrussian)

    @Override
    public Color getButtonColor() { 
        return new Color(255, 140, 0); // Vibrant Orange
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return new Color(255, 140, 0); // Vibrant Orange
    }

    @Override
    public Color getHighlightBorderColor() {
        return Color.BLACK;
    }

    @Override
    public Color getHighlightTextColor() {
        return Color.WHITE;
    }


    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;
        ExchangeForPrussianShare action = (ExchangeForPrussianShare) pa;
        return Objects.equal(this.companyToExchangeName, action.companyToExchangeName);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        CompanyManager cmgr = getCompanyManager();
        companyToExchange = cmgr.getPublicCompany(companyToExchangeName);
        if (companyToExchange == null) {
            companyToExchange = cmgr.getPrivateCompany(companyToExchangeName);
        }
    }
}
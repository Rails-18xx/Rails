// File: StartPrussian.java
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

public class StartPrussian extends PossibleAction implements GuiTargetedAction {
    
    private static final long serialVersionUID = 1L;
    private transient Company companyToFold;
    private String companyToFoldName;

    public StartPrussian(Company companyToFold) {
        super(companyToFold.getRoot());
        this.companyToFold = companyToFold;
        this.companyToFoldName = companyToFold.getId();
    }

    public Company getCompanyToFold() {
        return companyToFold;
    }

  @Override
    public Owner getActor() { 
        if (companyToFold instanceof net.sf.rails.game.PrivateCompany) {
            return ((net.sf.rails.game.PrivateCompany) companyToFold).getOwner();
        } else if (companyToFold instanceof net.sf.rails.game.PublicCompany) {
            return ((net.sf.rails.game.PublicCompany) companyToFold).getPresident();
        }
        return companyToFold; 
    }


    @Override
    public String getGroupLabel() { 
        return "Open Prussia?"; 
    }
    

    @Override
    public Object getTarget() {
        return companyToFold;
    }

    @Override
    public String getButtonLabel() { return "Start (Fold " + companyToFold.getId() + ")"; }

// UNIFIED "FORMATION" SIGNATURE (Pale Green / Forest Green)
    
    @Override
    public Color getButtonColor() { 
        return new Color(152, 251, 152); // PaleGreen 
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return new Color(152, 251, 152); // PaleGreen
    }

    @Override
    public Color getHighlightBorderColor() {
        return new Color(34, 139, 34); // ForestGreen
    }

    @Override
    public Color getHighlightTextColor() {
        return Color.BLACK;
    }
    // --- END FIX ---
    
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;
        StartPrussian action = (StartPrussian) pa;
        return Objects.equal(this.companyToFoldName, action.companyToFoldName);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        CompanyManager cmgr = getCompanyManager();
        companyToFold = cmgr.getPublicCompany(companyToFoldName);
        if (companyToFold == null) {
            companyToFold = cmgr.getPrivateCompany(companyToFoldName);
        }
    }
}
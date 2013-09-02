package rails.game.specific._1880;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;

public class CloseInvestor_1880 extends PossibleORAction {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private boolean treasuryToLinkedCompany = false;
    private boolean replaceToken = false;
    
    public CloseInvestor_1880(Investor_1880 investor) {
        this.company = investor;
        this.companyName = investor.getName();
    }
    
    public CloseInvestor_1880() {
        
    }

    public Investor_1880 getInvestor() {
        return (Investor_1880) company;
    }
    
    public boolean getTreasuryToLinkedCompany() {
        return treasuryToLinkedCompany;
    }

    public void setTreasuryToLinkedCompany(boolean treasuryToLinkedCompany) {
        this.treasuryToLinkedCompany = treasuryToLinkedCompany;
    }

    public boolean getReplaceToken() {
        return replaceToken;
    }

    public void setReplaceToken(boolean replaceToken) {
        this.replaceToken = replaceToken;
    }

    
    public String toString() {
        StringBuffer text = new StringBuffer();
        text.append("CloseInvestor_1880:");
        text.append("  Investor " + company.getName());
        text.append(",  TreasuryToLinkedCompany " + treasuryToLinkedCompany);
        text.append(",  ReplaceToken " + replaceToken);
        return text.toString();
    }


    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        if (pa instanceof CloseInvestor_1880) {
            if ((((CloseInvestor_1880) pa).getInvestor() == company) &&
                    (((CloseInvestor_1880) pa).getTreasuryToLinkedCompany() == treasuryToLinkedCompany) &&
                    (((CloseInvestor_1880) pa).getReplaceToken() == replaceToken)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equalsAsAction(PossibleAction pa) {
        if (pa instanceof CloseInvestor_1880) {
            if ((((CloseInvestor_1880) pa).getInvestor() == company) &&
                    (((CloseInvestor_1880) pa).getTreasuryToLinkedCompany() == treasuryToLinkedCompany) &&
                    (((CloseInvestor_1880) pa).getReplaceToken() == replaceToken)) {
                return true;
            }
        }
        return false;
    }    
}

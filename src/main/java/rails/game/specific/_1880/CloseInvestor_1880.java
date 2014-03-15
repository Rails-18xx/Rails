package rails.game.specific._1880;

import com.google.common.base.Objects;

import net.sf.rails.game.specific._1880.Investor_1880;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;

/**
 * @author Michael Alexander
 * 
 * Rails 2.0: Updated equals and toString methods
 */
public class CloseInvestor_1880 extends PossibleORAction {
    
    private static final long serialVersionUID = 1L;
    private boolean treasuryToLinkedCompany = false;
    private boolean replaceToken = false;
    
    public CloseInvestor_1880(Investor_1880 investor) {
        this.company = investor;
        this.companyName = investor.getId();
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

    
    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        // identity always true
        if (pa == this) return true;
        // no further checks required
        return super.equalsAsOption(pa); 
    }

    @Override
    public boolean equalsAsAction(PossibleAction pa) {
        // first check if equal as option
        if (!this.equalsAsOption(pa)) return false;
        
        // check further attributes
        CloseInvestor_1880 action = (CloseInvestor_1880)pa; 
        return Objects.equal(this.treasuryToLinkedCompany, action.treasuryToLinkedCompany)
                && Objects.equal(this.replaceToken, action.replaceToken)
        ;
    }

    @Override
    public String toString() {
        return super.toString()
                + RailsObjects.stringHelper(this)
                .addToStringOnlyActed("TreasuryToLinkedCompany", treasuryToLinkedCompany)
                .addToStringOnlyActed("ReplaceToken", replaceToken)
                .toString()
        ;
    }


}
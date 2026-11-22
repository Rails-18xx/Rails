package net.sf.rails.game.specific._1817;

import net.sf.rails.game.model.BondsModel;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.CompanyManager;

public class BondsModel_1817 extends BondsModel {

    private final RailsRoot root;
    protected final net.sf.rails.game.state.IntegerState currentInterestRate;

    public BondsModel_1817(net.sf.rails.game.RailsItem parent, net.sf.rails.game.RailsItem owner, RailsRoot root) {
        super(parent, owner);
        this.root = root;
        currentInterestRate = net.sf.rails.game.state.IntegerState.create(this, "currentInterestRate", 5);
    }
    
   
    /**
     * Calculates the projected interest rate based on the current number of loans in play.
     */
    public int calculateProjectedInterestRate() {
        int totalLoans = 0;
        CompanyManager cm = root.getCompanyManager();
        if (cm != null) {
            for (PublicCompany c : cm.getAllPublicCompanies()) {
                if (c != null && !c.isClosed()) {
                    totalLoans += c.getNumberOfBonds();
                }
            }
        }
        
        if (totalLoans == 0) return 5;
        // Rate increases by $5 for every 5 loans, starting at loan 1.
return (((totalLoans - 1) / 5) + 1) * 5;
    }

    /**
     * Locks in the interest rate at the start of the operating round.
     */
    public void updateInterestRate() {
        currentInterestRate.set(calculateProjectedInterestRate());
    }
    

    public int getInterestRate() {
        return currentInterestRate.value();
    }
}
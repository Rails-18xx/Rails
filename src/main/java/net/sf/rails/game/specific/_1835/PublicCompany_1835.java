package net.sf.rails.game.specific._1835;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.Owner;

public class PublicCompany_1835 extends PublicCompany {


    public PublicCompany_1835(RailsItem parent, String Id) {
        super(parent, Id);
    }

    @Override
    public boolean isSoldOut() {
        // sold out is only possible for started companies (thus M2 has to been exchanged for PR)  
        if (!hasStarted()) return false;

        for (PublicCertificate cert : certificates.view()) {
            Owner owner = cert.getOwner();
            // check if any shares are in the bank (except unavailable for reserved shares)
            if (owner instanceof BankPortfolio && owner != Bank.getUnavailable(this)) {
                return false;
            }
        }
        return true;
    }
}

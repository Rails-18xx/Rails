package net.sf.rails.game.specific._1835;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;

public class PublicCompany_1835 extends PublicCompany {

    /**
     * If during emergency train buying the president gets bankrupt,
     * and no other player can take the president certificate,
     * the Bank loans money to buy the intended train.
     * The company must withhold until the load has been paid back fully.
     */
    private IntegerState bankLoan = IntegerState.create (
            this, "bankLoan_" + getId(), 0);

    public PublicCompany_1835 (RailsItem parent, String Id) {
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

    public int getBankLoan() {
        return bankLoan.value();
    }

    public void setBankLoan(int bankLoan) {
        this.bankLoan.set(bankLoan);
    }

    public void repayBankLoan (int repayment) {
        this.bankLoan.add (-repayment);
    }

    public boolean hasBankLoan () {
        return getBankLoan() > 0;
    }
}

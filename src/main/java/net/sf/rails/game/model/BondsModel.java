package net.sf.rails.game.model;

import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.state.IntegerState;

/**
 * ShareModel for displaying the share percentages
 */

public class BondsModel extends RailsModel {

    private net.sf.rails.game.state.IntegerState bondsCount;

   protected BondsModel(net.sf.rails.game.RailsItem parent, net.sf.rails.game.RailsItem owner) {
        super(parent, "bondsModel_" + parent.getId() + "_for_" + owner.getId());
        bondsCount = net.sf.rails.game.state.IntegerState.create(parent, owner.getId() + "_bonds");
        bondsCount.addModel(this);
    }

    public static BondsModel create(net.sf.rails.game.RailsItem parent, net.sf.rails.game.RailsItem owner) {
        return new BondsModel(parent, owner);
    }

    /**
     * Calculates the total loans currently held by all active companies in the
     * game.
     */
    public int getTotalLoansTaken() {
        int totalLoans = 0;
        // Navigate through the parent to find the CompanyManager via the RailsRoot
        net.sf.rails.game.CompanyManager cm = getParent().getRoot().getCompanyManager();
        if (cm != null) {
            for (net.sf.rails.game.PublicCompany c : cm.getAllPublicCompanies()) {
                if (c != null && !c.isClosed()) {
                    totalLoans += c.getNumberOfBonds();
                }
            }
        }
        return totalLoans;
    }

    public int getBondsCount() {
        return bondsCount.value();
    }

    public void setBondsCount(int bondsCount) {
        this.bondsCount.set(bondsCount);
    }

    public void addBondsCount(int bondsCount) {
        this.bondsCount.add(bondsCount);
    }

    public void transferTo(int bonds, BondsModel to) {
        this.bondsCount.add(-bonds);
        to.addBondsCount(bonds);
    }

    @Override
    public String toText() {
        if (bondsCount.value() > 0) {
            return bondsCount.toString();
        } else {
            return "";
        }
    }

}

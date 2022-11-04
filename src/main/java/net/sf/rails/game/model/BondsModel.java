package net.sf.rails.game.model;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.state.IntegerState;

/**
 * ShareModel for displaying the share percentages
 */
public final class BondsModel extends RailsModel {

    private IntegerState bondsCount;

    private BondsModel(RailsOwner parent) {
        super(parent, "bondsModel_" + parent.getId());
        bondsCount = IntegerState.create(parent, "_bonds");
        bondsCount.addModel(this);
    }

    public static BondsModel create(RailsOwner parent) {
        return new BondsModel(parent);
    }
    
    @Override
    public PortfolioModel getParent() {
        return (PortfolioModel) super.getParent();
    }

    public int getBondsCount() {
        return bondsCount.value();
    }

    public void setBondsCount(int bondsCount) {
        this.bondsCount.set (bondsCount);
    }

    public void addBondsCount (int bondsCount) {
        this.bondsCount.add(bondsCount);
    }

    public void transferTo (int bonds, BondsModel to) {
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

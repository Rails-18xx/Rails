package net.sf.rails.game.model;

import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.state.IntegerState;

/**
 * ShareModel for displaying the share percentages
 */
public final class BondsModel extends RailsModel {

    private IntegerState bondsCount;

    private BondsModel(RailsOwner parent, RailsOwner owner) {
        super(parent, "bondsModel_" + parent.getId()+"_for_"+owner);
        bondsCount = IntegerState.create(parent, owner+"_bonds");
        bondsCount.addModel(this);
    }

    public static BondsModel create(RailsOwner parent, RailsOwner owner) {
        return new BondsModel(parent, owner);
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

package net.sf.rails.game.model;

import net.sf.rails.game.Bonus;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.ArrayListState;

public class BonusModel extends RailsModel {
    
    private ArrayListState<Bonus> bonuses;
    
    protected BonusModel(RailsItem parent, String id) {
        super(parent, id);
    }

    public static BonusModel create(RailsItem parent, String id){
        return new BonusModel(parent, id);
    }
    
    @Override
    public RailsItem getParent() {
        return (RailsItem)super.getParent();
    }

    public void setBonuses(ArrayListState<Bonus> bonuses) {
        this.bonuses = bonuses;
        bonuses.addModel(this);
    }

    @Override
    public String toText() {
        if (bonuses == null || bonuses.isEmpty()) return "";

        StringBuffer b = new StringBuffer("<html><center>");

        for (Bonus bonus : bonuses.view()) {
            if (b.length() > 14) {
                b.append("<br>");
            }
            b.append(bonus.getIdForView()).append("+").append(Bank.format(getParent(), bonus.getValue()));
        }

        return b.toString();
    }
}

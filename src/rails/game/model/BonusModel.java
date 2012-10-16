package rails.game.model;

import rails.game.Bonus;
import rails.game.Currency;
import rails.game.RailsItem;
import rails.game.state.ArrayListState;
import rails.game.state.Model;

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
            b.append(bonus.getIdForView()).append("+").append(Currency.format(getParent(), bonus.getValue()));
        }

        return b.toString();
    }
}

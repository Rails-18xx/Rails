package rails.game.model;

import rails.game.Bank;
import rails.game.Bonus;
import rails.game.state.ArrayListState;
import rails.game.state.Item;
import rails.game.state.Model;

public class BonusModel extends Model {
    
    private ArrayListState<Bonus> bonuses;
    
    protected BonusModel(Item parent, String id) {
        super(parent, id);
    }

    public static BonusModel create(Item parent, String id){
        return new BonusModel(parent, id);
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
            b.append(bonus.getIdForView()).append("+").append(Bank.format(bonus.getValue()));
        }

        return b.toString();
    }
}

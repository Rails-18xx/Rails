package rails.game.model;

import rails.game.Bank;
import rails.game.Bonus;
import rails.game.state.ArrayListState;
import rails.game.state.Model;

public class BonusModel extends Model {
    
    private ArrayListState<Bonus> bonuses;
    
    private BonusModel() {}
    
    public static BonusModel create(){
        return new BonusModel();
    }

    public void setBonuses(ArrayListState<Bonus> bonuses) {
        this.bonuses = bonuses;
        bonuses.addModel(this);
    }

    @Override
    public String toString() {
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

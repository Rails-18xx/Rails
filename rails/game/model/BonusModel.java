package rails.game.model;

import rails.game.Bank;
import rails.game.Bonus;
import rails.game.state.ArrayListState;
import rails.game.state.Item;

public class BonusModel extends Model {
    
    private ArrayListState<Bonus> bonuses;
    
    /**
     * BonusModel is initialized with default id "BonusModel"
     */
    public BonusModel() {
        super("BonusModel");
    }
    
    /** 
     * Creates an initialized BonusModel
     */
    public BonusModel create(Item parent){
        return new BonusModel().init(parent);
    }
    
    public BonusModel init(Item parent) {
        super.init(parent);
        return this;
    }
    
    public void setBonuses(ArrayListState<Bonus> bonuses) {
        this.bonuses = bonuses;
        bonuses.addModel(this);
    }

    @Override
    protected String getText() {
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

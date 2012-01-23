package rails.game.model;

import rails.game.Bank;
import rails.game.Bonus;
import rails.game.state.ArrayListState;
import rails.game.state.Item;

public class BonusModel extends Model {
    
    public static final String ID = "BonusModel";
    
    private ArrayListState<Bonus> bonuses;
    
    private BonusModel() {
        super(ID);
    }
    
    /** 
     * Creates an owned BonusModel
     * BonusModel is initialized with a default id "BonusModel"
    */
    public static BonusModel create(Item parent){
        return new BonusModel().init(parent);
    }
    
    /** 
     * Creates an unowned BonusModel
     * BonusModel is initialized with a default id "BonusModel"
     * Remark: Still requires a call to the init-method
     */
    public static BonusModel create(){
        return new BonusModel();
    }

    @Override
    public BonusModel init(Item parent) {
        super.init(parent);
        return this;
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

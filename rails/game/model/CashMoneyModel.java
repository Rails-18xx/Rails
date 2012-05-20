package rails.game.model;

import rails.game.state.BooleanState;
import rails.game.state.CountableItem;
import rails.game.state.IntegerState;
import rails.game.state.Item;

/**
 * A MoneyModel that stores the money value inside
 * @author freystef
 */
public final class CashMoneyModel extends MoneyModel implements CountableItem {

    private final IntegerState value;
    private final BooleanState initialised = BooleanState.create();

    private CashMoneyModel(int amount) {
        value = IntegerState.create( amount);
    }
    
    public static CashMoneyModel create(){
        return new CashMoneyModel(0);
    }
    
    public static CashMoneyModel create(int amount){
        return new CashMoneyModel(amount);
    }
    
    @Override
    public void init(Item parent, String id){
        super.init(parent, id);
        value.init(this, "value");
        initialised.init(this, "initialised");
    }

    /**
     * @param amount the new cash amount
     */
    public void set(int amount) {
        if (!initialised.booleanValue()) {
            initialised.set(true);
        }
        value.set(amount); 
    }

    // Countable interface
    public void change(int amount) {
        if (initialised.booleanValue()) {
            value.add(amount);
        } else {
            set(amount);
        }
    }

    // MoneyModel abstracts
    @Override
    public int value() {
        return value.value();
    }

    @Override
    public boolean initialised() {
        return initialised.booleanValue();
    }


}

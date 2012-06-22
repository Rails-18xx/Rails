package rails.game.model;

import rails.game.state.BooleanState;
import rails.game.state.CountableItem;
import rails.game.state.IntegerState;
import rails.game.state.Item;

/**
 * A MoneyModel that stores the money value inside
 * FIXME: Check if initialised is done correctly
 */
public final class CashMoneyModel extends MoneyModel implements CountableItem {

    private final IntegerState value;
    private final BooleanState initialised;

    private CashMoneyModel(Item parent, String id, int amount, boolean init) {
        super(parent, id);
        value = IntegerState.create(this, "value", amount);
        initialised = BooleanState.create(this, "initialised", init);
    }
    
    public static CashMoneyModel create(Item parent, String id, boolean init){
        return new CashMoneyModel(parent, id, 0, init);
    }
    
    public static CashMoneyModel create(Item parent, String id, int amount){
        return new CashMoneyModel(parent, id, amount, true);
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

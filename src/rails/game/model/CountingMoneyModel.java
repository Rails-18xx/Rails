package rails.game.model;

import rails.game.RailsItem;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;

public class CountingMoneyModel extends MoneyModel {

    private final IntegerState value;
    private final BooleanState initialised;

    private CountingMoneyModel(RailsItem parent, String id, int amount, boolean init) {
        super(parent, id, parent.getRoot().getCurrency());
        value = IntegerState.create(this, "counting", amount);
        initialised = BooleanState.create(this, "initialised", init);
    }
    
    public static CountingMoneyModel create(RailsItem parent, String id, boolean init){
        return new CountingMoneyModel(parent, id, 0, init);
    }
    
    public static CountingMoneyModel create(RailsItem parent, String id, int amount){
        return new CountingMoneyModel(parent, id, amount, true);
    }
    
    /**
     * @param amount the new cash amount
     */
    public void set(int amount) {
        if (!initialised.value()) {
            initialised.set(true);
        }
        value.set(amount); 
    }

    // Countable interface
    public void change(int amount) {
        if (initialised.value()) {
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
        return initialised.value();
    }



}

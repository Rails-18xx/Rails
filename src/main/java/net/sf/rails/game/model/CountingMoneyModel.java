package net.sf.rails.game.model;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;

public class CountingMoneyModel extends MoneyModel {

    private final IntegerState value;
    private final BooleanState initialised;

    private CountingMoneyModel(RailsItem parent, String id, int amount, boolean init, Currency currency) {
        super(parent, id, currency);
        value = IntegerState.create(this, "counting", amount);
        initialised = BooleanState.create(this, "initialised", init);
    }
    
    public static CountingMoneyModel create(RailsItem parent, String id, boolean init){
        Currency currency = parent.getRoot().getBank().getCurrency();
        return new CountingMoneyModel(parent, id, 0, init, currency);
    }
    
    public static CountingMoneyModel create(RailsItem parent, String id, int amount){
        Currency currency = parent.getRoot().getBank().getCurrency();
        return new CountingMoneyModel(parent, id, amount, true, currency);
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

package net.sf.rails.game.model;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.Currency;

/**
 * This is MoneyModel that derives it value from a calculation method.
 * TODO: Rewrite all methods implementing the interface
 */
public final class CalculatedMoneyModel extends MoneyModel {

    public interface CalculationMethod {
        public int calculate();
        public boolean initialised();
    }
    
    private final CalculationMethod method;

    private CalculatedMoneyModel(RailsItem parent, String id, CalculationMethod method, Currency currency) {
        super(parent, id, currency);
        this.method = method;
    }

    public static CalculatedMoneyModel create(RailsItem parent, String id, CalculationMethod method) {
        Currency currency = parent.getRoot().getBank().getCurrency();
        return new CalculatedMoneyModel(parent, id, method, currency);
    }
    
    @Override
    public int value() {
        return method.calculate();
    }
   
    @Override
    public boolean initialised() {
        return method.initialised();
    }

}

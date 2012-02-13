package rails.game.model;

import rails.game.state.Item;

/**
 * This is MoneyModel that derives it value from a calculation method.
 * TODO: Rewrite all methods implementing the interface
 */
public final class CalculatedMoneyModel extends MoneyModel {

    public interface CalculationMethod {
        public int calculate();
        public boolean initialised();
    }
    
    private CalculationMethod method;

    private CalculatedMoneyModel() {}

    public CalculatedMoneyModel create() {
        return new CalculatedMoneyModel();
    }
    
    @Override
    public CalculatedMoneyModel init(Item parent, String id) {
        super.init(parent, id);
        return this;
    }

    /**
    * @param method the method is defined inside the CalculationMethod interface
    * This is not a state variable, so do not change after the MoneyModel is used
    */
    public CalculatedMoneyModel initMethod(CalculationMethod method) {
        this.method = method;
        return this;
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

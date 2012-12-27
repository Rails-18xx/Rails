package rails.game.model;

import rails.game.Currency;
import rails.game.RailsItem;
import rails.game.state.StringState;

/**
 * The base model for money
 * FIXME: Removed "" equivalence for null in setText
 * FIXME: PublicCompany money is shown as "" as long as it has not started, this
 * was coded inside the toString() method
 */
public abstract class MoneyModel extends RailsModel {
    
    public static final int CASH_DEFAULT = 0;
    
    // Data
    private final StringState fixedText = StringState.create(this, "fixedText");
    private final Currency currency;
    
    // Format Options (with defaults)
    private boolean suppressZero = false;
    private boolean suppressInitialZero = false;
    private boolean addPlus = false;
    private boolean displayNegative = false;
 
    protected MoneyModel(RailsItem parent, String id, Currency currency) {
        super(parent, id);
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }
    
    /**
     * @param suppressZero true: displays an empty string instead of a zero value
     * This is not a state variable, so do not change after the MoneyModel is used
     */
    public void setSuppressZero(boolean suppressZero) {
        this.suppressZero = suppressZero;
    }

    /**
     * @param suppressInitialZero true: displays an empty string for the initial zero value
     * This is not a state variable, so do not change after the MoneyModel is used
     */
    public void setSuppressInitialZero(boolean suppressInitialZero) {
        this.suppressInitialZero = suppressInitialZero;
    }

    /**
     * @param addPlus true: adds a plus sign for positive values
     * This is not a state variable, so do not change after the MoneyModel is used
     */
    public void setAddPlus(boolean addPlus) {
        this.addPlus = addPlus;
    }
    
    /**
     * @param displayNegative true: does not display negative values
     * This is not a state variable, so do not change after the MoneyModel is used
     */
    public void setDisplayNegative(boolean displayNegative){
        this.displayNegative = displayNegative;
    }

    /** 
     * @param text fixed text to be displayed instead of money value
     * using null removes text and displays value again
     * Remark: Setting the text triggers an update of the model
     */
    public void setText (String text) {
        fixedText.set(text); // this triggers the update of the model
    }
    
    /**
     * @return current value of the MoneyModel
     */
    public abstract int value();
    
    
    /**
     * @return formatted value of the MoneyModel
     */
    public String formattedValue() {
        return currency.format(value());
    }
    
    /**
     * @return true if MoneyValue has a value set already
     */
    public abstract boolean initialised();

    @Override
    public String toText() {
        if (fixedText.value() != null) {
            return fixedText.value();
        }
        int amount = this.value();
        if (amount == 0
            && (suppressZero 
                    || suppressInitialZero
                        && !initialised())) {
            return "";
        } else if (amount < 0 && !displayNegative) {
            return "";
        } else if (addPlus) {
            return "+" + currency.format(amount);
        } else {
            return currency.format(amount);
        }
    }


}

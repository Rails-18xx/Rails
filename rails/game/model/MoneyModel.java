package rails.game.model;

import rails.game.Bank;
import rails.game.GameManager;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.StringState;

/**
 * The base model for money
 * FIXME: Removed "" equivalence for null in setText
 * FIXME: PublicCompany money is shown as "" as long as it has not started, this
 * was coded inside the toString() method
 */
public abstract class MoneyModel extends Model {
    
    public static final int CASH_DEFAULT = 0;
    
    // Data
    private final StringState fixedText = StringState.create();
    
    // Format Options (with defaults)
    private boolean suppressZero = false;
    private boolean suppressInitialZero = false;
    private boolean addPlus = false;
    private boolean displayNegative = false;
 
    @Override
    public void init(Item parent, String id){
        super.init(parent, id);
        fixedText.init(this, "fixedText");
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
     * @return true if MoneyValue has a value set already
     */
    public abstract boolean initialised();

    @Override
    public String toString() {
        if (fixedText.stringValue() != null) {
            return fixedText.stringValue();
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
            return "+" + Bank.format(amount);
        } else {
            return Bank.format(amount);
        }
    }

    public static void cashMoveFromBank(CashOwner to, int amount) {
        // TODO: get this from the GameContext
        Bank bank = GameManager.getInstance().getBank();
        cashMove(bank, to, amount);
    }

    public static void cashMoveToBank(CashOwner from, int amount) {
        // TODO: get this from the GameContext
        Bank bank = GameManager.getInstance().getBank();
        cashMove(from, bank, amount);
    }

    /**
     * Facilitates a move of cash. In this specific case either from or to may
     * be null, in which case the Bank is implied.
     */
    public static void cashMove(CashOwner from, CashOwner to, int amount) {
        to.getCash().change(amount);
        from.getCash().change(-amount);
    }



}

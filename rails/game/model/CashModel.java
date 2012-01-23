package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompany;
import rails.game.state.IntegerState;
import rails.game.state.Item;
import rails.game.state.StringState;

public final class CashModel extends Model {

    public static final String ID = "CashModel";

    // stores the cash amount
    private final IntegerState cash = IntegerState.create("Cash");

    // stores the fixed part of the displayed text
    private final StringState displayText = StringState.create("BankCashDisplayText");

    private boolean suppressZero;

    private CashModel() {
        super(ID);
    }

    /**
     * Creates an owned CashModel
     * CashModel is initialized with a default id "CashModel"
     */
    public static CashModel create(Item parent){
        return new CashModel().init(parent);
    }

    /**
     * Creates an unowned CashModel
     * CashModel is initialized with a default id "CashModel"
     * Remark: Still requires a call to the init-method
     */
    public static CashModel create(){
        return new CashModel();
    }
    
    @Override
    public CashModel init(Item parent){
        super.init(parent);
        suppressZero = false;
        cash.init(this);
        displayText.init(this);
        return this;
    }

    public void setSuppressZero(boolean value) {
        this.suppressZero = value;
    }
    
    public void set(int newCash) {
        cash.set(newCash);
    }

    public void add(int addedCash) {
        cash.add(addedCash);
    }

    public int value() {
        return cash.intValue();
    }

    public void setText (String text) {
        displayText.set (text);
    }

    @Override
    public String toString() {
        String fixedText = displayText.stringValue();
        if (!"".equals(fixedText)) {
            return fixedText;
        } else if (cash.intValue() == 0 && suppressZero
            || getParent() instanceof PublicCompany
            && !((PublicCompany) getParent()).hasStarted()) {
            return "";
        } else {
            return Bank.format(cash.intValue());
        }
    }

}

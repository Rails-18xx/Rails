package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompany;
import rails.game.state.IntegerState;
import rails.game.state.Item;
import rails.game.state.StringState;

public final class CashModel extends Model {

    // stores the cash amount
    private final IntegerState cash;

    // stores the fixed part of the displayed text
    private final StringState displayText;

    private boolean suppressZero;

    /**
     * CashModel is initialized with a default id "CashModel"
     */    
    public CashModel() {
        super("CashModel");
        cash = new IntegerState("Cash");
        displayText = new StringState("BankCashDisplayText");
        
    }

    /**
     * Creates an initialized CashModel
     */
    public static CashModel create(Item parent){
        return new CashModel().init(parent);
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
    protected String getText() {
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

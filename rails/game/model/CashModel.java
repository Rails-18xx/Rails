package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompany;
import rails.game.state.IntegerState;
import rails.game.state.StringState;

public final class CashModel extends AbstractModel<String> {

    private final IntegerState cash;
    private final CashOwner owner;

    /** Text to be displayed instead of the cash amount (if length > 0) */
    private final StringState displayText;

    private boolean suppressZero;

    public CashModel(CashOwner owner) {
        super(owner, "CashModel");
        this.owner = owner;
        
        suppressZero = false;

        cash = new IntegerState(owner, "Cash");
        cash.addObserver(this);
        displayText = new StringState(owner, "BankCashDisplayText");
        displayText.addObserver(this);
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

    public String getData() {
        String fixedText = displayText.stringValue();
        if (!"".equals(fixedText)) {
            return fixedText;
        } else if (cash.intValue() == 0 && suppressZero
            || owner instanceof PublicCompany
            && !((PublicCompany) owner).hasStarted()) {
            return "";
        } else {
            return Bank.format(cash.intValue());
        }
    }

    public void setText (String text) {
        displayText.set (text);
    }
}

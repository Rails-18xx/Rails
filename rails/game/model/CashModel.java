package rails.game.model;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.PublicCompanyI;
import rails.game.state.IntegerState;
import rails.game.state.StringState;

public final class CashModel extends AbstractModel<String> {

    private final IntegerState cash;
    private final CashHolder owner;

    /** Text to be displayed instead of the cash amount (if length > 0) */
    private final StringState displayText;

    private boolean suppressZero;

    public CashModel(CashHolder owner) {
        super(owner, "CashModel");
        this.owner = owner;
        suppressZero = false;

        cash = new IntegerState(owner, "Cash");
        cash.addModel(this);
        displayText = new StringState(owner, "BankCashDisplayText");
        displayText.addModel(this);
    }

    public void setSuppressZero(boolean value) {
        this.suppressZero = value;
    }
    
    public void setCash(int newCash) {
        cash.set(newCash);
    }

    public void addCash(int addedCash) {
        cash.add(addedCash);
    }

    public int getCash() {
        return cash.intValue();
    }

    public String getData() {
        String fixedText = displayText.stringValue();
        if (!"".equals(fixedText)) {
            return fixedText;
        } else if (cash.intValue() == 0 && suppressZero
            || owner instanceof PublicCompanyI
            && !((PublicCompanyI) owner).hasStarted()) {
            return "";
        } else {
            return Bank.format(cash.intValue());
        }
    }

    public void setText (String text) {
        displayText.set (text);
    }
}

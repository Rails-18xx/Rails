/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/MoneyModel.java,v 1.9 2010/01/01 14:00:31 evos Exp $*/
package rails.game.model;

import rails.game.Bank;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.game.state.StringState;
import rails.util.Util;

public class MoneyModel extends IntegerState {
    public static final int SUPPRESS_ZERO = 1;
    public static final int SUPPRESS_INITIAL_ZERO = 2;
    public static final int ADD_PLUS = 4;
    public static final int ALLOW_NEGATIVE = 8;
    private BooleanState initialised;
    private StringState fixedText = null;
    
    public MoneyModel(String name) {
        super(name, 0);
    }

    public MoneyModel(String name, int value) {

        super(name, value);
    }

    public void set(int value) {

        boolean forced = false;

        /* Set initialisation state only if it matters */
        if (Util.bitSet(option, SUPPRESS_INITIAL_ZERO) && initialised == null) {
            initialised = new BooleanState(name + "_initialised", false);
        }
        if (initialised != null && !initialised.booleanValue()) {
            initialised.set(true);
            forced = true;
        }

        /*
         * At the end, as update() is called from here. Used setForced() to
         * ensure clients are updated even at an initial zero revenue.
         */
        if (forced) {
            super.setForced(value);
        } else {
            super.set(value);
        }
    }

    /** Set a fixed text, which will override the money value
     * as long as it is not null and not "".
     * @param text
     */
    public void setText (String text) {
        if (fixedText == null) {
            fixedText = new StringState (name+"_FixedText", text);
        } else {
            fixedText.set(text);
        }
        update();
    }
    
    public String getText() {
        if (fixedText != null && !"".equals(fixedText.getText())) {
            return fixedText.getText();
        }
        int amount = intValue();
        if (amount == 0
            && (Util.bitSet(option, SUPPRESS_ZERO) 
                    || Util.bitSet(option, SUPPRESS_INITIAL_ZERO)
                        && (initialised == null || !initialised.booleanValue()))) {
            return "";
        } else if (amount < 0 && !Util.bitSet(option, ALLOW_NEGATIVE)) {
            return "";
        } else if (Util.bitSet(option, ADD_PLUS)) {
            return "+" + Bank.format(amount);
        } else {
            return Bank.format(amount);
        }
    }

}

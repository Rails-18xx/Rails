/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/CashModel.java,v 1.5 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import rails.game.*;

public class CashModel extends ModelObject {

    private int cash;
    private CashHolder owner;

    public static final int SUPPRESS_ZERO = 1;

    public CashModel(CashHolder owner) {
        cash = 0;
        this.owner = owner;
    }

    public void setCash(int newCash) {
        cash = newCash;
        update();
    }

    public void addCash(int addedCash) {
        cash += addedCash;
        update();
    }

    public int getCash() {
        return cash;
    }

    /*
     * (non-Javadoc)
     *
     * @see rails.rails.game.model.ModelObject#getValue()
     */
    @Override
    public String getText() {
        if (cash == 0 && (option & SUPPRESS_ZERO) > 0
            || owner instanceof PublicCompanyI
            && !((PublicCompanyI) owner).hasStarted()) {
            return "";
        } else {
            return Bank.format(cash);
        }
    }

}

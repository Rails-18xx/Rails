/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/MoneyModel.java,v 1.1 2005/12/11 00:03:36 evos Exp $
 * 
 * Created on 10-Dec-2005
 * Change Log:
 */
package game.model;

import game.Bank;

/**
 * @author Erik Vos
 */
public class MoneyModel extends ModelObject {
    
    private int cash;
    
    public MoneyModel (int initialCash) {
        cash = initialCash;
    }
    
    public void setCash (int newCash) {
        cash = newCash;
        notifyViewObjects();
    }
    
    public void addCash (int addedCash) {
        cash += addedCash;
        notifyViewObjects();
    }
    
    public int getCash () {
        return cash;
    }

    /* (non-Javadoc)
     * @see game.model.ModelObject#getValue()
     */
    public String toString() {
        return Bank.format(cash);
    }

}

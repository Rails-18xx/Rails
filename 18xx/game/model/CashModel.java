/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/CashModel.java,v 1.2 2005/12/17 20:51:34 evos Exp $
 * 
 * Created on 10-Dec-2005
 * Change Log:
 */
package game.model;

import game.Bank;
import game.CashHolder;
import game.PublicCompanyI;

/**
 * @author Erik Vos
 */
public class CashModel extends ModelObject {
    
    private int cash;
    private CashHolder owner;
    
    public CashModel (CashHolder owner) {
        cash = 0;
        this.owner = owner;
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
        if (owner instanceof PublicCompanyI && !((PublicCompanyI)owner).hasStarted()) {
            return  "";
        }
        return Bank.format(cash);
    }

}

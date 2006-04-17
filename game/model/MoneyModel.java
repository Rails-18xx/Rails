/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/MoneyModel.java,v 1.3 2006/04/17 21:43:59 evos Exp $
 * 
 * Created on 17-Apr-2006
 * Change Log:
 */
package game.model;

import game.Bank;

/**
 * @author Erik Vos
 */
public class MoneyModel extends ModelObject {
    
    int amount = 0;
    boolean initialised = false;
    Object owner = null;
    
    public MoneyModel (Object owner) {
        this.owner = owner;
    }
    
    public void setAmount (int amount) {
        this.amount = amount;
        initialised = true;
        notifyViewObjects();
    }
    
    public int getAmount () {
        return amount;
    }
    
    public String toString () {
        
        return (initialised ? Bank.format(amount) : "");
    }

}

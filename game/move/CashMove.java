/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/CashMove.java,v 1.2 2007/01/16 20:32:31 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import game.*;

/**
 * @author Erik Vos
 */
public class CashMove extends Move {
    
    int amount;
    CashHolder from;
    CashHolder to;
    
    public CashMove (CashHolder from, CashHolder to, int amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public boolean execute() {

        Bank.transferCash(from, to, amount);
        return true;
    }

    public boolean undo() {

        Bank.transferCash(to, from, amount);
        return true;
    }

    public String toString() {
    	return "CashMove: " + Bank.format(amount) 
    		+ " from " + from.getName()
    		+ " to " + to.getName();
    }
}

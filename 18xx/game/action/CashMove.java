/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/CashMove.java,v 1.1 2006/07/17 22:00:23 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.action;

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

}

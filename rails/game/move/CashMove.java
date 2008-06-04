/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/CashMove.java,v 1.4 2008/06/04 19:00:33 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class CashMove extends Move {

    int amount;
    CashHolder from;
    CashHolder to;

    private static Bank bank = Bank.getInstance();

    /**
     * Create a CashMove instance. In this specific case either from or to may
     * be null, in which case the Bank is implied.
     * 
     * @param from The cash payer (null implies the Bank).
     * @param to The cash payee (null implies the Bank).
     * @param amount
     */
    public CashMove(CashHolder from, CashHolder to, int amount) {
        this.from = from != null ? from : bank;
        this.to = to != null ? to : bank;
        this.amount = amount;

        MoveSet.add(this);
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
        return "CashMove: " + Bank.format(amount) + " from " + from.getName()
               + " to " + to.getName();
    }
}

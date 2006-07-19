/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/PriceMove.java,v 1.1 2006/07/19 22:08:50 evos Exp $
 * 
 * Created on 18-Jul-2006
 * Change Log:
 */
package game.action;

import game.PublicCompanyI;
import game.StockMarket;
import game.StockSpaceI;

/**
 * @author Erik Vos
 */
public class PriceMove extends Move {
    
    protected StockSpaceI from, to;
    protected PublicCompanyI company;
    
    public PriceMove (StockSpaceI from, StockSpaceI to, PublicCompanyI company) {
        this.from = from;
        this.to = to;
        this.company = company;
    }

    public boolean execute() {
        StockMarket.getInstance().processMove (company, from, to);
        return true;
    }

    /* (non-Javadoc)
     * @see game.action.Move#undo()
     */
    public boolean undo() {
        StockMarket.getInstance().processMove (company, to, from);
        return true;
    }

}

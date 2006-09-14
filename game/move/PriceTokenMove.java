/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/PriceTokenMove.java,v 1.1 2006/09/14 19:33:31 evos Exp $
 * 
 * Created on 22-Jul-2006
 * Change Log:
 */
package game.move;

import game.PublicCompanyI;
import game.StockMarket;
import game.StockSpaceI;

/**
 * @author Erik Vos
 */
public class PriceTokenMove extends Move {
    
    private PublicCompanyI company;
    private StockSpaceI from, to;
    
    public PriceTokenMove (PublicCompanyI company, StockSpaceI from, StockSpaceI to) {
        this.company = company;
        this.from = from;
        this.to = to;
    }

    public boolean execute() {
        StockMarket.getInstance().processMove (company, from, to);
        return true;
    }

    public boolean undo() {
        StockMarket.getInstance().processMove (company, to, from);
        return true;
    }
    
    public String toString() {
        return "PriceTokenMove: "+company.getName()+" from "+from+" to "+to;
    }

}

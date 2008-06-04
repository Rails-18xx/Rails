/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/PriceTokenMove.java,v 1.3 2008/06/04 19:00:33 evos Exp $
 * 
 * Created on 22-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.PublicCompanyI;
import rails.game.StockMarket;
import rails.game.StockSpaceI;

/**
 * @author Erik Vos
 */
public class PriceTokenMove extends Move {

    private PublicCompanyI company;
    private StockSpaceI from, to;

    public PriceTokenMove(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to) {
        this.company = company;
        this.from = from;
        this.to = to;

        MoveSet.add(this);
    }

    public boolean execute() {
        StockMarket.getInstance().processMove(company, from, to);
        return true;
    }

    public boolean undo() {
        StockMarket.getInstance().processMove(company, to, from);
        return true;
    }

    public String toString() {
        return "PriceTokenMove: " + company.getName() + " from " + from
               + " to " + to;
    }

}

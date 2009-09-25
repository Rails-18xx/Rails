/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/PriceTokenMove.java,v 1.4 2009/09/25 19:29:56 evos Exp $
 * 
 * Created on 22-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.PublicCompanyI;
import rails.game.StockMarket;
import rails.game.StockMarketI;
import rails.game.StockSpaceI;

/**
 * @author Erik Vos
 */
public class PriceTokenMove extends Move {

    private PublicCompanyI company;
    private StockSpaceI from, to;
    private StockMarketI stockMarket = null;

    public PriceTokenMove(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to, StockMarketI stockMarket) {
        this.company = company;
        this.from = from;
        this.to = to;
        this.stockMarket = stockMarket;

        MoveSet.add(this);
    }

    public boolean execute() {
        stockMarket.processMove(company, from, to);
        return true;
    }

    public boolean undo() {
        stockMarket.processMove(company, to, from);
        return true;
    }

    public String toString() {
        return "PriceTokenMove: " + company.getName() + " from " + from
               + " to " + to;
    }

}

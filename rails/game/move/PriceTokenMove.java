/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/PriceTokenMove.java,v 1.6 2010/01/31 22:22:30 macfreek Exp $
 *
 * Created on 22-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.*;

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

    @Override
    public boolean execute() {
        stockMarket.processMove(company, from, to);
        return true;
    }

    @Override
    public boolean undo() {
        stockMarket.processMove(company, to, from);
        return true;
    }

    @Override
    public String toString() {
        return "PriceTokenMove: " + company.getName() + " from " + from
               + " to " + to;
    }

}

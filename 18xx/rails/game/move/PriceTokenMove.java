/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/PriceTokenMove.java,v 1.7 2010/03/10 17:26:49 stefanfrey Exp $
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
    private int fromStackPosition;
    private StockMarketI stockMarket = null;

    public PriceTokenMove(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to, StockMarketI stockMarket) {
        this.company = company;
        this.from = from;
        this.to = to;
        if (from != null) 
            fromStackPosition = from.getStackPosition(company);
        else
            fromStackPosition = 0;
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
        stockMarket.processMoveToStackPosition(company, to, from, fromStackPosition);
        return true;
    }

    @Override
    public String toString() {
        return "PriceTokenMove: " + company.getName() + " from " + from + " (at stack "
            + fromStackPosition + ") to " + to;
    }

}

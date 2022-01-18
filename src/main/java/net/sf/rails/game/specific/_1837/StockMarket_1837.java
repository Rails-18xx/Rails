package net.sf.rails.game.specific._1837;

import com.google.common.collect.Lists;
import net.sf.rails.common.GameOption;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.Owner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Brumm
 *
 */
public class StockMarket_1837 extends StockMarket {

    private HashMapState<StockSpace, Integer> parSpaceUsers
            = HashMapState.create(this, "parSpaceUsers");

    public StockMarket_1837(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * Return start prices as list of prices
     * but no more that two on each space
     */
    public List<Integer> getStartPrices() {
        List<Integer> prices = Lists.newArrayList();
        for (StockSpace space : startSpaces) {
            if (canAddParSpaceUser(space)) {
                prices.add(space.getPrice());
            }
        }
        return prices;
    }

    @Override
    public StockSpace getStartSpace(int price) {
        StockSpace space = super.getStartSpace(price);
        if (canAddParSpaceUser(space)) {
            return space;
        } else {
            return null;
        }
    }

    /**
     * Add 1 to the number of companies that have a given par price
     * (whether this is allowed should have been checked first using canAddParSpaceUser())
     * @param space The StartSpace of the given par price
     */
    public void addParSpaceUser (StockSpace space) {
        if (parSpaceUsers.get(space) == null) {
            parSpaceUsers.put (space, 1);
        } else {
            parSpaceUsers.put(space, parSpaceUsers.get(space) + 1);
        }
    }

    /**
     * Check if a new company can use a specific par price.
     * In 1837, the maximum number of companies that can use a par price is 2.
     * @param space The StartSpace of a given par price
     * @return True if starting a company with that price is allowed
     */
    public boolean canAddParSpaceUser (StockSpace space) {
        Integer count = parSpaceUsers.get(space);
        return count == null || count < 2;
    }

    /*----- Sell action interface methods -----*/

    public void payOut(PublicCompany company, boolean split) {
        if (!split) {
            moveRightOrUp(company);
        } else {
            moveRightandDown(company);
        }
    }

    public int spacesDownOnSale (int sharesSold, Owner seller) {
        return 1;
    }

    @Override
    public void soldOut(PublicCompany company) {

        if (GameOption.getValue(this, GameOption.VARIANT).matches("Basegame|1837-2ndEd.")
                && company.getPresident().getPortfolioModel().getCertificates(company).size() >= 4) {
            //President has 4 shares (50% or more) except in the Romoth variant
            moveLeftAndUp(company);
        } else {
            moveUp(company);
        }
    }

    /*----- Execute share price moves. -----*/
    // In principle, these should be internal (private) methods

    private void moveRightandDown(PublicCompany company) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();

        /* Drop the indicated number of rows */
        int newrow = row + 1;
        int newcol = col + 1;

        /* Don't drop below the bottom of the chart */
        if (getStockSpace(newrow, newcol) == null) {
            
            while (newrow >= numRows || getStockSpace(newrow, col) == null)
                newrow--;
            while (newcol >= numCols || getStockSpace(newrow, newcol) == null)
                newcol--;
        }

        // Changed the "||" into "&&", because both movements must be possible
        if ((newrow > row) && (newcol > col)) {
            newsquare = getStockSpace(newrow, newcol);
        }
        
        if (newsquare != null) {
            prepareMove(company, oldsquare, newsquare);
        }  
    }

    private void moveLeftAndUp(PublicCompany company) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if ((row > 0) && (col > 0)){
            newsquare = getStockSpace(row - 1, col -1);
        }
        if (newsquare != null) prepareMove(company, oldsquare, newsquare);
        
    }

    @Override
    public void moveUp(PublicCompany company) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (row > 0) {
            newsquare = getStockSpace(row - 1, col);
        } else if (upOrDownRight && col < numCols - 1) {
            newsquare = getStockSpace(row + 1, col + 1);
        }
        if (newsquare != null) prepareMove(company, oldsquare, newsquare);
    }

    protected void moveRightOrUp(PublicCompany company) {
        /* Ignore the amount for now */
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (col < numCols - 1 && !oldsquare.isLeftOfLedge()
                && (newsquare = getStockSpace(row, col + 1)) != null) {}
        else if (row > 0
                && (newsquare = getStockSpace(row - 1, col)) != null) {}
        prepareMove(company, oldsquare, newsquare);
    }

}

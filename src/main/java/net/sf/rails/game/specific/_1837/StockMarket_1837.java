/**
 * 
 */
package net.sf.rails.game.specific._1837;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;

/**
 * @author Martin Brumm
 *
 */
public class StockMarket_1837 extends StockMarket {

    /**
     * @param parent
     * @param id
     */
    public StockMarket_1837(RailsRoot parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }
    
    public void payOut(PublicCompany company, boolean split) {
        if (!split) {
        moveRightOrUp(company);
        } else {
            moveRightandDown(company);
        }
    }
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
            
        if ((newrow > row) || (newcol > col)) {
            newsquare = getStockSpace(newrow, newcol);
        }
        
        if (newsquare != oldsquare) {
            prepareMove(company, oldsquare, newsquare);
        }  
    }



    @Override
    public void soldOut(PublicCompany company) {
        if (company.getPresident().getPortfolioModel().getCertificates(company).size()>=4) { //President has 4 shares (50% or more)
          moveLeftAndUp(company);  
        } else {
        moveUp(company);
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

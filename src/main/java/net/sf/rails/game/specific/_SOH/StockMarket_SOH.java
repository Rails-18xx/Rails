package net.sf.rails.game.specific._SOH;

import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.Owner;

public class StockMarket_SOH extends StockMarket {

    public StockMarket_SOH(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * Determine where the price token must land when selling shares.
     * @param company Company of which shares are sold
     * @param seller The selling entity (player or company)
     * @param numberOfSpaces The number of spaces to move.
     */
    protected void moveLeft(PublicCompany company, Owner seller, int numberOfSpaces) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare;
        int row = oldsquare.getRow(); // Always 1
        int col = oldsquare.getColumn();

        // A company selling its own shares drops one less
        if (seller == company) numberOfSpaces--;
        if (numberOfSpaces == 0) return;

        for (int i=1; i<=numberOfSpaces && --col>=0; i++) {
            // If player is not the president, don't drop below ledge
            if (seller instanceof Player && seller != company.getPresident()
                    && getStockSpace (row, col).isLeftOfLedge()) {
                col++;
                break;
            }
        }

        newsquare = getStockSpace(row, col);

        if (newsquare != oldsquare) {
            prepareMove(company, oldsquare, newsquare);
        }
    }
}

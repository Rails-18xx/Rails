package rails.game.specific._1856;

import rails.game.*;
import rails.game.state.IntegerState;

/** Needed to copy behaviour on share selling from StockRound_1856. */
public class ShareSellingRound_1856 extends ShareSellingRound {

    /* Cope with multiple 5% share sales in one turn */
    private IntegerState sharesSoldSoFar;
    private IntegerState squaresDownSoFar;

    public ShareSellingRound_1856 (GameManagerI aGameManager,
            RoundI parentRound) {
        super (aGameManager, parentRound);

        sharesSoldSoFar = new IntegerState("CGR_SharesSoldSoFar", 0);
        squaresDownSoFar = new IntegerState("CGR_SquaresDownSoFar", 0);
    }

    @Override
	protected void adjustSharePrice (PublicCompanyI company, int numberSold, boolean soldBefore) {

        if (!company.canSharePriceVary()) return;

        int numberOfSpaces = numberSold;
        if (company instanceof PublicCompany_CGR) {
            if (company.getShareUnit() == 5) {
                // Take care for selling 5% shares in multiple blocks per turn
                numberOfSpaces
                    = (sharesSoldSoFar.intValue() + numberSold)/2
                    - squaresDownSoFar.intValue();
                sharesSoldSoFar.add(numberSold);
                squaresDownSoFar.add(numberOfSpaces);
            }
        }

        super.adjustSharePrice (company, numberOfSpaces, soldBefore);
    }

}

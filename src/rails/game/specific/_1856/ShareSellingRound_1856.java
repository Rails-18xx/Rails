package rails.game.specific._1856;

import rails.game.GameManager;
import rails.game.PublicCompany;
import rails.game.Round;
import rails.game.ShareSellingRound;
import rails.game.state.IntegerState;

/** Needed to copy behaviour on share selling from StockRound_1856. */
public final class ShareSellingRound_1856 extends ShareSellingRound {

    /* Cope with multiple 5% share sales in one turn */
    private final IntegerState sharesSoldSoFar = IntegerState.create(this, "sharesSoldSoFar");
    private final IntegerState squaresDownSoFar = IntegerState.create(this, "squaresDownSoFar");

    private ShareSellingRound_1856(GameManager parent, String id, Round parentRound) {
        super(parent, id, parentRound);
    }

    public static ShareSellingRound_1856 create(GameManager parent, String id, Round parentRound) {
        return new ShareSellingRound_1856(parent, id, parentRound);
    }

    @Override
	protected void adjustSharePrice (PublicCompany company, int numberSold, boolean soldBefore) {

        if (!company.canSharePriceVary()) return;

        int numberOfSpaces = numberSold;
        if (company instanceof PublicCompany_CGR) {
            if (company.getShareUnit() == 5) {
                // Take care for selling 5% shares in multiple blocks per turn
                numberOfSpaces
                    = (sharesSoldSoFar.value() + numberSold)/2
                    - squaresDownSoFar.value();
                sharesSoldSoFar.add(numberSold);
                squaresDownSoFar.add(numberOfSpaces);
            }
        }

        super.adjustSharePrice (company, numberOfSpaces, soldBefore);
    }

}

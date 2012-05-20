package rails.game.specific._1856;

import rails.game.GameManager;
import rails.game.PublicCompany;
import rails.game.Round;
import rails.game.ShareSellingRound;
import rails.game.state.IntegerState;
import rails.game.state.Item;

/** Needed to copy behaviour on share selling from StockRound_1856. */
public class ShareSellingRound_1856 extends ShareSellingRound {

    /* Cope with multiple 5% share sales in one turn */
    private final IntegerState sharesSoldSoFar = IntegerState.create();
    private final IntegerState squaresDownSoFar = IntegerState.create();

    public ShareSellingRound_1856 (GameManager aGameManager,
            Round parentRound) {
        super (aGameManager, parentRound);
    }

    @Override
    public void init(Item parent, String id){
        super.init(parent, id);
        sharesSoldSoFar.init(this, "CGR_SharesSoldSoFar");
        squaresDownSoFar.init(this, "CGR_SquaresDownSoFar");
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

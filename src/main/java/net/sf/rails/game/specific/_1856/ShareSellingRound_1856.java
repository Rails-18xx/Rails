package net.sf.rails.game.specific._1856;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;

/** Needed to copy behaviour on share selling from StockRound_1856. */
public final class ShareSellingRound_1856 extends ShareSellingRound {

    /* Cope with multiple 5% share sales in one turn */
    private final IntegerState sharesSoldSoFar = IntegerState.create(this, "sharesSoldSoFar");
    private final IntegerState squaresDownSoFar = IntegerState.create(this, "squaresDownSoFar");

    /**
     * Created using Configure
     */
    public ShareSellingRound_1856(GameManager parent, String id) {
        super(parent, id);
    }
    @Override
	protected void adjustSharePrice (PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {

        if (!company.canSharePriceVary()) return;

        int numberOfSpaces = sharesSold;
        if (company instanceof PublicCompany_CGR) {
            if (company.getShareUnit() == 5) {
                // Take care for selling 5% shares in multiple blocks per turn
                numberOfSpaces
                    = (sharesSoldSoFar.value() + sharesSold)/2
                    - squaresDownSoFar.value();
                sharesSoldSoFar.add(sharesSold);
                squaresDownSoFar.add(numberOfSpaces);
            }
        }

        super.adjustSharePrice (company, seller, numberOfSpaces, soldBefore);
    }

}

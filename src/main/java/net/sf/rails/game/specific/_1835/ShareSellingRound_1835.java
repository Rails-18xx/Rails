package net.sf.rails.game.specific._1835;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.state.Owner;
import rails.game.action.AdjustSharePrice;
import rails.game.action.PossibleAction;

import java.util.EnumSet;

public class ShareSellingRound_1835 extends ShareSellingRound {

    public ShareSellingRound_1835(GameManager parent, String id) {
        super(parent, id);
    }

    /** Share price goes down 1 space for any number of shares sold.
     *  Rules explicitly state that different sale actions do NOT sell at the same price.
     *  So the parameter values of both numberSold and soldBefore are ignored.
     *
     *  EV 08/2022: well, the rules may say so, but that does not work here,
     *  because Rails currently does not allow selling shares of different
     *  sizes in one action. In such cases sell actions *must* be split,
     *  and it would be unfair to apply the mentioned rule in such cases.
     *
     *  A new "AdjustSharePrice" action will be added to the Special menu
     *  for this purpose where applicable.
     */

    @Override
    protected void adjustSharePrice (PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {
        if (soldBefore) {
            lastSoldCompany = company;
        } else {
            super.adjustSharePrice(company, seller, 1, soldBefore);
        }
    }

    public boolean setPossibleActions() {
        boolean result = super.setPossibleActions();
        setGameSpecificActions();
        return result;
    }


    protected void setGameSpecificActions() {
        /* If in one turn multiple sales of the same company occur,
         * this is normally done at the same price.
         * In 1835 the rules state otherwise, a special action
         * enables following that rule strictly.
         */
        if (lastSoldCompany != null) {
            possibleActions.add(new AdjustSharePrice(lastSoldCompany, EnumSet.of(AdjustSharePrice.Direction.DOWN)));
        }
    }

    protected boolean processGameSpecificAction(PossibleAction action) {
        if (action instanceof AdjustSharePrice) {
            super.adjustSharePrice ((AdjustSharePrice)action);
            return true;
        } else {
            return false;
        }
    }

}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Train.java,v 1.16 2010/01/08 21:30:46 evos Exp $ */
package rails.game;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolder;
import rails.game.move.ObjectMove;
import rails.game.state.BooleanState;

public class Train implements TrainI {

    protected TrainTypeI type;

    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected String uniqueId;

    protected Portfolio holder;
    protected BooleanState obsolete;

    protected static Logger log =
            Logger.getLogger(Train.class.getPackage().getName());

    public Train() {}

    public void init(TrainTypeI type, String uniqueId) {

        this.type = type;
        this.uniqueId = uniqueId;

        obsolete = new BooleanState(uniqueId, false);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return type.getCityScoreFactor();
    }

    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return type.getCost();
    }

    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return type.getMajorStops();
    }

    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return type.getMinorStops();
    }

    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return type.getTownCountIndicator();
    }

    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return type.getTownScoreFactor();
    }

    /**
     * @return Returns the type.
     */
    public TrainTypeI getType() {
        return type;
    }

    public String getName() {
        return type.getName();
    }

    public Portfolio getHolder() {
        return holder;
    }

    public CashHolder getOwner() {
        return holder.getOwner();
    }

    public boolean isObsolete() {
        return obsolete.booleanValue();
    }

    /**
     * Move the train to another Portfolio.
     */
    public void setHolder(Portfolio newHolder) {
        holder = newHolder;
    }

    public void moveTo(MoveableHolder to) {

        new ObjectMove(this, holder, to);

    }

    public void setRusted() {
        new ObjectMove(this, holder, GameManager.getInstance().getBank().getScrapHeap());
    }

    public void setObsolete() {
        obsolete.set(true);
    }

    public boolean canBeExchanged() {
        return type.nextCanBeExchanged();
    }

    public String toDisplay() {
        return getName();
    }

    public boolean isTradeable() {
        return tradeable;
    }

    public void setTradeable(boolean tradeable) {
        this.tradeable = tradeable;
    }

}

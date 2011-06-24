/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Train.java,v 1.16 2010/01/08 21:30:46 evos Exp $ */
package rails.game;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolder;
import rails.game.move.ObjectMove;
import rails.game.state.BooleanState;
import rails.game.state.GenericState;

public class Train implements TrainI {

    protected TrainCertificateType certificateType;
    
    protected GenericState<TrainType> type;
    
    /** Temporary variable, only used during moves. */
    protected TrainType previousType = null;

    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected String uniqueId;

    protected Portfolio holder;
    protected BooleanState obsolete;

    protected static Logger log =
            Logger.getLogger(Train.class.getPackage().getName());

    public Train() {}

    public void init(TrainCertificateType certType, TrainType type, String uniqueId) {

        this.certificateType = certType;
        this.uniqueId = uniqueId;
        this.type = new GenericState<TrainType>(certType.getName()+"_CurrentType", type);
        this.previousType = type;

        obsolete = new BooleanState(uniqueId, false);
    }
    
    public void setType (TrainType type) {
        previousType = this.type.get();
        this.type.set(type);
    }

    /**
     * @return Returns the type.
     */
    public TrainCertificateType getCertType() {
        return certificateType;
    }
    
    public TrainType getType() {
        return isAssigned() ? type.get() : null;
    }

    public TrainType getPreviousType() {
        return previousType;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return getType().getCityScoreFactor();
    }

    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return getType().getCost();
    }

    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return getType().getMajorStops();
    }

    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return getType().getMinorStops();
    }

    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return getType().getTownCountIndicator();
    }

    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return getType().getTownScoreFactor();
    }

    public boolean isAssigned() {
        return type.get() != null;
    }
    
    public boolean isPermanent() {
        return certificateType.isPermanent();
    }
    
    public String getName() {
        return isAssigned() ? type.get().getName() : certificateType.getName();
    }

    public Portfolio getHolder() {
        return holder;
    }

    public CashHolder getOwner() {
        return holder != null ? holder.getOwner() : null;
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
        return certificateType.nextCanBeExchanged();
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

    public String toString() {
        StringBuilder b = new StringBuilder(uniqueId);
        b.append(" certType=").append(getCertType());
        b.append(" type=").append(getType());
        b.append(" holder=").append(holder.getName());
        return b.toString();
    }
}

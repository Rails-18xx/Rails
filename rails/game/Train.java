package rails.game;

import org.apache.log4j.Logger;

import rails.game.state.BooleanState;
import rails.game.state.Context;
import rails.game.state.GameItem;
import rails.game.state.GenericState;
import rails.game.state.Item;
import rails.game.state.OwnableItem;
import rails.game.state.Portfolio;

public class Train extends GameItem implements OwnableItem<Train> {

    protected TrainCertificateType certificateType;
    
    protected GenericState<TrainType> type;
    
    /** Temporary variable, only used during moves. */
    protected TrainType previousType = null;

    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected String uniqueId;

    protected BooleanState obsolete;
    
    private Portfolio<Train> portfolio;

    protected static Logger log =
            Logger.getLogger(Train.class.getPackage().getName());

    public Train() {}

    public void init(TrainCertificateType certType, TrainType type, String uniqueId) {

        this.certificateType = certType;
        this.uniqueId = uniqueId;
        this.type = GenericState.create(this, certType.getName()+"_CurrentType", type);
        this.previousType = type;

        obsolete = BooleanState.create(this, uniqueId, false);
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

    /**
     * @return true => hex train (examples 1826, 1844), false => standard 1830 type train
     */
    public boolean isHTrain() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAssigned() {
        return type.get() != null;
    }
    
    public boolean isPermanent() {
        return certificateType.isPermanent();
    }
    
    public String getId() {
        return isAssigned() ? type.get().getName() : certificateType.getName();
    }

    public boolean isObsolete() {
        return obsolete.booleanValue();
    }

    public void setRusted() {
        moveTo(GameManager.getInstance().getBank().getScrapHeap());
    }

    public void setObsolete() {
        obsolete.set(true);
    }

    public boolean canBeExchanged() {
        return certificateType.nextCanBeExchanged();
    }

    public String toDisplay() {
        return getId();
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
        return b.toString();
    }

    // OwnableItem interface
    
    public void setPortfolio(Portfolio<Train> p) {
        portfolio = p;
    }

    public Portfolio<Train> getPortfolio() {
        return portfolio;
    }


}

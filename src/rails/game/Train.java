package rails.game;

import org.apache.log4j.Logger;

import rails.common.parser.ConfigurationException;
import rails.game.state.BooleanState;
import rails.game.state.GenericState;
import rails.game.state.Item;
import rails.game.state.OwnableItem;
import rails.game.state.Portfolio;

public class Train extends OwnableItem<Train> {

    protected TrainCertificateType certificateType;
    
    protected GenericState<TrainType> type = GenericState.create();
    
    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected BooleanState obsolete = BooleanState.create(false);
    
    private Portfolio<Train> portfolio;

    protected static Logger log =
            Logger.getLogger(Train.class.getPackage().getName());

    public Train() {}
    // TODO: Train creation is shared by three classes, simplify that
    
    public static Train create(Item parent, String id, TrainCertificateType certType, TrainType type)
            throws ConfigurationException {
        Train train = certType.createTrain();
        train.init(parent, id);
        train.setCertificateType(certType);
        train.setType(type);
        return train;
    }

    @Override
    public void init(Item parent, String id) {
        super.init(parent,id);
        type.init(this, "currentType");
        obsolete.init(this, "obsolete");
    }
    
    public void setCertificateType(TrainCertificateType type) {
        this.certificateType = type;
    }
    
    public void setType (TrainType type) {
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

    /**import rails.game.state.AbstractItem;

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
        GameManager.getInstance().getBank().getScrapHeap().getTrainsModel().getPortfolio().moveInto(this);
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
        StringBuilder b = new StringBuilder(getId());
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

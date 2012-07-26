package rails.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.parser.ConfigurationException;
import rails.game.state.BooleanState;
import rails.game.state.GenericState;
import rails.game.state.Item;
import rails.game.state.OwnableItem;
import rails.game.state.Portfolio;
import rails.game.state.Typable;

// FIXME: Trains a tricky as they can swap their type
// This change has to be tracked if used in a PortfolioMap
public class Train extends OwnableItem<Train> implements Typable<TrainType> {

    protected TrainCertificateType certificateType;
    
    protected final GenericState<TrainType> type = GenericState.create(this, "type");
    
    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected final BooleanState obsolete = BooleanState.create(this, "obsolete");
    
    private Portfolio<Train> portfolio;

    protected static Logger log =
            LoggerFactory.getLogger(Train.class.getPackage().getName());

    protected Train(Item parent, String id) {
        super(parent, id, Train.class);
    }
    // TODO: Train creation is shared by three classes, simplify that
    public static Train create(Item parent, String id, TrainCertificateType certType, TrainType type)
            throws ConfigurationException {
        Train train = certType.createTrain();
        train.setCertificateType(certType);
        train.setType(type);
        return train;
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
        return isAssigned() ? type.value() : null;
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
        return type.value() != null;
    }
    
    public boolean isPermanent() {
        return certificateType.isPermanent();
    }
    
    public String getId() {
        return isAssigned() ? type.value().getName() : certificateType.getName();
    }

    public boolean isObsolete() {
        return obsolete.value();
    }

    public void setRusted() {
        // FIXME: Awful long list of calls
        GameManager.getInstance().getBank().getScrapHeap().getPortfolioModel().getTrainsModel().getPortfolio().moveInto(this);
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

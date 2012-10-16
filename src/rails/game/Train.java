package rails.game;

import com.google.common.collect.ComparisonChain;

import rails.common.parser.ConfigurationException;
import rails.game.state.BooleanState;
import rails.game.state.Creatable;
import rails.game.state.GenericState;
import rails.game.state.Ownable;


public class Train extends RailsOwnableItem<Train> implements Creatable {

    protected TrainCertificateType certificateType;
    
    protected final GenericState<TrainType> type = GenericState.create(this, "type");
    
    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected final BooleanState obsolete = BooleanState.create(this, "obsolete");

    /**
     * Used by Configure (via reflection) only
     */
    public Train(RailsItem parent, String id) {
        super(parent, id, Train.class);
    }
    
    public static Train create(RailsItem parent, String id, TrainCertificateType certType, TrainType type)
            throws ConfigurationException {
        Train train = certType.createTrain(parent, id);
        train.setCertificateType(certType);
        train.setType(type);
        return train;
    }
    
    @Override
    public RailsItem getParent() {
        return (RailsItem)super.getParent();
    }

    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
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
    
    public boolean isObsolete() {
        return obsolete.value();
    }

    public void setRusted() {
        this.moveTo(GameManager.getInstance().getBank().getScrapHeap());
    }

    public void setObsolete() {
        obsolete.set(true);
    }

    public boolean canBeExchanged() {
        return certificateType.nextCanBeExchanged();
    }

    @Override
    public String toText() {
        return isAssigned() ? type.value().getName() : certificateType.toText();
    }

    public boolean isTradeable() {
        return tradeable;
    }

    public void setTradeable(boolean tradeable) {
        this.tradeable = tradeable;
    }

    @Override
    public int compareTo(Ownable other) {
        if (other instanceof Train) {
            Train oTrain = (Train)other;
            return ComparisonChain.start()
                    .compare(this.getCertType(), oTrain.getCertType())
                    .compare(this.getId(), oTrain.getId())
                    .result();
        }
        return 0;
    }
    
}

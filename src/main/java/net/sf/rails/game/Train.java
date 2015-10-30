package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Creatable;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Ownable;

import com.google.common.collect.ComparisonChain;

public class Train extends RailsOwnableItem<Train> implements Creatable {

    protected TrainCertificateType certificateType;
    
    protected final GenericState<TrainType> type = GenericState.create(this, "type");
    
    /** Some specific trains cannot be traded between companies */
    protected boolean tradeable = true;

    protected final BooleanState obsolete = BooleanState.create(this, "obsolete");

    // sorting id to correctly sort them inside a portfolio
    // this is a workaround to have 2.0 compatible with 1.x save files
    // it should be removed in the mid-term by selecting trains from a portfolio based only on type, not on id
    protected int sortingId;
    
    /**
     * Used by Configure (via reflection) only
     */
    public Train(RailsItem parent, String id) {
        super(parent, id, Train.class);
    }
    
    public static Train create(RailsItem parent, int uniqueId, TrainCertificateType certType, TrainType type)
            throws ConfigurationException {
        String id = certType.getId() + "_"+ uniqueId;
        Train train = certType.createTrain(parent, id, uniqueId);
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

    public void setSortingId(int sortingId) {
        this.sortingId = sortingId;
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

    /**
     * @return true => train is express train; false =>
     */
    public boolean isETrain() {
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
        // if not on scrapheap already
        if (this.getOwner() != Bank.getScrapHeap(this)) {
            this.moveTo(Bank.getScrapHeap(this));
        }
    }

    public void setObsolete() {
        obsolete.set(true);
    }

    public boolean canBeExchanged() {
        return certificateType.nextCanBeExchanged();
    }
    
    public void discard() {
        BankPortfolio discardTo;
        if (isObsolete()) {
            discardTo = Bank.getScrapHeap(this);
        } else {
            discardTo = getRoot().getTrainManager().discardTo();
        }
        String discardText =  LocalText.getText("CompanyDiscardsTrain", getOwner().getId(), this.toText(), discardTo.getId());
        ReportBuffer.add(this, discardText);
        this.moveTo(discardTo);
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
                    .compare(this.sortingId, oTrain.sortingId)
                    .result();
        }
        return 0;
    }
    
}

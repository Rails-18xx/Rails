package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.state.*;

import com.google.common.collect.ComparisonChain;

public class Train extends RailsOwnableItem<Train> implements Creatable {

    //protected TrainCardType trainCardType;
    protected TrainCard trainCard;

    protected final GenericState<TrainType> type = new GenericState<>(this, "type");

    /**
     * Some specific trains cannot be traded between companies
     */
    //protected boolean tradeable = true;

    protected final BooleanState obsolete = new BooleanState(this, "obsolete");

    protected String name;

    // sorting id to correctly sort them inside a portfolio
    // this is a workaround to have 2.0 compatible with 1.x save files
    // it should be removed in the mid-term by selecting trains from a portfolio based only on type, not on id
    /* No longer used
    protected int sortingId;
     */

    /**
     * Used by Configure (via reflection) only
     */
    public Train(RailsItem parent, String id) {
        super(parent, id, Train.class);
    }

    /* obsolete
    public static Train create(RailsItem parent, int uniqueId, TrainCertificateType certType, TrainType trainType)
            throws ConfigurationException {
        String id = trainType.getName() + "_" + uniqueId;
        Train train = certType.createTrain(parent, id, uniqueId);
        train.setCertificateType(certType);
        train.setType(trainType);
        return train;
    }
     */

    @Override
    public RailsItem getParent() {
        return super.getParent();
    }

    @Override
    public RailsRoot getRoot() {
        return super.getRoot();
    }

    /*
    public void setSortingId(int sortingId) {
        this.sortingId = sortingId;
    }

     */

    public void setCard(TrainCard card) {
        this.trainCard = card;
    }

    public void setType(TrainType type) {
        this.type.set(type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the type.
     */
    public TrainCardType getCardType() {
        return trainCard.getType();
    }

    public TrainCard getCard() {
        return trainCard;
    }

    public TrainType getType() {
        return isAssigned() ? type.value() : null;
    }

    /**
     * import rails.game.state.AbstractItem;
     *
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
        return trainCard.getType().isPermanent();
    }

    public boolean isObsolete() {
        return obsolete.value();
    }

    public void setRusted() {
        // if not on scrapheap already
        if (trainCard.getOwner() != Bank.getScrapHeap(this)) {
            trainCard.moveTo(Bank.getScrapHeap(this));
        }
    }

    public void setObsolete() {
        obsolete.set(true);
    }

    public boolean canBeExchanged() {
        return type.value().canBeExchanged();
    }

    public void discard() {
        trainCard.discard ();
    }

    @Override
    public String toText() {
        return isAssigned() ? type.value().getName() : type.toText();
    }

    public boolean isTradeable() {
        return trainCard.isTradeable();
    }

    // To avoid a lot of changes
    public Owner getOwner() {
        return trainCard.getOwner();
    }

    /*
    public void setTradeable(boolean tradeable) {
        this.tradeable = tradeable;
    }*/

    @Override
    public int compareTo(Ownable other) {
        if (other instanceof Train) {
            Train oTrain = (Train) other;
            return ComparisonChain.start()
                    .compare(this.getCardType(), oTrain.getCardType())
                    .compare(this.getId(), oTrain.getId())
                    .result();
        }
        return 0;
    }

}

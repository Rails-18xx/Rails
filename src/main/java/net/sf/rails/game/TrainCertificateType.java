package net.sf.rails.game;

import java.util.*;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TrainCertificateType indicates the type of a TrainCertficate
 * TrainCertficates can be multi-sided (thus provide several TrainType options)
 */
public class TrainCertificateType extends RailsAbstractItem implements Configurable, Comparable<TrainCertificateType> {
    // FIXME: Rails 2.0, move this to some default .xml!
    private final static String DEFAULT_TRAIN_CLASS = "net.sf.rails.game.Train";

    // Static definitions
    private int index; // for sorting

    private int quantity = 0;
    private boolean infiniteQuantity = false;

    private List<TrainType> potentialTrainTypes = new ArrayList<TrainType>(2);

    private Map<Integer, String> newPhaseNames;

    private boolean permanent = true;
    private boolean obsoleting = false;

    private boolean canBeExchanged = false;
    private int cost;
    private int exchangeCost;

    // store the trainClassName to allow dual configuration
    private String trainClassName = DEFAULT_TRAIN_CLASS;
    private Class<? extends Train> trainClass;

    /**
     * In some cases, trains start their life in the Pool, default is IPO
     */
    private String initialPortfolio = "IPO";

    // Dynamic state variables
    private final IntegerState numberBoughtFromIPO = IntegerState.create(this, "numberBoughtFromIPO");
    private final BooleanState available = new BooleanState(this, "available");
    private final BooleanState rusted = new BooleanState(this, "rusted");

    private static final Logger log = LoggerFactory.getLogger(TrainCertificateType.class);

    private TrainCertificateType(TrainManager parent, String id, int index) {
        super(parent, id);
        this.index = index;
    }

    public static TrainCertificateType create(TrainManager parent, String id, int index) {
        return new TrainCertificateType(parent, id, index);
    }

    @Override
    public TrainManager getParent() {
        return (TrainManager) super.getParent();
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        trainClassName = tag.getAttributeAsString("class", trainClassName);
        trainClass = Configure.getClassForName(Train.class, trainClassName);

        // Quantity
        quantity = tag.getAttributeAsInteger("quantity", quantity);
        quantity += tag.getAttributeAsInteger("quantityIncrement", 0);

        // From where is this type initially available
        initialPortfolio =
                tag.getAttributeAsString("initialPortfolio",
                        initialPortfolio);

        // New style phase changes (to replace 'startPhase' attribute and <Sub> tag)
        List<Tag> newPhaseTags = tag.getChildren("NewPhase");
        if (newPhaseTags != null) {
            int index;
            String phaseName;
            newPhaseNames = new HashMap<Integer, String>();
            for (Tag newPhaseTag : newPhaseTags) {
                phaseName = newPhaseTag.getAttributeAsString("phaseName");
                if (!Util.hasValue(phaseName)) {
                    throw new ConfigurationException("TrainType " + getId() + " has NewPhase without phase name");
                }
                index = newPhaseTag.getAttributeAsInteger("trainIndex", 1);
                newPhaseNames.put(index, phaseName);
            }
        }

        // Exchangeable
        Tag swapTag = tag.getChild("Exchange");
        if (swapTag != null) {
            exchangeCost = swapTag.getAttributeAsInteger("cost", 0);
            canBeExchanged = (exchangeCost > 0);
        }

        // Can run as obsolete train
        obsoleting = tag.getAttributeAsBoolean("obsoleting");

    }

    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        if (quantity == -1) {
            infiniteQuantity = true;
        } else if (quantity <= 0) {
            throw new ConfigurationException("Invalid quantity " + quantity + " for train cert type " + this);
        }
    }

    public Map<Integer, String> getNewPhaseNames() {
        return newPhaseNames;
    }

    public Train createTrain(RailsItem parent, String id, int sortingId) throws ConfigurationException {
        Train train = Configure.create(trainClass, parent, id);
        train.setSortingId(sortingId);
        return train;
    }

    public List<TrainType> getPotentialTrainTypes() {
        return potentialTrainTypes;
    }

    protected void addPotentialTrainType(TrainType type) {
        potentialTrainTypes.add(type);
    }

    /**
     * @return Returns the available.
     */
    public boolean isAvailable() {
        return available.value();
    }

    /**
     * Make a train type available for buying by public companies.
     */
    public void setAvailable() {
        available.set(true);
    }

    public void setRusted() {
        rusted.set(true);
    }

    public boolean hasRusted() {
        return rusted.value();
    }

    public boolean isPermanent() {
        return permanent;
    }

    public boolean isObsoleting() {
        return obsoleting;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean hasInfiniteQuantity() {
        return infiniteQuantity;
    }

    public boolean nextCanBeExchanged() {
        return canBeExchanged;
    }

    public void addToBoughtFromIPO() {
        numberBoughtFromIPO.add(1);
    }

    public int getNumberBoughtFromIPO() {
        return numberBoughtFromIPO.value();
    }

    public int getCost() {
        return cost;
    }

    public int getExchangeCost() {
        return exchangeCost;
    }

    public String getInitialPortfolio() {
        return initialPortfolio;
    }

    public String getInfo() {
        StringBuilder b = new StringBuilder("<html>");
        b.append(LocalText.getText("TrainInfo", getId(), Bank.format(this, cost), quantity));
        if (b.length() == 6) b.append(LocalText.getText("None"));

        return b.toString();
    }

    public int getIndex() {
        return index;
    }

    // Comparable interface
    public int compareTo(TrainCertificateType o) {
        return ((Integer) index).compareTo(o.getIndex());
    }

}

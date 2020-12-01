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
public class TrainCardType extends RailsAbstractItem implements Configurable, Comparable<TrainCardType> {
    // FIXME: Rails 2.0, move this to some default .xml!
    private final static String DEFAULT_TRAIN_CLASS = "net.sf.rails.game.Train";

    // Static definitions
    private int index; // for sorting
    private String name;

    private int quantity = 0;
    private boolean infiniteQuantity = false;

    private List<TrainType> potentialTrainTypes = new ArrayList<>(2);

    private Map<Integer, String> newPhaseNames;

    private boolean permanent = true;
    private boolean obsoleting = false;

    //private int cost;
    /**
     * Here used to determine canBeExchanged. Value is passed tp TrainType.
     */
    private int exchangeCost;

    // store the trainClassName to allow dual configuration
    // TODO Shouldn't this also fall through to the Train class?
    private String trainClassName = DEFAULT_TRAIN_CLASS;
    private Class<? extends Train> trainClass;

    /**
     * In some cases, trains start their life in the Pool, default is IPO
     */
    private String initialPortfolio = "IPO";

    /**
     * Train types released at the same time as this one
     */
    protected List<TrainCardType> alsoReleased;
    private String alsoReleasedNames;

    // Dynamic state variables
    private final IntegerState numberBoughtFromIPO = IntegerState.create(this, "numberBoughtFromIPO");
    private final BooleanState available = new BooleanState(this, "available");
    private final BooleanState rusted = new BooleanState(this, "rusted");

    private static final Logger log = LoggerFactory.getLogger(TrainCardType.class);

    private TrainCardType(TrainManager parent, String id, int index) {
        super(parent, id);
        this.index = index;
    }

    public static TrainCardType create(TrainManager parent, String id, int index) {
        return new TrainCardType(parent, id, index);
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

        // Released at the same time
        alsoReleasedNames = tag.getAttributeAsString("alsoReleased", null);

        // New style phase changes (to replace 'startPhase' attribute and <Sub> tag)
        List<Tag> newPhaseTags = tag.getChildren("NewPhase");
        if (newPhaseTags != null) {
            int index;
            String phaseName;
            newPhaseNames = new HashMap<>();
            for (Tag newPhaseTag : newPhaseTags) {
                phaseName = newPhaseTag.getAttributeAsString("phaseName");
                if (!Util.hasValue(phaseName)) {
                    throw new ConfigurationException("TrainType " + getId() + " has NewPhase without phase name");
                }
                index = newPhaseTag.getAttributeAsInteger("trainIndex", 1);
                newPhaseNames.put(index, phaseName);
            }
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

        if (alsoReleasedNames != null) {
            alsoReleased = new ArrayList<>();
            TrainCardType otherTCType;
            for (String otherTCTypeName : alsoReleasedNames.split(",")) {
                otherTCType = getRoot().getTrainManager().getCardTypeByName(otherTCTypeName);
                if (otherTCType != null) alsoReleased.add (otherTCType);
            }
        }
    }

    public Map<Integer, String> getNewPhaseNames() {
        return newPhaseNames;
    }

    /* Obsolete
    public Train createTrain(RailsItem parent, String id, int sortingId) throws ConfigurationException {
        Train train = Configure.create(trainClass, parent, id);
        train.setSortingId(sortingId);
        return train;
    }
     */

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

    public List<TrainCardType> getAlsoReleased() {
        return alsoReleased;
    }

    /* Obsolete?
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

     */

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

    public void addToBoughtFromIPO() {
        numberBoughtFromIPO.add(1);
    }

    public int getNumberBoughtFromIPO() {
        return numberBoughtFromIPO.value();
    }

    public String getInitialPortfolio() {
        return initialPortfolio;
    }

    public boolean isDual() {
        return potentialTrainTypes.size() == 2;
    }

    /* No longer used
    public Class<? extends Train> getTrainClass() {
        return trainClass;
    }
     */

    public String getInfo() {
        StringBuilder b = new StringBuilder("<html>");
        b.append(LocalText.getText("TrainCardInfo", getId(), quantity));
        if (b.length() == 6) b.append(LocalText.getText("None"));

        return b.toString();
    }

    public int getIndex() {
        return index;
    }

    // Comparable interface
    public int compareTo(TrainCardType o) {
        return Integer.compare(index, o.getIndex());
    }

    @Override
    public String toString() { return getId(); }

}

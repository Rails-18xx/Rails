package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.util.*;

public class TrainCertificateType {

    protected String name;
    protected int quantity = 0;
    protected boolean infiniteQuantity = false;

    protected String startedPhaseName = null;
    // Phase startedPhase;

    protected List<TrainType> potentialTrainTypes = new ArrayList<TrainType>(2);
    
    protected Map<Integer, String> newPhaseNames;
    
    protected Map<Integer, String> rustedTrainTypeNames = null;
    protected Map<Integer, TrainCertificateType> rustedTrainType = null;
    
    protected boolean permanent = true;
    protected boolean obsoleting = false;

    protected String releasedTrainTypeNames = null;
    protected List<TrainCertificateType> releasedTrainTypes = null;

    protected boolean canBeExchanged = false;
    protected int cost;
    protected int exchangeCost;
    
    protected String trainClassName = "rails.game.Train";
    protected Class<? extends Train> trainClass;

    // State variables
    protected IntegerState numberBoughtFromIPO;
    protected BooleanState available;
    protected BooleanState rusted;

    // References
    protected TrainManager trainManager;

    /** In some cases, trains start their life in the Pool */
    protected String initialPortfolio = "IPO";

    protected static Logger log =
        Logger.getLogger(TrainCertificateType.class.getPackage().getName());
    
    public TrainCertificateType () {
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {

        trainClassName = tag.getAttributeAsString("class", trainClassName);
        try {
            trainClass = Class.forName(trainClassName).asSubclass(Train.class);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Class " + trainClassName
                    + "not found", e);
        }

        // Name
        name = tag.getAttributeAsString("name");

        // Quantity
        quantity = tag.getAttributeAsInteger("quantity", quantity);
        quantity += tag.getAttributeAsInteger("quantityIncrement", 0);

        // Phase started
        startedPhaseName = tag.getAttributeAsString("startPhase", "");

        // Train type rusted
        String rustedTrainTypeName1 = tag.getAttributeAsString("rustedTrain");
        if (Util.hasValue(rustedTrainTypeName1)) {
            rustedTrainTypeNames = new HashMap<Integer, String>();
            rustedTrainTypeNames.put(1, rustedTrainTypeName1);
        }

        // Other train type released for buying
        releasedTrainTypeNames = tag.getAttributeAsString("releasedTrain");

        // From where is this type initially available
        initialPortfolio =
            tag.getAttributeAsString("initialPortfolio",
                    initialPortfolio);
        
        // Configure any actions on other than the first train of a type
        List<Tag> subs =  tag.getChildren("Sub");
        if (subs != null) {
            for (Tag sub : tag.getChildren("Sub")) {
                int index = sub.getAttributeAsInteger("index");
                rustedTrainTypeName1 = sub.getAttributeAsString("rustedTrain");
                if (rustedTrainTypeNames == null) {
                    rustedTrainTypeNames = new HashMap<Integer, String>();
                }
                rustedTrainTypeNames.put(index, rustedTrainTypeName1);
            }
        }
        
        // New style phase changes (to replace 'startPhase' attribute and <Sub> tag)
        List<Tag> newPhaseTags = tag.getChildren("NewPhase");
        if (newPhaseTags != null) {
            int index;
            String phaseName;
            newPhaseNames = new HashMap<Integer, String>();
            for (Tag newPhaseTag : newPhaseTags) {
                phaseName = newPhaseTag.getAttributeAsString("phaseName");
                if (!Util.hasValue(phaseName)) {
                    throw new ConfigurationException ("TrainType "+name+" has NewPhase without phase name");
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

        // Final initialisations
        numberBoughtFromIPO = new IntegerState(name + "-trains_Bought", 0);
        available = new BooleanState(name + "-trains_Available", false);
        rusted = new BooleanState(name + "-trains_Rusted", false);

    }

    public void finishConfiguration (GameManagerI gameManager) 
    throws ConfigurationException {

        trainManager = gameManager.getTrainManager();
        
        if (name == null) {
            throw new ConfigurationException("No name specified for TrainType");
        }
        
        if (quantity == -1) {
            infiniteQuantity = true;
        } else if (quantity <= 0) {
            throw new ConfigurationException("Invalid quantity "+quantity+" for train cert type "+name);
        }
    }

    public Map<Integer, String> getNewPhaseNames() {
        return newPhaseNames;
    }

    public TrainI createTrain () throws ConfigurationException {

        TrainI train;
        try {
            train = trainClass.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "Cannot instantiate class " + trainClassName, e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Cannot access class "
                    + trainClassName
                    + "constructor", e);
        }
        return train;
    }

    public List<TrainType> getPotentialTrainTypes() {
        return potentialTrainTypes;
    }

    protected void addPotentialTrainType (TrainType type) {
        potentialTrainTypes.add(type);
    }
    
    /**
     * @return Returns the available.
     */
    public boolean isAvailable() {
        return available.booleanValue();
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
        return rusted.booleanValue();
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the releasedTrainTypeName.
     */
    public String getReleasedTrainTypeNames() {
        return releasedTrainTypeNames;
    }

    /**
     * @return Returns the rustedTrainTypeName.
     */
    public Map<Integer,String> getRustedTrainTypeNames() {
        return rustedTrainTypeNames;
    }

    /**
     * @param releasedTrainType The releasedTrainType to set.
     */
    public void setReleasedTrainTypes(List<TrainCertificateType> releasedTrainTypes) {
        this.releasedTrainTypes = releasedTrainTypes;
    }

    /**
     * @param rustedTrainType The rustedTrainType to set.
     */
    public void setRustedTrainType(int index, TrainCertificateType rustedTrainType) {
        if (this.rustedTrainType == null) {
            this.rustedTrainType = new HashMap<Integer, TrainCertificateType>();
        }
        this.rustedTrainType.put(index, rustedTrainType);
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

   /**
     * @return Returns the releasedTrainTypes.
     */
    public List<TrainCertificateType> getReleasedTrainTypes() {
        return releasedTrainTypes;
    }

    /**
     * @return Returns the rustedTrainType.
     */
    public TrainCertificateType getRustedTrainType(int index) {
        if (rustedTrainType == null) return null;
        return rustedTrainType.get(index);
    }

    /**
     * @return Returns the startedPhaseName.
     */
    public String getStartedPhaseName() {
        return startedPhaseName;
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
        return numberBoughtFromIPO.intValue();
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
        StringBuilder b = new StringBuilder ("<html>");
        b.append(LocalText.getText("TrainInfo", name, Bank.format(cost), quantity));
        if (Util.hasValue(startedPhaseName)) {
            appendInfoText(b, LocalText.getText("StartsPhase", startedPhaseName));
        }
        if (rustedTrainTypeNames != null) {
            appendInfoText(b, LocalText.getText("RustsTrains", rustedTrainTypeNames.get(1)));
            // Ignore any 'Sub' cases for now
        }
        if (releasedTrainTypeNames != null) {
            appendInfoText(b, LocalText.getText("ReleasesTrains", releasedTrainTypeNames));
        }
        if (b.length() == 6) b.append(LocalText.getText("None"));

        return b.toString();
    }

    private void appendInfoText (StringBuilder b, String text) {
        if (text == null || text.length() == 0) return;
        if (b.length() > 6) b.append("<br>");
        b.append(text);
    }

    public String toString() {
        return name;
    }
}

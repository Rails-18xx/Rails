/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Phase.java,v 1.19 2010/03/21 17:43:50 evos Exp $ */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.util.Util;

public class Phase implements PhaseI {

    protected int index;

    protected String name;

    protected String realName;

    protected List <String> tileColours;
    protected String tileColoursString;
    protected Map<String, Integer> tileLaysPerColour;
    /** For how many turns can extra tiles be laid (per company type and colour)?
     * Default: infinite.
     * <p>This attribute is only used during configuration. It is finally passed to CompanyType.
     * NOT CLONED from previous phase.*/
    protected Map<String, Integer> tileLaysPerColourTurns;

    protected boolean privateSellingAllowed = false;

    protected boolean privatesClose = false;

    protected int numberOfOperatingRounds = 1;

    protected int offBoardRevenueStep = 1;

    /** New style train limit configuration.
     */
    protected int trainLimitStep = 1;

    protected int privatesRevenueStep = 1; // sfy 1889

    protected boolean trainTradingAllowed = false;

    /** May company buy more than one Train from the Bank per turn? */
    protected boolean oneTrainPerTurn = false;

    /** May company buy more than one Train of each type from the Bank per turn? */
    protected boolean oneTrainPerTypePerTurn = false;

    /** Is loan taking allowed */
    protected boolean loanTakingAllowed = false;

    /** Previous phase, defining the current one's defaults */
    protected Phase defaults = null;

    /** Items to close if a phase gets activated */
    protected List<Closeable> closedObjects = null;

    /** Train types to rust or obsolete if a phase gets activated */
    protected List<TrainCertificateType> rustedTrains;
    String rustedTrainNames;

    /** Train types to release (make available for buying) if a phase gets activated */
    protected List<TrainCertificateType> releasedTrains;
    String releasedTrainNames;

    /** Actions for this phase.
     * When this phase is activated, the GameManager method phaseAction() will be called,
     * which in turn will call the current Round, which is responsible to handle the action.
     * <p>
     * Set actions have a name and may have a value. */
    protected Map<String, String> actions;

    private GameManagerI gameManager;
    private Portfolio lastTrainBuyer;

    protected String extraInfo = "";

    /** A HashMap to contain phase-dependent parameters
     * by name and value.
     */
    protected Map<String, String> parameters = null;

    protected static Logger log =
        Logger.getLogger(Phase.class.getPackage().getName());

    public Phase(int index, String name, Phase previousPhase) {
        this.index = index;
        this.name = name;
        this.defaults = previousPhase;
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        if (defaults != null) {
            tileColours = defaults.tileColours;
            tileLaysPerColour = defaults.tileLaysPerColour;
            privateSellingAllowed = defaults.privateSellingAllowed;
            numberOfOperatingRounds = defaults.numberOfOperatingRounds;
            offBoardRevenueStep = defaults.offBoardRevenueStep;
            trainLimitStep = defaults.trainLimitStep;
            privatesRevenueStep = defaults.privatesRevenueStep;
            trainTradingAllowed = defaults.trainTradingAllowed;
            oneTrainPerTurn = defaults.oneTrainPerTurn;
            oneTrainPerTypePerTurn = defaults.oneTrainPerTypePerTurn;
            loanTakingAllowed = defaults.loanTakingAllowed;
            if (defaults.parameters != null) {
                this.parameters = new HashMap<String, String>();
                for (String key : defaults.parameters.keySet()) {
                    parameters.put(key, defaults.parameters.get(key));
                }
            }
        }

        // Real name (as in the printed game)
        realName = tag.getAttributeAsString("realName", null);

        // Allowed tile colours
        Tag tilesTag = tag.getChild("Tiles");
        if (tilesTag != null) {
            String colourList = tilesTag.getAttributeAsString("colour", null);
            if (Util.hasValue(colourList)) {
                tileColoursString = colourList;
                tileColours = new ArrayList<String>();
                String[] colourArray = colourList.split(",");
                for (int i = 0; i < colourArray.length; i++) {
                    tileColours.add(colourArray[i]);
                }
            }

            List<Tag> laysTag = tilesTag.getChildren("Lays");
            if (laysTag != null && !laysTag.isEmpty()) {
                // First create a copy of the previous map, if it exists, otherwise create the map.
                if (tileLaysPerColour == null) {
                    tileLaysPerColour = new HashMap<String, Integer>(4);
                } else if (!tileLaysPerColour.isEmpty()) {
                    // Wish there was a one-liner to deep-clone a map.  Does Guava have one?
                    Map <String, Integer> newTileLaysPerColour = new HashMap <String, Integer>(4);
                    for (String key : tileLaysPerColour.keySet()) {
                        newTileLaysPerColour.put (key, tileLaysPerColour.get(key));
                    }
                    tileLaysPerColour = newTileLaysPerColour;
                }

                for (Tag layTag : laysTag) {
                    String colourString = layTag.getAttributeAsString("colour");
                    if (!Util.hasValue(colourString))
                        throw new ConfigurationException(
                        "No colour entry for number of tile lays");
                    String typeString = layTag.getAttributeAsString("companyType");
                    if (!Util.hasValue(typeString))
                        throw new ConfigurationException(
                        "No company type entry for number of tile lays");
                    int number = layTag.getAttributeAsInteger("number", 1);
                    int validForTurns =
                        layTag.getAttributeAsInteger("occurrences", 0);

                    String key = typeString + "~" + colourString;
                    if (number == 1) {
                        tileLaysPerColour.remove(key);
                    } else {
                        tileLaysPerColour.put(key, number);
                    }

                    if (validForTurns != 0) {
                        if (tileLaysPerColourTurns == null) {
                            tileLaysPerColourTurns = new HashMap<String, Integer>(4);
                        }
                        tileLaysPerColourTurns.put(key, validForTurns);
                    }
                }
            }
        }

        // Private-related properties
        Tag privatesTag = tag.getChild("Privates");
        if (privatesTag != null) {
            privateSellingAllowed =
                privatesTag.getAttributeAsBoolean("sellingAllowed",
                        privateSellingAllowed);
            privatesClose = privatesTag.getAttributeAsBoolean("close", false);
            privatesRevenueStep = privatesTag.getAttributeAsInteger("revenueStep", privatesRevenueStep); // sfy 1889
        }

        // Operating rounds
        Tag orTag = tag.getChild("OperatingRounds");
        if (orTag != null) {
            numberOfOperatingRounds =
                orTag.getAttributeAsInteger("number",
                        numberOfOperatingRounds);
        }

        // Off-board revenue steps (starts at 1)
        Tag offBoardTag = tag.getChild("OffBoardRevenue");
        if (offBoardTag != null) {
            offBoardRevenueStep =
                offBoardTag.getAttributeAsInteger("step",
                        offBoardRevenueStep);
        }

        Tag trainsTag = tag.getChild("Trains");
        if (trainsTag != null) {
            trainLimitStep = trainsTag.getAttributeAsInteger("limitStep", trainLimitStep);
            rustedTrainNames = trainsTag.getAttributeAsString("rusted", null);
            releasedTrainNames = trainsTag.getAttributeAsString("released", null);
            trainTradingAllowed =
                trainsTag.getAttributeAsBoolean("tradingAllowed",
                        trainTradingAllowed);
            oneTrainPerTurn =
                trainsTag.getAttributeAsBoolean("onePerTurn",
                        oneTrainPerTurn);
            oneTrainPerTypePerTurn =
                trainsTag.getAttributeAsBoolean("onePerTypePerTurn",
                        oneTrainPerTypePerTurn);
        }

        Tag loansTag = tag.getChild("Loans");
        if (loansTag != null) {
            loanTakingAllowed = loansTag.getAttributeAsBoolean("allowed",
                    loanTakingAllowed);
        }

        Tag parameterTag = tag.getChild("Parameters");
        if (parameterTag != null) {
            if (parameters == null) parameters = new HashMap<String, String>();
            Map<String,String> attributes = parameterTag.getAttributes();
            for (String key : attributes.keySet()) {
                parameters.put (key, attributes.get(key));
            }
        }

        Tag setTag = tag.getChild("Action");
        if (setTag != null) {
            if (actions == null) actions = new HashMap<String, String>();
            String key = setTag.getAttributeAsString("name");
            if (!Util.hasValue(key)) {
                throw new ConfigurationException ("Phase "+name+": <Set> without action name");
            }
            String value = setTag.getAttributeAsString("value", null);
            actions.put (key, value);
        }

        // Extra info text(usually related to extra-share special properties)
        Tag infoTag = tag.getChild("Info");
        if (infoTag != null) {
            String infoKey = infoTag.getAttributeAsString("key");
            String[] infoParms = infoTag.getAttributeAsString("parm", "").split(",");
            extraInfo += "<br>"+LocalText.getText(infoKey, (Object[])infoParms);
        }

    }

    public void finishConfiguration (GameManagerI gameManager)
    throws ConfigurationException {

        this.gameManager = gameManager;
        TrainManager trainManager = gameManager.getTrainManager();
        TrainCertificateType type;

        if (rustedTrainNames != null) {
            rustedTrains = new ArrayList<TrainCertificateType>(2);
            for (String typeName : rustedTrainNames.split(",")) {
                type = trainManager.getCertTypeByName(typeName);
                if (type == null) {
                    throw new ConfigurationException (" Unknown rusted train type '"+typeName+"' for phase '"+name+"'");
                }
                rustedTrains.add(type);
                type.setPermanent(false);
            }
        }

        if (releasedTrainNames != null) {
            releasedTrains = new ArrayList<TrainCertificateType>(2);
            for (String typeName : releasedTrainNames.split(",")) {
                type = trainManager.getCertTypeByName(typeName);
                if (type == null) {
                    throw new ConfigurationException (" Unknown released train type '"+typeName+"' for phase '"+name+"'");
                }
                releasedTrains.add(type);
            }
        }

        // Push any extra tile lay turns to the appropriate company type.
        if (tileLaysPerColourTurns != null) {
            CompanyManagerI companyManager = gameManager.getCompanyManager();
            companyManager.addExtraTileLayTurnsInfo (tileLaysPerColourTurns);
        }
        tileLaysPerColourTurns = null;  // We no longer need it.
    }

    /** Called when a phase gets activated */
    public void activate() {
        log.debug("Phase " + name + " activated");
        if (closedObjects != null && !closedObjects.isEmpty()) {
            for (Closeable object : closedObjects) {
                log.debug("Closing object " + object.toString());
                object.close();
            }
        }

        TrainManager trainManager = gameManager.getTrainManager();

        if (rustedTrains != null && !rustedTrains.isEmpty()) {
            for (TrainCertificateType type : rustedTrains) {
                trainManager.rustTrainType(type, lastTrainBuyer);
            }
        }

        if (releasedTrains != null && !releasedTrains.isEmpty()) {
            for (TrainCertificateType type : releasedTrains) {
                trainManager.makeTrainAvailable(type);
            }
        }

        if (actions != null && !actions.isEmpty()) {
            for (String actionName : actions.keySet()) {
                gameManager.processPhaseAction (actionName, actions.get(actionName));
            }
        }
    }

    public void setLastTrainBuyer(Portfolio lastTrainBuyer) {
        this.lastTrainBuyer = lastTrainBuyer;
    }

    public String getInfo() {
        return extraInfo;
    }

    public boolean isTileColourAllowed(String tileColour) {
        return tileColours.contains(tileColour);
    }

    public List<String> getTileColours() {
        return tileColours;
    }

    public String getTileColoursString() {
        return tileColoursString;
    }

    public int getTileLaysPerColour (String companyTypeName, String colourName) {

        if (tileLaysPerColour == null) return 1;

        String key = companyTypeName + "~" + colourName;
        if (tileLaysPerColour.containsKey(key)) {
            return tileLaysPerColour.get(key);
        } else {
            return 1;
        }
    }

    public int getTrainLimitStep() {
        return trainLimitStep;
    }

    public int getTrainLimitIndex() {
        return trainLimitStep - 1;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getRealName() {
        return realName;
    }

    /**
     * @return Returns the privatesClose.
     */
    public boolean doPrivatesClose() {
        return privatesClose;
    }

    /**
     * @return Returns the privateSellingAllowed.
     */
    public boolean isPrivateSellingAllowed() {
        return privateSellingAllowed;
    }
    // sfy 1889
    public int getPrivatesRevenueStep() {
        return privatesRevenueStep;
    }
    public boolean isTrainTradingAllowed() {
        return trainTradingAllowed;
    }

    public boolean canBuyMoreTrainsPerTurn() {
        return !oneTrainPerTurn;
    }

    public boolean canBuyMoreTrainsPerTypePerTurn() {
        return !oneTrainPerTypePerTurn;
    }

    public boolean isLoanTakingAllowed() {
        return loanTakingAllowed;
    }

    public int getNumberOfOperatingRounds() {
        return numberOfOperatingRounds;
    }

    public List<TrainCertificateType> getRustedTrains() {
        return rustedTrains;
    }

    public List<TrainCertificateType> getReleasedTrains() {
        return releasedTrains;
    }

    /**
     * @return Returns the offBoardRevenueStep.
     */
    public int getOffBoardRevenueStep() {
        return offBoardRevenueStep;
    }

    public void addObjectToClose(Closeable object) {
        if (closedObjects == null) {
            closedObjects = new ArrayList<Closeable>(4);
        }
        if (!closedObjects.contains(object)) closedObjects.add(object);
    }

    public String getParameterAsString (String key) {
        if (parameters != null) {
            return parameters.get(key);
        } else {
            return null;
        }
    }

    public int getParameterAsInteger (String key) {
        String stringValue = getParameterAsString(key);
        if (stringValue == null) {
            return 0;
        }
        try {
            return Integer.parseInt(stringValue);
        } catch (Exception e) {
            log.error ("Error while parsing parameter "+key+" in phase "+name, e);
            return 0;
        }

    }

    public List<Closeable> getClosedObjects() {
        return closedObjects;
    }

    @Override
    public String toString() {
        if (realName == null) {
            return name;
        } else {
            return name + " [" + realName+ "]";
        }
    }
}

package net.sf.rails.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;


public class Phase extends RailsModel implements Configurable {

    private static Logger log =
            LoggerFactory.getLogger(Phase.class);

    // static data
    private final int index;
    
    private String name;
    private ImmutableList <String> tileColours;
    private Map<String, Integer> tileLaysPerColour;

    /** For how many turns can extra tiles be laid (per company type and colour)?
     * Default: infinite.
     * <p>This attribute is only used during configuration. It is finally passed to CompanyType.
     * NOT CLONED from previous phase.*/
    private Map<String, Integer> tileLaysPerColourTurns;
    private boolean privateSellingAllowed = false;
    private boolean privatesClose = false;
    private int numberOfOperatingRounds = 1;
    private int offBoardRevenueStep = 1;

    /** New style train limit configuration.
     */
    private int trainLimitStep = 1;

    private int privatesRevenueStep = 1; // sfy 1889

    private boolean trainTradingAllowed = false;

    /** May company buy more than one Train from the Bank per turn? */
    private boolean oneTrainPerTurn = false;

    /** May company buy more than one Train of each type from the Bank per turn? */
    private boolean oneTrainPerTypePerTurn = false;

    /** Is loan taking allowed */
    private boolean loanTakingAllowed = false;

    /** Previous phase, defining the current one's defaults */
    private Phase defaults = null;

    /** Items to close if a phase gets activated */
    private List<Closeable> closedObjects;

    /** Train types to rust or obsolete if a phase gets activated */
    private ImmutableList<TrainCertificateType> rustedTrains;
    private String rustedTrainNames;

    /** Train types to release (make available for buying) if a phase gets activated */
    private ImmutableList<TrainCertificateType> releasedTrains;
    private String releasedTrainNames;

    /** Actions for this phase.
     * When this phase is activated, the GameManager method phaseAction() will be called,
     * which in turn will call the current Round, which is responsible to handle the action.
     * <p>
     * Set actions have a name and may have a value. 
     * TODO: Replace this by triggers
     * */
    private Map<String, String> actions;

    private String extraInfo = "";

    /** A HashMap to contain phase-dependent parameters
     * by name and value.
     */
    private Map<String, String> parameters = null;

    // dynamic information
    // is this really dynamic, is it used over time?
    private final GenericState<Owner> lastTrainBuyer = GenericState.create(this, "lastTrainBuyer");

    public Phase(PhaseManager parent, String id, int index, Phase previousPhase) {
        super(parent, id);
        this.index = index;
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
                parameters = ImmutableMap.copyOf(defaults.parameters);
            }
        }

        // Real name (as in the printed game)
        name = tag.getAttributeAsString("realName", null);

        // Allowed tile colours
        Tag tilesTag = tag.getChild("Tiles");
        if (tilesTag != null) {
            String colourList = tilesTag.getAttributeAsString("colour", null);
            if (Util.hasValue(colourList)) {
                tileColours = ImmutableList.copyOf(Splitter.on(",").split(colourList));
            }

            List<Tag> laysTag = tilesTag.getChildren("Lays");
            if (laysTag != null && !laysTag.isEmpty()) {
                // First create a copy of the previous map, if it exists, otherwise create the map.
                Map <String, Integer> newTileLaysPerColour;
                if (tileLaysPerColour == null || tileLaysPerColour.isEmpty()) {
                     newTileLaysPerColour = Maps.newHashMap();
                } else {
                    newTileLaysPerColour = Maps.newHashMap(tileLaysPerColour);
                }

                ImmutableMap.Builder<String, Integer> newTileLaysPerColourTurns = null;
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
                        newTileLaysPerColour.remove(key);
                    } else {
                        newTileLaysPerColour.put(key, number);
                    }

                    if (validForTurns != 0) {
                        if (newTileLaysPerColourTurns == null) {
                            newTileLaysPerColourTurns = ImmutableMap.builder();
                        }
                        newTileLaysPerColourTurns.put(key, validForTurns);
                    }
                }
                tileLaysPerColour = ImmutableMap.copyOf(newTileLaysPerColour);
                if (newTileLaysPerColourTurns != null) {
                    tileLaysPerColourTurns = newTileLaysPerColourTurns.build();
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

    public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        TrainManager trainManager = getRoot().getTrainManager();
        TrainCertificateType type;

        if (rustedTrainNames != null) {
            ImmutableList.Builder<TrainCertificateType> newRustedTrains = 
                    ImmutableList.builder();
            for (String typeName : rustedTrainNames.split(",")) {
                type = trainManager.getCertTypeByName(typeName);
                if (type == null) {
                    throw new ConfigurationException (" Unknown rusted train type '"+typeName+"' for phase '"+ getId()+"'");
                }
                newRustedTrains.add(type);
                type.setPermanent(false);
            }
            rustedTrains = newRustedTrains.build();
        }

        if (releasedTrainNames != null) {
            ImmutableList.Builder<TrainCertificateType> newReleasedTrains = 
                    ImmutableList.builder();
            for (String typeName : releasedTrainNames.split(",")) {
                type = trainManager.getCertTypeByName(typeName);
                if (type == null) {
                    throw new ConfigurationException (" Unknown released train type '"+typeName+"' for phase '"+getId()+"'");
                }
                newReleasedTrains.add(type);
            }
            releasedTrains = newReleasedTrains.build();
        }

        // Push any extra tile lay turns to the appropriate company type.
        if (tileLaysPerColourTurns != null) {
            CompanyManager companyManager = getRoot().getCompanyManager();
            companyManager.addExtraTileLayTurnsInfo (tileLaysPerColourTurns);
        }
        tileLaysPerColourTurns = null;  // We no longer need it.
    }

    /** Called when a phase gets activated */
    public void activate() {
        ReportBuffer.add(this, LocalText.getText("StartOfPhase", getId()));
        
        // Report any extra info
        if (Util.hasValue(extraInfo)) {
            ReportBuffer.add(this, extraInfo.replaceFirst("^<[Bb][Rr]>", "").replaceAll("<[Bb][Rr]>", "\n"));
        }

        if (closedObjects != null && !closedObjects.isEmpty()) {
            for (Closeable object : closedObjects) {
                log.debug("Closing object " + object.toString());
                object.close();
            }
        }

        TrainManager trainManager = getRoot().getTrainManager();

        if (rustedTrains != null && !rustedTrains.isEmpty()) {
            for (TrainCertificateType type : rustedTrains) {
                trainManager.rustTrainType(type, lastTrainBuyer.value());
            }
        }

        if (releasedTrains != null && !releasedTrains.isEmpty()) {
            for (TrainCertificateType type : releasedTrains) {
                trainManager.makeTrainAvailable(type);
            }
        }

        if (actions != null && !actions.isEmpty()) {
            for (String actionName : actions.keySet()) {
                getRoot().getGameManager().processPhaseAction (actionName, actions.get(actionName));
            }
        }
        
        if (doPrivatesClose()) {
            getRoot().getCompanyManager().closeAllPrivates();
        }
    }

    public void setLastTrainBuyer(Owner lastTrainBuyer) {
        this.lastTrainBuyer.set(lastTrainBuyer);
    }

    public String getInfo() {
        return extraInfo;
    }

    @Deprecated
    // FIXME: Replace this with TileColor object
    public boolean isTileColourAllowed(String tileColour) {
        return tileColours.contains(tileColour);
    }

    @Deprecated
    // FIXME: Replace this with TileColor objects
    public List<String> getTileColours() {
        return tileColours;
    }

    public String getTileColoursString() {
        StringBuilder b = new StringBuilder();
        for (String colour : tileColours) {
            if (b.length() > 0) b.append(",");
            b.append (colour);
        }
        return b.toString();
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

    public String getRealName() {
        return (name != null) ? name : getId();
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
            log.error ("Error while parsing parameter "+key+" in phase "+ getId(), e);
            return 0;
        }

    }

    public List<Closeable> getClosedObjects() {
        return closedObjects;
    }
    
    @Override
    public String toText() {
        return getRealName();
    }
    
    public static Phase getCurrent(RailsItem item) {
        return item.getRoot().getPhaseManager().getCurrentPhase();
    }
    
}

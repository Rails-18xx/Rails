/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Phase.java,v 1.8 2008/06/04 19:00:31 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.util.Tag;

import org.apache.log4j.Logger;

public class Phase implements PhaseI {

    protected int index;

    protected String name;

    protected String colourList = "";

    protected HashMap<String, Integer> tileColours;

    protected boolean privateSellingAllowed = false;

    protected boolean privatesClose = false;

    protected int numberOfOperatingRounds = 1;

    protected int offBoardRevenueStep = 1;

    protected boolean trainTradingAllowed = false;

    /** May company buy more than one Train from the Bank per turn? */
    protected boolean oneTrainPerTurn = false;

    /** May company buy more than one Train of each type from the Bank per turn? */
    protected boolean oneTrainPerTypePerTurn = false;

    /** Previous phase, defining the current one's defaults */
    protected Phase defaults = null;

    /** Items to close if a phase gets activated */
    protected List<Closeable> closedObjects = null;

    // protected static boolean previousPrivateSellingAllowed = false;
    // protected static int previousNumberOfOperatingRounds = 1;
    // protected static String previousTileColours = "";
    // protected static int previousOffBoardRevenueStep = 1;

    protected static Logger log =
            Logger.getLogger(Phase.class.getPackage().getName());

    public Phase(int index, String name, Phase previousPhase) {
        this.index = index;
        this.name = name;
        this.defaults = previousPhase;
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        if (defaults != null) {
            colourList = defaults.colourList;
            privateSellingAllowed = defaults.privateSellingAllowed;
            numberOfOperatingRounds = defaults.numberOfOperatingRounds;
            offBoardRevenueStep = defaults.offBoardRevenueStep;
            trainTradingAllowed = defaults.trainTradingAllowed;
            oneTrainPerTurn = defaults.oneTrainPerTurn;
            oneTrainPerTypePerTurn = defaults.oneTrainPerTypePerTurn;
        }

        // String colourList;
        String[] colourArray = new String[0];
        tileColours = new HashMap<String, Integer>();

        // Allowed tile colours
        Tag tilesTag = tag.getChild("Tiles");
        if (tilesTag != null) {
            colourList = tilesTag.getAttributeAsString("colour", colourList);
        }
        if (colourList != null) colourArray = colourList.split(",");
        for (int i = 0; i < colourArray.length; i++) {
            tileColours.put(colourArray[i], null);
        }

        // Private-related properties
        Tag privatesTag = tag.getChild("Privates");
        if (privatesTag != null) {
            privateSellingAllowed =
                    privatesTag.getAttributeAsBoolean("sellingAllowed",
                            privateSellingAllowed);
            privatesClose = privatesTag.getAttributeAsBoolean("close", false);
        }

        // Operating rounds
        Tag orTag = tag.getChild("OperatingRounds");
        if (orTag != null) {
            numberOfOperatingRounds =
                    orTag.getAttributeAsInteger("number",
                            numberOfOperatingRounds);
        }

        // Off-board revenue steps
        Tag offBoardTag = tag.getChild("OffBoardRevenue");
        if (offBoardTag != null) {
            offBoardRevenueStep =
                    offBoardTag.getAttributeAsInteger("step",
                            offBoardRevenueStep);
        }

        Tag trainsTag = tag.getChild("Trains");
        if (trainsTag != null) {
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
    }

    public boolean isTileColourAllowed(String tileColour) {
        return tileColours.containsKey(tileColour);
    }

    public Map<String, Integer> getTileColours() {
        return tileColours;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
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

    public boolean isTrainTradingAllowed() {
        return trainTradingAllowed;
    }

    public boolean canBuyMoreTrainsPerTurn() {
        return !oneTrainPerTurn;
    }

    public boolean canBuyMoreTrainsPerTypePerTurn() {
        return !oneTrainPerTypePerTurn;
    }

    public int getNumberOfOperatingRounds() {
        return numberOfOperatingRounds;
    }

    /**
     * @return Returns the offBoardRevenueStep.
     */
    public int getOffBoardRevenueStep() {
        return offBoardRevenueStep;
    }

    public void addObjectToClose(Closeable object) {
        if (closedObjects == null) {
            closedObjects = new ArrayList<Closeable>();
        }
        closedObjects.add(object);
    }

    public String toString() {
        return name;
    }
}

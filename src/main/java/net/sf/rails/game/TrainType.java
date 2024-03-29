package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.BooleanState;

import net.sf.rails.game.state.IntegerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrainType implements Cloneable {

    public static final int TOWN_COUNT_MAJOR = 2;
    public static final int TOWN_COUNT_MINOR = 1;
    public static final int NO_TOWN_COUNT = 0;

    public static final String defaultCategory = "passenger";

    protected String name;
    protected TrainCardType trainCardType;
    protected String trainCategory; // Default passenger; or goods etc.

    protected String reachBasis = "stops";
    protected boolean countHexes = false;

    /* "town count" in fact refers to all "minor" stops,
     * including mines, ports etc. */
    protected String countTowns = "major";
    protected int townCountIndicator = TOWN_COUNT_MAJOR;

    /* "town score" in fact refers to all "minor" stops,
     * including mines, ports etc. */
    protected String scoreTowns = "yes";
    protected int townScoreFactor = 1;

    protected String scoreCities = "single";

    protected IntegerState cityScoreFactor; // For 1826 E-trains
    protected int tempCityScoreFactor = 1;

    protected int cost;
    protected int exchangeCost;
    protected IntegerState majorStops; // For 1826 E-trains
    protected int tempMajorStops;
    protected int minorStops;

    protected BooleanState rusted;

    protected TrainManager trainManager;

    private static final Logger log = LoggerFactory.getLogger(TrainType.class);

    public TrainType() {}

    public void configureFromXML(Tag tag) throws ConfigurationException {
        name = tag.getAttributeAsString("name");
        cost = tag.getAttributeAsInteger("cost");
        exchangeCost = tag.getAttributeAsInteger("exchangeCost");
        tempMajorStops = tag.getAttributeAsInteger("majorStops");
        minorStops = tag.getAttributeAsInteger("minorStops");
        trainCategory = tag.getAttributeAsString("category", "passenger");

        // Reach
        Tag reachTag = tag.getChild("Reach");
        if (reachTag != null) {
            // Reach basis
            reachBasis = reachTag.getAttributeAsString("base", reachBasis);

            // Are towns counted (only relevant if reachBasis = "stops")
            // This refers to all "minor" stops, including mines, ports etc.
            countTowns = reachTag.getAttributeAsString("countTowns", countTowns);
        }

        // Score
        Tag scoreTag = tag.getChild("Score");
        if (scoreTag != null) {
            // Do towns score (values "yes" and "no"
            scoreTowns = scoreTag.getAttributeAsString("towns", scoreTowns);
            // How do cities score ("single" or "double")
            scoreCities = scoreTag.getAttributeAsString("cities", scoreCities);
        }

        // Check the reach and score values
        countHexes = "hexes".equals(reachBasis);
        townCountIndicator = "no".equals(countTowns) ? NO_TOWN_COUNT
                : minorStops > 0 ? TOWN_COUNT_MINOR
                : TOWN_COUNT_MAJOR;
        /* FIXME: TownCount should only affect train length counting, not revenue calculation.
           This seems to go wrong in NetworkVertex near line 166. */
        tempCityScoreFactor = ("double".equalsIgnoreCase(scoreCities) ? 2 : 1);
        townScoreFactor = "yes".equalsIgnoreCase(scoreTowns) ? 1 : 0;
        // Actually we should meticulously check all values....
    }

    public void finishConfiguration (RailsRoot root, TrainCardType trainCardType) throws ConfigurationException {

        trainManager = root.getTrainManager();
        cityScoreFactor = IntegerState.create(trainManager, "cityScoreFactor_" + name, tempCityScoreFactor);
        majorStops = IntegerState.create(trainManager, "majorStops+" + name, tempMajorStops);
        this.trainCardType = trainCardType;

        if (name == null) {
            throw new ConfigurationException("No name specified for Train");
        }
        if (cost == 0) {
            throw new ConfigurationException("No price specified for Train "+name);
        }
        if (majorStops.value() == 0) {
            throw new ConfigurationException("No major stops specified for Train "+name);
        }
    }

    public TrainCardType getTrainCardType() {
        return trainCardType;
    }

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return cityScoreFactor.value();
    }

    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return cost;
    }

    public int getExchangeCost() {
        return exchangeCost;
    }

    public boolean canBeExchanged () {
        return exchangeCost > 0;
    }

    /**
     * @return Returns the countHexes.
     */
    public boolean countsHexes() {
        return countHexes;
    }

    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return majorStops.value();
    }

    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return minorStops;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    public String getCategory() {
        return trainCategory;
    }

    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return townCountIndicator;
    }

    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return townScoreFactor;
    }

    // Two setters, used with 1826 E-trains.
    public void setCityScoreFactor(int cityScoreFactor) {
        this.cityScoreFactor.set (cityScoreFactor);
    }

    public void setMajorStops(int majorStops) {
        this.majorStops.set (majorStops);
    }

    @Override
    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Cannot clone traintype {}", name, e);
            return null;
        }

        return clone;
    }

    public TrainManager getTrainManager() {
        return trainManager;
    }

    public String getInfo() {
        StringBuilder b = new StringBuilder ("<html>");
        b.append(LocalText.getText("TrainInfo", name, Bank.format(trainManager, cost), 0));
        if (b.length() == 6) b.append(LocalText.getText("None"));

        return b.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}

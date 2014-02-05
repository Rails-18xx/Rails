package rails.game;

import java.text.NumberFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import rails.algorithms.RevenueBonusTemplate;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.model.RailsModel;
import rails.game.state.HashSetState;

/**
 * Represents a certain tile <i>type</i>, identified by its id (tile number).
 * <p> For each tile number, only one tile object is created. The list
 * <b>tilesLaid</b> records in which hexes a certain tile number has been laid.
 */
public class Tile extends RailsModel implements Comparable<Tile> {
    
    public static enum Quantity { LIMITED, UNLIMITED, FIXED; }
    
    private static final Logger log = LoggerFactory.getLogger(Tile.class);

    /**
     * The 'external id', which is shown in the UI. Usually equal to the
     * internal id, but different in case of duplicates.
     */
    private String externalId;
    
    /**
     * The 'picture id', identifying the picture number to be loaded. Usually
     * equal to the internal id, but different in case of graphical variants
     * (such as the 18EU tiles 80-83).
     */
    private String pictureId;

    /**
     * The 'sorting id' which defines the ordering
     */
    private String sortingId;
    
    private TileColour colour;
    private ImmutableSortedMap<Integer, Station> stations;

    private TrackConfig trackConfig;
    private HexSidesSet possibleRotations;
    private List<TileUpgrade> upgrades;

    private Quantity quantity;
    private int count;
    private boolean allowsMultipleBasesOfOneCompany = false;

    /** Fixed orientation; null if free to rotate */
    private HexSide fixedOrientation = null;

    // Stop properties
    private StopType stopType = null;

    /**
     * Flag indicating that player must reposition any basetokens during the
     * upgrade.
     */
    private boolean relayBaseTokensOnUpgrade = false;

    /**
     * Records in which hexes a certain tile number has been laid. The size of
     * the collection indicates the number of tiles laid on the map board.
     */
    private final HashSetState<MapHex> tilesLaid = 
            HashSetState.create(this, "tilesLaid");

    /** Storage of revenueBonus that are bound to the tile */
    private List<RevenueBonusTemplate> revenueBonuses = null;
    
    /** CountModel to display the number of available tiles */
    private final CountModel countModel = new CountModel();

    private Tile(RailsItem owner, String id) {
        super(owner, id);
    }

    public static Tile create(TileManager parent, String id) {
        return new Tile(parent, id);
    }

    @Override
    public TileManager getParent() {
        return (TileManager) super.getParent();
    }

    /**
     * @param se &lt;Tile&gt; element from TileSet.xml
     * @param te &lt;Tile&gt; element from Tiles.xml
     */
    public void configureFromXML(Tag setTag, Tag defTag)
            throws ConfigurationException {

        if (defTag == null) {
            throw new ConfigurationException(LocalText.getText("TileMissing",
                    getId()));
        }

        String colourName = defTag.getAttributeAsString("colour");
        if (colourName == null)
            throw new ConfigurationException(LocalText.getText(
                    "TileColorMissing", getId()));
        if (colourName.equalsIgnoreCase("gray")) colourName = "grey";
        try {
            colour = TileColour.valueOfIgnoreCase(colourName);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(LocalText.getText(
                    "InvalidTileColourName", getId(), colourName), e);
        }

        /* Stations */
        List<Tag> stationTags = defTag.getChildren("Station");
        ImmutableSortedMap.Builder<Integer, Station> stationBuilder =
                ImmutableSortedMap.naturalOrder();
        if (stationTags != null) {
            for (Tag stationTag : stationTags) {
                Station station = Station.create(this, stationTag);
                stationBuilder.put(station.getNumber(), station);
            }
        }
        stations = stationBuilder.build();

        /* Tracks (only number per side, no cities yet) */
        List<Tag> trackTags = defTag.getChildren("Track");
        ImmutableSet.Builder<Track> trackBuilder = ImmutableSet.builder();
        if (trackTags != null) {
            for (Tag trackTag : trackTags) {
                String fromStr = trackTag.getAttributeAsString("from");
                String toStr = trackTag.getAttributeAsString("to");
                if (fromStr == null || toStr == null) {
                    throw new ConfigurationException(LocalText.getText(
                            "FromOrToMissing", getId()));
                }
                TrackPoint from = TrackPoint.create(this, fromStr);
                TrackPoint to = TrackPoint.create(this, toStr);
                Track track = new Track(from, to);
                trackBuilder.add(track);
            }
        }
        trackConfig = new TrackConfig(this, trackBuilder.build());

        // define possibleRotations
        Set<TrackConfig> trackConfigsBuilder = new HashSet<TrackConfig>(6);
        HexSidesSet.Builder rotationsBuilder = HexSidesSet.builder();

        trackConfigsBuilder.add(trackConfig);
        rotationsBuilder.set(HexSide.defaultRotation());
        for (HexSide rotation:HexSide.allExceptDefault()) {
            TrackConfig nextConfig =
                    TrackConfig.createByRotation(trackConfig, rotation);
            if (trackConfigsBuilder.contains(nextConfig)) continue;
            trackConfigsBuilder.add(nextConfig);
            rotationsBuilder.set(rotation);
        }
        possibleRotations = rotationsBuilder.build();
        log.debug("Allowed rotations for " + getId() + " are "
                  + possibleRotations);

        /* External (printed) id */
        externalId = setTag.getAttributeAsString("extId", getId());

        /* Picture id */
        pictureId = setTag.getAttributeAsString("pic", getId());

        /* Quantity */
        count = setTag.getAttributeAsInteger("quantity", 0);
        /* Value '99' and '-1' mean 'unlimited' */
        /*
         * BR: added option for unlimited plain tiles: tiles with one track and
         * no stations
         */
        String unlimitedTiles = getRoot().getGameOptions().get("UnlimitedTiles");
        if (count == 99 || count == -1
                 || "yes".equalsIgnoreCase(unlimitedTiles) 
                 || ("yellow plain".equalsIgnoreCase(unlimitedTiles))
                     && trackConfig.size() == 1 && stations.isEmpty()) {
            quantity = Quantity.UNLIMITED;
            count = 0;
        } else if (count == 0) {
            quantity = Quantity.FIXED;
        } else {
            quantity = Quantity.LIMITED;
            count += setTag.getAttributeAsInteger("quantityIncrement", 0);
        }

        /* Multiple base tokens of one company allowed */
        allowsMultipleBasesOfOneCompany =
                setTag.hasChild("AllowsMultipleBasesOfOneCompany");

        int orientation = setTag.getAttributeAsInteger("orientation", -1);
        if (orientation != -1) {
            fixedOrientation = HexSide.get(orientation);
        }

        /* Upgrades */
        List<Tag> upgradeTags = setTag.getChildren("Upgrade");

        if (upgradeTags != null) {
            upgrades = TileUpgrade.createFromTags(this, upgradeTags);
        } else {
            upgrades = ImmutableList.of();
        }

        // Set reposition base tokens flag
        relayBaseTokensOnUpgrade =
                setTag.getAttributeAsBoolean("relayBaseTokens",
                        relayBaseTokensOnUpgrade);

        // revenue bonus
        List<Tag> bonusTags = setTag.getChildren("RevenueBonus");
        if (bonusTags != null) {
            revenueBonuses = new ArrayList<RevenueBonusTemplate>();
            for (Tag bonusTag : bonusTags) {
                RevenueBonusTemplate bonus = new RevenueBonusTemplate();
                bonus.configureFromXML(bonusTag);
                revenueBonuses.add(bonus);
            }
        }

        // Stop properties
        Tag accessTag = setTag.getChild("Access");
        stopType =
                StopType.parseStop(this, accessTag,
                        getParent().getDefaultStopTypes());
    }

    public void finishConfiguration(RailsRoot root, int sortingDigits)
            throws ConfigurationException {

        try {
            int externalNb = Integer.parseInt(externalId);
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumIntegerDigits(sortingDigits);
            sortingId = nf.format(externalNb);
        } catch (NumberFormatException e) {
           sortingId = externalId; 
        }
        
        for (TileUpgrade upgrade : upgrades) {
            upgrade.finishConfiguration(root);
        }
    }

    public TileColour getColour() {
        return colour;
    }

    public String getColourText() {
        return colour.toText();
    }

    public int getColourNumber() {
        return colour.getNumber();
    }

    public String getPictureId() {
        return pictureId;
    }
    
    public boolean hasTracks(HexSide side) {
        return trackConfig.hasSideTracks(side);
    }

    public Set<TrackPoint> getTracks(HexSide side) {
        return trackConfig.getSideTracks(side);
    }

    public TrackConfig getTrackConfig() {
        return trackConfig;
    }

    /**
     * Is a tile upgradeable at any time (regardless of the phase)?
     */
    public boolean isUpgradeable() {
        return colour.isUpgradeable();
    }

    public boolean allowsMultipleBasesOfOneCompany() {
        return allowsMultipleBasesOfOneCompany;
    }

    public List<TileUpgrade> getTileUpgrades() {
        return upgrades;
    }

    /**
     * Get all possible upgrades for a specific tile on a certain hex
     */

    public List<Tile> getAllUpgrades(MapHex hex) {
        List<Tile> upgr = new ArrayList<Tile>();
        for (TileUpgrade upgrade : upgrades) {
            Tile tile = upgrade.getTargetTile();
            if (upgrade.isAllowedForHex(hex)) {
                upgr.add(tile);
            }
        }
        return upgr;
    }

    public TileUpgrade getSpecificUpgrade(Tile targetTile) {
        for (TileUpgrade upgrade : upgrades) {
            if (upgrade.getTargetTile() == targetTile) {
                return upgrade;
            }
        }
        return TileUpgrade.createSpecific(this, targetTile);
    }

    /** Get a delimited list of all possible upgrades, regardless current phase */
    public String getUpgradesString(MapHex hex) {
        StringBuffer b = new StringBuffer();
        Tile tile;
        for (TileUpgrade upgrade : upgrades) {
            tile = upgrade.getTargetTile();
            if (upgrade.isAllowedForHex(hex)) {
                if (b.length() > 0) b.append(",");
                b.append(tile.toText());
            }
        }

        return b.toString();
    }

    public boolean hasStations() {
        return stations.size() > 0;
    }

    public Station getStation(int id) {
        return stations.get(id);
    }

    public ImmutableSet<Station> getStations() {
        return ImmutableSet.copyOf(stations.values());
    }

    public Set<Track> getTracks() {
        return trackConfig.getTracks();
    }

    public boolean hasNoStationTracks() {
        return trackConfig.hasNoStationTracks();
    }

    public Set<TrackPoint> getTracksPerStation(Station station) {
        return trackConfig.getStationTracks(station);
    }

    public HexSidesSet getPossibleRotations() {
        return possibleRotations;
    }

    public int getNumStations() {
        return stations.size();
    }
    
    private int getNumSlots() {
        int slots = 0;
        for (Station station:stations.values()) {
            slots += station.getBaseSlots();
        }
        return slots;
    }

    public boolean relayBaseTokensOnUpgrade() {
        return relayBaseTokensOnUpgrade;
    }

    public StopType getStopType() {
        return stopType;
    }

    /** Register a tile of this type being laid on the map. */
    public boolean add(MapHex hex) {
        tilesLaid.add(hex);
        return true;
    }

    /** Register a tile of this type being removed from the map. */
    public boolean remove(MapHex hex) {
        return tilesLaid.remove(hex);
    }
    
    public int getInitialCount() {
        return count;
    }
    
    public boolean isUnlimited() {
        return quantity == Quantity.UNLIMITED;
    }
    
    public boolean isFixed() {
        return quantity == Quantity.FIXED;
    }

    /** Return the number of free tiles */
    public int getFreeCount() {
        switch (quantity) {
            case LIMITED:
                return count - tilesLaid.size();
            case UNLIMITED:
                return 1;
            case FIXED:
                return 0;
        }
        return 0; // cannot happen but still
    }
    
    public CountModel getCountModel() {
        return countModel;
    }

    public HexSide getFixedOrientation() {
        return fixedOrientation;
    }

    public List<RevenueBonusTemplate> getRevenueBonuses() {
        return revenueBonuses;
    }
    
    
    @Override
    public String toText() {
        return externalId;
    }

    /** ordering of tiles based first on colour, then on external id.
     * Here the external id is 
     *  */
    
    public int compareTo(Tile other) {
        return ComparisonChain.start()
                .compare(this.colour, other.colour)
                .compare(other.getNumSlots(), this.getNumSlots())
                .compare(other.getNumStations(), this.getNumStations())
                .compare(this.getTracks().size(), other.getTracks().size())
                .compare(this.sortingId, other.sortingId)
                .result();
    }

    public class CountModel extends RailsModel {

        private CountModel() {
            super(Tile.this, "CountModel");
        }
        
        @Override
        public String toText() {
            String count = null;
            switch (quantity) {
                case LIMITED:
                    count = " (" + String.valueOf(getFreeCount()) + ")";
                    break;
                case UNLIMITED:
                    count = " (+)";
                    break;
                case FIXED:
                    count = "";
            }
            return "#" + externalId + count;
        }
    }
}

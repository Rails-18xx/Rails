package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Tile;
import net.sf.rails.game.TileManager;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialTileLay;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * Rails 2.0: Updated equals and toString methods (however see TODO below)
*/
public class LayTile extends PossibleORAction implements Comparable<LayTile> {

    /* LayTile types */
    public static final int GENERIC = 0; // Stop-gap only
    public static final int LOCATION_SPECIFIC = 1; // Valid hex and allowed tiles
    public static final int SPECIAL_PROPERTY = 2; // Directed by a special property
    public static final int CORRECTION = 99; // Correction tile lays

    protected int type = 0;

    /*--- Preconditions ---*/

    /** Where to lay a tile (null means anywhere) */
    private transient List<MapHex> locations = null;
    private String locationNames;

    /** Highest tile colour (empty means unspecified) */
    private Map<String, Integer> tileColours = null;

    /** Allowed tiles on a specific location (empty means unspecified) */
    private transient List<Tile> tiles = null;
    private int[] tileIds;
    private String[] sTileIds;

    /**
     * Special property that will be fulfilled by this tile lay. If null, this
     * is a normal tile lay.
     */
    private transient SpecialTileLay specialProperty = null;
    private int specialPropertyId;

    /**
     * Need base tokens be relaid?
     */
    private boolean relayBaseTokens = false;

    /*--- Postconditions ---*/

    /** The tile actually laid */
    private transient Tile laidTile = null;
    private int laidTileId;
    private String sLaidTileId;

    /** The map hex on which the tile is laid */
    private transient MapHex chosenHex = null;
    private String chosenHexName;

    /** The tile orientation */
    private int orientation;

    /** Any manually assigned base token positions */
    private Map<String, Integer> relaidBaseTokens = null;
    private String relaidBaseTokensString = null;

    public static final long serialVersionUID = 1L;

    public LayTile(RailsRoot root, int type) {
        super(root);
        this.type = type;
    }

    public LayTile(RailsRoot root, Map<String, Integer> tileColours) {
        super(root);
        type = GENERIC;
        setTileColours (tileColours);
        // NOTE: tileColours is currently only used for Help purposes.
    }

    public LayTile(SpecialTileLay specialProperty) {
        super(specialProperty.getRoot());
        type = SPECIAL_PROPERTY;
        this.locations = specialProperty.getLocations();
        if (locations != null) buildLocationNameString();
        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
        Tile tile = specialProperty.getTile();
        if (tile != null) {
            tiles = new ArrayList<>();
            tiles.add(tile);
        }
    }

    /**
     * @return Returns the chosenHex.
     */
    public MapHex getChosenHex() {
        return chosenHex;
    }

    /**
     * @param chosenHex The chosenHex to set.
     */
    public void setChosenHex(MapHex chosenHex) {
        this.chosenHex = chosenHex;
        this.chosenHexName = chosenHex.getId();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    /**
     * @return Returns the laidTile.
     */
    public Tile getLaidTile() {
        return laidTile;
    }

    /**
     * @param laidTile The laidTile to set.
     */
    public void setLaidTile(Tile laidTile) {
        this.laidTile = laidTile;
        this.sLaidTileId = laidTile.getId();
    }

    /**
     * @return Returns the specialProperty.
     */
    public SpecialTileLay getSpecialProperty() {
        return specialProperty;
    }

    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialTileLay specialProperty) {
        this.specialProperty = specialProperty;
        // TODO this.specialPropertyName = specialProperty.getUniqueId();
    }

    /**
     * @return Returns the tiles.
     */
    public List<Tile> getTiles() {
        return tiles;
    }

    /**
     * @param tiles The tiles to set.
     */
    public void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
        this.sTileIds = new String[tiles.size()];
        for (int i = 0; i < tiles.size(); i++) {
            sTileIds[i] = tiles.get(i).getId();
        }
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public void setLocations(List<MapHex> locations) {
        this.locations = locations;
        if (locations != null) buildLocationNameString();
    }

    public int getType() {
        return type;
    }

    /**
     * @return Returns the tileColours.
     */
    public Map<String, Integer> getTileColours() {
        return tileColours;
    }

    public boolean isTileColourAllowed(String tileColour) {
        return tileColours != null
        && tileColours.containsKey(tileColour)
        && tileColours.get(tileColour) > 0;
    }

    public void setTileColours(Map<String, Integer> map) {
        tileColours = new HashMap<String, Integer>();
        // Check the map. Sometimes 0 values creep in, and these can't easily
        // be intercepted in the UI code (see comment at previous method).
        // TODO This is a dirty fix, but the quickest one too.
        if (map != null) {
            for (String colourName : map.keySet()) {
                if (map.get(colourName) > 0) tileColours.put(colourName, map.get(colourName));
            }
        }
    }


    public boolean isRelayBaseTokens() {
        return relayBaseTokens;
    }

    public void setRelayBaseTokens(boolean relayBaseTokens) {
        this.relayBaseTokens = relayBaseTokens;
    }

    public void addRelayBaseToken (String companyName, Integer cityNumber) {
        if (relaidBaseTokens == null) {
            relaidBaseTokens = new HashMap<String, Integer>();
        }
        relaidBaseTokens.put(companyName, cityNumber);
        relaidBaseTokensString = Util.appendWithDelimiter(relaidBaseTokensString,
                Util.appendWithDelimiter(companyName, String.valueOf(cityNumber), ":"),
        ",");
    }

    public Map<String, Integer> getRelaidBaseTokens() {
        return relaidBaseTokens;
    }

    public int getPotentialCost(MapHex hex) {
        if (specialProperty != null) {
            if (specialProperty.isFree()) {
                return 0;
            } else {
                return Math.max(0, hex.getTileCost() - specialProperty.getDiscount());
            }
        }
        return hex.getTileCost();
    }


    // TODO: Check for and add the missing attributes
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        LayTile action = (LayTile)pa;
        boolean options = (this.locations == null || this.locations.isEmpty() || this.locations.contains(action.chosenHex))
                && (this.tiles == null || this.tiles.isEmpty() || Objects.equal(this.tiles, action.tiles) || this.tiles.contains(action.getLaidTile()) )
//              && Objects.equal(this.type, action.type) // type is not always stored
                && Objects.equal(this.specialProperty, action.specialProperty)
        ;

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
            && Objects.equal(this.laidTile, action.laidTile)
            && Objects.equal(this.chosenHex, action.chosenHex)
            && Objects.equal(this.orientation, action.orientation)
            && Objects.equal(this.relaidBaseTokens, action.relaidBaseTokens)
        ;

    }

    // TODO: Check for and add the missing attributes
    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("locations", locations)
                    .addToString("tiles", tiles)
                    .addToString("type", type)
                    .addToString("specialProperty", specialProperty)
                    .addToStringOnlyActed("laidTile", laidTile)
                    .addToStringOnlyActed("chosenHex", chosenHex)
                    .addToStringOnlyActed("orientation", orientation)
                    .addToStringOnlyActed("relaidBaseTokens", relaidBaseTokens)
                .toString()
        ;
    }

    /** Deserialize */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (in instanceof GameLoader.RailsObjectInputStream) {
            // Custom reading for backwards compatibility
            ObjectInputStream.GetField fields = in.readFields();

            locationNames = (String) fields.get("locationNames", locationNames);
            tileColours = (Map<String, Integer>) fields.get("tileColours", tileColours);
            // FIXME: Rewrite this with Rails1.x version flag
            tileIds = (int[]) fields.get("tileIds", tileIds);
            sTileIds = (String[]) fields.get("tileIds", sTileIds);

            specialPropertyId = fields.get("specialPropertyId", specialPropertyId);
            // FIXME: Rewrite this with Rails1.x version flag
            laidTileId = fields.get("laidTileId", laidTileId);
            sLaidTileId = (String) fields.get("sLaidTileId", sLaidTileId);

            chosenHexName = (String) fields.get("chosenHexName", chosenHexName);
            orientation = fields.get("orientation", orientation);
            relayBaseTokens = fields.get("relayBaseTokens", relayBaseTokens);
            relaidBaseTokens = (Map<String, Integer>) fields.get("relaidBaseTokens", relaidBaseTokens);
            relaidBaseTokensString = (String) fields.get("relaidBaseTokensString", relaidBaseTokensString);

            MapManager mmgr = getRoot().getMapManager();
            TileManager tmgr = getRoot().getTileManager();

            if (Util.hasValue(locationNames)) {
                locations = new ArrayList<>();
                for (String hexName : locationNames.split(",")) {
                    locations.add(mmgr.getHex(hexName));
                }
            }

            // FIXME: Rewrite this with Rails1.x version flag
            if (tileIds != null && tileIds.length > 0) {
                tiles = new ArrayList<>();
                for (int tileNb : tileIds) {
                    tiles.add(tmgr.getTile(String.valueOf(tileNb)));
                }
            }

            if (sTileIds != null && sTileIds.length > 0) {
                tiles = new ArrayList<>();
                for (String tileId : sTileIds) {
                    tiles.add(tmgr.getTile(tileId));
                }
            }

            if (specialPropertyId > 0) {
                specialProperty = (SpecialTileLay) SpecialProperty.getByUniqueId(getRoot(), specialPropertyId);
            }
            // FIXME: Rewrite this with Rails1.x version flag
            if (laidTileId != 0) {
                sLaidTileId = String.valueOf(laidTileId);
            }
            if (sLaidTileId != null) {
                laidTile = tmgr.getTile(sLaidTileId);
            }

            if (chosenHexName != null && chosenHexName.length() > 0) {
                chosenHex = mmgr.getHex(chosenHexName);
            }
        } else {
            in.defaultReadObject();
        }
    }

    public void applyRailsRoot(RailsRoot root) {
        super.applyRailsRoot(root);

        MapManager mmgr = getRoot().getMapManager();
        TileManager tmgr = getRoot().getTileManager();

        if (Util.hasValue(locationNames)) {
            locations = new ArrayList<>();
            for (String hexName : locationNames.split(",")) {
                locations.add(mmgr.getHex(hexName));
            }
        }

        // FIXME: Rewrite this with Rails1.x version flag
        if (tileIds != null && tileIds.length > 0) {
            tiles = new ArrayList<>();
            for (int tileNb : tileIds) {
                tiles.add(tmgr.getTile(String.valueOf(tileNb)));
            }
        }

        if (sTileIds != null && sTileIds.length > 0) {
            tiles = new ArrayList<>();
            for (String tileId : sTileIds) {
                tiles.add(tmgr.getTile(tileId));
            }
        }

        if (specialPropertyId > 0) {
            specialProperty = (SpecialTileLay) SpecialProperty.getByUniqueId(getRoot(), specialPropertyId);
        }
        // FIXME: Rewrite this with Rails1.x version flag
        if (laidTileId != 0) {
            sLaidTileId = String.valueOf(laidTileId);
        }
        if (sLaidTileId != null) {
            laidTile = tmgr.getTile(sLaidTileId);
        }

        if (chosenHexName != null && chosenHexName.length() > 0) {
            chosenHex = mmgr.getHex(chosenHexName);
        }
    }

    private void buildLocationNameString() {
        StringBuilder b = new StringBuilder();
        for (MapHex hex : locations) {
            if (b.length() > 0) b.append(",");
            b.append(hex.getId());
        }
        locationNames = b.toString();
    }

    @Override
    public boolean isCorrection() {
        return (type == LayTile.CORRECTION);
    }

    @Override
    public int compareTo(LayTile o) {
        return ComparisonChain.start()
                .compare(this.type, o.type)
                .compare(this.specialProperty, o.specialProperty, Ordering.natural().nullsLast())
                .result()
        ;
    }
}

package rails.game;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.common.parser.*;
import rails.game.Stop.Loop;
import rails.game.Stop.RunThrough;
import rails.game.Stop.RunTo;
import rails.game.Stop.Score;
import rails.game.Stop.Type;
import rails.game.state.AbstractItem;
import rails.game.state.Configurable;
import rails.game.state.Item;
import rails.util.Util;

public final class TileManager extends AbstractItem implements Configurable {

    protected Map<Integer, Tile> tileMap = new HashMap<Integer, Tile>();
    protected List<Integer> tileIds = new ArrayList<Integer>();

    // Stop property defaults per stop type
    protected Map<Type,RunTo> runToDefaults = new HashMap<Type, RunTo>();
    protected Map<Type,RunThrough> runThroughDefaults = new HashMap<Type, RunThrough>();
    protected Map<Type,Loop> loopDefaults = new HashMap<Type, Loop>();
    protected Map<Type,Score> scoreTypeDefaults = new HashMap<Type, Score>();

    private static final Logger log =
        LoggerFactory.getLogger(TileManager.class);

    /**
     * Used by Configure (via reflection) only
     */
    public TileManager(Item parent, String id) {
        super(parent, id);
    }

    /**
     * @see rails.game.state.Configurable#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tileSetTop) throws ConfigurationException {
        /*
         * Note: prefix se is used for elements from TileSet.xml, prefix te for
         * elements from Tiles.xml.
         */

        String tileDefFileName = tileSetTop.getAttributeAsString("tiles");
        if (tileDefFileName == null)
            throw new ConfigurationException(LocalText.getText("NoTilesXML"));

        String directory = "data/" + GameManager.getInstance().getGameName();
        Tag tileDefTop =
            Tag.findTopTagInFile(tileDefFileName, directory, "Tiles");
        if (tileDefTop == null)
            throw new ConfigurationException(LocalText.getText("NoTilesTag"));

        List<Tag> tileSetList = tileSetTop.getChildren("Tile");
        List<Tag> tileDefList = tileDefTop.getChildren("Tile");

        /*
         * The XML files TileSet.xml and Tiles.xml are read side by side, as
         * each one configures different tile aspects. The reason for having two
         * XML files is, that Tiles.xml defines per-tile aspects that are the
         * same for all games (such as the colour, tracks and stations; this
         * file is an automatically generated subset of the generic file
         * tiles/Tiles.xml), whereas TileSet.xml specifies the aspects that are
         * (or can be) specific to each rails.game (such as the possible
         * upgrades). <p>TileSet.xml is leading.
         */

        int tileId;
        Tile tile;

        // Creates maps to the tile definitions in both files.
        Map<Integer, Tag> tileSetMap = new HashMap<Integer, Tag>();
        Map<Integer, Tag> tileDefMap = new HashMap<Integer, Tag>();

        for (Tag tileSetTag : tileSetList) {
            tileId = tileSetTag.getAttributeAsInteger("id");
            /*
             * Check for duplicates (this also covers missing tile ids, as this
             * returns 0, and we always have a tile numbered 0!
             */
            if (tileSetMap.containsKey(tileId)) {
                throw new ConfigurationException(LocalText.getText(
                        "DuplicateTilesetID", String.valueOf(tileId)));
            }
            tileSetMap.put(tileId, tileSetTag);
            tileIds.add(tileId);
        }

        for (Tag tileDefTag : tileDefList) {
            tileId = tileDefTag.getAttributeAsInteger("id");
            /*
             * Check for duplicates (this also covers missing tile ids, as this
             * returns 0, and we always have a tile numbered 0!
             */
            if (tileDefMap.containsKey(tileId)) {
                throw new ConfigurationException(LocalText.getText(
                        "DuplicateTileD", String.valueOf(tileId)));
            } else if (!tileSetMap.containsKey(tileId)) {
                log.warn ("Tile #"+tileId+" exists in Tiles.xml but not in TileSet.xml (this can be OK if the tile only exists in some variants");
            }
            tileDefMap.put(tileId, tileDefTag);
        }

        // Create the Tile objects (must be done before further parsing)
        for (Integer id : tileSetMap.keySet()) {
            tile = Tile.create(this, id);
            tileMap.put(id, tile);
        }

        // Finally, parse the <Tile> subtags
        for (Integer id : tileMap.keySet()) {
            tile = tileMap.get(id);
            tile.configureFromXML(tileSetMap.get(id), tileDefMap.get(id));
        }

        // Parse default stop types
        Type type;
        RunTo runTo;
        RunThrough runThrough;
        Loop loop;
        Score scoreType;
        String s;
        Tag defaultsTag = tileSetTop.getChild("Defaults");
        if (defaultsTag != null) {
            List<Tag> accessTags = defaultsTag.getChildren("Access");
            for (Tag accessTag : accessTags) {
                // Type
                s = accessTag.getAttributeAsString("type", null);
                if (Util.hasValue(s)) {
                    try {
                        type = Type.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for default property type: "+s, e);
                    }
                } else {
                    type = null; // For default defaults
                }
                // RunTo
                s = accessTag.getAttributeAsString("runTo", null);
                if (Util.hasValue(s)) {
                    try {
                        runTo = RunTo.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default runTo property: "+s, e);
                    }
                    runToDefaults.put(type, runTo);
                }
                // RunThrough
                s = accessTag.getAttributeAsString("runThrough", null);
                if (Util.hasValue(s)) {
                    try {
                        runThrough = RunThrough.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default runThrough property: "+s, e);
                    }
                    runThroughDefaults.put(type, runThrough);
                }
                // Loop
                s = accessTag.getAttributeAsString("loop", null);
                if (Util.hasValue(s)) {
                    try {
                        loop = Loop.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default loop property: "+s, e);
                    }
                    loopDefaults.put(type, loop);
                }
                // Score type (not allowed for a null stop type)
                s = accessTag.getAttributeAsString("scoreType", null);
                if (type != null && Util.hasValue(s)) {
                    try {
                        scoreType = Score.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException ("Illegal value for "
                                +type+" default score type property: "+s, e);
                    }
                    scoreTypeDefaults.put(type, scoreType);
                }
            }
        }
    }

    public void finishConfiguration (GameManager gameManager)
    throws ConfigurationException {
        for (Tile tile : tileMap.values()) {
            tile.finishConfiguration(this);
        }
    }

    public Tile getTile(int id) {
        return tileMap.get(id);
    }

    /** Get the tile IDs in the XML definition sequence */
    public List<Integer> getTileds() {
        return tileIds;
    }

    /** returns the set of all possible upgrade tiles */
    public List<Tile> getAllUpgrades(Tile tile, MapHex hex) {
        TreeSet<Tile> tileSet = new TreeSet<Tile>();
        return new ArrayList<Tile>(recursiveUpgrades(tile, hex, tileSet));
    }

    private TreeSet<Tile> recursiveUpgrades(Tile tile, MapHex hex, TreeSet<Tile> tileSet) {

        tileSet.add(tile);

        List<Tile> directUpgrades = tile.getAllUpgrades(hex);
        for (Tile upgrade:directUpgrades)
            if (!tileSet.contains(upgrade))
                tileSet = recursiveUpgrades(upgrade, hex, tileSet);

        return tileSet;
    }

    public RunTo getRunToDefault(Type type) {
        return runToDefaults.containsKey(type) ? runToDefaults.get(type) : null;
    }

    public RunThrough getRunThroughDefault(Type type) {
        return runThroughDefaults.containsKey(type) ? runThroughDefaults.get(type) : null;
    }

    public Loop getLoopDefault(Type type) {
        return loopDefaults.containsKey(type) ? loopDefaults.get(type) : null;
    }

    public Score getScoreTypeDefault(Type type) {
        return scoreTypeDefaults.containsKey(type) ? scoreTypeDefaults.get(type) : null;
    }


}

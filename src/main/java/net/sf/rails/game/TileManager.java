package net.sf.rails.game;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ResourceLoader;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;


public class TileManager extends RailsManager implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(TileManager.class);

    // Map of all Tiles (for quick retrieval)
    private ImmutableMap<String, Tile> tileMap;
    // SortedSet of all Tiles (for a pre-sorted list
    private ImmutableSortedSet<Tile> tileSet;

    private int sortingDigits;

    // Stop property defaults per stop type
    private ImmutableMap<String, StopType> defaultStopTypes;

    /**
     * Used by Configure (via reflection) only
     */
    public TileManager(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tileSetTop) throws ConfigurationException {
        /*
         * Note: prefix se is used for elements from TileSet.xml, prefix te for
         * elements from Tiles.xml.
         */

        String tileDefFileName = tileSetTop.getAttributeAsString("tiles");
        if (tileDefFileName == null)
            throw new ConfigurationException(LocalText.getText("NoTilesXML"));

        String directory = "data" + ResourceLoader.SEPARATOR + getRoot().getGameName();
        Tag tileDefTop =
            Tag.findTopTagInFile(tileDefFileName, directory, "Tiles", getRoot().getGameOptions());
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

        // Creates maps to the tile definitions in both files.
        Map<String, Tag> tileSetMap = Maps.newHashMapWithExpectedSize(tileSetList.size());

        for (Tag tileSetTag : tileSetList) {
            String tileId = tileSetTag.getAttributeAsString("id");
            /*
             * Check for duplicates (this also covers missing tile ids, as this
             * returns 0, and we always have a tile numbered 0!
             */
            if (tileSetMap.containsKey(tileId)) {
                throw new ConfigurationException(LocalText.getText("DuplicateTilesetID", String.valueOf(tileId)));
            }
            tileSetMap.put(tileId, tileSetTag);
        }

        Map<String, Tag> tileDefMap = Maps.newHashMapWithExpectedSize(tileDefList.size());

        for (Tag tileDefTag : tileDefList) {
            String tileId = tileDefTag.getAttributeAsString("id");
            /*
             * Check for duplicates (this also covers missing tile ids, as this
             * returns 0, and we always have a tile numbered 0!
             */
            if (tileDefMap.containsKey(tileId)) {
                throw new ConfigurationException(LocalText.getText("DuplicateTileD", String.valueOf(tileId)));
            } else if (!tileSetMap.containsKey(tileId)) {
                log.debug("Tile #{} exists in Tiles.xml but not in TileSet.xml (this can be OK if the tile only exists in some variants)", tileId);
            }
            tileDefMap.put(tileId, tileDefTag);
        }

        // Create the Tile objects (must be done before further parsing)
        ImmutableMap.Builder<String, Tile> tileMapBuilder = ImmutableMap.builder();
        for (String id : tileSetMap.keySet()) {
            Tile tile = Tile.create(this, id);
            tileMapBuilder.put(id, tile);
        }
        tileMap = tileMapBuilder.build();

        // Finally, parse the <Tile> subtags
        for (String id : tileMap.keySet()) {
            Tile tile = tileMap.get(id);
            tile.configureFromXML(tileSetMap.get(id), tileDefMap.get(id));
            sortingDigits = Math.max(sortingDigits, tile.toText().length());
        }

        // Parse default stop types
        Tag defaultsTag = tileSetTop.getChild("Defaults");
        if (defaultsTag != null) {
            List<Tag> accessTags = defaultsTag.getChildren("Access");
            defaultStopTypes = StopType.parseDefaults(this, accessTags);
        } else {
            defaultStopTypes = ImmutableMap.of();
        }
    }

    public void finishConfiguration (RailsRoot root)
            throws ConfigurationException {

        ImmutableSortedSet.Builder<Tile> tileSetBuilder = ImmutableSortedSet.naturalOrder();
        for (Tile tile : tileMap.values()) {
            tile.finishConfiguration(root, sortingDigits);
            tileSetBuilder.add(tile);
        }
        tileSet = tileSetBuilder.build();
    }

    public Tile getTile(String id) {
        return tileMap.get(id);
    }

    /** Get the tile IDs in sorted Sequence */
    public SortedSet<Tile> getTiles() {
        return tileSet;
    }

    public ImmutableMap<String, StopType> getDefaultStopTypes() {
        return defaultStopTypes;
    }
}

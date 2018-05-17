using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class TileManager : RailsManager, IConfigurable
    {
        private static Logger<TileManager> log = new Logger<TileManager>();

        // Map of all Tiles (for quick retrieval)
        private Dictionary<string, Tile> tileMap;
        // SortedSet of all Tiles (for a pre-sorted list
        private SortedSet<Tile> tileSet;

        private int sortingDigits;

        // Stop property defaults per stop type
        private Dictionary<string, StopType> defaultStopTypes;


        /**
         * Used by Configure (via reflection) only
         */
        public TileManager(RailsRoot parent, string id) : base(parent, id)
        {
        }

        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tileSetTop)
        {
            /*
             * Note: prefix se is used for elements from TileSet.xml, prefix te for
             * elements from Tiles.xml.
             */

            string tileDefFileName = tileSetTop.GetAttributeAsString("tiles");
            if (tileDefFileName == null)
                throw new ConfigurationException(LocalText.GetText("NoTilesXML"));

            string directory = "data" + ResourceLoader.SEPARATOR + GetRoot.GameName;
            // #FIXME file access
            var file = GameInterface.Instance.XmlLoader.LoadXmlFile(tileDefFileName, directory);
            Tag tileDefTop = Tag.FindTopTagInFile(file, /*tileDefFileName, directory,*/ "Tiles", GetRoot.GameOptions);
            if (tileDefTop == null)
                throw new ConfigurationException(LocalText.GetText("NoTilesTag"));

            List<Tag> tileSetList = tileSetTop.GetChildren("Tile");
            List<Tag> tileDefList = tileDefTop.GetChildren("Tile");

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
            Dictionary<string, Tag> tileSetMap = new Dictionary<string, Tag>(tileSetList.Count);

            foreach (Tag tileSetTag in tileSetList)
            {
                string tileId = tileSetTag.GetAttributeAsString("id");
                /*
                 * Check for duplicates (this also covers missing tile ids, as this
                 * returns 0, and we always have a tile numbered 0!
                 */
                if (tileSetMap.ContainsKey(tileId))
                {
                    throw new ConfigurationException(LocalText.GetText("DuplicateTilesetID", tileId ?? "null"));
                }
                tileSetMap[tileId] = tileSetTag;
            }

            Dictionary<string, Tag> tileDefMap = new Dictionary<string, Tag>(tileDefList.Count);

            foreach (Tag tileDefTag in tileDefList)
            {
                string tileId = tileDefTag.GetAttributeAsString("id");
                /*
                 * Check for duplicates (this also covers missing tile ids, as this
                 * returns 0, and we always have a tile numbered 0!
                 */
                if (tileDefMap.ContainsKey(tileId))
                {
                    throw new ConfigurationException(LocalText.GetText("DuplicateTileD", tileId ?? "null"));
                }
                else if (!tileSetMap.ContainsKey(tileId))
                {
                    log.Warn("Tile #" + tileId + " exists in Tiles.xml but not in TileSet.xml (this can be OK if the tile only exists in some variants");
                }
                tileDefMap[tileId] = tileDefTag;
            }

            // Create the Tile objects (must be done before further parsing)
            Dictionary<string, Tile> tileMapBuilder = new Dictionary<string, Tile>();
            foreach (string id in tileSetMap.Keys)
            {
                Tile tile = Tile.Create(this, id);
                tileMapBuilder[id] = tile;
            }
            tileMap = tileMapBuilder;

            // Finally, parse the <Tile> subtags
            foreach (string id in tileMap.Keys)
            {
                Tile tile = tileMap[id];
                tile.ConfigureFromXML(tileSetMap[id], tileDefMap[id]);
                sortingDigits = Math.Max(sortingDigits, tile.ToText().Length);
            }

            // Parse default stop types
            Tag defaultsTag = tileSetTop.GetChild("Defaults");
            if (defaultsTag != null)
            {
                List<Tag> accessTags = defaultsTag.GetChildren("Access");
                defaultStopTypes = new Dictionary<string, StopType>(
                    (IDictionary<string, StopType>)(IEnumerable<KeyValuePair<string, StopType>>)StopType.ParseDefaults(this, accessTags));
            }
            else
            {
                defaultStopTypes = new Dictionary<string, StopType>();
            }
        }

        public void FinishConfiguration(RailsRoot root)
        {

            SortedSet<Tile> tileSetBuilder = new SortedSet<Tile>();
            foreach (Tile tile in tileMap.Values)
            {
                tile.FinishConfiguration(root, sortingDigits);
                tileSetBuilder.Add(tile);
            }
            tileSet = tileSetBuilder;
        }

        public Tile GetTile(string id)
        {
            return tileMap[id];
        }

        /** Get the tile IDs in sorted Sequence */
        public SortedSet<Tile> Tiles
        {
            get
            {
                return tileSet;
            }
        }

        public Dictionary<string, StopType> DefaultStopTypes
        {
            get
            {
                return defaultStopTypes;
            }
        }
    }
}

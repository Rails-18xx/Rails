using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Tile orientation enumeration.
 * 
 * Map orientation refers to "flat edges" parallel with coordinates system
 * axis. Thus there are two orientations: North-South
 * ({@link MapOrientation#NS NS}) and East-West
 * ({@link MapOrientation#EW EW}).
 * 
 * Although it seems neither is dominating in 18xx games North-South is used by
 * default for management and classification. So North-South orientation is
 * treated here as the natural one.
 *
 * * <p> The term "rotation" is used to indicate the amount of rotation (in 60
 * degree units) from the standard orientation of the tile (sometimes the term
 * orientation is also used to refer to rotation).
 * <p>Rotation is always relative to the standard orientation, which has the
 * printed tile number on the S edge for {@link MapOrientation#NS}-oriented
 * tiles, or on the SW edge for {@link MapOrientation#EW}-oriented tiles. The
 * rotation numbers are indicated in the below picture for an
 * {@code NS}-oriented tile: <p> <code>
 *
 *       ____3____
 *      /         \
 *     2           4
 *    /     NS      \
 *    \             /
 *     1           5
 *      \____0____/
 * </code> <p> For {@code EW}-oriented
 * tiles the above picture should be rotated 30 degrees clockwise.
 *
 */

namespace GameLib.Net.Game
{
    public class MapOrientation
    {
        public enum MapOrientations
        {
            /**
             * North-South tile orientation.
             * 
             * <p>This is default orientation for internal uses (which includes SVG
             * images).</p>
             */
            NS,

            /**
             * East-West tile orientation.
             */
            EW,
        }

        private bool lettersGoHorizontal;
        private bool letterAHasEvenNumbers;
        private MapOrientations mapOrientationDir;

        private MapOrientation(MapOrientations dir)
        {
            mapOrientationDir = dir;
        }

        public static MapOrientation Create(Tag tag)
        {
            MapOrientation mapOrientation;
            string orientation = tag.GetAttributeAsString("tileOrientation");
            if (orientation == null)
                throw new ConfigurationException("Map orientation undefined");
            try
            {
                mapOrientation = new MapOrientation((MapOrientations)Enum.Parse(typeof(MapOrientations), orientation));
            }
            catch (ArgumentException exception)
            {
                throw new ConfigurationException("Invalid Map orientation: " + orientation, exception);
            }

            string letterOrientation = tag.GetAttributeAsString("letterOrientation");
            if (letterOrientation.Equals("horizontal"))
            {
                mapOrientation.lettersGoHorizontal = true;
            }
            else if (letterOrientation.Equals("vertical"))
            {
                mapOrientation.lettersGoHorizontal = false;
            }
            else
            {
                throw new ConfigurationException("Invalid letter orientation: "
                      + letterOrientation);
            }

            string even = tag.GetAttributeAsString("even");
            mapOrientation.letterAHasEvenNumbers = ((even.ToUpper()[0] - 'A')) % 2 == 0;
            return mapOrientation;
        }

        private static string[] nsOrNames = { "S", "SW", "NW", "N", "NE", "SE" };
        private static string[] ewOrNames = { "SW", "W", "NW", "NE", "E", "SE" };

        public string GetORNames(HexSide orientation)
        {
            switch (this.mapOrientationDir)
            {
                case MapOrientations.NS:
                    return nsOrNames[orientation.TrackPointNumber];
                case MapOrientations.EW:
                    return ewOrNames[orientation.TrackPointNumber];
                default:
                    throw new InvalidOperationException();
            }
        }

        /**
         * @return the lettersGoHorizontal
         */
        public bool LettersGoHorizontal
        {
            get
            {
                return lettersGoHorizontal;
            }
        }

        /**
         * @return the letterAHasEvenNumbers
         */
        public bool LetterAHasEvenNumbers
        {
            get
            {
                return letterAHasEvenNumbers;
            }
        }

        public MapOrientations Orientation
        {
            get
            {
                return mapOrientationDir;
            }
        }

        public string GetUIClassName()
        {
            // FIXME: Rails 2.0, move this to some default .xml!
            switch (this.mapOrientationDir)
            {
                // #java_classes_need_replaced
                case MapOrientations.NS:
                    return "net.sf.rails.ui.swing.hexmap.NSHexMap";
                case MapOrientations.EW:
                    return "net.sf.rails.ui.swing.hexmap.EWHexMap";
                default:
                    throw new InvalidOperationException();
            }

        }

        // information to define neighbors
        private static int[] rowDeltaNS =
                new int[] { +2, +1, -1, -2, -1, +1 };
        private static int[] colDeltaNS =
                new int[] { 0, -1, -1, 0, +1, +1 };
        private static int[] rowDeltaEW =
                new int[] { +1, 0, -1, -1, 0, +1 };
        private static int[] colDeltaEW =
                new int[] { -1, -2, -1, +1, +2, +1 };

        public MapHex.Coordinates GetAdjacentCoordinates(MapHex.Coordinates origin, HexSide orientation)
        {
            int p = orientation.TrackPointNumber;
            switch (mapOrientationDir)
            {
                case MapOrientations.NS:
                    return origin.Translate(rowDeltaNS[p], colDeltaNS[p]);
                case MapOrientations.EW:
                    return origin.Translate(rowDeltaEW[p], colDeltaEW[p]);
                default:
                    throw new InvalidOperationException();
            }
        }

        public static float SQRT3 = (float)Math.Sqrt(3.0);

        public Dictionary<HexSide, HexPoint> SetGUIVertices(float cx, float cy, float scale)
        {

            Dictionary<HexSide, HexPoint> coordinates = new Dictionary<HexSide, HexPoint>();

            switch (mapOrientationDir)
            {
                case MapOrientations.NS:
                    /* The numbering is the following:
                     *      3--4
                     *     /    \
                     *    2      5
                     *     \    /
                     *      1--0
                     */
                    coordinates[HexSide.Get(0)] = new HexPoint(cx + 2 * scale, cy + 2 * SQRT3 * scale);
                    coordinates[HexSide.Get(1)] = new HexPoint(cx, cy + 2 * SQRT3 * scale);
                    coordinates[HexSide.Get(2)] = new HexPoint(cx - scale, cy + SQRT3 * scale);
                    coordinates[HexSide.Get(3)] = new HexPoint(cx, cy);
                    coordinates[HexSide.Get(4)] = new HexPoint(cx + 2 * scale, cy);
                    coordinates[HexSide.Get(5)] = new HexPoint(cx + 3 * scale, cy + SQRT3 * scale);
                    break;
                case MapOrientations.EW:
                    /* The numbering is the following:
                     *         3
                     *        / \
                     *       2   4
                     *       |   |
                     *       1   5
                     *        \ /
                     *         0
                     */
                    coordinates[HexSide.Get(0)] = new HexPoint(cx + SQRT3 * scale, cy + scale);
                    coordinates[HexSide.Get(1)] = new HexPoint(cx, cy);
                    coordinates[HexSide.Get(2)] = new HexPoint(cx, cy - 2 * scale);
                    coordinates[HexSide.Get(3)] = new HexPoint(cx + SQRT3 * scale, cy - 3 * scale);
                    coordinates[HexSide.Get(4)] = new HexPoint(cx + 2 * SQRT3 * scale, cy - 2 * scale);
                    coordinates[HexSide.Get(5)] = new HexPoint(cx + 2 * SQRT3 * scale, cy);
                    break;
                default:
                    throw new InvalidOperationException();
            }
            return coordinates;
        }

        public static float DEG30 = (float)(Math.PI / 6.0);

        private float RotationInRadians(HexSide rotation)
        {
            switch (mapOrientationDir)
            {
                case MapOrientations.NS:
                    return (2 * rotation.TrackPointNumber) * DEG30;
                case MapOrientations.EW:
                    return (2 * rotation.TrackPointNumber + 1) * DEG30;
                default:
                    throw new InvalidOperationException();
            }
        }

        /**
         * Returns rotation to be applied to {@link MapOrientation#NS}-oriented
         * tile to achieve this orientation.
         * 
         * <p>The rotation has to be done around center point of the tile.</p>
         * 
         * <p>This function returns {@literal 0} for {@link MapOrientation#NS}
         * since {@code NS}-oriented tile does not need any rotation to be
         * transformed into {@code NS}-oriented tile.</p>
         * 
         * @return Rotation to be applied to {@link MapOrientation#NS}-oriented
         *         tile to achieve this orientation.
         */

        public static float RotationInRadians(IRailsItem item, HexSide rotation)
        {
            return item.GetRoot.MapManager.MapOrientation.RotationInRadians(rotation);
        }

        /**
         * @return orientation of the map (NS or EW)
         */
        public static MapOrientation Get(IRailsItem item)
        {
            return item.GetRoot.MapManager.MapOrientation;
        }
    }
}

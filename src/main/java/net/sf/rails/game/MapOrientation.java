package net.sf.rails.game;

import java.util.Map;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.ui.swing.hexmap.GUIHex.HexPoint;


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

public enum MapOrientation {
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
    EW;


    private boolean lettersGoHorizontal;
    private boolean letterAHasEvenNumbers;
    
    public static MapOrientation create(Tag tag) throws ConfigurationException {
        MapOrientation mapOrientation;
        String orientation = tag.getAttributeAsString("tileOrientation");
        if (orientation == null)
            throw new ConfigurationException("Map orientation undefined");
        try {
            mapOrientation =  MapOrientation.valueOf(orientation);
        }
        catch(IllegalArgumentException exception) {
            throw new ConfigurationException("Invalid Map orientation: " + orientation, exception);
        }

        String letterOrientation = tag.getAttributeAsString("letterOrientation");
        if (letterOrientation.equals("horizontal")) {
            mapOrientation.lettersGoHorizontal = true;
        } else if (letterOrientation.equals("vertical")) {
            mapOrientation.lettersGoHorizontal = false;
        } else {
            throw new ConfigurationException("Invalid letter orientation: "
                  + letterOrientation);
        }

        String even = tag.getAttributeAsString("even");
        mapOrientation.letterAHasEvenNumbers = ((even.toUpperCase().charAt(0) - 'A')) % 2 == 0;
        return mapOrientation;
    }
    
    private static String[] nsOrNames = {"S", "SW", "NW", "N", "NE", "SE"}; 
    private static String[] ewOrNames = {"SW", "W", "NW", "NE", "E", "SE"};
    
    public String getORNames(HexSide orientation) {
        switch (this) {
        case NS:
            return nsOrNames[orientation.getTrackPointNumber()];
        case EW:
            return ewOrNames[orientation.getTrackPointNumber()];
        default:
            throw new AssertionError(this);
        }
    }

    /**
     * @return the lettersGoHorizontal
     */
    public boolean lettersGoHorizontal() {
        return lettersGoHorizontal;
    }

    /**
     * @return the letterAHasEvenNumbers
     */
    public boolean letterAHasEvenNumbers() {
        return letterAHasEvenNumbers;
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
    
    public static final double DEG30 = Math.PI / 6.0;
    
    private double rotationInRadians(HexSide rotation) {
        switch(this) {
        case NS:
            return (2 * rotation.getTrackPointNumber()) * DEG30;
        case EW:
            return (2 * rotation.getTrackPointNumber() + 1) * DEG30;
        default:
            throw new AssertionError(this);
        }
    }
    
    public static double rotationInRadians(HexMap hexMap, HexSide rotation) {
        return hexMap.getMapManager().getMapOrientation().rotationInRadians(rotation);
    }
    
    public String getUIClassName() {
        // FIXME: Rails 2.0, move this to some default .xml!
        switch(this) {
        case NS:
            return "net.sf.rails.ui.swing.hexmap.NSHexMap";
        case EW:
            return "net.sf.rails.ui.swing.hexmap.EWHexMap";
        default:
            throw new AssertionError(this);
        }
        
    }

    // information to define neighbours
    private static final int[] rowDeltaNS =
            new int[] { +2, +1, -1, -2, -1, +1 };
    private static final int[] colDeltaNS = 
            new int[] {  0, -1, -1,  0, +1, +1 };
    private static final int[] rowDeltaEW = 
            new int[] { +1,  0, -1, -1,  0, +1 };
    private static final int[] colDeltaEW =
            new int[] { -1, -2, -1, +1, +2, +1 };

    public MapHex.Coordinates getAdjacentCoordinates(MapHex.Coordinates origin, HexSide orientation) {
        int p = orientation.getTrackPointNumber();
        switch(this) {
        case NS:
            return origin.translate(rowDeltaNS[p], colDeltaNS[p]);
        case EW:
            return origin.translate(rowDeltaEW[p], colDeltaEW[p]);
        default:
            throw new AssertionError(this);
        }
    }
    
    public static final double SQRT3 = Math.sqrt(3.0);
    
    public void setGUIVertices(Map<HexSide, HexPoint> coordinates, double cx, double cy, double scale) {
        
        switch(this) {
        case NS:
            /* The numbering is the following:
             *      3--4
             *     /    \
             *    2      5
             *     \    /
             *      1--0
             */
            coordinates.put(HexSide.get(0), new GUIHex.HexPoint(cx + 2 * scale, cy + 2 * SQRT3 * scale));
            coordinates.put(HexSide.get(1), new GUIHex.HexPoint(cx, cy + 2 * SQRT3 * scale));
            coordinates.put(HexSide.get(2), new GUIHex.HexPoint(cx - scale, cy + SQRT3 * scale));
            coordinates.put(HexSide.get(3), new GUIHex.HexPoint(cx, cy));
            coordinates.put(HexSide.get(4), new GUIHex.HexPoint(cx + 2 * scale, cy));
            coordinates.put(HexSide.get(5), new GUIHex.HexPoint(cx + 3 * scale, cy + SQRT3 * scale));
            break;
        case EW:
            /* The numbering is the following:
             *         3
             *        / \
             *       2   4
             *       |   |
             *       1   5
             *        \ /
             *         0
             */
            coordinates.put(HexSide.get(0), new GUIHex.HexPoint(cx + SQRT3 * scale, cy + scale));
            coordinates.put(HexSide.get(1), new GUIHex.HexPoint(cx, cy));
            coordinates.put(HexSide.get(2), new GUIHex.HexPoint(cx, cy - 2 * scale));
            coordinates.put(HexSide.get(3), new GUIHex.HexPoint(cx + SQRT3 * scale, cy - 3 * scale));
            coordinates.put(HexSide.get(4), new GUIHex.HexPoint(cx +2 * SQRT3 * scale, cy - 2 * scale));
            coordinates.put(HexSide.get(5), new GUIHex.HexPoint(cx +2 * SQRT3 * scale, cy));
            break;
        default:
            throw new AssertionError(this);
        }
    }
}

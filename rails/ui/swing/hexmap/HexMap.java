/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/HexMap.java,v 1.27 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.Config;
import rails.common.parser.ConfigurationException;
import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.*;
import rails.util.Util;

/**
 * Base class that stores common info for HexMap independant of Hex
 * orientations.
 * The hex map manages several layers. Content is seperated in layers in order to ensure
 * good performance in case of only some aspects of the map need to be redrawn.
 * 
 * In order to avert race conditions during layer drawing, the critical code is
 * synchronized on the hex map instance as monitor object.
 */
public abstract class HexMap implements MouseListener,
        MouseMotionListener {

    /**
     * class for managing sets of rectangles. Apart from several convenience methods,
     * this class aims at keeping the set as minimal as possible.
     */
    private class RectangleSet {
        private List<Rectangle> rs = new LinkedList<Rectangle>();
        /**
         * @param rOp Rectangle to be added to the set. Only added if not contained in
         * a rectangle of the set. If added, all of the set's rectangles which are a
         * sub-area of this rectangle are dropped (in order to keep the rectangle list
         * as small as possible).
         */
        public void add(Rectangle rOp) {
            // exit if rectangle already contained in set of rectangles
            for (Rectangle r : rs) {
                if (r.contains(rOp)) return;
            }
            
            // build new set (do not include rectangles contained by new rectangle)
            List<Rectangle> newRs = new LinkedList<Rectangle>();
            for (Rectangle r : rs) {
                if (!rOp.contains(r)) newRs.add(r);
            }
            newRs.add(rOp);
            rs = newRs;
        }
        
        /**
         * As a side-effect, the area defined by the given rectangle is removed from the
         * area defined by the set of rectangles. This might lead to splitting the set's
         * rectangles if only parts of their areas become removed.
         * @return The intersection between the given rectangle and the set of rectangles.
         * Returns null if the intersection is empty.
         */
        public Rectangle getIntersectionAndRemoveFromSet(Rectangle rOp) {
            Rectangle intersection = null;
            RectangleSet newRs = new RectangleSet();
            for (Rectangle r : rs) {
                Rectangle intersectionPart = null;

                //check for the most common case: set's rectangle is a sub-area
                //of the given rectangle (common because repaint creates unions)
                //avoid further (complex) processing for this case
                if (rOp.contains(r)) {
                    intersectionPart = r;
                } else if (r.intersects(rOp)) {
                    //update intersection region
                    intersectionPart = r.intersection(rOp);
                    
                    //adjust rectangle: potentially split into 4 sub-rectangles
                    // ***************************
                    // *      |     3     |      *
                    // *      *************      *
                    // *   1  *    rOp    *   2  *
                    // *      *************      *
                    // *      |     4     |      *
                    // ***************************
                    
                    //region 1
                    if (r.x < rOp.x && (r.x + r.width) > rOp.x) {
                        newRs.add(new Rectangle(r.x,r.y,(rOp.x-r.x),r.height));
                    }
                    //region 2
                    if ((r.x + r.width) > (rOp.x + rOp.width) && r.x < (rOp.x + rOp.width)) {
                        newRs.add(new Rectangle((rOp.x+rOp.width),r.y,
                                (r.x+r.width-rOp.x-rOp.width),r.height));
                    }
                    //region 3
                    if (r.y < rOp.y) {
                        int x1 = Math.max(r.x, rOp.x);
                        int x2 = Math.min(r.x+r.width, rOp.x+rOp.width);
                        if (x1 < x2) newRs.add(new Rectangle(x1,r.y,x2-x1,rOp.y-r.y));
                    }
                    //region 4
                    if ((r.y + r.height) > (rOp.y + rOp.height)) {
                        int x1 = Math.max(r.x, rOp.x);
                        int x2 = Math.min(r.x+r.width, rOp.x+rOp.width);
                        if (x1 < x2) newRs.add(new Rectangle(x1,(rOp.y+rOp.height),
                                x2-x1,(r.y+r.height-rOp.y-rOp.height)));
                    }
                }
                
                if (intersectionPart == null) {
                    //if no intersection part, this rectangle remains unchanged in the set
                    newRs.add(r);
                } else {
                    //expand the intersection region if intersection part found
                    if (intersection == null) {
                        intersection = (Rectangle)intersectionPart.clone();
                    } else {
                        intersection.add(intersectionPart);
                    }
                }
            }
            rs = newRs.rs;
            return intersection;
        }
    }
    private abstract class HexLayer extends JComponent {
        private static final long serialVersionUID = 1L;
        private BufferedImage bufferedImage;
        /*
         * list of regions for which the layer's image buffer is dirty
         */
        private RectangleSet bufferDirtyRegions = new RectangleSet();;
        protected abstract void paintImage(Graphics g);
        final public void repaint() {
            bufferDirtyRegions.add( new Rectangle(0,0,getWidth(),getHeight()) );
            super.repaint();
        }
        public void repaint(Rectangle r) {
            bufferDirtyRegions.add( r );
            super.repaint(r);
        }
        final public void paintComponent(Graphics g) {
            super.paintComponents(g);

            //avoid that paintComponent is processed concurrently
            synchronized (HexLayer.this) {

                // Abort if called too early or if bounds are invalid.
                Rectangle rectClip = g.getClipBounds();
                if (rectClip == null) return;
                
                //ensure that image buffer of this layer is valid
                if (bufferedImage == null 
                        || bufferedImage.getWidth() != getWidth()
                        || bufferedImage.getHeight() != getHeight() ) {
                    //create new buffer image
                    bufferedImage = new BufferedImage(getWidth(), getHeight(),BufferedImage.TYPE_INT_ARGB);
                    
                    //clear information of the image buffer's dirty regions
                    bufferDirtyRegions = new RectangleSet();;
                    bufferDirtyRegions.add( new Rectangle(0,0,getWidth(),getHeight()) );
                    
                    //since the buffered image is empty, it has to be completely redrawn
                    rectClip = new Rectangle (0, 0, getWidth(), getHeight());
                }
                
                //determine which parts of the clip are dirty and have to be redrawn
                Rectangle dirtyClipArea = bufferDirtyRegions.getIntersectionAndRemoveFromSet(rectClip);
                if (dirtyClipArea != null) {
                    //buffer redraw is necessary
                    Graphics2D imageGraphics = (Graphics2D)bufferedImage.getGraphics();
                    
                    //apply the clip of the component's repaint to its image buffer
                    imageGraphics.setClip(dirtyClipArea.x,
                            dirtyClipArea.y, 
                            dirtyClipArea.width, 
                            dirtyClipArea.height);
                    
                    //set the background to transparent so that only drawn parts of the
                    //buffer will be taken over
                    imageGraphics.setBackground(new Color(0,0,0,0));
                    imageGraphics.setColor(Color.BLACK);
                    
                    //clear the clip (for a non-virtual graphic, this would have been
                    //done by super.paintComponent)
                    imageGraphics.clearRect(dirtyClipArea.x,
                            dirtyClipArea.y, 
                            dirtyClipArea.width, 
                            dirtyClipArea.height);
                    
                    //paint within the buffer
                    paintImage(imageGraphics);
                    
                    imageGraphics.dispose();
                }
    
                //now buffer is valid and can be used
                BufferedImage bufferedRect = bufferedImage.getSubimage(
                        rectClip.x, rectClip.y, rectClip.width, rectClip.height);
                g.drawImage(bufferedRect, rectClip.x, rectClip.y, null);
            }
        }
    }
    
    /**
     * Layer containing tiles
     */
    private class TilesLayer extends HexLayer {
        private static final long serialVersionUID = 1L;
        @Override
        public void paintImage(Graphics g) {
            try {
                // Paint tiles
                for (GUIHex hex : hexes) {
                    Rectangle hexrect = hex.getBounds();

                    if (g.hitClip(hexrect.x, hexrect.y, hexrect.width,
                            hexrect.height)) {
                        hex.paintTile(g);
                    }
                }

                // Paint the impassability bars
                for (GUIHex hex : hexes) {
                    Rectangle hexrect = hex.getBounds();

                    if (g.hitClip(hexrect.x, hexrect.y, hexrect.width,
                            hexrect.height)) {
                        hex.paintBars(g);
                    }
                }
                
            } catch (NullPointerException ex) {
                // If we try to paint before something is loaded, just retry later.
            }
        }
    }
    
    /**
     * Layer containing visualization of train routes
     */
    private class RoutesLayer extends HexLayer {
        private static final long serialVersionUID = 1L;

        public Rectangle getRoutesBounds(List<GeneralPath> p1,List<GeneralPath> p2) {
            int margin = (int)Math.ceil(strokeWidth * zoomFactor);

            List<Rectangle> pathRects = new ArrayList<Rectangle>();
            if (p1 != null) {
                for (GeneralPath p : p1 ) pathRects.add(p.getBounds());
            }
            if (p2 != null) {
                for (GeneralPath p : p2 ) pathRects.add(p.getBounds());
            }

            Rectangle r = null;
            for (Rectangle pathRect : pathRects ) {
                //enlarge path rectangle with margin
                Rectangle pathMarginRect = new Rectangle(
                        pathRect.x - margin,
                        pathRect.y - margin,
                        pathRect.width + margin * 2,
                        pathRect.height + margin * 2);
                if (r == null) {
                    r = pathMarginRect;
                } else {
                    r.add(pathMarginRect);
                }
            }
            return r;  
        };

        @Override
        public void paintImage(Graphics g) {
            try {
                // Abort if called too early.
                Rectangle rectClip = g.getClipBounds();
                if (rectClip == null) {
                    return;
                }

                // paint train paths
                if (trainPaths != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    Stroke oldStroke = g2.getStroke();
                    Color oldColor = g2.getColor();
                    Stroke trainStroke =
                        new BasicStroke((int)(strokeWidth * zoomFactor), strokeCap, strokeJoin);
                    g2.setStroke(trainStroke);
        
                    Color[] trainColors = new Color[]{colour1, colour2, colour3, colour4};
                    int color = 0;
                    for (GeneralPath path:trainPaths) {
                        g2.setColor(trainColors[color++ % trainColors.length]);
                        g2.draw(path);
                    }
                    g2.setStroke(oldStroke);
                    g2.setColor(oldColor);
                }
            } catch (NullPointerException ex) {
                // If we try to paint before something is loaded, just retry later.
            }
        }
    }
    
    /**
     * Layer containing marks on hexes (selected, selectable, highlighted).
     * Content may change very fast (due to mouse overs)
     */
    private class MarksLayer extends HexLayer {
        private static final long serialVersionUID = 1L;
        @Override
        public void paintImage(Graphics g) {
            try {
                // Abort if called too early.
                Rectangle rectClip = g.getClipBounds();
                if (rectClip == null) {
                    return;
                }

                // Paint tiles
                for (GUIHex hex : hexes) {
                    Rectangle hexrect = hex.getBounds();

                    if (g.hitClip(hexrect.x, hexrect.y, hexrect.width,
                            hexrect.height)) {
                        hex.paintMarks(g);
                    }
                }

            } catch (NullPointerException ex) {
                // If we try to paint before something is loaded, just retry later.
            }
        }
    }
    
    /**
     * Layer containing tokens and (if no background map is used) text annotations 
     */
    private class TokensTextsLayer extends HexLayer {
        private static final long serialVersionUID = 1L;
        private void drawLabel(Graphics2D g2, int index, int xCoordinate, int yCoordinate, boolean letter) {
            String label = letter ? getLetterLabel (index) : getNumberLabel (index);

            xCoordinate -= 4.0*label.length();
            yCoordinate += 4.0;
            g2.drawString(label,
                    xCoordinate,
                    yCoordinate);

//            log.debug("Draw Label " + label + " for " + index + " at x = " + xCoordinate + ", y = " + yCoordinate);
        }

        private String getLetterLabel (int index) {
            if (index > 26) {
                return "A" + String.valueOf((char)('@'+(index-26)));  // For 1825U1 row "AA"
            } else {
                return String.valueOf((char)('@'+index));
            }
        }

        @Override
        public void paintImage(Graphics g) {
            try {
                // Abort if called too early.
                Rectangle rectClip = g.getClipBounds();
                if (rectClip == null) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g;

                // Paint station tokens and texts
                for (GUIHex hex : hexes) {
                    Rectangle hexrect = hex.getBounds();

                    if (g.hitClip(hexrect.x, hexrect.y, hexrect.width,
                            hexrect.height)) {
                        hex.paintTokensAndText(g);
                    }
                }
                
                //paint coordinates
                boolean lettersGoHorizontal = mapManager.lettersGoHorizontal();
                int xLeft = (int)calcXCoordinates(minCol, - coordinateXMargin);
                int xRight = (int)calcXCoordinates(maxCol, coordinateXMargin);

                int yTop = (int)calcYCoordinates(minRow, - coordinateYMargin);
                int yBottom = (int)calcYCoordinates(maxRow,  coordinateYMargin);

                for (int iCol = minCol; iCol <= maxCol; iCol++) {
                    int xCoordinate = (int)(calcXCoordinates(iCol, 0));
                    drawLabel(g2, iCol, xCoordinate, yTop, lettersGoHorizontal);
                    drawLabel(g2, iCol, xCoordinate, yBottom, lettersGoHorizontal);
                }

                for (int iRow = minRow; iRow <= maxRow; iRow++) {
                    int yCoordinate = (int)(calcYCoordinates(iRow, 0));
                    drawLabel(g2, iRow, xLeft, yCoordinate, !lettersGoHorizontal);
                    drawLabel(g2, iRow, xRight, yCoordinate, !lettersGoHorizontal);
                }

            } catch (NullPointerException ex) {
                // If we try to paint before something is loaded, just retry later.
            }
        }
    }
    
    /**
     * The only "real" (=swing managed) layer that is used for tool tips
     */
    private class ToolTipsLayer extends JComponent {
        private static final long serialVersionUID = 1L;
    }
    
    private TilesLayer tilesLayer;
    private RoutesLayer routesLayer;
    private MarksLayer marksLayer;
    private TokensTextsLayer tokensTextsLayer;
    private ToolTipsLayer toolTipsLayer;
    private List<JComponent> layers;
    
    protected static Logger log =
            Logger.getLogger(HexMap.class.getPackage().getName());

    protected ORUIManager orUIManager;
    protected MapManager mapManager;

    // GUI hexes need to be recreated for each object, since scale varies.
    protected GUIHex[][] h;

    protected MapHex[][] hexArray;
    protected Map<String, GUIHex> hexesByName = new HashMap<String, GUIHex>();
    protected ArrayList<GUIHex> hexes;
    protected double scale;
    protected int zoomStep = 10; // can be overwritten in config
    protected double zoomFactor = 1;  // defined dynamically if zoomStep changed
    protected double peakMargin = 1.0;
    protected double flatMargin = 0.80;
    protected double coordinatePeakMargin = 0.80;
    protected double coordinateFlatMargin = 0.60;
    protected GUIHex selectedHex = null;
    protected Dimension originalSize;
    protected Dimension currentSize;
    protected int minX, minY, maxX, maxY;
    protected int minCol, maxCol, minRow, maxRow;

    /**
     * The hex over which the mouse pointer is currently situated
     */
    private GUIHex hexAtMousePosition = null;
    
    /** A list of all allowed tile lays */
    /* (may be redundant) */
    protected List<LayTile> allowedTileLays = null;

    /** A Map linking tile allowed tiles to each map hex */
    protected Map<MapHex, LayTile> allowedTilesPerHex = null;

    /** A list of all allowed token lays */
    /* (may be redundant) */
    protected List<LayToken> allowedTokenLays = null;

    /** A Map linking tile allowed tiles to each map hex */
    protected Map<MapHex, List<LayToken>> allowedTokensPerHex = null;

    protected boolean bonusTokenLayingEnabled = false;

    /** list of generalpath elements to indicate train runs */
    protected List<GeneralPath> trainPaths;

    private static Color colour1, colour2, colour3, colour4;
    protected int strokeWidth = 5;
    protected int strokeCap = BasicStroke.CAP_ROUND;
    protected int strokeJoin = BasicStroke.JOIN_BEVEL;

    // Abstract Methods, implemented depending on the map type (EW or NS)
    protected abstract double calcXCoordinates(int col, double offset);
    protected abstract double calcYCoordinates(int row, double offset);
    protected abstract void setOriginalSize();

    // ("Abstract") Variables to be initialized by map type subclasses
    protected double tileXOffset;
    protected double tileYOffset;
    protected double coordinateXMargin;
    protected double coordinateYMargin;
    
    protected boolean displayMapImage;

    public static void setRouteColours () {
        try {
            colour1 = Util.parseColour(Config.get("route.colour.1", null));
            colour2 = Util.parseColour(Config.get("route.colour.2", null));
            colour3 = Util.parseColour(Config.get("route.colour.3", null));
            colour4 = Util.parseColour(Config.get("route.colour.4", null));
        } catch (ConfigurationException e) {
        } finally {
            if (colour1 == null) colour1 = Color.CYAN;
            if (colour2 == null) colour2 = Color.PINK;
            if (colour3 == null) colour3 = Color.ORANGE;
            if (colour4 == null) colour4 = Color.GRAY;
        }
    }

    public void init(ORUIManager orUIManager, MapManager mapManager) {

        this.orUIManager = orUIManager;
        this.mapManager = mapManager;
        
        displayMapImage = mapManager.isMapImageUsed();

        minX = mapManager.getMinX();
        minY = mapManager.getMinY();
        maxX = mapManager.getMaxX();
        maxY = mapManager.getMaxY();
        minRow = mapManager.getMinRow();
        minCol = mapManager.getMinCol();
        maxRow = mapManager.getMaxRow();
        maxCol = mapManager.getMaxCol();
        log.debug("HexMap init: minX="+ minX + ",minY=" + minY + ",maxX=" +maxX + ",maxY=" + maxY);
        log.debug("HexMap init: minCol="+ minCol + ",minRow=" + minRow + ",maxCol=" +maxCol + ",maxRow=" + maxRow);

        //the following order of instantiation and list-adding defines the layering
        //from the top to the bottom
        layers = new ArrayList<JComponent>();
        toolTipsLayer = new ToolTipsLayer();
        layers.add(toolTipsLayer);
        tokensTextsLayer = new TokensTextsLayer();
        layers.add(tokensTextsLayer);
        marksLayer = new MarksLayer();
        layers.add(marksLayer);
        routesLayer = new RoutesLayer();
        layers.add(routesLayer);
        tilesLayer = new TilesLayer();
        layers.add(tilesLayer);
        
        setScale();
        setupHexes();
        setOriginalSize();
        currentSize = (Dimension)originalSize.clone();
        setPreferredSize (originalSize);

        initializeSettings();

        setRouteColours();
    }

    /**
     * defines settings from the config files
     */
    private void initializeSettings() {

        // define zoomStep from config
        String zoomStepSetting = Config.getGameSpecific("map.zoomstep");
        if (Util.hasValue(zoomStepSetting)) {
            try {
                int newZoomStep = Integer.parseInt(zoomStepSetting);
                if (zoomStep != newZoomStep) {
                    zoomStep = newZoomStep;
                    zoom();
                }
            } catch (NumberFormatException e) {
                // otherwise keep default defined above
            }
        }
    }

    public void addLayers (JLayeredPane p, int startingZOffset) {
        int z = startingZOffset;
        for (JComponent l : layers ) {
            p.add(l, z++);
        }
    }


    protected void setupHexesGUI() {

        hexes = new ArrayList<GUIHex>();

        hexArray = mapManager.getHexes();
        MapHex mh;

        h = new GUIHex[hexArray.length][hexArray[0].length];
        for (int i = minX; i < hexArray.length; i++) {
            for (int j = minY; j < hexArray[0].length; j++) {
                mh = hexArray[i][j];
                if (mh != null) {
                    GUIHex hex = new GUIHex(this, calcXCoordinates(mh.getColumn(), tileXOffset),
                            calcYCoordinates(mh.getRow(), tileYOffset),
                            scale, i-minX+1, j-minY+1);
                    hex.setHexModel(mh);
                    hex.originalTileId = hex.currentTileId;
                    hexesByName.put(mh.getName(), hex);
                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
    }

    protected void scaleHexesGUI  () {
        hexArray = mapManager.getHexes();
        GUIHex hex;
        for (int i = minX; i < hexArray.length; i++) {
            for (int j = minY; j < hexArray[0].length; j++) {
                hex = h[i][j];
                MapHex mh = hexArray[i][j];
                if (hex != null) {
                    hex.scaleHex(calcXCoordinates(mh.getColumn(), tileXOffset), calcYCoordinates(mh.getRow(), tileYOffset),
                                 scale, zoomFactor);
                }
            }
        }
        
    }

    private String getNumberLabel (int index) {
    	if (index < 0) {
    		return String.valueOf(100 + index); // For 1825U1 column "99"
    	} else {
    		return String.valueOf(index);
    	}
    }

    public void setupHexes() {
        setupHexesGUI();
        setupBars();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setupBars() {
        List<Integer> barSides;
        for (GUIHex hex : hexes) {
            barSides = hex.getHexModel().getImpassableSides();
            if (barSides != null) {
                for (int k : barSides) {
                    if (k < 3) hex.addBar (k);
                }
            }
        }
    }

    GUIHex getHexContainingPoint(Point point) {
        for (GUIHex hex : hexes) {
            if (hex.contains(point)) {
                return hex;
            }
        }

        return null;
    }

    public GUIHex getHexByName (String hexName) {
        return hexesByName.get (hexName);
    }

    public boolean hasMapImage() {
        return displayMapImage;
    }
    
    public boolean isTilePainted (int tileID) {
        return !displayMapImage || tileID > 0;
    }
    
    public void zoom (boolean in) {
        if (in) zoomStep++; else zoomStep--;
        zoom();
    }
    
    public void setZoomStep (int zoomStep) {
        this.zoomStep = zoomStep;
        zoom();
    }
    
    public int getZoomStep () {
        return zoomStep;
    }

    private void zoom() {
        zoomFactor = GameUIManager.getImageLoader().getZoomFactor(zoomStep);
        log.debug("HexMap: zoomStep = "+ zoomStep);
        log.debug("HexMap: zoomFactor = " + zoomFactor);
        setScale();
        scaleHexesGUI();
        currentSize.width = (int)(originalSize.width * zoomFactor);
        currentSize.height = (int)(originalSize.height * zoomFactor);
        setPreferredSize (currentSize);
    }

    protected void setScale() {
        scale = (Scale.get() * zoomFactor);
    }

    public Dimension getOriginalSize() {
        return originalSize;
    }

    public Dimension getCurrentSize() {
        return currentSize;
    }
    public void selectHex(GUIHex clickedHex) {
        log.debug("selecthex called for hex "
                  + (clickedHex != null ? clickedHex.getName() : "null")
                  + ", selected was "
                  + (selectedHex != null ? selectedHex.getName() : "null"));

        if (selectedHex == clickedHex) return;
        if (selectedHex != null) {
            selectedHex.setSelected(false);
            log.debug("Hex " + selectedHex.getName()
                      + " deselected and repainted");
        }

        if (clickedHex != null) {
            clickedHex.setSelected(true);
            log.debug("Hex " + clickedHex.getName() + " selected and repainted");
        }
        selectedHex = clickedHex;

    }

    public GUIHex getSelectedHex() {
        return selectedHex;
    }

    public void setSelectedHex (GUIHex hex) {
    	selectedHex = hex;
    }

    public boolean isAHexSelected() // Not used
    {
        return selectedHex != null;
    }
    
    public List<GUIHex> getHexesByCurrentTileId (int tileId) {
        List<GUIHex> hs = new ArrayList<GUIHex>();
        for (GUIHex h : hexes) {
            TileI tile = h.getCurrentTile();
            if (tile != null && tile.getId() == tileId) {
                hs.add(h);
            }
        }
        return hs;
    }

    public void setAllowedTileLays(List<LayTile> allowedTileLays) {

        this.allowedTileLays = allowedTileLays;
        allowedTilesPerHex = new HashMap<MapHex, LayTile>();

        /* Build the per-hex allowances map */
        for (LayTile allowance : this.allowedTileLays) {
            List<MapHex> locations = allowance.getLocations();
            if (locations == null) {
                /*
                 * The location may be null, which means: anywhere. This is
                 * intended to be a temporary fixture, to be replaced by a
                 * detailed allowed-tiles-per-hex specification later.
                 */
                allowedTilesPerHex.put(null, allowance);
            } else {
                for (MapHex location : locations) {
                    allowedTilesPerHex.put(location, allowance);
                }
            }
        }
    }

    public List<LayTile> getTileAllowancesForHex(MapHex hex) {

        List<LayTile> lays = new ArrayList<LayTile>();
        if (allowedTilesPerHex.containsKey(hex)) {
            lays.add(allowedTilesPerHex.get(hex));
        }
        if (allowedTilesPerHex.containsKey(null)) {
            lays.add(allowedTilesPerHex.get(null));
        }

        return lays;
    }

    @SuppressWarnings("unchecked")
    public <T extends LayToken> void setAllowedTokenLays(
            List<T> allowedTokenLays) {

        this.allowedTokenLays = (List<LayToken>) allowedTokenLays;
        allowedTokensPerHex = new HashMap<MapHex, List<LayToken>>();
        bonusTokenLayingEnabled = false;

        /* Build the per-hex allowances map */
        for (LayToken allowance : this.allowedTokenLays) {
            List<MapHex> locations = allowance.getLocations();
            if (locations == null) {
                /*
                 * The location may be null, which means: anywhere. This is
                 * intended to be a temporary fixture, to be replaced by a
                 * detailed allowed-tiles-per-hex specification later.
                 */
                // For now, allow all hexes having non-filled city stations
                if (allowance instanceof LayBaseToken) {
                    MapHex hex;
                    for (GUIHex guiHex : hexes) {
                        hex = guiHex.getHexModel();
                        if (hex.hasTokenSlotsLeft()) {
                            allowTokenOnHex(hex, allowance);
                        }
                    }
                } else {
                    allowTokenOnHex(null, allowance);
                }
            } else {
                for (MapHex location : locations) {
                    allowTokenOnHex(location, allowance);
                }
            }
            if (allowance instanceof LayBonusToken) {
                bonusTokenLayingEnabled = true;
            }
        }
    }

    private void allowTokenOnHex(MapHex hex, LayToken allowance) {
        if (!allowedTokensPerHex.containsKey(hex)) {
            allowedTokensPerHex.put(hex, new ArrayList<LayToken>());
        }
        allowedTokensPerHex.get(hex).add(allowance);
    }

    public List<LayToken> getTokenAllowanceForHex(MapHex hex) {
        List<LayToken> allowances = new ArrayList<LayToken>(2);
        if (hex != null && allowedTokensPerHex.containsKey(hex)) {
            allowances.addAll(allowedTokensPerHex.get(hex));
        }
        if (allowedTokensPerHex.containsKey(null)) {
            allowances.addAll(allowedTokensPerHex.get(null));
        }
        return allowances;
    }

    public List<LayBaseToken> getBaseTokenAllowanceForHex(MapHex hex) {
        List<LayBaseToken> allowances = new ArrayList<LayBaseToken>(2);
        for (LayToken allowance : getTokenAllowanceForHex(hex)) {
            if (allowance instanceof LayBaseToken) {
                allowances.add((LayBaseToken) allowance);
            }
        }
        return allowances;
    }

    public List<LayBonusToken> getBonusTokenAllowanceForHex(MapHex hex) {
        List<LayBonusToken> allowances = new ArrayList<LayBonusToken>(2);
        for (LayToken allowance : getTokenAllowanceForHex(hex)) {
            if (allowance instanceof LayBonusToken) {
                allowances.add((LayBonusToken) allowance);
            }
        }
        return allowances;
    }

    public void setTrainPaths(List<GeneralPath> trainPaths) {
        Rectangle dirtyRect = routesLayer.getRoutesBounds(this.trainPaths, trainPaths);
        this.trainPaths = trainPaths;
        
        //only repaint if routes existed before or exist now
        if (dirtyRect != null) repaintRoutes(dirtyRect);
    }

    /**
     * Off-board tiles must be able to retrieve the current phase.
     *
     * @return The current Phase object.
     */
    public PhaseI getPhase () {
        if (orUIManager != null) {
            GameUIManager u = orUIManager.getGameUIManager();
            GameManagerI g = u.getGameManager();
            PhaseManager p = g.getPhaseManager();
            return p.getCurrentPhase();
        }
        return null;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public ORUIManager getOrUIManager() {
		return orUIManager;
	}
    
    public void updateOffBoardToolTips() {
//      for (GUIHex hex : hexes) {
//          if (hex.getHexModel().hasOffBoardValues()) {
//              hex.setToolTip();
//          }
//      }
      // do nothing as tooltip update before display
  }
    
    /**
     * Mouse Listener methods (hexMap offers listener for all layers)
     */
    
	public synchronized void mouseClicked(MouseEvent arg0) {
        Point point = arg0.getPoint();
        GUIHex clickedHex = getHexContainingPoint(point);

        //if no action/correction was expected on the map panel
        if (!orUIManager.hexClicked(clickedHex, selectedHex)) {

            // force the tool tip popup to appear immediately
            ToolTipManager ttm = ToolTipManager.sharedInstance();
            MouseEvent phantomME = new MouseEvent(
                    toolTipsLayer,
                    MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(),
                    0,
                    arg0.getX(),
                    arg0.getY(),
                    0,
                    false);
    
            int priorToolTipDelay = ttm.getInitialDelay(); 
            ttm.setInitialDelay(0);
            ttm.mouseMoved(phantomME);
            ttm.setInitialDelay(priorToolTipDelay);

//        int priorToolTipDelay = ttm.getInitialDelay(); 
//        ttm.mouseEntered(new MouseAdapter());
//        ToolTipManager.sharedInstance().setInitialDelay(0);
//        try {
//            this.wait(1);
//        } catch (InterruptedException e) {}
//        map = map;
        }
    }

    public void mouseDragged(MouseEvent arg0) {}

    public synchronized void mouseMoved(MouseEvent arg0) {
        Point point = arg0.getPoint();
        GUIHex newHex = getHexContainingPoint(point);
        
        //ignore if mouse has not entered a new hex
        if (hexAtMousePosition == newHex) return;
        
        //provide for hex highlighting
        if (hexAtMousePosition != null) hexAtMousePosition.removeHighlightRequest();
        if (newHex != null) newHex.addHighlightRequest();
        
        //provide for upgrade tiles highlighting (remaining tiles window)
        if ("yes".equals(Config.get("map.highlightHexes"))) {
            RemainingTilesWindow rtw = orUIManager.getRemainingTilesWindows();
            if (rtw != null) {
                List<TileI> upgrades = null;
                if (newHex != null) upgrades = newHex.getHexModel().getCurrentTile().getAllUpgrades(newHex.getHexModel());
                rtw.setHighlightedTiles(upgrades);
            }
        }
        
        //display tool tip
        setToolTipText(newHex != null ? newHex.getToolTip() : null);
        
        hexAtMousePosition = newHex;
    }

    public void mouseEntered(MouseEvent arg0) {}

    public synchronized void mouseExited(MouseEvent arg0) {
        //provide for hex highlighting
        if (hexAtMousePosition != null) {
            hexAtMousePosition.removeHighlightRequest();
            hexAtMousePosition = null;
        }
        
        //provide for upgrade tiles highlighting (remaining tiles window)
        if ("yes".equals(Config.get("map.highlightHexes"))) {
            RemainingTilesWindow rtw = orUIManager.getRemainingTilesWindows();
            if (rtw != null) rtw.setHighlightedTiles(null);
        }
    }

    public void mousePressed(MouseEvent arg0) {}

    public void mouseReleased(MouseEvent arg0) {}

    /**
     * Triggers for asynchronous repaint of specific layers 
     * If possible, these triggers:
     * - only apply for a specified area
     */

    public synchronized void repaintTiles (Rectangle r) {
        tilesLayer.repaint(r);
    }

    private synchronized void repaintRoutes (Rectangle r) {
        routesLayer.repaint(r);
    }
    
    public synchronized void repaintMarks (Rectangle r) {
        marksLayer.repaint(r);
    }
    
    public synchronized void repaintTokens (Rectangle r) {
        tokensTextsLayer.repaint(r);
    }
    
    /**
     * Do only call this method if you are sure that a complete repaint is needed!
     */
    public synchronized void repaintAll (Rectangle r) {
        for (JComponent l : layers ) {
            l.repaint(r);
        }
    }
    
    /**
     * JComponent methods delegating to the hexmap layers
     */

    public void setBounds (int x, int y, int width, int height) {
        for (JComponent l : layers) {
            l.setBounds(x, y, width, height);
        }
    }
    
    private void setPreferredSize (Dimension size) {
        for (JComponent l : layers) {
            l.setPreferredSize(size);
        }
    }
    
    private void setToolTipText (String text) {
        toolTipsLayer.setToolTipText(text);
    }

    public Dimension getSize () {
        //get size from top-most layer (all layers have the same size anyways)
        return layers.get(layers.size()-1).getSize();
    }
    
    private void addMouseListener (MouseListener ml) {
        for (JComponent l : layers) {
            l.addMouseListener(ml);
        }
    }

    private void addMouseMotionListener (MouseMotionListener ml) {
        for (JComponent l : layers) {
            l.addMouseMotionListener(ml);
        }
    }

}

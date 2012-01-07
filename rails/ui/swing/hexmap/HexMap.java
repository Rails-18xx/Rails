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

import rails.common.parser.Config;
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
 */
public abstract class HexMap implements MouseListener,
        MouseMotionListener {

    private abstract class HexLayer extends JComponent {
        private static final long serialVersionUID = 1L;
        private BufferedImage bufferedImage;
        private boolean isBufferDirty = false;
        protected abstract void paintImage(Graphics g);
        public void repaint() {
            isBufferDirty = true;
            super.repaint();
        }
        public void repaint(Rectangle r) {
            isBufferDirty = true;
            super.repaint(r);
        }
        final public void paintComponent(Graphics g) {
            super.paintComponents(g);

            // Abort if called too early.
            Rectangle rectClip = g.getClipBounds();
            if (rectClip == null) {
                return;
            }
            
            //ensure that image buffer of this layer is valid
            if (bufferedImage == null 
                    || bufferedImage.getWidth() != getWidth()
                    || bufferedImage.getHeight() != getHeight() ) {
                //create new buffer image
                bufferedImage = new BufferedImage(getWidth(), getHeight(),BufferedImage.TYPE_INT_ARGB);
                isBufferDirty = true;
                
                //since the buffered image is empty, it has to be completely redrawn
                rectClip = new Rectangle (0, 0, getWidth(), getHeight());
            }
            
            if (isBufferDirty) {
                //buffer redraw is necessary
                Graphics2D imageGraphics = (Graphics2D)bufferedImage.getGraphics();
                
                //apply the clip of the component's repaint to its image buffer
                imageGraphics.setClip(rectClip.x, rectClip.y, rectClip.width, rectClip.height);
                
                //set the background to transparent so that only drawn parts of the
                //buffer will be taken over
                imageGraphics.setBackground(new Color(0,0,0,0));
                imageGraphics.setColor(Color.BLACK);
                
                //clear the clip (for a non-virtual graphic, this would have been
                //done by super.paintComponent)
                imageGraphics.clearRect(rectClip.x, rectClip.y, rectClip.width, rectClip.height);
                
                //paint within the buffer
                paintImage(imageGraphics);
                
                imageGraphics.dispose();
                isBufferDirty = false;
            }

            //buffer is valid and can be used
            BufferedImage bufferedRect = bufferedImage.getSubimage(
                    rectClip.x, rectClip.y, rectClip.width, rectClip.height);
            g.drawImage(bufferedRect, rectClip.x, rectClip.y, null);
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
    
    private TilesLayer tilesLayer;
    private RoutesLayer routesLayer;
    private MarksLayer marksLayer;
    private TokensTextsLayer tokensTextsLayer; 
    private List<HexLayer> hexLayers;
    
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
        hexLayers = new ArrayList<HexLayer>();
        tokensTextsLayer = new TokensTextsLayer();
        hexLayers.add(tokensTextsLayer);
        marksLayer = new MarksLayer();
        hexLayers.add(marksLayer);
        routesLayer = new RoutesLayer();
        hexLayers.add(routesLayer);
        tilesLayer = new TilesLayer();
        hexLayers.add(tilesLayer);
        
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
        for (HexLayer l : hexLayers ) {
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
    
/**
 * Zoom-to-fit functionality is based on the discrete zoom steps.
 * This means that no pixel precision is to be expected 
 */
    public void zoomFit (Dimension availableSize, boolean fitToWidth, boolean fitToHeight) {
        double idealFactorWidth = availableSize.getWidth() / originalSize.width;
        double idealFactorHeight = availableSize.getHeight() / originalSize.height;
        //increase zoomFactor until constraints do not hold
        //OR zoom cannot be increased any more
        while
            (
                    (
                            (!fitToWidth || idealFactorWidth > GameUIManager.getImageLoader().getZoomFactor(zoomStep))
                            &&
                            (!fitToHeight || idealFactorHeight > GameUIManager.getImageLoader().getZoomFactor(zoomStep))
                    )
                    &&
                    GameUIManager.getImageLoader().getZoomFactor(zoomStep+1) != GameUIManager.getImageLoader().getZoomFactor(zoomStep)
            )
            zoomStep++;
        //decrease zoomFactor until constraints do hold
        //OR zoom cannot be decreased any more
        while
            (
                    (
                            (fitToWidth && idealFactorWidth < GameUIManager.getImageLoader().getZoomFactor(zoomStep))
                            ||
                            (fitToHeight && idealFactorHeight < GameUIManager.getImageLoader().getZoomFactor(zoomStep))
                    )
                    &&
                    GameUIManager.getImageLoader().getZoomFactor(zoomStep-1) != GameUIManager.getImageLoader().getZoomFactor(zoomStep)
            )
            zoomStep--;
        //trigger zoom execution
        zoom();
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

    public int getZoomStep () {
        return zoomStep;
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
        this.trainPaths = trainPaths;
        repaintRoutes();
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
    
	public void mouseClicked(MouseEvent arg0) {
        Point point = arg0.getPoint();
        GUIHex clickedHex = getHexContainingPoint(point);

        orUIManager.hexClicked(clickedHex, selectedHex);
    }

    public void mouseDragged(MouseEvent arg0) {}

    public void mouseMoved(MouseEvent arg0) {
        Point point = arg0.getPoint();
        GUIHex hex = getHexContainingPoint(point);
        setToolTipText(hex != null ? hex.getToolTip() : "");
    }

    public void mouseEntered(MouseEvent arg0) {}

    public void mouseExited(MouseEvent arg0) {}

    public void mousePressed(MouseEvent arg0) {}

    public void mouseReleased(MouseEvent arg0) {}

    /**
     * Triggers for asynchronous repaint of specific layers 
     * If possible, these triggers:
     * - only apply for a specified area
     */

    public void repaintTiles (Rectangle r) {
        tilesLayer.repaint(r);
    }

    public void repaintRoutes () {
        routesLayer.repaint();
    }
    
    public void repaintMarks (Rectangle r) {
        marksLayer.repaint(r);
    }
    
    public void repaintTokens (Rectangle r) {
        tokensTextsLayer.repaint(r);
    }
    
    /**
     * Do only call this method if you are sure that a complete repaint is needed!
     */
    public void repaintAll (Rectangle r) {
        for (HexLayer l : hexLayers ) {
            l.repaint(r);
        }
    }
    
    /**
     * JComponent methods delegating to the hexmap layers
     */

    public void setBounds (int x, int y, int width, int height) {
        for (HexLayer l : hexLayers) {
            l.setBounds(x, y, width, height);
        }
    }
    
    private void setPreferredSize (Dimension size) {
        for (HexLayer l : hexLayers) {
            l.setPreferredSize(size);
        }
    }
    
    private void setToolTipText (String text) {
        //set tool tip on top-most layer (so that it is always visible)
        hexLayers.get(hexLayers.size()-1).setToolTipText(text);
    }

    public Dimension getSize () {
        //get size from top-most layer (all layers have the same size anyways)
        return hexLayers.get(hexLayers.size()-1).getSize();
    }
    
    private void addMouseListener (MouseListener ml) {
        for (HexLayer l : hexLayers) {
            l.addMouseListener(ml);
        }
    }

    private void addMouseMotionListener (MouseMotionListener ml) {
        for (HexLayer l : hexLayers) {
            l.addMouseMotionListener(ml);
        }
    }

}

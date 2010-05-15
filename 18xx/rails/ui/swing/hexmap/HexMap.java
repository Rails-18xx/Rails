/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/HexMap.java,v 1.26 2010/05/15 22:44:46 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.*;
import java.util.List;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.ORUIManager;
import rails.util.*;

/**
 * Base class that stores common info for HexMap independant of Hex
 * orientations.
 */
public abstract class HexMap extends JComponent implements MouseListener,
        MouseMotionListener {

    protected static Logger log =
            Logger.getLogger(HexMap.class.getPackage().getName());

    protected ORUIManager orUIManager;
    protected MapManager mapManager;

    // Abstract Methods
    protected abstract void setupHexesGUI();
    protected abstract void scaleHexesGUI();

    // GUI hexes need to be recreated for each object, since scale varies.
    protected GUIHex[][] h;

    protected MapHex[][] hexArray;
    protected Map<String, GUIHex> hexesByName = new HashMap<String, GUIHex>();
    protected ArrayList<GUIHex> hexes;
    protected int defaultScale;
    protected int scale;
    protected int zoomStep = 10;
    protected double zoomFactor = 1.0;
    protected int cx;
    protected int cy;
    protected GUIHex selectedHex = null;
    protected Dimension preferredSize;
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
    protected Color[] trainColors = new Color[4];
    protected int strokeWidth = 5;
    protected int strokeCap = BasicStroke.CAP_ROUND;
    protected int strokeJoin = BasicStroke.JOIN_BEVEL;
        
    static {
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
        minX = mapManager.getMinX();
        minY = mapManager.getMinY();
        maxX = mapManager.getMaxX();
        maxY = mapManager.getMaxY();
        minRow = mapManager.getMinRow();
        minCol = mapManager.getMinCol();
        maxRow = mapManager.getMaxRow();
        maxCol = mapManager.getMaxCol();
        setupHexes();
        
        trainColors = new Color[]{colour1, colour2, colour3, colour4};
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        try {
            // Abort if called too early.
            Rectangle rectClip = g.getClipBounds();
            if (rectClip == null) {
                return;
            }

            for (GUIHex hex : hexes) {
                Rectangle hexrect = hex.getBounds();

                if (g.hitClip(hexrect.x, hexrect.y, hexrect.width,
                        hexrect.height)) {
                    hex.paint(g);
                }
            }

            // Paint the impassability bars latest
            for (GUIHex hex : hexes) {
                Rectangle hexrect = hex.getBounds();

                if (g.hitClip(hexrect.x, hexrect.y, hexrect.width,
                        hexrect.height)) {
                    hex.paintBars(g);
                }
            }
            
            // paint train paths
            Graphics2D g2 = (Graphics2D) g;
            Stroke trainStroke = 
                new BasicStroke((int)(strokeWidth * zoomFactor), strokeCap, strokeJoin);
            g2.setStroke(trainStroke);
            int color = 0;
            for (GeneralPath path:trainPaths) {
                g2.setColor(trainColors[color++ % trainColors.length]);
                g2.draw(path);
            }

        } catch (NullPointerException ex) {
            // If we try to paint before something is loaded, just retry later.
        }
    }

    public void zoomIn () {
        zoomStep++;
        zoom();
    }
    public void zoomOut() {
        zoomStep--;
        zoom();
    }

    protected void zoom() {
        zoomFactor = GameUIManager.getImageLoader().getZoomFactor(zoomStep);
        setScale();
        scaleHexesGUI();
        revalidate();
    }

    protected void setScale() {
        scale = (int)(defaultScale * zoomFactor);
    }

    public int getZoomStep () {
        return zoomStep;
    }

    /*
    @Override
    public Dimension getMinimumSize() {
        Dimension dim = new Dimension();
        Rectangle r = (h[h.length][h[0].length]).getBounds();
        dim.height = (maxRow-minRow) + 40;
        dim.width = (maxCol-minCol) + 100;
        return dim;
    }
    */

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    public void selectHex(GUIHex clickedHex) {
        log.debug("selecthex called for hex "
                  + (clickedHex != null ? clickedHex.getName() : "null")
                  + ", selected was "
                  + (selectedHex != null ? selectedHex.getName() : "null"));

        if (selectedHex == clickedHex) return;
        if (selectedHex != null) {
            selectedHex.setSelected(false);
            repaint(selectedHex.getBounds());
            log.debug("Hex " + selectedHex.getName()
                      + " deselected and repainted");
        }

        if (clickedHex != null) {
            clickedHex.setSelected(true);
            repaint(clickedHex.getBounds());
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
    }
    
    /**
     * Off-board tiles must be able to retrieve the current phase.
     *
     * @return The current Phase object.
     */
    public PhaseI getPhase () {
        if (orUIManager != null) {
            //return orUIManager.getGameUIManager().getGameManager().getPhaseManager().getCurrentPhase();
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

    public void mouseClicked(MouseEvent arg0) {
        Point point = arg0.getPoint();
        GUIHex clickedHex = getHexContainingPoint(point);

        orUIManager.hexClicked(clickedHex, selectedHex);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent arg0) {}

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent arg0) {
        Point point = arg0.getPoint();
        GUIHex hex = getHexContainingPoint(point);
        setToolTipText(hex != null ? hex.getToolTip() : "");
    }

    public void mouseEntered(MouseEvent arg0) {}

    public void mouseExited(MouseEvent arg0) {}

    public void mousePressed(MouseEvent arg0) {}

    public void mouseReleased(MouseEvent arg0) {}

    public void updateOffBoardToolTips() {
        for (GUIHex hex : hexes) {
            if (hex.getHexModel().hasOffBoardValues()) {
                hex.setToolTip();
            }
        }
    }
}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MapPanel.java,v 1.15 2010/06/24 21:48:08 stefanfrey Exp $*/
package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import net.sf.rails.common.Config;
import net.sf.rails.game.MapManager;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.ui.swing.hexmap.HexMapImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MapWindow class displays the Map Window. It's shocking, I know.
 */
public class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    // defines how many pixels should be left as safety margin when calculating fit
    // zooms
    private static final int ZOOM_FIT_SAFETY_MARGIN = 4;

    private MapManager mmgr;
    private HexMap map;
    private HexMapImage mapImage;
    private JScrollPane scrollPane;

    private GameUIManager gameUIManager;

    private JLayeredPane layeredPane;
    private Dimension originalMapSize;
    private Dimension currentMapSize;

    // active fit-to zoom options
    private boolean fitToWidth = false;
    private boolean fitToHeight = false;

    private static final Logger log = LoggerFactory.getLogger(MapPanel.class);

    public MapPanel(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;

        setLayout(new BorderLayout());

        mmgr = gameUIManager.getRoot().getMapManager();
        try {
            map = (HexMap) Class.forName(mmgr.getMapUIClassName()).newInstance();
            map.init(gameUIManager.getORUIManager(), mmgr);
            originalMapSize = map.getOriginalSize();

            // Load persistent map layer settings
            map.setDisplayCityNames(Config.getBoolean("layer.displayCityNames", map.getDisplayCityNames()));
            map.setDisplayOffboardValues(Config.getBoolean("layer.displayOffboardValues", map.getDisplayOffboardValues()));
            map.setDisplayLastRevenueRuns(Config.getBoolean("layer.displayLastRevenueRuns", map.getDisplayLastRevenueRuns()));

        } catch (Exception e) {

            log.error("CRITICAL: Map class instantiation or initialization error:", e);
            e.printStackTrace(); // Force output to the console

            return;
        }

        // lightwight tooltip possible since tool tip has its own layer in hex map
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);

        // tooltip should not be dismissed after at all
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        setupLayersButton(gameUIManager.getORUIManager());
        layeredPane.setPreferredSize(originalMapSize);
        map.setBounds(0, 0, originalMapSize.width, originalMapSize.height);
        map.addLayers(layeredPane, 1);

        if (mmgr.isMapImageUsed()) {
            mapImage = new HexMapImage();
            mapImage.init(mmgr, map);
            mapImage.setPreferredSize(originalMapSize);
            mapImage.setBounds(0, 0, originalMapSize.width, originalMapSize.height);
            layeredPane.add(mapImage, -1);
        }

        scrollPane = new JScrollPane(layeredPane);
        scrollPane.setSize(originalMapSize);
        add(scrollPane, BorderLayout.CENTER);

        setSize(originalMapSize);
        setLocation(25, 25);

        // add listener for auto fit upon resize events
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                zoomFit(fitToWidth, fitToHeight);
            }
        });

        // NEW: Load saved layer properties from window settings ini
        loadLayerSettings();

    }

    /**
     * Redirects the panel's data source to the current MapManager of the active
     * root.
     */
    public void updateData() {
        this.mmgr = gameUIManager.getRoot().getMapManager();
        if (this.map != null) {
            // Re-initialize the HexMap with the new manager's data
            this.map.init(gameUIManager.getORUIManager(), mmgr);
        }
        this.repaint();
    }

    public void zoomIn() {
        if (map != null) {
            zoom(true); // Call LOCAL method, not map.zoomIn()
            this.revalidate();
            this.repaint();
        }
    }

    public void zoomOut() {
        if (map != null) {
            zoom(false); // Call LOCAL method
            this.revalidate();
            this.repaint();
        }
    }

    public void toggleDisplayHexNames() {
        if (map != null) {
            // Fix: Use getDisplayHexNames() instead of isDisplayHexNames()
            // If getDisplayHexNames() doesn't exist, check HexMap.java for the correct
            // getter
            boolean current = map.getDisplayHexNames();
            map.setDisplayHexNames(!current);
            this.repaint(); // Call repaint on the Panel, not the map object
        }
    }

    public void toggleDisplayBuildNumbers() {
        if (map != null) {
            // Fix: Use getDisplayBuildNumbers() instead of isDisplayBuildNumbers()
            boolean current = map.getDisplayBuildNumbers();
            map.setDisplayBuildNumbers(!current);
            this.repaint(); // Call repaint on the Panel
        }
    }

    public void scrollPaneShowRectangle(Rectangle rectangle) {

        if (rectangle == null)
            return;

        JViewport viewport = scrollPane.getViewport();
        log.debug("ScrollPane viewPort ={}", viewport);

        // check dimensions
        log.debug("Map size ={}", map.getSize());
        log.debug("ScrollPane visibleRect ={}", scrollPane.getVisibleRect());
        log.debug("viewport size ={}", viewport.getSize());

        double setX, setY;
        setX = Math.max(0, (rectangle.getCenterX() - viewport.getWidth() / (double) 2));
        setY = Math.max(0, (rectangle.getCenterY() - viewport.getHeight() / (double) 2));

        setX = Math.min(setX, Math.max(0, map.getSize().getWidth() - viewport.getWidth()));
        setY = Math.min(setY, Math.max(0, map.getSize().getHeight() - viewport.getHeight()));

        final Point viewPosition = new Point((int) setX, (int) setY);
        log.debug("ViewPosition for ScrollPane = {}", viewPosition);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getViewport().setViewPosition(viewPosition);
            }
        });
    }

    private void adjustToNewMapZoom() {
        currentMapSize = map.getCurrentSize();
        log.debug("Map.size = {}", currentMapSize);
        layeredPane.setPreferredSize(currentMapSize);
        map.setBounds(0, 0, currentMapSize.width, currentMapSize.height);
        if (mapImage != null) {
            mapImage.setBoundsAndResize(currentMapSize, map.getZoomStep());
        }
        // access from map panel to or panel not nice but currently necessary for route
        // drawing
        if (gameUIManager.getORUIManager() != null && gameUIManager.getORUIManager().getORPanel() != null)
            gameUIManager.getORUIManager().getORPanel().redrawRoutes();
        layeredPane.revalidate();
    }

    public void zoom(boolean in) {
        removeFitToOption();
        map.zoom(in);
        adjustToNewMapZoom();
    }

    /**
     * Zoom-to-fit functionality is based on the discrete zoom steps.
     * In order to achieve correctly fitting zoom, continuous adjustment factors are
     * determined on top of that.
     */
    private void zoomFit(boolean fitToWidth, boolean fitToHeight) {
        if (!fitToWidth && !fitToHeight)
            return;

        if (originalMapSize == null) {
            log.error("zoomFit aborted: originalMapSize is null. The Map failed to initialize.");
            return;
        }

        ImageLoader imageLoader = ImageLoader.getInstance();
        int zoomStep = map.getZoomStep();

        // reset adjustment factor
        imageLoader.resetAdjustmentFactor();

        // determine the available size to fit to
        // (double needed for subsequent calculations)
        double width = getSize().width - ZOOM_FIT_SAFETY_MARGIN;
        double height = getSize().height - ZOOM_FIT_SAFETY_MARGIN;

        double idealFactorWidth = width / originalMapSize.width;
        double idealFactorHeight = height / originalMapSize.height;

        // determine which dimension will be the critical one for the resize
        boolean isWidthCritical = (!fitToHeight
                || (fitToWidth && idealFactorWidth < idealFactorHeight));

        // check whether scrollbar will appear in the fit-to dimension and
        // reduce available size accordingly (not relevant for fit-to-window)
        if (isWidthCritical && idealFactorWidth > idealFactorHeight) {
            width -= scrollPane.getVerticalScrollBar().getPreferredSize().width;
            idealFactorWidth = width / originalMapSize.width;
        }
        if (!isWidthCritical && idealFactorWidth < idealFactorHeight) {
            height -= scrollPane.getHorizontalScrollBar().getPreferredSize().height;
            idealFactorHeight = height / originalMapSize.height;
        }

        // abort resize if no space available
        if (width < 0 || height < 0)
            return;

        // increase zoomFactor until constraints do not hold
        // OR zoom cannot be increased any more
        while (((!fitToWidth || idealFactorWidth > imageLoader.getZoomFactor(zoomStep))
                &&
                (!fitToHeight || idealFactorHeight > imageLoader.getZoomFactor(zoomStep)))
                &&
                imageLoader.getZoomFactor(zoomStep + 1) != imageLoader.getZoomFactor(zoomStep))
            zoomStep++;

        // decrease zoomFactor until constraints do hold
        // OR zoom cannot be decreased any more
        while (((fitToWidth && idealFactorWidth < imageLoader.getZoomFactor(zoomStep))
                ||
                (fitToHeight && idealFactorHeight < imageLoader.getZoomFactor(zoomStep)))
                &&
                imageLoader.getZoomFactor(zoomStep - 1) != imageLoader.getZoomFactor(zoomStep))
            zoomStep--;

        // Determine and apply adjustment factor for precise fit
        double idealFactor = isWidthCritical ? idealFactorWidth : idealFactorHeight;
        imageLoader.setZoomAdjustmentFactor(
                idealFactor / imageLoader.getZoomFactor(zoomStep));

        // trigger zoom execution
        map.setZoomStep(zoomStep);

        adjustToNewMapZoom();
    }

    private void fitToOption(boolean fitToWidth, boolean fitToHeight) {
        // ignore if nothing has changed
        if (this.fitToWidth == fitToWidth && this.fitToHeight == fitToHeight)
            return;

        this.fitToWidth = fitToWidth;
        this.fitToHeight = fitToHeight;
        zoomFit(fitToWidth, fitToHeight);
    }

    public void fitToWindow() {
        fitToOption(true, true);
    }

    public void fitToWidth() {
        fitToOption(true, false);
    }

    public void fitToHeight() {
        fitToOption(false, true);
    }

    public void removeFitToOption() {
        fitToWidth = false;
        fitToHeight = false;
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public HexMap getMap() {
        return map;
    }

    public GUIHex getSelectedHex() {
        return map.getSelectedHex();
    }

    /**
     * Aggressively clears map overlays (build numbers) AND resets highlighting.
     * Iterates ALL hexes to ensure no "ghost" state remains from previous phases.
     */
    public void clearOverlays() {
        if (map != null) {
            // log.info("MapPanel: Aggressive clear invoked. Resetting all hex states and
            // overlays.");

            // 1. Force the global flag to false
            // map.setDisplayBuildNumbers(false);

            // 2. NUCLEAR OPTION: Iterate EVERY hex to scrub state
            // This fixes "Ghost in some cases" where the upgrade list might be stale
            for (GUIHex hex : map.getHexes()) {
                boolean changed = false;

                // Clear Red/Selectable Highlights (Fixes Token Phase issue)
                if (hex.getState() != GUIHex.State.NORMAL) {
                    hex.setState(GUIHex.State.NORMAL);
                    changed = true;
                }

                // Clear Ghost Numbers
                if (hex.getCustomOverlayText() != null) {
                    hex.setCustomOverlayText(null);
                    changed = true;
                }
            }

            // 3. Force a full repaint
            this.repaint();
        }
    }

    private void setupLayersButton(final ORUIManager orUIManager) {
        // 1. Create the Button
        final JButton layersBtn = new JButton("Layers");
        layersBtn.setFocusable(false);
        // Increase font size
        layersBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Solid white background with black text for maximum contrast
        layersBtn.setBackground(Color.BLUE);
        layersBtn.setForeground(Color.WHITE);

        // Force the background to paint correctly across all operating systems
        layersBtn.setOpaque(true);
        layersBtn.setContentAreaFilled(true);

        // Use a raised bevel border to create a "drop shadow" floating effect
        layersBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        // 2. Position it (Top Left, moved slightly further in, made larger)
        layersBtn.setBounds(20, 20, 95, 35);

        

        // 3. Create the "Pop-out" Menu
       layersBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu menu = new JPopupMenu();
                WindowSettings ws = gameUIManager.getWindowSettings();

                // Section 1: ALWAYS OVERLAYS
                JLabel alwaysHeader = new JLabel(" ALWAYS");
                alwaysHeader.setFont(alwaysHeader.getFont().deriveFont(Font.BOLD));
                alwaysHeader.setForeground(Color.GRAY);
                menu.add(alwaysHeader);

                menu.add(new JCheckBoxMenuItem("Terrain Costs", orUIManager.isShowTerrainCosts()))
                        .addActionListener(evTerrain -> {
                            orUIManager.toggleTerrainCosts();
                            ws.setProperty("layer.TerrainCosts", String.valueOf(orUIManager.isShowTerrainCosts()));
                            ws.save();
                        });
                menu.add(new JCheckBoxMenuItem("Destination Markers", orUIManager.isShowDestinationMarkers()))
                        .addActionListener(evDest -> {
                            orUIManager.toggleDestinationMarkers();
                            ws.setProperty("layer.DestinationMarkers", String.valueOf(orUIManager.isShowDestinationMarkers()));
                            ws.save();
                        });
                menu.add(new JCheckBoxMenuItem("Home Identifiers", orUIManager.isShowHomeIdentifiers()))
                        .addActionListener(evHome -> {
                            orUIManager.toggleHomeIdentifiers();
                            ws.setProperty("layer.HomeIdentifiers", String.valueOf(orUIManager.isShowHomeIdentifiers()));
                            ws.save();
                        });

                JCheckBoxMenuItem offboardItem = new JCheckBoxMenuItem("Offboard Values", map.getDisplayOffboardValues());
                offboardItem.addActionListener(evOffboard -> {
                    boolean newState = !map.getDisplayOffboardValues();
                    map.setDisplayOffboardValues(newState);
                    ws.setProperty("layer.OffboardValues", String.valueOf(newState));
                    ws.save();
                    map.repaintAll(new Rectangle(map.getSize()));
                });
                menu.add(offboardItem);

                JCheckBoxMenuItem cityNamesItem = new JCheckBoxMenuItem("City Names", map.getDisplayCityNames());
                cityNamesItem.addActionListener(ae -> {
                    boolean newState = !map.getDisplayCityNames();
                    map.setDisplayCityNames(newState);
                    ws.setProperty("layer.CityNames", String.valueOf(newState));
                    ws.save();
                    map.repaintAll(new Rectangle(map.getSize()));
                });
                menu.add(cityNamesItem);

                menu.addSeparator();

                // Section 2: BUILDING PHASE OVERLAYS
                JLabel buildingHeader = new JLabel(" BUILDING PHASE");
                buildingHeader.setFont(buildingHeader.getFont().deriveFont(Font.BOLD));
                buildingHeader.setForeground(Color.GRAY);
                menu.add(buildingHeader);

                menu.add(new JCheckBoxMenuItem("Hex Names", orUIManager.isShowHexNames()))
                        .addActionListener(evHex -> {
                            orUIManager.toggleHexNames();
                            ws.setProperty("layer.HexNames", String.valueOf(orUIManager.isShowHexNames()));
                            ws.save();
                        });
                menu.add(new JCheckBoxMenuItem("Friendly Hexes", orUIManager.isShowFriendlyHexes()))
                        .addActionListener(evFriendly -> {
                            orUIManager.toggleFriendlyHexes();
                            ws.setProperty("layer.FriendlyHexes", String.valueOf(orUIManager.isShowFriendlyHexes()));
                            ws.save();
                        });

                JCheckBoxMenuItem lastRunsItem = new JCheckBoxMenuItem("Last Revenue Runs", map.getDisplayLastRevenueRuns());
                lastRunsItem.addActionListener(ae -> {
                    boolean newState = !map.getDisplayLastRevenueRuns();
                    map.setDisplayLastRevenueRuns(newState);
                    ws.setProperty("layer.LastRevenueRuns", String.valueOf(newState));
                    ws.save();
                    if (gameUIManager.getORUIManager() != null && gameUIManager.getORUIManager().getORPanel() != null) {
                        gameUIManager.getORUIManager().getORPanel().redrawRoutes();
                    }
                    map.repaintAll(new Rectangle(map.getSize()));
                });
                menu.add(lastRunsItem);

                menu.addSeparator();

                // Section 3: REVENUE PHASE OVERLAYS
                JLabel revenueHeader = new JLabel(" REVENUE PHASE");
                revenueHeader.setFont(revenueHeader.getFont().deriveFont(Font.BOLD));
                revenueHeader.setForeground(Color.GRAY);
                menu.add(revenueHeader);

                menu.add(new JCheckBoxMenuItem("Current Revenue Runs", orUIManager.isShowRevenueRoutes()))
                        .addActionListener(evRoute -> {
                            orUIManager.toggleRevenueRoutes();
                            ws.setProperty("layer.RevenueRoutes", String.valueOf(orUIManager.isShowRevenueRoutes()));
                            ws.save();
                        });

                menu.add(new JCheckBoxMenuItem("Fancy City Values", orUIManager.isShowFancyCityValues()))
                        .addActionListener(evFancy -> {
                            orUIManager.toggleFancyCityValues();
                            ws.setProperty("layer.FancyCityValues", String.valueOf(orUIManager.isShowFancyCityValues()));
                            ws.save();
                        });

                menu.add(new JCheckBoxMenuItem("Show Revenue Spinner", orUIManager.isShowRevenueSpinner()))
                        .addActionListener(evRevSpinner -> {
                            orUIManager.toggleRevenueSpinner();
                            ws.setProperty("layer.RevenueSpinner", String.valueOf(orUIManager.isShowRevenueSpinner()));
                            ws.save();
                        });

                menu.show(layersBtn, 0, layersBtn.getHeight());
            }
        });



        // 4. Add to the Layered Pane at a high level
        layeredPane.add(layersBtn, JLayeredPane.PALETTE_LAYER);
    }


    // Add these helper methods to the class body of MapPanel.java
private void loadLayerSettings() {
    WindowSettings ws = gameUIManager.getWindowSettings();
    if (ws == null) return;

    ORUIManager orui = gameUIManager.getORUIManager();
    
    // Synchronize ORUIManager's state properties using its dynamic toggles
    if (orui != null) {
        if (parseLayerBoolean(ws.getProperty("layer.TerrainCosts"), true) != orui.isShowTerrainCosts()) orui.toggleTerrainCosts();
        if (parseLayerBoolean(ws.getProperty("layer.DestinationMarkers"), true) != orui.isShowDestinationMarkers()) orui.toggleDestinationMarkers();
        if (parseLayerBoolean(ws.getProperty("layer.HomeIdentifiers"), true) != orui.isShowHomeIdentifiers()) orui.toggleHomeIdentifiers();
        if (parseLayerBoolean(ws.getProperty("layer.HexNames"), true) != orui.isShowHexNames()) orui.toggleHexNames();
        if (parseLayerBoolean(ws.getProperty("layer.FriendlyHexes"), true) != orui.isShowFriendlyHexes()) orui.toggleFriendlyHexes();
        if (parseLayerBoolean(ws.getProperty("layer.RevenueRoutes"), true) != orui.isShowRevenueRoutes()) orui.toggleRevenueRoutes();
        if (parseLayerBoolean(ws.getProperty("layer.FancyCityValues"), false) != orui.isShowFancyCityValues()) orui.toggleFancyCityValues();
        if (parseLayerBoolean(ws.getProperty("layer.RevenueSpinner"), true) != orui.isShowRevenueSpinner()) orui.toggleRevenueSpinner();
    }

    // Synchronize direct HexMap properties via their standard public setters
    if (map != null) {
        map.setDisplayOffboardValues(parseLayerBoolean(ws.getProperty("layer.OffboardValues"), map.getDisplayOffboardValues()));
        map.setDisplayCityNames(parseLayerBoolean(ws.getProperty("layer.CityNames"), map.getDisplayCityNames()));
        map.setDisplayLastRevenueRuns(parseLayerBoolean(ws.getProperty("layer.LastRevenueRuns"), map.getDisplayLastRevenueRuns()));
    }
}

private boolean parseLayerBoolean(String val, boolean defaultValue) {
    if (val == null || val.trim().isEmpty()) return defaultValue;
    return "true".equalsIgnoreCase(val.trim()) || "yes".equalsIgnoreCase(val.trim());
}


}

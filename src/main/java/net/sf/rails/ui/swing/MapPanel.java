/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MapPanel.java,v 1.15 2010/06/24 21:48:08 stefanfrey Exp $*/
package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

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
    //defines how many pixels should be left as safety margin when calculating fit zooms
    private static final int zoomFitSafetyMargin = 4;

    private MapManager mmgr;
    private HexMap map;
    private HexMapImage mapImage;
    private JScrollPane scrollPane;

    private GameUIManager gameUIManager;

    private JLayeredPane layeredPane;
    private Dimension originalMapSize;
    private Dimension currentMapSize;

    //active fit-to zoom options
    private boolean fitToWidth = false;
    private boolean fitToHeight = false;

    protected static Logger log =
            LoggerFactory.getLogger(MapPanel.class);

    public MapPanel(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        

        setLayout(new BorderLayout());

        mmgr = gameUIManager.getRoot().getMapManager();
        try {
            map =(HexMap) Class.forName(mmgr.getMapUIClassName()).newInstance();
            map.init(gameUIManager.getORUIManager(), mmgr);
            originalMapSize = map.getOriginalSize();
        } catch (Exception e) {
            log.error("Map class instantiation error:", e);
            e.printStackTrace();
            return;
        }

        //lightwight tooltip possible since tool tip has its own layer in hex map
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);

        //tooltip should not be dismissed after at all
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        layeredPane.setPreferredSize(originalMapSize);
        map.setBounds(0, 0, originalMapSize.width, originalMapSize.height);
        map.addLayers(layeredPane, 1);

        if (mmgr.isMapImageUsed()) {
            mapImage = new HexMapImage ();
            mapImage.init(mmgr,map);
            mapImage.setPreferredSize(originalMapSize);
            mapImage.setBounds(0, 0, originalMapSize.width, originalMapSize.height);
            layeredPane.add(mapImage, -1);
        }

        scrollPane = new JScrollPane(layeredPane);
        scrollPane.setSize(originalMapSize);
        add(scrollPane, BorderLayout.CENTER);

        setSize(originalMapSize);
        setLocation(25, 25);

        //add listener for auto fit upon resize events
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                zoomFit (fitToWidth, fitToHeight);
            }
        });
    }


    public void scrollPaneShowRectangle(Rectangle rectangle) {

        if (rectangle == null) return;

        JViewport viewport = scrollPane.getViewport();
        log.debug("ScrollPane viewPort =" + viewport);

        // check dimensions
        log.debug("Map size =" + map.getSize());
        log.debug("ScrollPane visibleRect =" + scrollPane.getVisibleRect());
        log.debug("viewport size =" + viewport.getSize());

        double setX, setY;
        setX = Math.max(0, (rectangle.getCenterX() - viewport.getWidth() / 2));
        setY = Math.max(0, (rectangle.getCenterY() - viewport.getHeight() / 2));

        setX = Math.min(setX, Math.max(0, map.getSize().getWidth() -  viewport.getWidth()));
        setY = Math.min(setY, Math.max(0, map.getSize().getHeight() - viewport.getHeight()));

        final Point viewPosition = new Point((int)setX, (int)setY);
        log.debug("ViewPosition for ScrollPane = " + viewPosition);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPane.getViewport().setViewPosition(viewPosition);
            }
        });
    }

    private void adjustToNewMapZoom () {
        currentMapSize = map.getCurrentSize();
        log.debug("Map.size = " +currentMapSize);
        layeredPane.setPreferredSize(currentMapSize);
        map.setBounds(0, 0, currentMapSize.width, currentMapSize.height);
        if (mapImage != null) {
            mapImage.setBoundsAndResize(currentMapSize,map.getZoomStep());
        }
        //access from map panel to or panel not nice but currently necessary for route drawing
        if (gameUIManager.getORUIManager() != null && gameUIManager.getORUIManager().getORPanel() != null)
            gameUIManager.getORUIManager().getORPanel().redrawRoutes();
        layeredPane.revalidate();
    }

    public void zoom (boolean in) {
        removeFitToOption();
        map.zoom(in);
        adjustToNewMapZoom();
    }

    /**
     * Zoom-to-fit functionality is based on the discrete zoom steps.
     * In order to achieve correctly fitting zoom, continuous adjustment factors are
     * determined on top of that.
     */
    private void zoomFit (boolean fitToWidth, boolean fitToHeight) {
        if (!fitToWidth && !fitToHeight) return;

        ImageLoader imageLoader = GameUIManager.getImageLoader();
        int zoomStep = map.getZoomStep();

        //reset adjustment factor
        imageLoader.resetAdjustmentFactor();

        //determine the available size to fit to
        //(double needed for subsequent calculations)
        double width = getSize().width - zoomFitSafetyMargin;
        double height = getSize().height - zoomFitSafetyMargin;

        double idealFactorWidth = width / originalMapSize.width;
        double idealFactorHeight = height / originalMapSize.height;

        //determine which dimension will be the critical one for the resize
        boolean isWidthCritical = ( !fitToHeight
                || (fitToWidth && idealFactorWidth < idealFactorHeight));

        //check whether scrollbar will appear in the fit-to dimension and
        //reduce available size accordingly (not relevant for fit-to-window)
        if (isWidthCritical && idealFactorWidth > idealFactorHeight) {
            width -= scrollPane.getVerticalScrollBar().getPreferredSize().width;
            idealFactorWidth = width / originalMapSize.width;
        }
        if (!isWidthCritical && idealFactorWidth < idealFactorHeight) {
            height -= scrollPane.getHorizontalScrollBar().getPreferredSize().height;
            idealFactorHeight = height / originalMapSize.height;
        }

        //abort resize if no space available
        if (width < 0 || height < 0) return;

        //increase zoomFactor until constraints do not hold
        //OR zoom cannot be increased any more
        while
            (
                    (
                            (!fitToWidth || idealFactorWidth > imageLoader.getZoomFactor(zoomStep))
                            &&
                            (!fitToHeight || idealFactorHeight > imageLoader.getZoomFactor(zoomStep))
                    )
                    &&
                    imageLoader.getZoomFactor(zoomStep+1) != imageLoader.getZoomFactor(zoomStep)
            )
            zoomStep++;

        //decrease zoomFactor until constraints do hold
        //OR zoom cannot be decreased any more
        while
            (
                    (
                            (fitToWidth && idealFactorWidth < imageLoader.getZoomFactor(zoomStep))
                            ||
                            (fitToHeight && idealFactorHeight < imageLoader.getZoomFactor(zoomStep))
                    )
                    &&
                    imageLoader.getZoomFactor(zoomStep-1) != imageLoader.getZoomFactor(zoomStep)
            )
            zoomStep--;

        //Determine and apply adjustment factor for precise fit
        double idealFactor = isWidthCritical ? idealFactorWidth : idealFactorHeight;
        imageLoader.setZoomAdjustmentFactor (
                idealFactor / imageLoader.getZoomFactor(zoomStep));

        //trigger zoom execution
        map.setZoomStep(zoomStep);

        adjustToNewMapZoom();
    }

    private void fitToOption (boolean fitToWidth, boolean fitToHeight) {
        //ignore if nothing has changed
        if (this.fitToWidth == fitToWidth && this.fitToHeight == fitToHeight ) return;

        this.fitToWidth = fitToWidth;
        this.fitToHeight = fitToHeight;
        zoomFit(fitToWidth, fitToHeight);
    }

    public void fitToWindow () {
        fitToOption (true, true);
    }

    public void fitToWidth () {
        fitToOption (true, false);
    }

    public void fitToHeight () {
        fitToOption (false, true);
    }

    public void removeFitToOption () {
        fitToWidth = false;
        fitToHeight = false;
    }

    public void keyPressed(KeyEvent e) {}

    public void keyReleased(KeyEvent e) {}

    public HexMap getMap() {
        return map;
    }
    
    public GUIHex getSelectedHex() {
        return map.getSelectedHex();
    }
}

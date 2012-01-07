/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MapPanel.java,v 1.15 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.MapManager;
import rails.game.action.LayTile;
import rails.game.action.LayToken;
import rails.ui.swing.hexmap.*;

/**
 * MapWindow class displays the Map Window. It's shocking, I know.
 */
public class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private MapManager mmgr;
    private HexMap map;
    private HexMapImage mapImage;
    private JScrollPane scrollPane;
   
    private GameUIManager gameUIManager;
    
    private JLayeredPane layeredPane;
    private Dimension originalMapSize;
    private Dimension currentMapSize;

    protected static Logger log =
            LoggerFactory.getLogger(MapPanel.class);

    public MapPanel(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        //Scale.set(15);
        Scale.set(16);
        
        setLayout(new BorderLayout());

        mmgr = gameUIManager.getGameManager().getMapManager();
        try {
            map =(HexMap) Class.forName(mmgr.getMapUIClassName()).newInstance();
            map.init(gameUIManager.getORUIManager(), mmgr);
            originalMapSize = map.getOriginalSize();
        } catch (Exception e) {
            log.error("Map class instantiation error:", e);
            e.printStackTrace();
            return;
        }

        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        layeredPane.setPreferredSize(originalMapSize);
        map.setBounds(0, 0, originalMapSize.width, originalMapSize.height);
        map.addLayers(layeredPane, 1);
        
        if (mmgr.isMapImageUsed()) {
            mapImage = new HexMapImage ();
            mapImage.init(mmgr);
            mapImage.setPreferredSize(originalMapSize);
            mapImage.setBounds(0, 0, originalMapSize.width, originalMapSize.height);
            layeredPane.add(mapImage, -1);
        }
        
        scrollPane = new JScrollPane(layeredPane);
        scrollPane.setSize(originalMapSize);
        add(scrollPane, BorderLayout.CENTER);
        
        setSize(originalMapSize);
        setLocation(25, 25);
        
    }

    
    public void scrollPaneShowRectangle(Rectangle rectangle) {
        
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
    
    public void setAllowedTileLays(List<LayTile> allowedTileLays) {
        map.setAllowedTileLays(allowedTileLays);
    }

    public <T extends LayToken> void setAllowedTokenLays(
            List<T> allowedTokenLays) {
        map.setAllowedTokenLays(allowedTokenLays);
    }

    private void adjustToNewMapZoom () {
        currentMapSize = map.getCurrentSize();
        log.debug("Map.size = " +currentMapSize);
        layeredPane.setPreferredSize(currentMapSize);
        map.setBounds(0, 0, currentMapSize.width, currentMapSize.height);
        if (mapImage != null) {
            mapImage.setBounds(0, 0, currentMapSize.width, currentMapSize.height);
            mapImage.setPreferredSize(currentMapSize);
            mapImage.zoom(map.getZoomStep());
            // FIXME setBounds() seems to be sufficient to resize a JSVGCanvas, but it doesn't always work...
        }
        layeredPane.revalidate();
    }
    
    public void zoom (boolean in) {
        map.zoom(in);
        adjustToNewMapZoom();
    }
    
    private void zoomFit (boolean fitWidth, boolean fitHeight) {
        map.zoomFit (getSize(), fitWidth, fitHeight);
        adjustToNewMapZoom();
    }
    
    public void fitToWindow () {
        zoomFit (true, true);
    }
    
    public void fitToWidth () {
        zoomFit (true, false);
    }
    
    public void fitToHeight () {
        zoomFit (false, true);
    }
    
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameUIManager.getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public HexMap getMap() {
        return map;
    }
}

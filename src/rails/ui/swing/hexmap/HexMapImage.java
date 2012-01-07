package rails.ui.swing.hexmap;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.parser.Config;
import rails.game.MapManager;
import rails.ui.swing.GameUIManager;
import rails.util.Util;

/**
 * Class to display a full map image. This class has been split off from HexMap to allow
 * it to be displayed in a lower layer of a LayeredPane.
 */
public final class HexMapImage extends JSVGCanvas  {

    // TODO: Is this still compatible
    private static final long serialVersionUID = 1L;

    protected static Logger log =
            LoggerFactory.getLogger(HexMapImage.class);

    private MapManager mapManager;
    private HexMap hexMap;
    private double zoomFactor = 1;  // defined dynamically if zoomStep changed
    private int zoomStep = 10; // default value, can be overwritten in config
    private boolean initialized = false;

    public void init(MapManager mapManager,HexMap hexMap) {

       this.mapManager = mapManager;
       this.hexMap = hexMap;
       
       this.setRecenterOnResize(false);

       initializeSettings();
       loadMap();
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
    
    private void loadMap() {
        
        try {
//             File imageFile = new File (mapManager.getMapImageFilepath());
//             setURI(imageFile.toURI().toString());
//             log.debug("ImageFile="+ imageFile.getName());
            setURI(getClass().getResource(mapManager.getMapImageFilepath()).toString());
        } catch (Exception e) {
            log.error ("Cannot load map image file " + mapManager.getMapImageFilepath(), e);
        }
        
        addGVTTreeRendererListener (new GVTTreeRendererAdapter() {
            //prepare: map scaling has to occur before displaying it for the first time
            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {
                if (!initialized) {
                    // store the rendering Transform
                    initialTransform = getRenderingTransform();
                    scaleMap();
                    initialized = true;
                }
                addGVTTreeRendererListener(null);
            }
        });
        
    }
    
    private void scaleMap () {
        AffineTransform at = new AffineTransform();

        log.debug("MapImage zoomFactor" + zoomFactor);
        at.scale (zoomFactor, zoomFactor);
        
        log.debug("MapImage XOffset = " + mapManager.getMapXOffset() + ", YOffset = " + mapManager.getMapYOffset());
        at.translate(mapManager.getMapXOffset(), mapManager.getMapYOffset());

        double mapScale = mapManager.getMapScale();
        log.debug("MapImage MapScale = " + mapManager.getMapScale());
        at.scale(mapScale, mapScale);

        log.debug("MapImage Affine Transform " + at);
        setRenderingTransform (at, true);
    }

    public void zoom (boolean in) {
        if (in) zoomStep++; else zoomStep--;
        zoom();
    }
    
    public void zoom (int zoomStep) {
        this.zoomStep = zoomStep;
        zoom();
    }
    
    private void zoom() {
        zoomFactor = GameUIManager.getImageLoader().getZoomFactor(zoomStep);
        log.debug("ImageMap zoomStep = " + zoomStep);
        log.debug("ImageMap zoomFactor = " + zoomFactor);
        scaleMap();
    }

    public int getZoomStep () {
        return zoomStep;
    }
    
    /**
     * paint component synchronized with hex map in order to ensure that
     * - painting background image is not affected by concurrent changes in the hexmap
     *   (such glitches were observed in the past)
     */
    @Override
    public void paintComponent(Graphics g) {
        synchronized (hexMap) {
            super.paintComponent(g);
        }
    }
    
}

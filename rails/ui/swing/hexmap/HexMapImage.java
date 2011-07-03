/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/HexMapImage2/HexMapImage2.java,v 1.27 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing.hexmap;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.*;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.log4j.Logger;

import rails.common.parser.Config;
import rails.game.*;
import rails.ui.swing.*;
import rails.util.Util;

/**
 * Class to display a full map image. This class has been split off from HexMap to allow
 * it to be displayed in a lower layer of a LayeredPane.
 */
public class HexMapImage extends JSVGCanvas  {

    protected static Logger log =
            Logger.getLogger(HexMapImage.class.getPackage().getName());

    protected MapManager mapManager;

    protected int scale;
    protected int zoomStep = 10; // can be overwritten in config
    protected double zoomFactor = 1;  // defined dynamically if zoomStep changed

    protected String mapImageFilepath = null;
    protected int mapXOffset;
    protected int mapYOffset;
    protected double mapScale;
    protected boolean displayMap = false;
    
    private boolean initialized = false;

    public void init(MapManager mapManager) {

       this.mapManager = mapManager;
        
        if (mapManager.isMapImageUsed()) {
            mapImageFilepath = mapManager.getMapImageFilepath();
            displayMap = true;
            mapXOffset = mapManager.getMapXOffset();
            mapYOffset = mapManager.getMapYOffset();
            mapScale = mapManager.getMapScale();
            log.debug("ImageFile="+mapImageFilepath+" X="+mapXOffset+" Y="+mapYOffset+" scale="+mapScale);
        }


        setScale();

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
             File imageFile = new File (mapImageFilepath);
             setURI (imageFile.toURL().toString());
        } catch (Exception e) {
            log.error ("Cannot load map image file "+mapImageFilepath, e);
            
        }
        
        addGVTTreeRendererListener (new GVTTreeRendererAdapter() {
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                if (!initialized) {
                    initScaleMap();
                    initialized = true;
                }
            }
        });
        
    }
    
    private void initScaleMap () {
        
        AffineTransform at1 = getRenderingTransform();
        AffineTransform at2 = new AffineTransform();
        double currentScale = mapScale * zoomFactor;
        at2.scale (currentScale, currentScale);
        at2.translate(mapXOffset, mapYOffset);
        at2.concatenate(at1);
        setRenderingTransform (at2);
    }

    private void scaleMap () {
        
        AffineTransform at = getRenderingTransform();
        double currentScale = mapScale * zoomFactor;
        at.scale (currentScale/at.getScaleX(), currentScale/at.getScaleY());
        at.translate(mapXOffset * zoomFactor * 0.7 - at.getTranslateX(), mapYOffset * zoomFactor * 0.7 - at.getTranslateY());
        setRenderingTransform (at);
    }

    public void zoom (boolean in) {
        if (in) zoomStep++; else zoomStep--;
        zoom();
    }
    
    private void zoom() {
        zoomFactor = GameUIManager.getImageLoader().getZoomFactor(zoomStep);
        scaleMap();
    }

    protected void setScale() {
        scale = (int)(Scale.get() * zoomFactor);
    }

    public int getZoomStep () {
        return zoomStep;
    }

	public void mouseClicked(MouseEvent arg0) {
        Point point = arg0.getPoint();
        //GUIHex clickedHex = getHexContainingPoint(point);

        //orUIManager.hexClicked(clickedHex, selectedHex);
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
        //GUIHex hex = getHexContainingPoint(point);
        //setToolTipText(hex != null ? hex.getToolTip() : "");
    }

    public void mouseEntered(MouseEvent arg0) {}

    public void mouseExited(MouseEvent arg0) {}

    public void mousePressed(MouseEvent arg0) {}

    public void mouseReleased(MouseEvent arg0) {}

}

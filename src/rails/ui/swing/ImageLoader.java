/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ImageLoader.java,v 1.16 2010/01/31 22:22:34 macfreek Exp $*/
package rails.ui.swing;

import java.awt.image.BufferedImage;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import rails.common.Config;
import rails.common.ResourceLoader;
import rails.ui.swing.hexmap.GUIHex;
import rails.util.*;

/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader {

    private static Map<Integer, Map<Integer, BufferedImage>> tileMap;

    private static Map<Integer, Document> svgMap;
    private static double[] zoomFactors = new double[21];
    //defines adjustment of zoom factor (should be close to 1) 
    //(used for perfect-fit sizing that requires arbitrary zoom)
    private static double zoomAdjustmentFactor = 1;

    private static double svgWidth = 75;
    private static double svgHeight = svgWidth * 0.5 * Math.sqrt(3.0);
    private static String svgTileDir = "tiles/svg";
    private static String tileRootDir = Config.get("tile.root_directory");
    private static String directory;
    
    private final DocumentBuilder svgDocBuilder; 

    static {
        GUIHex.setScale(1.0);
    }

    private static Logger log =
            LoggerFactory.getLogger(ImageLoader.class);

    static {
        if (Util.hasValue(tileRootDir) && !tileRootDir.endsWith("/")) {
            tileRootDir += "/";
        }
    }

    /* cheat, using batik transcoder API. we only want the Image */
    private static class BufferedImageTranscoder extends ImageTranscoder {

        private BufferedImage image;

        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage image, TranscoderOutput output)
                throws TranscoderException {
            this.image = image;
        }

        public BufferedImage getImage() {
            return image;
        }
    }

    private BufferedImage getSVGTile(int tileID, double zoomFactor) {
        String fn = "tile" + Integer.toString(tileID) + ".svg";

        BufferedImage image = null;

        try {
            // Experimental new version, that stacks the XML to allow zooming
            if (svgMap == null) {
                svgMap = new HashMap<Integer, Document>(64);
            }
            if (!svgMap.containsKey(tileID)) {
                 Document doc = null;

                // Step 3: parse the input file to get a Document object
                doc = 
                        svgDocBuilder.parse(ResourceLoader.getInputStream(fn,
                                directory));
                // Cache the doc
                svgMap.put(tileID, doc);
                log.debug("SVG document for tile id " + tileID + " succeeded ");
            }
            BufferedImageTranscoder t = new BufferedImageTranscoder();
            t.addTranscodingHint(ImageTranscoder.KEY_MAX_WIDTH, new Float(svgWidth * zoomFactor));
            t.addTranscodingHint(ImageTranscoder.KEY_MAX_HEIGHT, new Float(svgHeight * zoomFactor));
            TranscoderInput input = new TranscoderInput(svgMap.get(tileID));
            t.transcode(input, null);
            image = t.getImage();
            log.debug("SVG transcoding for tile id " + tileID + " and zoomFactor " + zoomFactor + " succeeded ");

        } catch (Exception e) {
            log.error("SVG transcoding for tile id " + tileID + " failed with "
                      + e);
            return null;
        }

        return image;
    }

    public BufferedImage getTile(int tileID, int zoomStep) {

        if (tileMap == null) {
            tileMap = new HashMap<Integer, Map<Integer, BufferedImage>>(64);
        }
        if (!tileMap.containsKey(tileID)) {
            tileMap.put(tileID, new HashMap<Integer, BufferedImage>(4));
        }
        if (!tileMap.get(tileID).containsKey(zoomStep)) {
            BufferedImage image = getSVGTile(tileID, getZoomFactor(zoomStep));
            tileMap.get(tileID).put(zoomStep, image);
        }

        return tileMap.get(tileID).get(zoomStep);
    }

    public double getZoomFactor (int zoomStep) {
        if (zoomStep < 0) zoomStep = 0;
        else if (zoomStep > 20) zoomStep = 20;
        if (zoomFactors[zoomStep] == 0.0) {
            zoomFactors[zoomStep] = zoomAdjustmentFactor * Math.pow(2.0, 0.25*(zoomStep-10));
        }
        return zoomFactors[zoomStep];

    }
    
    /**
     * @param zoomAdjustmentFactor Additional factor applied to zoom factor. Used
     * for precisely adjusting zoom-step based zoom factors for perfect fit requirements.  
     */
    public void setZoomAdjustmentFactor (double zoomAdjustmentFactor) {
        ImageLoader.zoomAdjustmentFactor = zoomAdjustmentFactor;
        
        //invalidate buffered zoom step zoom factors
        for (int i = 0 ; i < zoomFactors.length ; i++) {
            zoomFactors[i] = 0;
        }
        
        //invalidate buffered tile scalings
        if (tileMap != null) tileMap.clear();
    }
    
    public void resetAdjustmentFactor() {
        setZoomAdjustmentFactor(1);
    }

    public ImageLoader() {
        directory = (tileRootDir + svgTileDir);

        // Step 1: create a DocumentBuilderFactory and setNamespaceAware
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // Step 2: create a DocumentBuilder
        DocumentBuilder db = null;
        try{
            db = dbf.newDocumentBuilder(); }
        catch (ParserConfigurationException e) {
            // do nothing
        }
        svgDocBuilder = db;
    }

}

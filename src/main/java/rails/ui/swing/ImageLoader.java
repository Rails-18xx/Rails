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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;

import rails.common.Config;
import rails.common.ResourceLoader;
import rails.util.*;

/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader {

    private static final Logger log =
            LoggerFactory.getLogger(ImageLoader.class);

    private final DocumentBuilder svgDocBuilder; 

    private final Map<String, Document> svgMap = Maps.newHashMap();
    private final HashBasedTable<String, Integer, BufferedImage> tileImages = 
            HashBasedTable.create();

    private double[] zoomFactors = new double[21];

    //defines adjustment of zoom factor (should be close to 1) 
    //(used for perfect-fit sizing that requires arbitrary zoom)
    private double zoomAdjustmentFactor = 1;

    private double svgWidth = 75;
    private double svgHeight = svgWidth * 0.5 * Math.sqrt(3.0);
    
    private String svgTileDir = "tiles/svg";
    private String tileRootDir = Config.get("tile.root_directory");
    private String directory;


    public ImageLoader() {
        if (Util.hasValue(tileRootDir) && !tileRootDir.endsWith("/")) {
            tileRootDir += "/";
        }
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

    private BufferedImage getSVGTile(String tileID, double zoomFactor) {
        String fn = "tile" + tileID + ".svg";

        BufferedImage image = null;

        try {
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

    public BufferedImage getTile(String tileID, int zoomStep) {
        if (tileImages.contains(tileID, zoomStep)) {
            return tileImages.get(tileID, zoomStep);
        } else {
            BufferedImage image = getSVGTile(tileID, getZoomFactor(zoomStep));
            tileImages.put(tileID, zoomStep, image);
            return image;
        }
    }

    public double getZoomFactor (int zoomStep) {
        if (zoomStep < 0) zoomStep = 0;
        else if (zoomStep > 20) zoomStep = 20;
        if (zoomFactors[zoomStep] == 0.0) {
            zoomFactors[zoomStep] = zoomAdjustmentFactor * Math.pow(2.0, 0.25*(zoomStep-10));
        }
        return zoomFactors[zoomStep]* GUIGlobals.getMapScale();

    }
    
    /**
     * @param zoomAdjustmentFactor Additional factor applied to zoom factor. Used
     * for precisely adjusting zoom-step based zoom factors for perfect fit requirements.  
     */
    public void setZoomAdjustmentFactor (double zoomAdjustmentFactor) {
        this.zoomAdjustmentFactor = zoomAdjustmentFactor;
        
        //invalidate buffered zoom step zoom factors
        for (int i = 0 ; i < zoomFactors.length ; i++) {
            zoomFactors[i] = 0;
        }
        
        //invalidate buffered tile scalings
        tileImages.clear();
    }
    
    public void resetAdjustmentFactor() {
        setZoomAdjustmentFactor(1);
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


}

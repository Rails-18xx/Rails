/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ImageLoader.java,v 1.14 2009/12/18 20:04:32 evos Exp $*/
package rails.ui.swing;

import java.awt.image.BufferedImage;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

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
    //private static Map<Integer, String> tileTypes = new HashMap<Integer, String>(64);

    //private static int svgWidth = 60;
    //private static int svgHeight = 55;
    private static double svgWidth = 75;
    private static double svgHeight = svgWidth * 0.5 * Math.sqrt(3.0);
    private static String svgTileDir = "tiles/svg";
    //private static String gifTileDir = "tiles/images";
    private static String tileRootDir = Config.get("tile.root_directory");
    //private static String preference = Config.get("tile.format_preference");
    private static List<String> directories = new ArrayList<String>();

    static {
        //GUIHex.setScale(preference.equalsIgnoreCase("svg") ? 1.0 : 0.33);
        //GUIHex.setScale(preference.equalsIgnoreCase("svg") ? 1.0 : 0.163);
        GUIHex.setScale(1.0);
    }

    private static Logger log =
            Logger.getLogger(ImageLoader.class.getPackage().getName());

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

    /*
    private BufferedImage loadTile(int tileID, double zoomFactor) {
        BufferedImage image = null;

        //String tileType;
        if (preference.equalsIgnoreCase("gif")) {

            image = getGIFTile(tileID);
            if (image == null) {
                // If loading the GIF fails, try loading the SVG.
                log.warn("Attempting to load SVG version of tile " + tileID);
                image = getSVGTile(tileID, zoomFactor);
                //tileType = "svg";
            } else {
            	tileType = "gif";
            }

        } else {

            image = getSVGTile(tileID, zoomFactor);
            if (image == null) {
                // If loading the SVG fails, try loading the GIF.
                log.warn("Attempting to load GIF version of tile " + tileID);
                image = getGIFTile(tileID);
                tileType = "gif";
            } else {
            	tileType = "svg";
            }
        }
        //tileTypes.put(tileID, tileType);
        return image;
    }*/

    private BufferedImage getSVGTile(int tileID, double zoomFactor) {
        String fn = "tile" + Integer.toString(tileID) + ".svg";
        //log.debug("Loading SVG tile " + fn);

        BufferedImage image = null;

        try {
        	/*
            InputStream stream = ResourceLoader.getInputStream(fn, directories);
            if (stream != null) {
                BufferedImageTranscoder t = new BufferedImageTranscoder();
                t.addTranscodingHint(ImageTranscoder.KEY_WIDTH, new Float(
                        svgWidth));
                t.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, new Float(
                        svgHeight));
                TranscoderInput input = new TranscoderInput(stream);
                t.transcode(input, null);
                image = t.getImage();
            }
            */
        	// Experimental new version, that stacks the XML to allow zooming
        	if (svgMap == null) {
        		svgMap = new HashMap<Integer, Document>(64);
        	}
        	if (!svgMap.containsKey(tileID)) {
 	            Document doc = null;
	            // Step 1: create a DocumentBuilderFactory and setNamespaceAware
	            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            dbf.setNamespaceAware(true);
	            // Step 2: create a DocumentBuilder
	            DocumentBuilder db = dbf.newDocumentBuilder();

	            // Step 3: parse the input file to get a Document object
	            doc =
	                    db.parse(ResourceLoader.getInputStream(fn,
	                            directories));
	            // Cache the doc
	            svgMap.put(tileID, doc);
        	}
            BufferedImageTranscoder t = new BufferedImageTranscoder();
            t.addTranscodingHint(ImageTranscoder.KEY_MAX_WIDTH, new Float(svgWidth * zoomFactor));
            t.addTranscodingHint(ImageTranscoder.KEY_MAX_HEIGHT, new Float(svgHeight * zoomFactor));
            TranscoderInput input = new TranscoderInput(svgMap.get(tileID));
            t.transcode(input, null);
            image = t.getImage();

        } catch (Exception e) {
            log.error("SVG transcoding for tile id " + tileID + " failed with "
                      + e);
            return null;
        }

        return image;
    }

    /*
    private BufferedImage getGIFTile(int tileID) {
        String fn = "tile" + Integer.toString(tileID) + ".gif";
        //log.debug("Loading tile " + fn);

        BufferedImage image = null;

        try {

            InputStream str = ResourceLoader.getInputStream(fn, directories);
            if (str != null) {
                image = ImageIO.read(str);
            }
        } catch (Exception e) {
            log.error("Error loading file: " + fn + "\nLoad failed with " + e);
            return null;
        }
        return image;
    }
    */

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
    		zoomFactors[zoomStep] = 1.0 * Math.pow(2.0, 0.25*(zoomStep-10));
    	}
    	return zoomFactors[zoomStep];

    }

    //public String getTileType (int tileID) {
    //	return tileTypes.get(tileID);
    //}

    public ImageLoader() {
        directories.add(tileRootDir + svgTileDir);
        //directories.add(tileRootDir + gifTileDir);
        directories.add(tileRootDir);
    }

}

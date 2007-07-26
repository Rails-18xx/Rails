package rails.ui.swing;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import rails.ui.swing.hexmap.GUIHex;
import rails.util.*;

import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;

import org.apache.log4j.*;

/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader {

	private static HashMap<String, BufferedImage> tileMap;

	private static int svgWidth = 60;
	private static int svgHeight = 55;
	private static String svgTileDir = "tiles/svg/";
	private static String gifTileDir = "tiles/images/";
	private static String tileRootDir = Config.get("tile.root_directory");
	private static String preference = Config.get("tile.format_preference");

	static {
		GUIHex.setScale(preference.equalsIgnoreCase("svg") ? 1.0 : 0.33);
	}

	private static Logger log = Logger.getLogger(ImageLoader.class.getPackage()
			.getName());

	static {
		if (Util.hasValue(tileRootDir) && !tileRootDir.endsWith("/")) {
			tileRootDir += "/";
		}
	}

	/* cheat, using batik transcoder API. we only want the Image */
	private static class BufferedImageTranscoder extends ImageTranscoder {

		private BufferedImage image;

		public BufferedImage createImage(int width, int height) {
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}

		public void writeImage(BufferedImage image, TranscoderOutput output)
				throws TranscoderException {
			this.image = image;
		}

		public BufferedImage getImage() {
			return image;
		}
	}

	private void loadTile(int tileID) {
		BufferedImage image = null;

		if (preference.equalsIgnoreCase("gif")) {

			image = getGIFTile(tileID);
			if (image == null) {
				// If loading the GIF fails, try loading the SVG.
				log.warn("Attempting to load SVG version of tile " + tileID);
				image = getSVGTile(tileID);
			}

		} else {

			image = getSVGTile(tileID);
			if (image == null) {
				// If loading the SVG fails, try loading the GIF.
				log.warn("Attempting to load GIF version of tile " + tileID);
				image = getGIFTile(tileID);
			}
		}

		/* Image will be stored, even if null, to prevent further searches. */
		tileMap.put(Integer.toString(tileID), image);
	}

	private BufferedImage getSVGTile(int tileID) {
		String fn = "tile" + Integer.toString(tileID) + ".svg";
		log.debug("Loading tile " + fn);

		BufferedImage image = null;

		try {
			InputStream stream = Util.getStreamForFile(tileRootDir + svgTileDir
					+ fn);
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
		} catch (FileNotFoundException e) {
			log.warn("SVG Tile ID: " + tileID + " not found.");
			return null;
		} catch (Exception e) {
			log.error("SVG transcoding for tile id " + tileID + " failed with "
					+ e);
			return null;
		}

		return image;
	}

	private BufferedImage getGIFTile(int tileID) {
		String fn = "tile" + Integer.toString(tileID) + ".gif";
		log.debug("Loading tile " + fn);

		BufferedImage image = null;

		try {
			InputStream str = Util.getStreamForFile(tileRootDir + gifTileDir
					+ fn);
			if (str != null) {
				image = ImageIO.read(str);
			}
		} catch (FileNotFoundException e) {
			log.warn("GIF Tile ID: " + tileID + " not found.");
			return null;
		} catch (Exception e) {
			log.error("Error loading file: " + fn + "\nLoad failed with " + e);
			return null;
		}
		return image;
	}

	public BufferedImage getTile(int tileID) {
		// Check for cached copy before loading from disk.
		if (!tileMap.containsKey(Integer.toString(tileID)))
			loadTile(tileID);

		return (BufferedImage) tileMap.get(Integer.toString(tileID));
	}

	public ImageLoader() {
		tileMap = new HashMap<String, BufferedImage>();
	}

}

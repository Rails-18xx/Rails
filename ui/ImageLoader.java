package ui;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import util.*;

import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
//import org.apache.batik.swing.JSVGCanvas;

import org.apache.log4j.*;

/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader
{

	private static HashMap tileMap;
	//private static HashMap canvasMap;
	private static int width = 180;
	private static int height = 167;
	private static String svgTileDir = "tiles/svg/";
	private static String gifTileDir = "tiles/images/";
	
	private static String preference = Config.get("tile.format_preference");
	private static Logger log = Logger.getLogger(ImageLoader.class.getPackage().getName());

	/* cheat, using batik transcoder API. we only want the Image */
	private static class BufferedImageTranscoder extends ImageTranscoder
	{

		private BufferedImage image;

		public BufferedImage createImage(int width, int height)
		{
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}

		public void writeImage(BufferedImage image, TranscoderOutput output)
				throws TranscoderException
		{
			this.image = image;
		}

		public BufferedImage getImage()
		{
			return image;
		}
	}

	private void loadTile(int tileID)
	{
		BufferedImage image = null;
		
		if (preference.equalsIgnoreCase("gif")) {

			image = getGIFTile(tileID);
			if (image == null)
			{
				//If loading the GIF fails, try loading the SVG. 
				log.warn("Attempting to load SVG version of tile "+tileID);
				image = getSVGTile(tileID);
			}

		} else {
			
			image = getSVGTile(tileID);
			if (image == null)
			{
				//If loading the SVG fails, try loading the GIF. 
				log.warn("Attempting to load GIF version of tile "+tileID);
				image = getGIFTile(tileID);
			}
		}
		
		/* Image will be stored, even if null, to prevent further searches. */
		tileMap.put(Integer.toString(tileID), image);
	}

	/* Redundant method removed (EV 12jan2007)
	private URL buildURL(String filename, String dir)
	{
		URL url = null;

		try
		{
			url = new java.net.URL("file:" + dir + filename);
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}

		return url;
	}
	*/

	private BufferedImage getSVGTile(int tileID)
	{
		//String fn = "tile" + tileID + ".svg";
		String fn = "tile" + Integer.toString(tileID) + ".svg";
		log.debug("Loading tile "+fn);
		//URL tileURL = buildURL(fn, svgTileDir);

		BufferedImage image = null;

		try
		{

			// url will not be null even is the file doesn't exist,
			// so we need to check if connection can be opened
			//if (tileURL != null)
			//{
				//InputStream stream = tileURL.openStream();
				//if (tileURL.openStream() != null)
				InputStream stream = Util.getStreamForFile(svgTileDir + fn);
				if (stream != null)
				{
					BufferedImageTranscoder t = new BufferedImageTranscoder();
					t.addTranscodingHint(ImageTranscoder.KEY_WIDTH,
							new Float(width));
					t.addTranscodingHint(ImageTranscoder.KEY_HEIGHT,
							new Float(height));
					TranscoderInput input = new TranscoderInput(stream);
					t.transcode(input, null);
					image = t.getImage();
				}
			//}
		}
		catch (FileNotFoundException e)
		{
			log.warn("SVG Tile ID: " + tileID	+ " not found.");
			return null;
		}
		catch (Exception e)
		{
		    log.error("SVG transcoding for tile id " + tileID + " failed with " + e);
		    return null;
		}
		return image;
	}

	private BufferedImage getGIFTile(int tileID)
	{
		String fn = "tile" + Integer.toString(tileID) + ".gif";
		//URL tileURL = buildURL(fn, gifTileDir);
		log.debug("Loading tile "+fn);

		BufferedImage image = null;

		try
		{
			InputStream str = Util.getStreamForFile(gifTileDir + fn);
			if (str != null)
			{
				image = ImageIO.read(str);
			}
		}
		catch (FileNotFoundException e)
		{
			log.warn("GIF Tile ID: " + tileID	+ " not found.");
			return null;
		}
		catch (Exception e)
		{
			log.error("Error loading file: " + fn + "\nLoad failed with "
					+ e);
			return null;
		}
		return image;
	}

	/* Redundant method removed (EV 12jan2007)
	public JSVGCanvas getSVGCanvas(int tileID)
	{
		if (!canvasMap.containsKey(Integer.toString(tileID)))
		{
			String fn = "tile" + tileID + ".svg";
			URL url = buildURL(fn, svgTileDir);
			JSVGCanvas canvas = new JSVGCanvas();
			canvas.setURI(url.toString());

			canvasMap.put(Integer.toString(tileID), canvas);
			return canvas;
		}
		else
			return (JSVGCanvas) canvasMap.get(Integer.toString(tileID));
	}
	*/

	public BufferedImage getTile(int tileID)
	{
		// Check for cached copy before loading from disk.
		if (!tileMap.containsKey(Integer.toString(tileID)))
			loadTile(tileID);

		return (BufferedImage) tileMap.get(Integer.toString(tileID));
	}

	public ImageLoader()
	{
		tileMap = new HashMap();
		//canvasMap = new HashMap();
	}

}

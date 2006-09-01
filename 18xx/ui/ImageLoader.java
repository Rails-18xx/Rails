package ui;

import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.net.*;
import util.*;
import game.Log;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.w3c.dom.Document;

/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader
{

	private static HashMap tileMap;
	private static HashMap canvasMap;
	int width = 180;
	int height = 167;
	String svgTileDir = "tiles/svg/";
	String gifTileDir = "tiles/images/";

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
		
		try
		{
			image = getSVGTile(tileID);
		}
		catch (FileNotFoundException e)
		{
			//If loading the SVG fails, try loading the GIF. 
			Log.error("SVG Tile ID: " + tileID	+ " not found. Attempting to load GIF version of tile.");
			image = getGIFTile(tileID);
		}
		catch (Exception e)
		{
			Log.error("SVG transcoding for tile id " + tileID + " failed with " + e);
		}

		tileMap.put(Integer.toString(tileID), image);
	}

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

	private BufferedImage getSVGTile(int tileID) throws Exception
	{
		String fn = "tile" + tileID + ".svg";
		URL tileURL = buildURL(fn, svgTileDir);

		BufferedImage image = null;

		try
		{

			// url will not be null even is the file doesn't exist,
			// so we need to check if connection can be opened
			if (tileURL != null)
			{
				InputStream stream = tileURL.openStream();
				if (tileURL.openStream() != null)
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
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		return image;
	}

	private BufferedImage getGIFTile(int tileID)
	{
		String fn = "tile" + Integer.toString(tileID) + ".gif";
		URL tileURL = buildURL(fn, gifTileDir);

		BufferedImage image = null;

		try
		{
			InputStream str = Util.getStreamForFile(gifTileDir + fn);
			if (str != null)
			{
				image = ImageIO.read(str);
			}
		}
		catch (IOException e)
		{
			Log.error("Error loading file: " + tileURL + "\nLoad failed with "
					+ e);
			return null;
		}
		return image;
	}

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
		canvasMap = new HashMap();
	}

}

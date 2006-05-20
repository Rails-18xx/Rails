package ui;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import util.Util;

/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader
{

	private static final String tileDir = "tiles/images/";
	private static HashMap tileMap;

	// String fn = "tile" + Integer.toString(getTileId()) + ".gif";

	private boolean loadTile(int tileID)
	{
		String fn = "tile" + Integer.toString(tileID) + ".gif";
		String id = Integer.toString(tileID);

		try
		{
			// File f = new File(tileDir + fn);
			// BufferedImage img = ImageIO.read(f);
			BufferedImage img = ImageIO.read(Util.getStreamForFile(tileDir + fn));
			tileMap.put(id, img);

			return true;
		}
		catch (IOException e)
		{
			System.out.println("Unable to load tile file: " + tileDir + fn);
			tileMap.put(id, null);

			return false;
		}
	}

	public BufferedImage getTile(int tileID)
	{
		String id = Integer.toString(tileID);
		if (!tileMap.containsKey(id))
			loadTile(tileID);

		return (BufferedImage) tileMap.get(id);
	}

	public ImageLoader()
	{
		tileMap = new HashMap();
	}

}

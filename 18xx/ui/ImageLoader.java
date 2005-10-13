package ui;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

/**
 * This class handles loading our tile images.
 * It provides BufferedImages to be associated with the Hex.
 *  
 * @author Brett
 *
 */
public class ImageLoader
{
    private static final String tileDir = "./tiles/images/";
    private static HashMap tileMap;
    
    //String fn = "tile" + Integer.toString(getTileId()) + ".gif";

    public boolean loadTile(int tileID)
    {
        String fn = "tile" + Integer.toString(tileID) + ".gif";

        try
        {
     	   File f = new File(tileDir + fn);
     	   BufferedImage img = ImageIO.read(f);
     	   tileMap.put(((Object)Integer.toString(tileID)), ((Object)img));
     	   
     	   return true;
        }
        catch(IOException e)
        {
        	System.out.println("Unable to load tile file: " + tileDir + fn);
        	
        	return false;
        }
    }
    
    public BufferedImage getTile(int tileID)
    {
    	if(tileMap.containsKey((Object)Integer.toString(tileID)))
    	{
    		return (BufferedImage) tileMap.get((Object)Integer.toString(tileID));
    	}
    	//If we haven't loaded it yet, we'll try once to load the image.
    	else if (loadTile(tileID))
    	{
    		return (BufferedImage) tileMap.get((Object)Integer.toString(tileID));
    	}
    	else
    	{
    		return null;
    	}
    }
    
    public ImageLoader()
    {
    	tileMap = new HashMap();
    }

}

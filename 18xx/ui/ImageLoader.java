package ui;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import util.*;
import game.Log;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;


/**
 * This class handles loading our tile images. It provides BufferedImages to be
 * associated with the Hex.
 */
public class ImageLoader
{

	private static String tileDir = "tiles/svg/";
	private static HashMap tileMap;

    /* cheat, using batik transcoder API. we only want the Image */
    private static class BufferedImageTranscoder extends ImageTranscoder
    {
        private BufferedImage image;

        public BufferedImage createImage(int width, int height)
        {
            return new BufferedImage(width, height, 
                BufferedImage.TYPE_INT_ARGB);
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
    
	private boolean loadTile(int tileID)
	{
		
		String fn = "tile" + tileID + ".svg";
        Image image = null;
        int width = 180;
        int height = 167;
        
        try
        {
            java.net.URL url;
            url = new java.net.URL("file:" +
                    tileDir + fn);
            // url will not be null even is the file doesn't exist,
            // so we need to check if connection can be opened
            if (url != null)
            {
                InputStream stream = url.openStream();
                if (url.openStream() != null)
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
        catch (FileNotFoundException e)
        {
        	//If we can't load the SVGs for some reason, revert to using the GIFs.
        	tileDir = "tiles/images/";
    		fn = "tile" + Integer.toString(tileID) + ".gif";
    		String id = Integer.toString(tileID);

    		try
    		{
    			// File f = new File(tileDir + fn);
    			// BufferedImage img = ImageIO.read(f);
    			BufferedImage img = ImageIO.read(Util.getStreamForFile(tileDir + fn));
    			tileMap.put(id, img);

    			return true;
    		}
    		catch (IOException ex)
    		{
    			System.out.println(LocalText.getText("FileLoadException") + tileDir + fn);
    			tileMap.put(id, null);

    			return false;
    		}
        }
        catch (Exception e)
        {
            Log.error("SVG transcoding for " + fn + " in " + tileDir +
                    " failed with " + e);
            // nothing to do
            return false;
        }
        
        tileMap.put(Integer.toString(tileID), image);
        return true;
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

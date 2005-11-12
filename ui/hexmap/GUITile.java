/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/hexmap/Attic/GUITile.java,v 1.2 2005/11/12 15:12:28 evos Exp $
 * 
 * Created on 12-Nov-2005
 * Change Log:
 */
package ui.hexmap;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import ui.ImageLoader;
import game.*;

/**
 * This class represents the GUI version of a tile. 
 * @author Erik Vos
 */
public class GUITile {
    
    protected int tileId;
    protected TileI tile = null;
    protected BufferedImage tileImage = null;
    protected int rotation = 0;
    protected double tileScale = 0.33;
    protected static int[] xAdjust;
    protected static int[] yAdjust;
    protected static double baseRotation;
    protected static boolean initialised = false;
    
	protected static ImageLoader imageLoader = new ImageLoader();
	protected AffineTransform af = new AffineTransform();

	protected final static int[] xEWadjustArr = { -14, 26, 40, 12, -26, -40 };
	protected final static int[] yEWadjustArr = { -38, -30, 8, 38, 30, -8 };
	protected final static int[] xNSadjustArr = { -30, 8, 38, 30, -8, -38 };
	protected final static int[] yNSadjustArr = { -24, -40, -12, 28, 40, 12 };
	//public static final double SQRT3 = Math.sqrt(3.0);
	public static final double DEG60 = Math.PI / 3;
   
    public GUITile (int tileId) {
        
        this.tileId = tileId;
        tile = TileManager.get().getTile(tileId);
		tileImage = imageLoader.getTile(tileId);
		
		if (!initialised) initialise();
		
    }
    
    private void initialise () {
		
		if (MapManager.getTileOrientation() == MapHex.EW) {
		    xAdjust = xEWadjustArr;
		    yAdjust = yEWadjustArr;
		    baseRotation = 0.5 * DEG60;
		} else {
		    xAdjust = xNSadjustArr;
		    yAdjust = yNSadjustArr;
		    baseRotation = 0.0;
		}
		initialised = true;
    }
    
    
    
    public void setRotation (int rotation) {
        this.rotation = rotation % 6;
    }
    
    public int getRotation() {
        return rotation;
    }
    
    public void setScale (double scale) {
        tileScale = scale;
    }
    
    public void paintTile (Graphics2D g2, int x, int y) {
        
		if (tileImage != null) {
		    double radians = baseRotation + rotation * DEG60;
			af = AffineTransform.getRotateInstance(radians);
			af.scale(tileScale, tileScale);

			// All adjustments to AffineTransform must be done before being
			// assigned to the ATOp here.
			AffineTransformOp aop = new AffineTransformOp(af,
					AffineTransformOp.TYPE_BILINEAR);

			g2.drawImage(tileImage, aop, x + xAdjust[rotation], y + yAdjust[rotation]);
		}
       
    }
    
    public TileI getTile () {
        return tile;
    }
    
    public int getTileId () {
        return tileId;
    }

}

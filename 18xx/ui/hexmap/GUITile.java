/* $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/hexmap/Attic/GUITile.java,v 1.1 2005/11/12 13:44:43 evos Exp $
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
    
	protected static ImageLoader imageLoader = new ImageLoader();
	protected AffineTransform af = new AffineTransform();

    
    public GUITile (int tileId) {
        
        this.tileId = tileId;
        tile = TileManager.get().getTile(tileId);
		tileImage = imageLoader.getTile(tileId);

    	}
    
    public void setRotation (int rotation) {
        this.rotation = rotation % 6;
    }
    
    public void setScale (double scale) {
        tileScale = scale;
    }
    
    public void paintTile (Graphics2D g2, int x, int y, double rotation) {
        
		if (tileImage != null) {
			af = AffineTransform.getRotateInstance(rotation);
			af.scale(tileScale, tileScale);

			// All adjustments to AffineTransform must be done before being
			// assigned to the ATOp here.
			AffineTransformOp aop = new AffineTransformOp(af,
					AffineTransformOp.TYPE_BILINEAR);

			g2.drawImage(tileImage, aop, x, y);
		}
       
    }

}

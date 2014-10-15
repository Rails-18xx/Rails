package net.sf.rails.ui.swing.hexmap;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapOrientation;
import net.sf.rails.game.Tile;
import net.sf.rails.ui.swing.GUIGlobals;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.ImageLoader;
import net.sf.rails.ui.swing.hexmap.GUIHex.HexPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides code to paint tiles
 * 
 * In Rails 1.x this used to be a class with object instances, one object per visible Tile
 */
public class GUITile {
    private static final Logger log =
            LoggerFactory.getLogger(GUITile.class);
    
    public static final double SVG_X_CENTER_LOC = 0.489;
    public static final double SVG_Y_CENTER_LOC = 0.426;

    // FIXME: Merge the two following tile painting routines
    public static void paintTile(Graphics2D g2, GUIHex.HexPoint origin, GUIHex hex, Tile tile, HexSide rotation, double tileScale, int zoomStep) {

        // Preprinted tiles can have a different picture ID, defined per hex or per tile.
        // MapHex refers back to Tile if necessary
        String picId = hex.getHex().getPictureId(tile);
        
        ImageLoader imageLoader = GameUIManager.getImageLoader();
        BufferedImage tileImage = imageLoader.getTile(picId, zoomStep);

        if (tileImage != null) {

            double radians = MapOrientation.rotationInRadians(tile, rotation);
            HexPoint center = new HexPoint(
                    tileImage.getWidth() * SVG_X_CENTER_LOC * tileScale,
                    tileImage.getHeight()* SVG_Y_CENTER_LOC * tileScale
            );
            HexPoint difference = HexPoint.difference(origin, center);
            AffineTransform af = AffineTransform.getTranslateInstance(difference.getX(), difference.getY());
            af.rotate(radians, center.getX(), center.getY());
            af.scale(tileScale, tileScale);

            AffineTransformOp aop = new AffineTransformOp(af,
                    GUIGlobals.getRenderingHints());
            // FIXME: Change this to a sub-pixel approach
            // compare with 
            // http://stackoverflow.com/questions/8676909/drawing-an-image-using-sub-pixel-level-accuracy-using-graphics2d
            //g2.drawImage(tileImage, aop, (int) difference.getX(), (int) difference.getY());
            // already a first approach, integrated into the affine transform, however it does not
            // increase the quality of the map
            g2.drawImage(tileImage, aop, 0, 0);
            
        } else {
            log.error("No image for tile "+ tile +" on hex "+hex.toText());
        }
    }

    /**
     * Provides the image of the tile based on the zoomStep.
     * tileScale is not considered for producing this image.
     */
    public static BufferedImage getTileImage(Tile tile, HexSide rotation, int zoomStep) {

        // STEP 1: GET IMAGE FROM SVG
        // image not centered as there will be a bottom border to assign square bounds to the image

        String picId = tile.getPictureId();

        ImageLoader imageLoader = GameUIManager.getImageLoader();
        BufferedImage uncenteredTileImage = imageLoader.getTile(picId, zoomStep);

        if (uncenteredTileImage == null) return null;

        //svg always in NS orientation, hence wide diagonal can be directly taken from image size
        int wideDiagonal = uncenteredTileImage.getWidth();

        //narrow diagonal cannot be taken from image height due to the bottom border
        int narrowDiagonal = (int)Math.round( wideDiagonal * 0.5 * Math.sqrt(3) );

        int border = wideDiagonal - narrowDiagonal;

        // STEP 2: CENTER TILE IN IMAGE
        // apply the bottom border also the left / top / right

        //center tile by translation
        AffineTransform centeringAT = AffineTransform.getTranslateInstance( border, border );
        AffineTransformOp centeringATOp = new AffineTransformOp(centeringAT, GUIGlobals.getRenderingHints());

        //centered tile image create manually since it also needs a border on the right
        BufferedImage centeredTileImage = new BufferedImage(
                uncenteredTileImage.getWidth() + border * 2,
                uncenteredTileImage.getHeight() + border,
                uncenteredTileImage.getType());

        centeringATOp.filter(uncenteredTileImage, centeredTileImage);

        // STEP 3: ROTATE TILE IMAGE
        // feasible only now since there are enough margins to ensure tile won't exceed bounds

        double radians = MapOrientation.rotationInRadians(tile, rotation);
        int xCenter = Math.round(centeredTileImage.getWidth() / 2.0f );
        int yCenter = Math.round(centeredTileImage.getHeight() / 2.0f );

        AffineTransform af = AffineTransform.getRotateInstance(radians, xCenter, yCenter);
        AffineTransformOp aop = new AffineTransformOp(af, GUIGlobals.getRenderingHints());

        BufferedImage rotatedTileImage = aop.filter(centeredTileImage, null);

        // STEP 4: CROP ROTATED TILE IMAGE
        // rotation result will have additional borders on the right/bottom as a result of the AOP

        int croppedWidth, croppedHeight;
        if (MapOrientation.get(tile) == MapOrientation.NS) {
            //tile in NS orientation after rotation
            croppedWidth = wideDiagonal;
            croppedHeight = narrowDiagonal;
        } else  {
            //tile in EW orientation after rotation
            croppedWidth = narrowDiagonal;
            croppedHeight = wideDiagonal;
        }

        BufferedImage croppedTileImage = rotatedTileImage.getSubimage(
                xCenter - croppedWidth / 2,
                yCenter - croppedHeight / 2,
                croppedWidth,
                croppedHeight );

        return croppedTileImage;
    }

}

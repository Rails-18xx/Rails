package net.sf.rails.ui.swing.hexmap;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import net.sf.rails.game.HexSide;
import net.sf.rails.game.HexUpgrade;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapOrientation;
import net.sf.rails.game.Tile;
import net.sf.rails.ui.swing.GUIGlobals;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.ImageLoader;
import net.sf.rails.ui.swing.hexmap.GUIHex.HexPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class represents the GUI version of a tile.
 */
public class GUITile {
    private static final Logger log =
            LoggerFactory.getLogger(GUITile.class);
    
    // static fields
    private final Tile tile;
    private final String picId;
    private final GUIHex guiHex;

    // dynamic fields
    private HexSide rotation = HexSide.defaultRotation();
    private double tileScale = GUIHex.NORMAL_SCALE;

    private double baseRotation;

    public static final double DEG60 = Math.PI / 3;
    public static final double SVG_X_CENTER_LOC = 0.489;
    public static final double SVG_Y_CENTER_LOC = 0.426;

    public GUITile(Tile tile, GUIHex guiHex) {
        this.guiHex = guiHex;
        this.tile = tile;
        MapHex hex = guiHex.getHexModel();
        // Preprinted tiles can have a different picture ID, defined per hex or per tile.
        // MapHex refers back to Tile if necessary
        this.picId = hex.getPictureId(tile);
        log.debug("GUITile: tile=" + tile);
        log.debug("GUITile: picId=" + picId);

        // FIMXE: Is this related to MapOrientation.getBaseRotation ?
        if (hex.getParent().getMapOrientation()== MapOrientation.EW) {
            baseRotation = 0.5 * DEG60;
        } else {
            baseRotation = 0.0;
        }
    }

    public void setRotation(HexSide hexSide) {
        this.rotation = hexSide;
    }
    
    public boolean rotate(HexUpgrade upgrade, GUITile previousGUITile) {
        HexSide fixedRotation = getTile().getFixedOrientation();
        if (fixedRotation != null) {
            setRotation (fixedRotation);
            return true;
        }

        HexSide nextRotation = upgrade.getRotations().getNext(rotation.next());
        if (nextRotation == null) return false;
        setRotation(nextRotation);
        return true;
    }
    
    
    public HexSide getRotation() {
        return rotation;
    }

    public void setScale(double scale) {
        tileScale = scale;
    }

    // FIXME: Merge the two following tile painting routines
    public void paintTile(Graphics2D g2, GUIHex.HexPoint origin) {

        int zoomStep = guiHex.getHexMap().getZoomStep();

        ImageLoader imageLoader = GameUIManager.getImageLoader();
        BufferedImage tileImage = imageLoader.getTile(picId, zoomStep);

        if (tileImage != null) {

            double radians = baseRotation + rotation.getTrackPointNumber() * DEG60;
            HexPoint center = new HexPoint(
                    tileImage.getWidth() * SVG_X_CENTER_LOC * tileScale,
                    tileImage.getHeight()* SVG_Y_CENTER_LOC * tileScale
            );

            AffineTransform af = AffineTransform.getRotateInstance(radians, center.getX(), center.getY());
            af.scale(tileScale, tileScale);

            AffineTransformOp aop = new AffineTransformOp(af,
                    GUIGlobals.getRenderingHints());
            
            HexPoint difference = HexPoint.difference(origin, center);
            
            // FIXME: Change this to a sub-pixel approach
            // compare with 
            // http://stackoverflow.com/questions/8676909/drawing-an-image-using-sub-pixel-level-accuracy-using-graphics2d
            g2.drawImage(tileImage, aop, (int) difference.getX(), (int) difference.getY());

        } else {
            log.error("No image for tile "+ tile +" on hex "+guiHex.toText());
        }
    }

    /**
     * Provides the image of the tile based on the zoomStep.
     * tileScale is not considered for producing this image.
     */
    public BufferedImage getTileImage(int zoomStep) {

        // STEP 1: GET IMAGE FROM SVG
        // image not centered as there will be a bottom border to assign square bounds to the image
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

        double radians = baseRotation + rotation.getTrackPointNumber() * DEG60;
        int xCenter = Math.round(centeredTileImage.getWidth() / 2.0f );
        int yCenter = Math.round(centeredTileImage.getHeight() / 2.0f );

        AffineTransform af = AffineTransform.getRotateInstance(radians, xCenter, yCenter);
        AffineTransformOp aop = new AffineTransformOp(af, GUIGlobals.getRenderingHints());

        BufferedImage rotatedTileImage = aop.filter(centeredTileImage, null);

        // STEP 4: CROP ROTATED TILE IMAGE
        // rotation result will have additional borders on the right/bottom as a result of the AOP

        int croppedWidth, croppedHeight;
        if (baseRotation == 0) {
            //tile in NS orientation after rotation
            croppedWidth = wideDiagonal;
            croppedHeight = narrowDiagonal;
        } else {
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

    public Tile getTile() {
        return tile;
    }

}

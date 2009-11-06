/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/Attic/BarredHexSide.java,v 1.1 2009/11/06 20:23:53 evos Exp $*/
package rails.ui.swing.hexmap;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import rails.game.MapHex;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.ImageLoader;

/**
 * This class represents the GUI version of a tile.
 */
public class BarredHexSide {

    protected BufferedImage tileImage = null;

    protected int rotation = 0;

    protected double tileScale = GUIHex.NORMAL_SCALE;

    protected double baseRotation;

    protected MapHex hex = null;

    protected int hexSide;

    protected static ImageLoader imageLoader = GameUIManager.getImageLoader();

    protected AffineTransform af = new AffineTransform();

    public static final double DEG60 = Math.PI / 3;

    protected static Logger log =
            Logger.getLogger(BarredHexSide.class.getPackage().getName());

    public BarredHexSide(MapHex hex, int hexSide) {
        this.hexSide = hexSide;
        this.hex = hex;

        if (hex.getTileOrientation() == MapHex.EW) {
            baseRotation = 0.5 * DEG60;
        } else {
            baseRotation = 0.0;
        }
    }

    public void setRotation(int rotation) {
        this.rotation = rotation % 6;
    }

    public int getRotation() {
        return rotation;
    }

    public void setScale(double scale) {
        tileScale = scale;
    }

    public void paintBar(Graphics2D g2, int x, int y) {

        double radians = baseRotation + rotation * DEG60;
        int xCenter =
                (int) Math.round(tileImage.getWidth() * 0.5 * tileScale);
        int yCenter =
                (int) Math.round(tileImage.getHeight() * 0.5 * tileScale);

        af = AffineTransform.getRotateInstance(radians, xCenter, yCenter);
        af.scale(tileScale, tileScale);

        RenderingHints rh = new RenderingHints(null);
        rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        rh.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        rh.put(RenderingHints.KEY_DITHERING,
                RenderingHints.VALUE_DITHER_DISABLE);
        rh.put(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        rh.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        AffineTransformOp aop = new AffineTransformOp(af, rh);

        g2.drawImage(tileImage, aop, x - xCenter, y - yCenter);
    }

}

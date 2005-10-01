package ui.hexmap;


import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Class GUIBattleHex holds GUI info for one hex with N-S orientation.
 * @version $Id: GUINSHex.java,v 1.8 2005/10/01 00:00:14 wakko666 Exp $
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class GUINSHex extends GUIHex
{
    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

	
    public GUINSHex(int cx, int cy, int scale, JComponent map,
        int xCoord, int yCoord)
    {
        super(new BattleHex(xCoord, yCoord));
        this.map = map;

        len = scale / 3.0;

        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + SQRT3 * scale;
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + 2 * SQRT3 * scale;
        xVertex[4] = cx;
        yVertex[4] = cy + 2 * SQRT3 * scale;
        xVertex[5] = cx - 1 * scale;
        yVertex[5] = cy + SQRT3 * scale;

        hexagon = makePolygon(6, xVertex, yVertex, true);
        rectBound = hexagon.getBounds();

        Point2D.Double center = findCenter2D();

        final double innerScale = 0.8;
        AffineTransform at = AffineTransform.getScaleInstance(innerScale,
            innerScale);
        innerHexagon = (GeneralPath)hexagon.createTransformedShape(at);

        // Translate innerHexagon to make it concentric.
        Rectangle2D innerBounds = innerHexagon.getBounds2D();
        Point2D.Double innerCenter = new Point2D.Double(innerBounds.getX() +
            innerBounds.getWidth() / 2.0, innerBounds.getY() +
            innerBounds.getHeight() / 2.0);
        at = AffineTransform.getTranslateInstance(center.getX() -
            innerCenter.getX(), center.getY() - innerCenter.getY());
        innerHexagon.transform(at);
        
        initRotationArrays();
    	tileScale = 0.33;
    	x_adjust = x_adjust_arr[0];
    	y_adjust = y_adjust_arr[0];
    	rotation = rotation_arr[0];
    	arr_index = 0;
    }

    public GUINSHex(int xCoord, int yCoord)
    {
        super(new BattleHex(xCoord, yCoord));
    }

    public boolean innerContains(Point point)
    {
        return (innerHexagon.contains(point));
    }
 
    private void initRotationArrays()
    {
    	int[] xadjustArr = { -30, 8, 38, 30, -8, -38, -30 };
    	int[] yadjustArr = { -24, -40, -12, 28, 40, 12, -24 };
    	
    	for(int x=0; x<7; x++)
    	{
    	    rotation_arr[x] = x * DEG60;
    		x_adjust_arr[x] = xadjustArr[x];
    		y_adjust_arr[x] = yadjustArr[x];
    	}
    }
}


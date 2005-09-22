package ui.hexmap;


import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

import ui.*;

/**
 * Class GUIBattleHex holds GUI info for one hex with E-W orientation.
 * @version $Id: GUIEWHex.java,v 1.16 2005/09/22 23:22:47 wakko666 Exp $
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class GUIEWHex extends GUIHex
{	
    // Hex labels are:
    // A1-A3, B1-B4, C1-C5, D1-D6, E1-E5, F1-F4.
    // Letters increase left to right; numbers increase bottom to top.

	public GUIEWHex(double cx, double cy, int scale, Component map,
        double xCoord, double yCoord)
    {
        super(new BattleHex(xCoord, yCoord));
        this.map = map;

        len = scale;

        xVertex[0] = cx + SQRT3/2 * scale;
        yVertex[0] = cy + 0.5 * scale;
        xVertex[1] = cx + SQRT3 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + SQRT3 * scale;
        yVertex[2] = cy - 1 * scale;
        xVertex[3] = cx + SQRT3/2 * scale;
        yVertex[3] = cy - 1.5 * scale;
        xVertex[4] = cx;
        yVertex[4] = cy - 1 * scale;
        xVertex[5] = cx;
        yVertex[5] = cy;

        hexagon = makePolygon(6, xVertex, yVertex, true);
        rectBound = hexagon.getBounds();

        Point2D.Double center = findCenter2D();

        final double innerScale = 0.8;
        AffineTransform at = AffineTransform.getScaleInstance(innerScale,
            innerScale);
        innerHexagon = (GeneralPath)hexagon.createTransformedShape(at);

        // Translate innerHexagon to make it concentric.
        Rectangle2D innerBounds = innerHexagon.getBounds2D();
        Point2D.Double innerCenter = new Point2D.Double(
              innerBounds.getX() + innerBounds.getWidth() / 2.0, 
              innerBounds.getY() + innerBounds.getHeight() / 2.0);
        at = AffineTransform.getTranslateInstance(
              center.getX() - innerCenter.getX(), 
              center.getY() - innerCenter.getY());
        innerHexagon.transform(at);
    }

    public GUIEWHex(int xCoord, int yCoord)
    {
        super(new BattleHex(xCoord, yCoord));
    }

    public boolean innerContains(Point point)
    {
        return (innerHexagon.contains(point));
    }

    //FIXME:  Horribly Kludgy
    public void paint (Graphics g)
    {
    	super.paint(g);
    	Graphics2D g2 = (Graphics2D)g;
    	
    	Point center = findCenter();    
    	
    	af = AffineTransform.getRotateInstance(rotation);
    	af.scale(tileScale,tileScale);
    	
    	//All adjustments to AffineTransform must be done before being assigned to the ATOp here.
    	AffineTransformOp aop = new AffineTransformOp(af, AffineTransformOp.TYPE_BILINEAR);    	  
    	    	
     	//g2.setClip(hexagon);
    	g2.drawImage(tileImage, aop, (center.x + x_adjust), (center.y + y_adjust));
    	
    	if(arr_index == 6)
    	{
    		arr_index = 1;
    	}
    	else
    		arr_index++;
    }
       
}


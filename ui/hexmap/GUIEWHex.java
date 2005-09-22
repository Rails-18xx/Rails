package ui.hexmap;


import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

import ui.*;

/**
 * Class GUIBattleHex holds GUI info for one hex with E-W orientation.
 * @version $Id: GUIEWHex.java,v 1.13 2005/09/22 22:03:08 evos Exp $
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class GUIEWHex extends GUIHex implements MouseListener
{

	protected double tileScale = 0.33;
	protected double[] rotation_arr = { 0.5, 1.05, 1.05, 1.05, 1.05, 1.05 };
	protected int[] x_adjust_arr = { -14, 26, 40, 14, -26, -40 };
	protected int[] y_adjust_arr = { -38, -30, 8, 38, 30, -8 };
	protected int x_adjust = x_adjust_arr[0];
	protected int y_adjust = y_adjust_arr[0];
	protected double rotation = rotation_arr[0];
	protected int arr_index = 0;	
	
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

    public void paint (Graphics g)
    {
    	super.paint(g);
    	Graphics2D g2 = (Graphics2D)g;
    	
    	Point center = findCenter();
    	
    	//rescale the image only once.
    	if (arr_index == 0)
    	{
    		af.scale(tileScale,tileScale);
    	}
    	x_adjust = x_adjust_arr[tileOrientation];
    	y_adjust = y_adjust_arr[tileOrientation];
    	rotation = rotation_arr[tileOrientation];
    	af.rotate(rotation);
     	   
    	//All adjustments to AffineTransform must be done before being assigned to the ATOp here.
    	//AffineTransformOp aop = new AffineTransformOp(af, AffineTransformOp.TYPE_BICUBIC);    	  
    	AffineTransformOp aop = new AffineTransformOp(af, AffineTransformOp.TYPE_BILINEAR);    	  
    	    	
     	g2.setClip(hexagon);
     	     	    	
    	//FIXME: This needs to be non-static. Ought to use (center.x - n * Scale)
    	g2.drawImage(tileImage, aop, (center.x + x_adjust), (center.y + y_adjust));
    	
    	if(arr_index == 5)
    		arr_index = 1;
    	else
    		arr_index++;
    	
    	x_adjust = x_adjust_arr[arr_index];
    	y_adjust = y_adjust_arr[arr_index];
    	rotation = rotation_arr[arr_index];
    }
       
    public void mouseClicked(MouseEvent arg0)
	{
    	System.out.println("Click.");
	}

	public void mouseEntered(MouseEvent arg0)
	{
	}

	public void mouseExited(MouseEvent arg0)
	{
	}

	public void mousePressed(MouseEvent arg0)
	{
	}

	public void mouseReleased(MouseEvent arg0)
	{
	}

}


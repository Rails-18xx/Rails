package ui.hexmap;


import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;

/**
 * Class GUIBattleHex holds GUI info for one hex with E-W orientation.
 * @version $Id: GUIEWHex.java,v 1.9 2005/09/21 21:45:29 wakko666 Exp $
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

    public void paint (Graphics g)
    {
    	super.paint(g);
    	Graphics2D g2 = (Graphics2D)g;
    	
    	// Get and install an AlphaComposite to do transparent drawing
    	//g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    	
        // FIXME: This is very kludgy.
        
        String cwd = "./tiles/images/";
        String fn = "tile" + Integer.toString(getTileId()) + ".gif";
    	
        try
        {
        	//FIXME: Need to figure out how to use transparencies for the rectangle bounding box that surrounds the hex.
     	   File f = new File(cwd + fn);
     	   BufferedImage img = ImageIO.read(f);
     	   
     	   AffineTransform af = new AffineTransform();
     	   af.scale(0.33,0.33);
     	   af.rotate(.5);
     	   
     	   AffineTransformOp aop = new AffineTransformOp(af, AffineTransformOp.TYPE_BICUBIC);    	  
     	   Point center = this.findCenter();
     	   
     	   if(f.exists())
     		   //FIXME: This needs to be non-static. Ought to use (center.x - n * Scale)
     		   g2.drawImage(img, aop, (center.x-14), (center.y-38));
     	   else
     		  System.out.println("File not found: " + f.getAbsolutePath());
        }
        catch(IOException e)
        {
     	   System.out.println("Unable to load tile file: " + cwd + fn);
        }
        
    }
    
    
}


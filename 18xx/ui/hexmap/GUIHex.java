package ui.hexmap;

import game.Log;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.*;

public abstract class GUIHex extends JPanel
{
   public static final double SQRT3 = Math.sqrt(3.0);
   protected Hex model;
   protected Component map;
   protected GeneralPath innerHexagon;
   protected static final Color highlightColor = Color.red;
   
   // Added by Erik Vos
   protected String hexName;
   protected int tileId;
   protected String tileFilename;

   /**
    * Stores the neighbouring views.
    * This parallels the neighors field in BattleHex, just on the view side. 
    * @todo check if we can avoid this
    */
   private GUIHex[] neighbors = new GUIHex[6];
   
   // GUI variables
   double[] xVertex = new double[6];
   double[] yVertex = new double[6];
   double len;
   GeneralPath hexagon;
   Rectangle rectBound;

   /** Globally turns antialiasing on or off for all hexes. */
   static boolean antialias;
   /** Globally turns overlay on or off for all hexes */
   static boolean useOverlay;
   // Selection is in-between GUI and game state.
   private boolean selected;

   public GUIHex(Hex model)
   {
      this.model = model;
   }

   public Hex getHexModel()
   {
      return this.model;
   }

   public void setHexModel(Hex model)
   {
      this.model = model;
   }

   public Rectangle getBounds()
   {
      return rectBound;
   }

   public boolean contains(Point2D.Double point)
   {
      return (hexagon.contains(point));
   }
   
   public boolean contains(Point point)
   {
      return (hexagon.contains(point));
   }
   
   public void select()
   {
      selected = true;
   }

   public void unselect()
   {
      selected = false;
   }

   public void setSelected(boolean selected)
   {
      this.selected = selected;
   }

   public boolean isSelected()
   {
      return selected;
   }

   static boolean getAntialias()
   {
      return antialias;
   }

   static void setAntialias(boolean enabled)
   {
      antialias = enabled;
   }

   static boolean getOverlay()
   {
      return useOverlay;
   }

   public static void setOverlay(boolean enabled)
   {
      useOverlay = enabled;
   }

   /**
    * Return a GeneralPath polygon, with the passed number of sides, and the
    * passed x and y coordinates. Close the polygon if the argument closed is
    * true.
    */
   static GeneralPath makePolygon(int sides, double[] x, double[] y,
         boolean closed)
   {
      GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, sides);
      polygon.moveTo((float) x[0], (float) y[0]);
      for (int i = 1; i < sides; i++)
      {
         polygon.lineTo((float) x[i], (float) y[i]);
      }
      if (closed)
      {
         polygon.closePath();
      }
           
      return polygon;
   }

   /** Return the Point closest to the center of the polygon. */
   public Point findCenter()
   {
      return new Point((int)((xVertex[2] + xVertex[5]) / 2),
            (int)((yVertex[0] + yVertex[3]) / 2));
   }

   /** Return the Point2D.Double at the center of the polygon. */
   Point2D.Double findCenter2D()
   {
      return new Point2D.Double((xVertex[2] + xVertex[5]) / 2.0,
            (yVertex[0] + yVertex[3]) / 2.0);
   }
   
   public void setNeighbor(int i, GUIHex hex)
   {
       if (i >= 0 && i < 6)
       {
           neighbors[i] = hex;
           getBattleHexModel().setNeighbor(i, hex.getBattleHexModel());
       }
   }
   
   public GUIHex getNeighbor(int i)
   {
       if (i < 0 || i > 6)
       {
           return null;
       }
       else
       {
           return neighbors[i];
       }
   }
   
   public BattleHex getBattleHexModel() {
      return (BattleHex)getHexModel();
  }
   
   public void paint(Graphics g)
   {
       Graphics2D g2 = (Graphics2D)g;
       if (getAntialias())
       {
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON);
       }
       else
       {
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);
       }

       Color terrainColor = getBattleHexModel().getTerrainColor();
       if (isSelected())
       {
           if (terrainColor.equals(highlightColor))
           {
               //g2.setColor(HTMLColor.invertRGBColor(highlightColor));
           }
           else
           {
               g2.setColor(highlightColor);
           }
           g2.fill(hexagon);

           g2.setColor(terrainColor);
           g2.fill(innerHexagon);

           g2.setColor(Color.black);
           g2.draw(innerHexagon);
       }
       else
       {
           g2.setColor(terrainColor);
           g2.fill(hexagon);
       }

       g2.setColor(Color.black);
       g2.draw(hexagon);

       if ((useOverlay) && (paintOverlay(g2)))
       {
           // well, ok...
       }
       else
       {
           // Draw hexside features.
           for (int i = 0; i < 6; i++)
           {
               char hexside = getBattleHexModel().getHexside(i);
               int n;
               if (hexside != ' ')
               {
                   n = (i + 1) % 6;
                   drawHexside(g2,
                       xVertex[i], yVertex[i],
                       xVertex[n], yVertex[n],
                       hexside);
               }

               // Draw them again from the other side.
               hexside = getBattleHexModel().getOppositeHexside(i);
               if (hexside != ' ')
               {
                   n = (i + 1) % 6;
                   drawHexside(g2,
                       xVertex[n], yVertex[n],
                       xVertex[i], yVertex[i],
                       hexside);
               }
           }
       }

       // Do not anti-alias text.
       g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
           RenderingHints.VALUE_ANTIALIAS_OFF);
       String name = getBattleHexModel().getTerrainName().toUpperCase();

       FontMetrics fontMetrics = g2.getFontMetrics();

       g2.drawString(name, rectBound.x + ((rectBound.width -
           fontMetrics.stringWidth(name)) / 2),
           rectBound.y +
           ((fontMetrics.getHeight() + rectBound.height) / 2));

       // Show hex label in upper left corner.
       g2.drawString(getBattleHexModel().getLabel(),
           rectBound.x + (rectBound.width -
           fontMetrics.stringWidth(getBattleHexModel().getLabel())) / 3,
           rectBound.y +
           ((fontMetrics.getHeight() + rectBound.height) / 4));
       
       // Added by Erik Vos: show hex name
       g2.drawString (hexName, 
               rectBound.x + (rectBound.width -
               fontMetrics.stringWidth(getBattleHexModel().getLabel())) / 2,
               rectBound.y +
               ((fontMetrics.getHeight() + rectBound.height) * 2 / 3));
       
       // Added by Erik Vos: show the preprinted tile id
       g2.drawString (tileId == -999 ? "?" : "#"+tileId,
            rectBound.x + (rectBound.width -
                    fontMetrics.stringWidth(getBattleHexModel().getLabel())) / 2,
                    rectBound.y +
                    ((fontMetrics.getHeight() + rectBound.height) * 4 / 9));
       
   }
   
   public void repaint()
   {
	   //FIXME: Temporary Kludge
	   try
	   {
		   map.repaint();
	   }
	   catch (NullPointerException e)
	   {
		   System.out.println("Map Window repaint error.");
	   }
	   
       // If an entrance needs repainting, paint the whole map.
       /* 
       if (getBattleHexModel().isEntrance())
       {
           map.repaint();
       }
       else
       {
           map.repaint(getBounds().x, getBounds().y, getBounds().width,
               getBounds().height);
       }
               */
   }
   
   public boolean paintOverlay(Graphics2D g)
   {
       Image overlay = loadOneOverlay(getBattleHexModel().getTerrain(),
           rectBound.width, rectBound.height);
       if (overlay != null)
       { // first, draw the Hex itself
           g.drawImage(overlay,
               rectBound.x,
               rectBound.y,
               rectBound.width,
               rectBound.height,
               map);
       }
       boolean didAllHexside = true;
       Shape oldClip = g.getClip();
       //make sure we draw only inside our hex
       g.setClip(null);
       g.clip(hexagon);
       // second, draw the opposite Hex HexSide
       for (int i = 0; i < 6; i++)
       {
           char op = getBattleHexModel().getOppositeHexside(i);
           if (op != ' ')
           {
               GUIHex neighbor = getNeighbor(i);

               int dx1 = 0, dx2 = 0, dy1 = 0, dy2 = 0;

               final float xm, ym;
               float xi, yi;
               xm = (float)neighbor.findCenter2D().x;
               ym = (float)neighbor.findCenter2D().y;
               xi = (float)neighbor.xVertex[5] - xm;
               yi = (float)neighbor.yVertex[0] - ym;
               xi *= 1.2; //1.14814814814814814814;
               yi *= 1.2; //1.17021276595744680851;
               xi += xm;
               yi += ym;
               dx1 = (int)xi;
               dy1 = (int)yi;
               xi = (float)neighbor.xVertex[2] - xm;
               yi = (float)neighbor.yVertex[3] - ym;
               xi *= 1.2; //1.14814814814814814814;
               yi *= 1.2; //1.17021276595744680851;
               xi += xm;
               yi += ym;
               dx2 = (int)xi;
               dy2 = (int)yi;

               Image sideOverlay = loadOneOverlay(
                   neighbor.getBattleHexModel().getHexsideName((i + 3) % 6),
                   dx2 - dx1, dy2 - dy1);

               if (sideOverlay != null)
               {
                   g.drawImage(sideOverlay,
                       dx1, dy1, dx2 - dx1, dy2 - dy1,
                       map);
               }
               else
               {
                   didAllHexside = false;
               }
           }
       }
       g.setClip(oldClip);
       return didAllHexside;
   }
   
   private static Image loadOneOverlay(String name, int width, int height)
   {
       Image overlay = null;
       //List directories = VariantSupport.getImagesDirectoriesList();
       //overlay = ResourceLoader.getImage(name + imagePostfix, directories,
       //    width, height);
       return overlay;
   }
   
   void drawHexside(Graphics2D g2, double vx1, double vy1, double vx2,
         double vy2, char hexsideType)
     {
         double x0;                     // first focus point
         double y0;
         double x1;                     // second focus point
         double y1;
         double x2;                     // center point
         double y2;
         double theta;                  // gate angle
         double[] x = new double[4];   // hexside points
         double[] y = new double[4];   // hexside points

         x0 = vx1 + (vx2 - vx1) / 6;
         y0 = vy1 + (vy2 - vy1) / 6;
         x1 = vx1 + (vx2 - vx1) / 3;
         y1 = vy1 + (vy2 - vy1) / 3;

         theta = Math.atan2(vy2 - vy1, vx2 - vx1);

         switch (hexsideType)
         {
             case 'c':     // cliff -- triangles
                 for (int j = 0; j < 3; j++)
                 {
                     x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                     y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                     x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                     y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                     x[0] = x0 - len * Math.sin(theta);
                     y[0] = y0 + len * Math.cos(theta);
                     x[1] = (x0 + x1) / 2 + len * Math.sin(theta);
                     y[1] = (y0 + y1) / 2 - len * Math.cos(theta);
                     x[2] = x1 - len * Math.sin(theta);
                     y[2] = y1 + len * Math.cos(theta);

                     GeneralPath polygon = makePolygon(3, x, y, false);

                     g2.setColor(Color.white);
                     g2.fill(polygon);
                     g2.setColor(Color.black);
                     g2.draw(polygon);
                 }
                 break;

             case 'd':     // dune --  arcs
                 for (int j = 0; j < 3; j++)
                 {
                     x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                     y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                     x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                     y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                     x[0] = x0 - len * Math.sin(theta);
                     y[0] = y0 + len * Math.cos(theta);
                     x[1] = x0 + len * Math.sin(theta);
                     y[1] = y0 - len * Math.cos(theta);
                     x[2] = x1 + len * Math.sin(theta);
                     y[2] = y1 - len * Math.cos(theta);
                     x[3] = x1 - len * Math.sin(theta);
                     y[3] = y1 + len * Math.cos(theta);

                     x2 = (x0 + x1) / 2;
                     y2 = (y0 + y1) / 2;
                     Rectangle2D.Double rect = new Rectangle2D.Double();
                     rect.x = x2 - len;
                     rect.y = y2 - len;
                     rect.width = 2 * len;
                     rect.height = 2 * len;

                     g2.setColor(Color.white);
                     Arc2D.Double arc = new Arc2D.Double(rect.x, rect.y,
                         rect.width, rect.height,
                         Math.toDegrees(2 * Math.PI - theta), 180,
                         Arc2D.OPEN);
                     g2.fill(arc);
                     g2.setColor(Color.black);
                     g2.draw(arc);
                 }
                 break;

             case 's':     // slope -- lines
                 for (int j = 0; j < 3; j++)
                 {
                     x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                     y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                     x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                     y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                     x[0] = x0 - len / 3 * Math.sin(theta);
                     y[0] = y0 + len / 3 * Math.cos(theta);
                     x[1] = x0 + len / 3 * Math.sin(theta);
                     y[1] = y0 - len / 3 * Math.cos(theta);
                     x[2] = x1 + len / 3 * Math.sin(theta);
                     y[2] = y1 - len / 3 * Math.cos(theta);
                     x[3] = x1 - len / 3 * Math.sin(theta);
                     y[3] = y1 + len / 3 * Math.cos(theta);

                     g2.setColor(Color.black);
                     g2.draw(new Line2D.Double(x[0], y[0], x[1], y[1]));
                     g2.draw(new Line2D.Double(x[2], y[2], x[3], y[3]));
                 }
                 break;

             case 'w':     // wall --  blocks
                 for (int j = 0; j < 3; j++)
                 {
                     x0 = vx1 + (vx2 - vx1) * (2 + 3 * j) / 12;
                     y0 = vy1 + (vy2 - vy1) * (2 + 3 * j) / 12;
                     x1 = vx1 + (vx2 - vx1) * (4 + 3 * j) / 12;
                     y1 = vy1 + (vy2 - vy1) * (4 + 3 * j) / 12;

                     x[0] = x0 - len * Math.sin(theta);
                     y[0] = y0 + len * Math.cos(theta);
                     x[1] = x0 + len * Math.sin(theta);
                     y[1] = y0 - len * Math.cos(theta);
                     x[2] = x1 + len * Math.sin(theta);
                     y[2] = y1 - len * Math.cos(theta);
                     x[3] = x1 - len * Math.sin(theta);
                     y[3] = y1 + len * Math.cos(theta);

                     GeneralPath polygon = makePolygon(4, x, y, false);

                     g2.setColor(Color.white);
                     g2.fill(polygon);
                     g2.setColor(Color.black);
                     g2.draw(polygon);
                 }
                 break;

             case 'r':     // river -- single blue line
                 //g2.setColor(HTMLColor.skyBlue);
                 Stroke oldStroke = g2.getStroke();
                 g2.setStroke(new BasicStroke((float)5.));
                 g2.draw((Shape)new Line2D.Double(vx1, vy1, vx2, vy2));
                 g2.setColor(Color.black);
                 g2.setStroke(oldStroke);
                 break;

             default:
                 Log.error("Bogus hexside type");
         }
     }
   
   // Added by Erik Vos
   /**
 * @return Returns the name.
 */
public String getName() {
    return hexName;
}
/**
 * @param name The name to set.
 */
public void setName(String name) {
    this.hexName = name;
}


/**
 * @return Returns the tileId.
 */
public int getTileId() {
	return tileId;
}
/**
 * @param tileId The tileId to set.
 */
public void setTileId(int tileId) {
	this.tileId = tileId;
}

/**
 * 
 * @return Filename of the tile image
 */
public String getTileFilename()
{
	return tileFilename;
}


public void setTileFilename(String tileFilename)
{
	this.tileFilename = tileFilename;
}
}

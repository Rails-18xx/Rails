package ui.hexmap;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

public abstract class GUIHex
{
   public static final double SQRT3 = Math.sqrt(3.0);
   public static final double SQRT6 = Math.sqrt(6.0);

   private Hex model;

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
      return new Point((int) Math.round((xVertex[2] + xVertex[5]) / 2),
            (int) Math.round((yVertex[0] + yVertex[3]) / 2));
   }

   /** Return the Point2D.Double at the center of the polygon. */
   Point2D.Double findCenter2D()
   {
      return new Point2D.Double((xVertex[2] + xVertex[5]) / 2.0,
            (yVertex[0] + yVertex[3]) / 2.0);
   }
}

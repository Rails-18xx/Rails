/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package ui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class StockChit extends JPanel
{
   private Color fgColor, bgColor;
   private double x, y, diameter;
   private Ellipse2D.Double circle, circle2;

   public void paintComponent(Graphics g)
   {
      clear(g);
      Graphics2D g2d = (Graphics2D) g;

      //Temporary kludge until we figure out
      //How to draw a circle with a circular border around it.
      //
      //This will probably be achieved by simply drawing two circles
      //of slightly differing sizes, one on top of another.

      this.setOpaque(false);

      this.setForeground(bgColor);
      g2d.setColor(bgColor);
      g2d.fill(circle2);
      g2d.draw(circle2);
      this.setVisible(true);
      
      this.setForeground(fgColor);
      g2d.setColor(fgColor);
      g2d.fill(circle);
      g2d.draw(circle);
      this.setVisible(true);
   }

   protected void clear(Graphics g)
   {
      super.paintComponent(g);
   }

   public StockChit()
   {
      this(Color.BLACK, Color.WHITE, 4, 4, 15);
   }

   public StockChit(Color fc, Color bc)
   {
      this(fc, bc, 4, 4, 15);
   }
   
   public StockChit(double xx, double yy)
   {
      this(Color.BLACK, Color.WHITE, xx, yy, 15);
   }
   
   public StockChit(Color fc, Color bc, double x, double y, double diameter)
   {
      fgColor = fc;
      bgColor = bc;
      
      circle = new Ellipse2D.Double(x, y, diameter, diameter);
      circle2 = new Ellipse2D.Double(x-3, y-3, diameter+6, diameter+6);
   }
   
}
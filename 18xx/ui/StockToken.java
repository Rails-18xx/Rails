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

public class StockToken extends JPanel
{
   private Color fgColor, bgColor;
   private double x, y, diameter;
   private Ellipse2D.Double circle, circle2;

   public void paintComponent(Graphics g)
   {
      clear(g);
      Graphics2D g2d = (Graphics2D) g;
      
      drawToken(g2d, bgColor, circle2);
      drawToken(g2d, fgColor, circle);
   }
   
   private void drawToken(Graphics2D g2d, Color c, Ellipse2D.Double circle)
   {     
      g2d.setColor(c);
      g2d.fill(circle);
      g2d.draw(circle);
   }

   protected void clear(Graphics g)
   {
      super.paintComponent(g);
   }

   public StockToken()
   {
      this(Color.BLACK, Color.WHITE, 4, 4, 15);
   }

   public StockToken(Color fc, Color bc)
   {
      this(fc, bc, 4, 4, 15);
   }
   
   public StockToken(double xx, double yy)
   {
      this(Color.BLACK, Color.WHITE, xx, yy, 15);
   }
   
   public StockToken(Color fc, Color bc, double x, double y, double diameter)
   {      
      super();
      
      fgColor = fc;
      bgColor = bc;
      
      circle = new Ellipse2D.Double(x, y, diameter, diameter);
      circle2 = new Ellipse2D.Double(x-3, y-3, diameter+6, diameter+6);
      
      this.setForeground(fgColor);
      this.setOpaque(false);
      this.setVisible(true);
   }
   
}
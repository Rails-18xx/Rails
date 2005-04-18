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
   private Ellipse2D.Double circle;

   private Font font;

   public void paintComponent(Graphics g)
   {
      clear(g);
      Graphics2D g2d = (Graphics2D) g;

      this.setForeground(fgColor);
      this.setBackground(bgColor);

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
      this(Color.BLACK, Color.WHITE, 1, 1, 15);
   }

   public StockChit(Color fc, Color bc)
   {
      this(fc, bc, 1, 1, 15);
   }
   
   public StockChit(double xx, double yy)
   {
      this(Color.BLACK, Color.WHITE, xx, yy, 15);
   }
   
   public StockChit(Color fc, Color bc, double xx, double yy, double dia)
   {
      x = xx;
      y = yy;
      diameter = dia;
      fgColor = fc;
      bgColor = bc;
      circle = new Ellipse2D.Double(x, y, diameter, diameter);
   }
   
}
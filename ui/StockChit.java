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
   private int locValue;

   private Color locColor;

   private Rectangle2D.Double square = new Rectangle2D.Double(0, 0, 100, 100);

   private Font font;

   public void paintComponent(Graphics g)
   {
      clear(g);
      Graphics2D g2d = (Graphics2D) g;

      this.setForeground(locColor);

      g2d.fill(square);
      g2d.draw(square);
      g2d.setFont(font);
      g2d.drawString(Integer.toString(locValue), 0, 0);
   }

   protected void clear(Graphics g)
   {
      super.paintComponent(g);
   }

   public StockChit()
   {
      this(0, Color.WHITE);
   }

   public StockChit(int value)
   {
      this(value, Color.WHITE);
   }

   public StockChit(Color color)
   {
      this(0, color);
   }

   public StockChit(int value, Color color)
   {
      locValue = value;
      locColor = color;
   }

}
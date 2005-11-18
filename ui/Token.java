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

/**
 * This class draws a company's token on the StockChart.
 * 
 * @author Brett
 *
 */

public class Token extends JPanel
{
   private Color fgColor, bgColor;
   private Ellipse2D.Double circle;
   private String name;

   public void paintComponent(Graphics g)
   {
      clear(g);
      Graphics2D g2d = (Graphics2D) g;
      Font f = new Font ("Helvetica", Font.BOLD, 8);
      
      drawToken(g2d, bgColor, circle);
      g2d.setFont(f);
      g2d.setColor(fgColor);
      g2d.drawString(name, 3, 14);
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

   public Token(String name)
   {
      this(Color.BLACK, Color.WHITE, name, 1, 1, 21);
   }

   public Token(Color fc, Color bc, String name)
   {
      this(fc, bc, name, 1, 1, 21);
   }
   
   public Token(double x, double y, String name)
   {
      this(Color.BLACK, Color.WHITE, name, x, y, 21);
   }
   
   public Token(Color fc, Color bc, String name, double x, double y, double diameter)
   {      
      super();
      
      fgColor = fc;
      bgColor = bc;
      
      circle = new Ellipse2D.Double(x, y, diameter, diameter);
      
      this.setForeground(fgColor);
      this.setOpaque(false);
      this.setVisible(true);
      this.name = name;
   }
   
}
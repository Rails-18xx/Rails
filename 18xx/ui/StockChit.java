/*
 * Created on Mar 6, 2005
 *
 */
package ui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * @author Brett Lentz
 *
 */
public class StockChit extends JPanel
{
   private int locValue;
   private Color locColor;
   private Rectangle2D.Double square = new Rectangle2D.Double(0,0,100,100);
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

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

import game.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.util.*;

/**
 * This class displays the StockMarket Window.
 * 
 *  @author Brett
 */

public class StockChart extends JFrame implements WindowListener, KeyListener
{
   private static JPanel stockPanel;
   private static GridLayout stockGrid;
   private GridBagConstraints gc;
   
   private void initialize()
   {
      setTitle("Rails: Stock Chart");
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      getContentPane().setLayout(new GridBagLayout());

      stockPanel = new JPanel();

      stockGrid = new GridLayout();
      stockGrid.setHgap(0);
      stockGrid.setVgap(0);
      stockPanel.setLayout(stockGrid);
      
      gc = new GridBagConstraints();
      
	  addKeyListener(this);

   }
   
   private void populateGridBag()
   {
      gc.gridx = 0;
      gc.gridy = 0;
      gc.weightx = 1.0;
      gc.weighty = 1.0;
      gc.gridwidth = 2;
      gc.fill = GridBagConstraints.BOTH;
      getContentPane().add(stockPanel, gc);
   }
   private static void populateStockPanel()
   {
      int depth = 0;
      Dimension size = new Dimension(40, 40);
      StockSpace[][] market = Game.getStockMarket().getStockChart();	
      
      JLabel priceLabel;
      JLayeredPane layeredPane; 
      ArrayList tokenList;     

      stockGrid.setColumns(market[0].length);
      stockGrid.setRows(market.length);

      for (int i = 0; i < market.length; i++)
      {
         for (int j = 0; j < market[0].length; j++)
         {
            layeredPane = new JLayeredPane();
            priceLabel = new JLabel();            
            
            stockPanel.add(layeredPane);            
                                   
            priceLabel.setBounds(1, 1, size.width, size.height);
            priceLabel.setOpaque(true);
            
            layeredPane.add(priceLabel, new Integer(0), depth);
            layeredPane.moveToBack(priceLabel);            
            layeredPane.setPreferredSize(new Dimension (40, 40));
            
            try
            {
               priceLabel.setText(Integer.toString(market[i][j].getPrice()));
            }
            catch (NullPointerException e)
            {
               priceLabel.setText("");
            }
           
            try
            {
               priceLabel.setBackground(stringToColor(market[i][j].getColour()));
            }
            catch (NullPointerException e)
            {
               priceLabel.setBackground(Color.WHITE);
            }

            try
            {
               if (market[i][j].isStart())
               {
                  priceLabel.setBorder(BorderFactory.createLineBorder(Color.red, 2));
               }
            }
            catch (NullPointerException e)
            {
            }
            
            try
            {
               if (market[i][j].hasTokens())
               {
                  tokenList = (ArrayList) market[i][j].getTokens();
                  
                  placeToken(tokenList, layeredPane);
               }
            }
            catch (NullPointerException e)
            {
            }
         }
      }
   }
   private static void placeToken(ArrayList tokenList, JLayeredPane layeredPane)
   {
      Point origin = new Point(16,0);
      Dimension size = new Dimension(40, 40);
      Color bgColour;
      Color fgColour;
      PublicCompany co;      
      Token token;
      
      for (int k = 0; k < tokenList.size(); k++)
      {
         co = (PublicCompany) tokenList.get(k);
         bgColour = co.getBgColour();
         fgColour = co.getFgColour();

         token = new Token(fgColour, bgColour, co.getName());
         token.setBounds(origin.x, origin.y, size.width, size.height);
         
         layeredPane.add(token, new Integer(0), 0);
         origin.y += 6;
      }      
   }
   
   /**
    *   Quick n' dirty method of converting strings to color objects.
    *   This has been replaced by using hex colors in the XML definitions.
    *   
    *   @deprecated
    */
   private static Color stringToColor(String color)
   {
      if (color.equalsIgnoreCase("yellow"))
      {
         return Color.YELLOW;
      }
      else if (color.equalsIgnoreCase("orange"))
      {
         return Color.ORANGE;
      }
      else if (color.equalsIgnoreCase("brown"))
      {
         return Color.RED;
         //There is no Color.BROWN
         //Perhaps we'll define one later.
      }
      else if (color.equalsIgnoreCase("red"))
      {
         return Color.RED;
      }
      else if (color.equalsIgnoreCase("green"))
      {
         return Color.GREEN;
      }
      else if (color.equalsIgnoreCase("blue"))
      {
         return Color.BLUE;
      }
      else if (color.equalsIgnoreCase("black"))
      {
         return Color.BLACK;
      }
      else if (color.equalsIgnoreCase("white"))
      {
         return Color.WHITE;
      }
      else
      {
         System.out.println("Warning: Unknown color: " + color + ".");
         return Color.MAGENTA;
      }
   }
 
   public static void refreshStockPanel()
   {
      stockPanel.removeAll();
      populateStockPanel();
   }
   
   public StockChart()
   {
      super();
      
      initialize();
      populateGridBag();
      populateStockPanel();

      stockPanel.setBackground(Color.LIGHT_GRAY);     

      addWindowListener(this);
      pack();
   }
   
	public void windowActivated(WindowEvent e)
	{
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		StatusWindow.uncheckMenuItemBox(StatusWindow.marketString);
		dispose();		
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
	}
	public void keyPressed(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_F1) {
	        HelpWindow.displayHelp(GameManager.getInstance().getHelp());
	        e.consume();
	    }
	}
	
	public void keyReleased(KeyEvent e) {}
	public void keyTyped (KeyEvent e) {}


}
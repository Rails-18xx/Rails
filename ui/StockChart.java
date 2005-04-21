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

/*
 * The layout is roughly this:
 * 
 * JFrame (GridBag) 
 * | ---> StockMarket JPanel (Grid) 
 * 		---> JLayeredPane
 * 			---> Shows the stockmarket chart 
 * 			---> Shows tokens for every company 
 * | ---> Status JPanel (Grid) 
 * 		---> Shows at-a-glance information about each player's holdings. 
 * 		---> Shows at-a-glance information about each company's performance. 
 * | ---> Button JPanel (Flow)
 * 		 ---> Buy Button 
 * 		---> Sell Button
 *  
 */

public class StockChart extends JFrame implements WindowStateListener
{
   private JPanel stockPanel;
   private JPanel statusPanel;
   private JPanel buttonPanel;
   private JButton buyButton;
   private JButton sellButton;
   private GridBagConstraints gc;
   private GridLayout stockGrid;
   private GridLayout statusGrid;
   private FlowLayout flow;
   private StockMarket stockMarket;
   private CompanyStatus companyStatus;
   
   private void initialize()
   {
      this.setSize(10, 10);
      this.setTitle("Stock Chart");
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.getContentPane().setLayout(new GridBagLayout());

      stockPanel = new JPanel();
      stockPanel.setPreferredSize(new Dimension(300, 400));
      
      statusPanel = new JPanel();
      buttonPanel = new JPanel();

      stockGrid = new GridLayout();
      statusGrid = new GridLayout();
      flow = new FlowLayout();

      stockPanel.setLayout(stockGrid);
      statusPanel.setLayout(statusGrid);
      buttonPanel.setLayout(flow);

      buyButton = new JButton("buy");
      sellButton = new JButton("sell");

      gc = new GridBagConstraints();

   }
   private void populateGridBag()
   {
      gc.gridx = 0;
      gc.gridy = 0;
      gc.weightx = 1.0;
      gc.weighty = 1.0;
      gc.gridwidth = 2;
      gc.ipadx = 500;
      gc.ipady = 50;
      gc.fill = GridBagConstraints.BOTH;
      this.getContentPane().add(stockPanel, gc);

      gc.gridx = 1;
      gc.gridy = 1;
      gc.fill = 0;
      gc.weightx = 0.5;
      gc.weighty = 0.5;
      gc.gridwidth = 1;
      this.getContentPane().add(statusPanel, gc);

      gc.gridx = 0;
      gc.gridy = 2;
      gc.weightx = 0.0;
      gc.weighty = 0.0;
      gc.gridwidth = 2;
      gc.fill = GridBagConstraints.HORIZONTAL;
      this.getContentPane().add(buttonPanel, gc);
   }
   private void populateStockPanel()
   {
      //http://www.redbrick.dcu.ie/help/reference/java/uiswing/components/layeredpane.html         
      StockSpace[][] market = stockMarket.getStockChart();

      stockGrid.setColumns(market[0].length);
      stockGrid.setRows(market.length);

      for (int i = 0; i < market.length; i++)
      {
         for (int j = 0; j < market[0].length; j++)
         {
            Point origin = new Point(20,0);
            Dimension size = new Dimension(40, 40);
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setPreferredSize(new Dimension (40, 30));
            stockPanel.add(layeredPane);
            
            JTextField textField;
            StockToken token;
            int depth = 0;

            try
            {
               textField = new JTextField(Integer.toString(market[i][j].getPrice()));
            }
            catch (NullPointerException e)
            {
               textField = new JTextField("");
            }
            
            try
            {
               textField.setBackground(stringToColor(market[i][j].getColour()));
            }
            catch (NullPointerException e)
            {
               textField.setBackground(Color.WHITE);
            }

            try
            {
               if (market[i][j].isStart())
               {
                  textField.setBorder(BorderFactory.createLineBorder(Color.red, 2));
               }
            }
            catch (NullPointerException e)
            {
            }

            textField.setBounds(1, 1, size.width, size.height);
            textField.setEditable(false);
            layeredPane.add(textField, new Integer(0), depth);
            layeredPane.moveToBack(textField);
            
            try
            {
               if (market[i][j].hasTokens())
               {
                  ArrayList tokenList = market[i][j].getTokens();

                  for (int k = 0; k < tokenList.size(); k++)
                  {
                     PublicCompany co = (PublicCompany) tokenList.get(k);
                     String bgColour = co.getBgColour();
                     String fgColour = co.getFgColour();

                     token = new StockToken(stringToColor(fgColour), stringToColor(bgColour));
                     token.setBounds(origin.x, origin.y, size.width, size.height);
                     
                     layeredPane.add(token, new Integer(0), depth);
                     layeredPane.moveToFront(token);
                     origin.y += 6;
                  }
               }
            }
            catch (NullPointerException e)
            {
            }
         }
      }
   }
   private Color stringToColor(String color)
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
 
   
   public StockChart(StockMarket sm, CompanyStatus cs)
   {
      super();

      stockMarket = sm;
      companyStatus = cs;
      
      initialize();
      populateGridBag();
      populateStockPanel();

      stockPanel.setBackground(Color.LIGHT_GRAY);
      statusPanel.setBackground(Color.BLACK);
      buttonPanel.setBackground(Color.LIGHT_GRAY);

      statusPanel.add(companyStatus);
      buttonPanel.add(buyButton);
      buttonPanel.add(sellButton);

      this.pack();
      this.setVisible(true);
   }

   /* (non-Javadoc)
    * @see java.awt.event.WindowStateListener#windowStateChanged(java.awt.event.WindowEvent)
    */
   public void windowStateChanged(WindowEvent arg0)
   {
      populateStockPanel();
   }
}
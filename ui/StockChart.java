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
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

/*
 * The layout idea is this:
 * 
 * JFrame (GridBag)
 *  | ---> StockMarket JPanel (Grid) 
 * 			---> Shows the stockmarket chart 
 * 			---> Shows chits for every company 
 *  | ---> Status JPanel (Grid) 
 * 			---> Shows at-a-glance information about each player's holdings. 
 * 			---> Shows at-a-glance information about each company's performance. 
 *  | ---> Button JPanel (Flow)
 * 			 ---> Buy Button 
 * 			 ---> Sell Button
 *  
 */

public class StockChart extends JFrame
{
   private Border lineBorder;
   private JTextField text;
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

   private void initialize()
   {
      this.setSize(10, 10);
      this.setTitle("Stock Chart");
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.getContentPane().setLayout(new GridBagLayout());

      text = new JTextField("This space reserved for game status info.");

      stockPanel = new JPanel();
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
      StockSpace[][] market = stockMarket.getStockChart();

      stockGrid.setColumns(market[0].length);
      stockGrid.setRows(market.length);

      for (int x = 0; x < market.length; x++)
      {
         for (int y = 0; y < market[0].length; y++)
         {
            JTextField foo;
            StockChit token;
            
            try
            {
               foo = new JTextField(Integer.toString(market[x][y].getPrice()));
            }
            catch (NullPointerException e)
            {
               foo = new JTextField("");
               foo.setBorder(null);
            }

            try
            {
               foo.setBackground(stringToColor(market[x][y].getColour()));
            }
            catch (NullPointerException e)
            {
               foo.setBackground(Color.WHITE);
            }

            try
            {
               if (market[x][y].isStart())
               {
                  foo.setBorder(BorderFactory.createLineBorder(Color.red));
               }
            }
            catch (NullPointerException e)
            {
            }
            
            try
            {
               if (market[x][y].hasTokens())
               {
                  ArrayList tokenList = market[x][y].getTokens();
                  
                  for(int i=0;i<tokenList.size();i++)
                  {
                     PublicCompany co = (PublicCompany) tokenList.get(i);
                     String bgColour = co.getBgColour();
                     String fgColour = co.getFgColour();

                     token = new StockChit(stringToColor(fgColour), stringToColor(bgColour));                     
                  }
               }
            }
            catch (NullPointerException e)
            {
            }

            foo.setEditable(false);
            stockPanel.add(foo);
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
         return Color.WHITE;
      }
   }

   public StockChart(StockMarket sm)
   {
      super();

      stockMarket = sm;
      initialize();
      populateGridBag();
      populateStockPanel();

      lineBorder = BorderFactory.createLineBorder(Color.black);
      text.setBorder(lineBorder);
      text.setEditable(false);
      text.setBackground(Color.LIGHT_GRAY);

      stockPanel.setBackground(Color.LIGHT_GRAY);
      statusPanel.setBackground(Color.BLACK);
      buttonPanel.setBackground(Color.LIGHT_GRAY);

      statusPanel.add(text);
      buttonPanel.add(buyButton);
      buttonPanel.add(sellButton);

      this.pack();
      this.setVisible(true);
   }
}
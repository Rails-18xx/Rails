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
import javax.swing.*;
import javax.swing.border.*;

/*
 * The layout idea is this:
 * 
 * JFrame (GridBag)
 *    |
 * 	  ---> StockMarket JPanel (Grid)
 * 		---> Shows the stockmarket chart
 * 		---> Shows chits for every company
 * 	  |
 *	  ---> Status JPanel (Grid)
 *		---> Shows at-a-glance information about each player's holdings.
 *		---> Shows at-a-glance information about each company's performance.
 *	  |
 *	  ---> Button JPanel (Flow)
 *		---> Buy Button
 *		---> Sell Button
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
   
   private game.StockChart modelChart;
   
   private void initialize()
   {
      this.setSize(10, 10);
      this.setTitle("Stock Chart");
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.getContentPane().setLayout(new GridBagLayout());
      
      text = new JTextField("Foo");
      
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
      
      modelChart = new game.StockChart("1830");
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
      game.StockPrice[][] market = modelChart.getStockChart();

      stockGrid.setColumns(market[0].length);
      stockGrid.setRows(market.length);
      
      for(int x = 0; x < market.length; x++)
      {
         for(int y = 0; y < market.length; y++)
         {
            JTextField foo;
            
            try
            {
               foo = new JTextField(market[x][y].getName());
            }
            catch (NullPointerException e)
            {
               foo = new JTextField("Null");
            }
            
            try
            {
               setStockBGColor(market[x][y].getColour(), foo);
            }
            catch (NullPointerException e)
            {
               foo.setBackground(Color.WHITE);
            }
            
            foo.setEditable(false);
            stockPanel.add(foo);
         }
      }
   }
   private void setStockBGColor(String color, JTextField square)
   {
      if(color.equalsIgnoreCase("yellow"))
      {
         square.setBackground(Color.YELLOW);
      }
      else if (color.equalsIgnoreCase("orange"))
      {
         square.setBackground(Color.ORANGE);
      }
      else if (color.equalsIgnoreCase("brown"))
      {
         square.setBackground(Color.RED); 	//There is no Color.BROWN
         									//Perhaps we'll define one later.
      }
      else if (color.equalsIgnoreCase("green"))
      {
         square.setBackground(Color.GREEN);
      }
      else
      {
         square.setBackground(Color.WHITE);
      }
   }
   public StockChart()
   {
      super();
      initialize();
      populateGridBag();
      populateStockPanel();
      
      lineBorder = BorderFactory.createLineBorder(Color.black);     
      text.setBorder(lineBorder);
      text.setEditable(false);
      text.setBackground(Color.RED);

      stockPanel.setBackground(Color.WHITE);
      statusPanel.setBackground(Color.BLACK);
      buttonPanel.setBackground(Color.LIGHT_GRAY);
    
      statusPanel.add(text);      
      buttonPanel.add(buyButton);
      buttonPanel.add(sellButton);

      this.pack();
      this.setVisible(true);
   }
}
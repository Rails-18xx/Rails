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
 * 		---> Buy Button 
 *		---> Sell Button
 *  
 */

public class StockChart extends JFrame implements ActionListener
{
   private JPanel stockPanel, statusPanel, buttonPanel;
   private JButton upButton, downButton, leftButton, rightButton, startCoButton;
   private GridLayout stockGrid;
   private GridBagConstraints gc;
   private StockMarket stockMarket;
   private CompanyStatus companyStatus;
   private PlayerStatus playerStatus;
   
   private void initialize()
   {
      this.setSize(10, 10);
      this.setTitle("Rails: Stock Chart");
      //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.getContentPane().setLayout(new GridBagLayout());

      stockPanel = new JPanel();
      statusPanel = new JPanel();
      buttonPanel = new JPanel();

      stockGrid = new GridLayout();
      stockGrid.setHgap(0);
      stockGrid.setVgap(0);
      stockPanel.setLayout(stockGrid);
      statusPanel.setLayout(new GridLayout(2,0));
      buttonPanel.setLayout(new FlowLayout());

      upButton = new JButton("up");
      downButton = new JButton("down");
      leftButton = new JButton("left");
      rightButton = new JButton("right");
      startCoButton = new JButton("Start Company");

      gc = new GridBagConstraints();

   }
   private void populateGridBag()
   {
      gc.gridx = 0;
      gc.gridy = 0;
      gc.weightx = 1.0;
      gc.weighty = 1.0;
      gc.gridwidth = 2;
      gc.fill = GridBagConstraints.BOTH;
      this.getContentPane().add(stockPanel, gc);

      gc.gridx = 1;
      gc.gridy = 1;
      gc.fill = 0;
      gc.weightx = 0.5;
      gc.weighty = 0.5;
      gc.ipadx = 500;
      gc.ipady = 50;
      gc.gridwidth = 1;
      this.getContentPane().add(statusPanel, gc);

      gc.gridx = 0;
      gc.gridy = 2;
      gc.weightx = 0.0;
      gc.weighty = 0.0;
      gc.gridwidth = 2;
      gc.ipady = 0;
      gc.fill = GridBagConstraints.HORIZONTAL;
      this.getContentPane().add(buttonPanel, gc);
   }
   private void populateStockPanel()
   {
      int depth = 0;
      Dimension size = new Dimension(40, 40);
      StockSpace[][] market = stockMarket.getStockChart();
      
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
                  tokenList = market[i][j].getTokens();
                  
                  placeToken(tokenList, layeredPane);
               }
            }
            catch (NullPointerException e)
            {
            }
         }
      }
   }
   private void placeToken(ArrayList tokenList, JLayeredPane layeredPane)
   {
      Point origin = new Point(16,0);
      Dimension size = new Dimension(40, 40);
      Color bgColour;
      Color fgColour;
      PublicCompany co;      
      StockToken token;
      
      for (int k = 0; k < tokenList.size(); k++)
      {
         co = (PublicCompany) tokenList.get(k);
         bgColour = co.getBgColour();
         fgColour = co.getFgColour();

         token = new StockToken(fgColour, bgColour);
         token.setBounds(origin.x, origin.y, size.width, size.height);
         
         layeredPane.add(token, new Integer(0), 0);
         origin.y += 6;
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
 
   public void refreshStockPanel()
   {
      stockPanel.removeAll();
      populateStockPanel();
      playerStatus.RefreshStatus();
      companyStatus.RefreshStatus();
   }
   public StockChart(StockMarket sm, CompanyStatus cs, PlayerStatus ps)
   {
      super();
      
      stockMarket = sm;
      companyStatus = cs;
      playerStatus = ps;
      
      initialize();
      populateGridBag();
      populateStockPanel();

      stockPanel.setBackground(Color.LIGHT_GRAY);
      
      statusPanel.setOpaque(false);

      statusPanel.add(companyStatus);
      statusPanel.add(playerStatus);
      buttonPanel.add(upButton);
      buttonPanel.add(downButton);
      buttonPanel.add(leftButton);
      buttonPanel.add(rightButton);
      buttonPanel.add(startCoButton);
      
      upButton.setActionCommand("up");
      downButton.setActionCommand("down");
      leftButton.setActionCommand("left");
      rightButton.setActionCommand("right");
      startCoButton.setActionCommand("startCo");
      
      upButton.addActionListener(this);
      downButton.addActionListener(this);
      leftButton.addActionListener(this);
      rightButton.addActionListener(this);
      startCoButton.addActionListener(this);

      this.pack();
      this.setVisible(true);
   }
   /* (non-Javadoc)
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   public void actionPerformed(ActionEvent arg0)
   {

      try
      {
         String companySelected = companyStatus.getCompanySelected();
         CompanyManager cm = (CompanyManager) Game.getCompanyManager();
         PublicCompany co = (PublicCompany) cm.getPublicCompany(companySelected);
      
         if(arg0.getActionCommand().equalsIgnoreCase("down"))
         {
            stockMarket.sell((PublicCompanyI) co, 1);
            refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("left"))
         {
            stockMarket.withhold((PublicCompanyI) co);
            refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("up"))
         {
            stockMarket.soldOut((PublicCompanyI) co);
            refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("right"))
         {
            stockMarket.payOut((PublicCompanyI) co);
            refreshStockPanel();
         }
         else
         {
            if(companyStatus.getCompanySelected() != null && playerStatus.getPlayerSelected() != null)
            {
               StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this, "Start company at what price?", 
                     						"What Price?", 
                     						JOptionPane.INFORMATION_MESSAGE,
                     						null,
                     						stockMarket.getStartSpaces().toArray(),
                     						stockMarket.getStartSpaces().get(0));
               
               PublicCompany.startCompany(playerStatus.getPlayerSelected(), companyStatus.getCompanySelected(), sp);
               
               Player player = Game.getPlayerManager().getPlayerByName(playerStatus.getPlayerSelected());
               
               //Buy Share doesn't completely work yet...
               player.buyShare((Certificate)co.getCertificates().get(0));
               
               companyStatus.setCompanySelected(null);
               playerStatus.setPlayerSelected(null);
               refreshStockPanel();
            }
            else
               JOptionPane.showMessageDialog(this,"Unable to start company.\r\nYou must select a player and a company first.", 
                     						"Company not started.", JOptionPane.OK_OPTION);
         }
      }
      catch (NullPointerException e)
      {   
         JOptionPane.showMessageDialog(this, "Unable to move selected company's token.");
         e.printStackTrace();
      }        
   }
}
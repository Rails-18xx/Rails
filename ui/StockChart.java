/*
Rails: an 18xx game system.
Copyright (C) 2005 Brett Lentz

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class StockChart extends JFrame
{
   private int numRows, numCols, 
   				hgap, vgap, 
   				bhoriz, bvert;
   
   private Border lineBorder;
   private JTextField text;
   private JPanel stockPanel;
   private JPanel statusPanel;
   private JPanel buttonPanel;   
   private JButton buyButton;
   private JButton sellButton;
   
   private GridBagConstraints gc;
   
   private void initialize()
   {
      this.setSize(640, 480);
      this.setTitle("Stock Chart");
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      lineBorder = BorderFactory.createLineBorder(Color.black);
      text = new JTextField("Foo");      
      text.setBorder(lineBorder);
      text.setEditable(false);
      
      stockPanel = new JPanel();
      statusPanel = new JPanel();
      buttonPanel = new JPanel();
      
      stockPanel.setBackground(Color.WHITE);
      statusPanel.setBackground(Color.RED);
      buttonPanel.setBackground(Color.LIGHT_GRAY);
      
      buyButton = new JButton("buy");
      sellButton = new JButton("sell");
      
      gc = new GridBagConstraints();
   }
 
   public StockChart()
   {
      super();
      
      hgap = 5;
      vgap = 5;
      bhoriz = 50;
      bvert = 100;
      
      //Need to create these methods, not necessarily with these 
      //method names.
      //numRows = getRowsFromXML();
      //numCols = getColsFromXML();
      
      numRows = 5;
      numCols = 5;
      
      initialize();
      
      this.getContentPane().setLayout(new GridBagLayout());
      
      gc.gridx = 0;
      gc.gridy = 0;
      gc.fill = GridBagConstraints.BOTH;
      this.getContentPane().add(stockPanel, gc);
      
      gc.gridx = 1;
      gc.gridy = 1;
      gc.fill = 0;
      this.getContentPane().add(statusPanel, gc);
      
      gc.gridx = 0;
      gc.gridy = 2;
      gc.gridwidth = 2;
      gc.fill = GridBagConstraints.HORIZONTAL;
      this.getContentPane().add(buttonPanel, gc);
      
      stockPanel.setLayout(new GridLayout(numRows, numCols));
            
      for(int x=0; x<20; x++)
      {
         stockPanel.add(new JTextField(Integer.toString(x)));
      }
      
      statusPanel.add(text);
      
      buttonPanel.add(buyButton);
      buttonPanel.add(sellButton);

      this.pack();
      this.setVisible(true);      
   }
}
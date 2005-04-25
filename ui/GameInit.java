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
import java.awt.event.*;
import javax.swing.*;
import test.*;

public class GameInit extends JFrame implements MouseListener
{
   GridBagConstraints gc;
   JPanel optionsPane, playersPane, buttonPane;
   JButton newButton, loadButton;
   
   private void initialize()
   {
      gc = new GridBagConstraints();
      optionsPane = new JPanel();
      playersPane = new JPanel();
      buttonPane = new JPanel();
      newButton = new JButton("New Game");
      loadButton = new JButton("Load Game");
      
      this.setLayout(new GridBagLayout());
      this.setTitle("Rails: New Game");
      this.setPreferredSize(new Dimension(300,400));
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         
      playersPane.add(new JLabel("Players:"));
      playersPane.setBorder(BorderFactory.createLoweredBevelBorder());
      
      optionsPane.add(new JLabel("Options:"));
      optionsPane.setBorder(BorderFactory.createLoweredBevelBorder());
      
      newButton.addMouseListener(this);
      
      buttonPane.add(newButton);
      buttonPane.add(loadButton);
      buttonPane.setBorder(BorderFactory.createLoweredBevelBorder());
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
      this.add(playersPane, gc);

      gc.gridx = 1;
      gc.gridy = 1;
      gc.fill = 0;
      gc.weightx = 0.5;
      gc.weighty = 0.5;
      gc.gridwidth = 1;
      this.add(optionsPane, gc);

      gc.gridx = 0;
      gc.gridy = 2;
      gc.weightx = 0.0;
      gc.weighty = 0.0;
      gc.gridwidth = 2;
      gc.fill = GridBagConstraints.HORIZONTAL;
      this.add(buttonPane, gc);
   }
   
   public GameInit()
   {
      super();
      
      initialize();
      populateGridBag();
      
      this.pack();
      this.setVisible(true);
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
    */
   public void mouseClicked(MouseEvent arg0)
   {
      System.out.println("Warning: Loading from StockTest.");
      StockTest.StockUITest();
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
    */
   public void mouseEntered(MouseEvent arg0)
   {
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
    */
   public void mouseExited(MouseEvent arg0)
   {
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
    */
   public void mousePressed(MouseEvent arg0)
   {
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
    */
   public void mouseReleased(MouseEvent arg0)
   {
   }
}

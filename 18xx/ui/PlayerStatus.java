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

import game.*;

public class PlayerStatus extends JPanel implements MouseListener
{
   private JLabel[] playerLabel;
   private JLabel[] cashLabel;
   private Player[] players;
   private String playerSelected;
   
   public void UpdateStatus()
   {
      this.add(new JLabel("Player:"));
      for(int i = 0; i < players.length; i++)
      {
         playerLabel[i] = new JLabel(players[i].getName());         
         playerLabel[i].setOpaque(true);
         playerLabel[i].setBackground(Color.WHITE);
         playerLabel[i].setForeground(Color.BLACK);
         playerLabel[i].addMouseListener(this);
         
         this.add(playerLabel[i]);
      }
      
      this.add(new JLabel("Cash:"));
      for(int i = 0; i < players.length; i++)
      {
         cashLabel[i] = new JLabel();         
         cashLabel[i].setText(Integer.toString(players[i].getCash()));
         cashLabel[i].setOpaque(true);
         cashLabel[i].setBackground(Color.WHITE);
         
         this.add(cashLabel[i]);
      }
   }
   
   public void RefreshStatus()
   {
      this.removeAll();
      this.UpdateStatus();
   }
   
   public PlayerStatus(Player[] players)
   {    
      super();
      
      this.setBackground(Color.WHITE);
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setLayout(new GridLayout(2,players.length+1));
      this.setOpaque(false);
      
      this.players = players;
      
      playerLabel = new JLabel[players.length];
      cashLabel = new JLabel[players.length];
      
      UpdateStatus();
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
    */
   public void mouseClicked(MouseEvent arg0)
   {
      JLabel label = (JLabel) arg0.getComponent();
      if(!label.getBackground().equals(Color.YELLOW))
      {
         try
         {
            if(!playerSelected.equalsIgnoreCase(label.getText()))
            {
               for(int i=0; i < playerLabel.length; i++)
               {
                  playerLabel[i].setBackground(Color.WHITE);
               }
            }
         }
         catch (NullPointerException e)
         {
         }
         
         label.setBackground(Color.YELLOW);
         playerSelected = label.getText();
      }
      else
      {
         label.setBackground(Color.WHITE);
         playerSelected = null;
      }
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
   /**
    * @return Returns the playerSelected.
    */
   public String getPlayerSelected()
   {
      return playerSelected;
   }
   /**
    * @param playerSelected The playerSelected to set.
    */
   public void setPlayerSelected(String playerSelected)
   {
      this.playerSelected = playerSelected;
   }
}

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
import java.util.*;

import javax.swing.*;

import game.*;

public class PlayerStatus extends JPanel
{
   private JLabel[] playerLabel;
   private JLabel[] cashLabel;
   private JLabel[] stockCountLabel;
   private Player[] players;
   private String playerSelected;
   private Map labelPerPlayer = new HashMap();
   
   public void updateStatus()
   {
       // Player names
      this.add(new JLabel("Player:"));
      for(int i = 0; i < players.length; i++)
      {
         playerLabel[i] = new MyLabel(players[i].getName());         
         if (players[i].getName().equals(playerSelected)) {
            playerLabel[i].setBackground(Color.YELLOW);
         } else {
         	 playerLabel[i].setBackground(Color.WHITE);
         }
         playerLabel[i].setForeground(Color.BLACK);
          
         this.add(playerLabel[i]);
         labelPerPlayer.put(players[i].getName(), playerLabel[i]);
      }
      
      // Player cash
      this.add(new JLabel("Cash:"));
      for(int i = 0; i < players.length; i++)
      {
         cashLabel[i] = new MyLabel(Integer.toString(players[i].getCash()));         
     	 cashLabel[i].setBackground(Color.WHITE);
         
         this.add(cashLabel[i]);
      }
      
      // Privates owned by player
      
      
   }
   public void refreshPanel()
   {
      removeAll();
      updateStatus();
      super.repaint();
   } 
   
   public PlayerStatus()
   {    
      super();
      
      players = Game.getPlayerManager().getPlayersArray();
      
      this.setBackground(Color.WHITE);
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setLayout(new GridLayout(0, players.length+1, 1, 1));
      this.setOpaque(false);
      
      playerLabel = new JLabel[players.length];
      cashLabel = new JLabel[players.length];
      
      updateStatus();
   }
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
      JLabel label;
      if ((label = (JLabel)labelPerPlayer.get(playerSelected)) != null) {
      	label.setBackground(Color.YELLOW);
      } else {
        	label.setBackground(Color.WHITE);
      }
   }
}

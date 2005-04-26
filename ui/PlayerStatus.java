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

import game.*;

public class PlayerStatus extends JPanel
{
   private JLabel[] playerLabel;
   private JLabel[] cashLabel;
   private Player[] players;
   
   public void UpdateStatus()
   {
      this.add(new JLabel("Player:"));
      for(int i = 0; i < players.length; i++)
      {
         playerLabel[i] = new JLabel(players[i].getName());         
         playerLabel[i].setOpaque(true);
         playerLabel[i].setBackground(Color.WHITE);
         playerLabel[i].setForeground(Color.BLACK);
         
         this.add(playerLabel[i]);
      }
      
      this.add(new JLabel("Cash:"));
      for(int i = 0; i < players.length; i++)
      {
         playerLabel[i] = new JLabel();         
         playerLabel[i].setText(Integer.toString(players[i].getCash()));
         playerLabel[i].setOpaque(true);
         playerLabel[i].setBackground(Color.WHITE);
         
         this.add(playerLabel[i]);
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
      this.setLayout(new GridLayout(0,players.length+1));
      this.setOpaque(false);
      
      this.players = players;
      
      playerLabel = new JLabel[players.length];
      cashLabel = new JLabel[players.length];
      
      UpdateStatus();
   }
}

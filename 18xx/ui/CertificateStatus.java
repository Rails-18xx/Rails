/*
 * Created on Apr 28, 2005
 */
package ui;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import game.*;

/**
 * @author blentz
 */
public class CertificateStatus extends JPanel
{
   JLabel[][] statusArray;
   ArrayList companies;
   ArrayList players;

   public void updateStatus()
   {
      for(int i=0; i <= companies.size(); i++)
      {
         for(int j=0; j <= players.size(); j++)
         {
            if(i==0 && j==0)
               statusArray[i][j] = new JLabel("Stock Ownership");
            else if(j==0)
                  statusArray[i][j] = new JLabel(((PublicCompany)companies.get(i-1)).getName());
            else if (i==0)
                  statusArray[i][j] = new JLabel(((Player)players.get(j-1)).getName());
            else
            {
               statusArray[i][j] = new JLabel(Integer.toString(((Player)players.get(j-1)).getPortfolio().countShares((PublicCompany)companies.get(i-1))));
               System.out.println(((Player)players.get(j-1)).getPortfolio().countShares((PublicCompany)companies.get(i-1)));
            }
                        
            statusArray[i][j].setOpaque(true);
            statusArray[i][j].setBackground(Color.WHITE);
            statusArray[i][j].setForeground(Color.BLACK);
            this.add(statusArray[i][j]);
         }
      }
   }
   public void refreshStatus()
   {
      removeAll();
      updateStatus();
   }
   
   public CertificateStatus()
   {
      companies = new ArrayList((ArrayList)Game.getCompanyManager().getAllPublicCompanies());
      players = new ArrayList(Game.getPlayerManager().getPlayersArrayList());
      statusArray = new JLabel[companies.size()+1][players.size()+1];
      
      this.setLayout(new GridLayout(companies.size()+1, players.size()+1));
      this.setOpaque(true);
      
      updateStatus();
   }
}

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

   public void UpdateStatus()
   {
      //FIXME: not done yet here
      /*
      for(int i=0; i < companies.size(); i++)
      {
         if(i==0)
         {
            statusArray[i][0].add(new JLabel("Co"));
         }
         else
         {
            for(int j=0; j < players.size(); j++)
            {
               if(j==0)
               {
                  if(i==0)
                  {
                  }
                  else
                  {
                     
                  }
               }
               statusArray[i][j].add(new JLabel(""));
            }
         }
      }
      */
   }
   public void RefreshStatus()
   {
      removeAll();
      UpdateStatus();
   }
   
   public CertificateStatus()
   {
      companies = new ArrayList((ArrayList)Game.getCompanyManager().getAllPublicCompanies());
      players = new ArrayList(Game.getPlayerManager().getPlayersArrayList());
      statusArray = new JLabel[companies.size()][players.size()];
      
      this.setLayout(new GridLayout(companies.size()+1, players.size()+1));
      this.setBackground(Color.WHITE);
      this.setOpaque(true);
   }
}

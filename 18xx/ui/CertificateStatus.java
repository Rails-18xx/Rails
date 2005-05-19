/*
 * Created on Apr 28, 2005
 */
package ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;
import game.*;

/**
 * @author blentz
 */
public class CertificateStatus extends JPanel implements MouseListener
{
   private JLabel[][] statusArray;
   private ArrayList companies;
   private ArrayList players;
   private JLabel selectedLabel;

   public void updateStatus()
   {
      for(int i=0; i <= companies.size(); i++)
      {
         for(int j=0; j <= players.size(); j++)
         {
            if(i==0 && j==0)
            {
               statusArray[i][j] = new JLabel("Stock Ownership: ");
               statusArray[i][j].setBackground(Color.LIGHT_GRAY);
               statusArray[i][j].setSize(50,10);
            }
            else if(j==0)
            {
                  statusArray[i][j] = new JLabel(((PublicCompany)companies.get(i-1)).getName());
            }
            else if (i==0)
            {
                  statusArray[i][j] = new JLabel(((Player)players.get(j-1)).getName());
            }
            else
            {
               statusArray[i][j] = new JLabel(Integer.toString(((Player)players.get(j-1)).getPortfolio().ownsShare((PublicCompany)companies.get(i-1))));               
               statusArray[i][j].addMouseListener(this);
               statusArray[i][j].setBackground(Color.WHITE);
            }
                        
            statusArray[i][j].setOpaque(true);                        
            this.add(statusArray[i][j]);
         }
      }
   }
   public void refreshPanel()
   {
      removeAll();
      updateStatus();
      super.repaint();
   }
   
   public CertificateStatus()
   {
      companies = new ArrayList((ArrayList)Game.getCompanyManager().getAllPublicCompanies());
      players = new ArrayList(Game.getPlayerManager().getPlayersArrayList());
      statusArray = new JLabel[companies.size()+1][players.size()+1];
      
      this.setLayout(new GridLayout(companies.size()+1, players.size()+1));
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setOpaque(true);
      
      updateStatus();
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
            if(!selectedLabel.equals(label))
            {
               for(int i=0; i < statusArray.length; i++)
               {
                  for(int j=0; j < statusArray[0].length; j++)
                  {
                     selectedLabel.setBackground(Color.WHITE);
                  }
               }
            }
         }
         catch (NullPointerException e)
         {
         }
         
         label.setBackground(Color.YELLOW);
         selectedLabel = label;
      }
      else
      {
         label.setBackground(Color.WHITE);
         selectedLabel = null;
      }
   }
   
   public JLabel findLabel(JLabel label)
   {
      for(int i=0; i < statusArray.length; i++)
      {
         for(int j=0; j < statusArray[i].length; j++)
         {
            if(label.equals(statusArray[i][j]))
            {
               return statusArray[i][j];
            }
         }
      }
      
      return null;
   }
   
   public int[] findLabelPosition(JLabel label)
   {
      for(int i=0; i < statusArray.length; i++)
      {
         for(int j=0; j < statusArray[i].length; j++)
         {
            if(label.equals(statusArray[i][j]))
            {
               return new int[] {i, j};
            }
         }
      }
      
      return null;
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
    * @return Returns the selectedLabel.
    */
   public JLabel getSelectedLabel()
   {
      return selectedLabel;
   }
   /**
    * @param selectedLabel The selectedLabel to set.
    */
   public void setSelectedLabel(JLabel selectedLabel)
   {
      this.selectedLabel = selectedLabel;
   }
}

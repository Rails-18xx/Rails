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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import game.*;

public class CompanyStatus extends JPanel implements MouseListener
{
   private CompanyManager companyManager;
   private JLabel[] nameLabel;
   private JLabel[] parLabel;
   private JLabel[] cashLabel;
   private JLabel[] ipoLabel;
   private JLabel[] stockLabel;
   
   private ArrayList publicCompanies;
   private PublicCompany co;
   private StockSpace sp;
   
   private String companySelected;
   
   public void UpdateStatus()
   {
      
      this.add(new JLabel("Company:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         co = (PublicCompany) publicCompanies.get(i);
         
         nameLabel[i] = new JLabel();         
         nameLabel[i].setText(co.getName());
         nameLabel[i].setOpaque(true);
         nameLabel[i].setBackground(Color.WHITE);
         nameLabel[i].addMouseListener(this);
         
         this.add(nameLabel[i]);
      }
      
      this.add(new JLabel("Par Value:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         parLabel[i] = new JLabel();
         parLabel[i].setOpaque(true);
         parLabel[i].setBackground(Color.WHITE);
         
         co = (PublicCompany) publicCompanies.get(i);
         sp = (StockSpace) co.getParPrice();
         try
         {
            parLabel[i].setText(Integer.toString(sp.getPrice()));            
         }
         catch (NullPointerException e)
         {
            parLabel[i].setText("00");
         }
         
         this.add(parLabel[i]);
      }
      
      this.add(new JLabel("Treasury:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         cashLabel[i] = new JLabel();
         cashLabel[i].setOpaque(true);
         cashLabel[i].setBackground(Color.WHITE);
         
         co = (PublicCompany) publicCompanies.get(i);
         try
         {
            cashLabel[i].setText(Integer.toString(co.getCash()));   
         }
         catch (NullPointerException e)
         {
            cashLabel[i].setText("0");
         }
         
         this.add(cashLabel[i]);
      }     
   }
   public void RefreshStatus()
   {
      removeAll();
      UpdateStatus();
   }
   
   public CompanyStatus(CompanyManagerI cm, Bank bank)
   {   
      super();
      
      companyManager = (CompanyManager) cm;      
      publicCompanies = (ArrayList) cm.getAllPublicCompanies();
      
      this.setBackground(Color.WHITE);
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setPreferredSize(new Dimension(200,50));
      this.setLayout(new GridLayout(0,publicCompanies.size()+1));
      
      nameLabel = new JLabel[publicCompanies.size()];
      parLabel = new JLabel[publicCompanies.size()];    
      cashLabel = new JLabel[publicCompanies.size()];
      ipoLabel = new JLabel[publicCompanies.size()];
      stockLabel = new JLabel[publicCompanies.size()];
      
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
            if(!companySelected.equalsIgnoreCase(label.getText()))
            {
               for(int i=0; i < nameLabel.length; i++)
               {
                  nameLabel[i].setBackground(Color.WHITE);
               }
            }
         }
         catch (NullPointerException e)
         {
         }
         
         label.setBackground(Color.YELLOW);
         companySelected = label.getText();
      }
      else
      {
         label.setBackground(Color.WHITE);
         companySelected = null;
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
    * @return Returns the companySelected.
    */
   public String getCompanySelected()
   {
      return companySelected;
   }
   /**
    * @param companySelected The companySelected to set.
    */
   public void setCompanySelected(String companySelected)
   {
      this.companySelected = companySelected;
   }
}

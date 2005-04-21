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
import java.util.*;
import game.*;

public class CompanyStatus extends JPanel
{
   CompanyManager companyManager;
   JLabel[] nameLabel;
   JLabel[] parLabel;
   JLabel[] cashLabel;
   JLabel[] ipoLabel;
   JLabel[] stockLabel;
   
   ArrayList publicCompanies;
   
   public CompanyStatus(CompanyManagerI cm, Bank bank)
   {   
      companyManager = (CompanyManager) cm;      
      ArrayList publicCompanies = (ArrayList) cm.getAllPublicCompanies();
      
      this.setBackground(Color.WHITE);
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setPreferredSize(new Dimension(100,200));
      this.setLayout(new GridLayout(0,publicCompanies.size()+1));
      
      nameLabel = new JLabel[publicCompanies.size()];
      parLabel = new JLabel[publicCompanies.size()];    
      cashLabel = new JLabel[publicCompanies.size()];
      ipoLabel = new JLabel[publicCompanies.size()];
      stockLabel = new JLabel[publicCompanies.size()];
      
      this.add(new JLabel("Company:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         nameLabel[i] = new JLabel();
         PublicCompany co = (PublicCompany) publicCompanies.get(i);
         nameLabel[i].setText(co.getName());
         this.add(nameLabel[i]);
      }
      
      this.add(new JLabel("Par Value:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         parLabel[i] = new JLabel();
         PublicCompany co = (PublicCompany) publicCompanies.get(i);
         StockSpace sp = (StockSpace) co.getParPrice();
         try
         {
            parLabel[i].setText(Integer.toString(sp.getPrice()));   
         }
         catch (NullPointerException e)
         {
            parLabel[i].setText("0");
         }
         
         this.add(parLabel[i]);
      }
      
      this.add(new JLabel("Treasury:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         cashLabel[i] = new JLabel();
         PublicCompany co = (PublicCompany) publicCompanies.get(i);
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
}

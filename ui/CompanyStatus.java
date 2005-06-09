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

public class CompanyStatus extends JPanel implements ActionListener
{
   private CompanyManager companyManager;
   private JLabel[] nameLabel;
   private JLabel[] parLabel;
   private JLabel[] cashLabel;
   private JComponent[] ipoLabel;
   private JComponent[] stockLabel;
   
   private ArrayList publicCompanies;
   private PublicCompany co;
   private StockSpace sp;
   
   private String companySelected;
   private StockRound sr;
   private ButtonGroup buyButtons = new ButtonGroup();
   private Portfolio ipo, pool, buyFrom;
   
   public void updateStatus()
   {
       int share;
       sr  = StatusWindow.round;
       ipo = Bank.getIpo();
       pool = Bank.getPool();
       
      this.add(new JLabel("Company:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
          co = (PublicCompany) publicCompanies.get(i);
         
         nameLabel[i] = new MyLabel(co.getName());         
         
         this.add(nameLabel[i]);
      }
      
      this.add(new JLabel("Par Value:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         parLabel[i] = new MyLabel("");
         parLabel[i].setOpaque(true);

         try
         {
            parLabel[i].setText(Integer.toString(((PublicCompany)publicCompanies.get(i)).getParPrice().getPrice()));
         }
         catch (NullPointerException e)
         {
            parLabel[i].setText("");
         }
         parLabel[i].setBackground(Color.WHITE);
         this.add(parLabel[i]);
      }
      
      this.add(new JLabel("Treasury:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         cashLabel[i] = new MyLabel("");
         
         try
         {
            cashLabel[i].setText(Integer.toString(((PublicCompany)publicCompanies.get(i)).getCash()));   
         }
         catch (NullPointerException e)
         {
            cashLabel[i].setText("0");
         }
         
         cashLabel[i].setBackground(Color.WHITE);
         this.add(cashLabel[i]);
      }     
      
      this.add(new JLabel("IPO:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
         co = (PublicCompany) publicCompanies.get(i);
         share = ipo.ownsShare(co);

         if (sr != null && share > 0 && 
                 (co.hasStarted() && sr.isCompanyBuyable(co.getName(), ipo)
                         ||!co.hasStarted() && sr.isCompanyStartable(co.getName()))) {
             MyButton button = new MyButton (""+share, 0, i);
             ipoLabel[i] = button;
             button.setActionCommand("BuyIPO");
             button.addActionListener(this);
             button.setToolTipText("Click to buy from IPO");
             buyButtons.add(button);
         } else {
             ipoLabel[i] = new MyLabel(""+share, 0, i);
             ipoLabel[i].setBackground(Color.WHITE);
        }
         
         this.add(ipoLabel[i]);
      }
      
      this.add(new JLabel("Pool:"));
      for(int i = 0; i < publicCompanies.size(); i++)
      {
          co = (PublicCompany) publicCompanies.get(i);
          share = pool.ownsShare(co);
          if (sr != null && share > 0 && sr.isCompanyBuyable(co.getName(), pool)) {
              MyButton button = new MyButton (""+share, 1, i);
              stockLabel[i] = button;
              button.setActionCommand("BuyPool");
              button.addActionListener(this);
              button.setToolTipText("Click to buy from Bank Pool");
              buyButtons.add(button);
          } else {
             stockLabel[i] = new MyLabel(""+share, 1, i);
             stockLabel[i].setBackground(Color.WHITE);
          }
          
          this.add(stockLabel[i]);
      }
   }
   
   public void refreshPanel()
   {
      removeAll();
      updateStatus();
      super.repaint();
   }
   
   public CompanyStatus(CompanyManagerI cm, Bank bank)
   {   
      super();
      
      companyManager = (CompanyManager) cm;      
      publicCompanies = (ArrayList) cm.getAllPublicCompanies();
      
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setPreferredSize(new Dimension(200,50));
      this.setLayout(new GridLayout(0,publicCompanies.size()+1, 1, 1));
      
      nameLabel = new JLabel[publicCompanies.size()];
      parLabel = new JLabel[publicCompanies.size()];    
      cashLabel = new JLabel[publicCompanies.size()];
      ipoLabel = new JComponent[publicCompanies.size()];
      stockLabel = new JComponent[publicCompanies.size()];
      
      updateStatus();
   }
   
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
    */
   public void actionPerformed (ActionEvent action) {
  		companySelected = nameLabel[((MyButton) action.getSource()).getCol()].getText();
  		buyFrom = ((MyButton) action.getSource()).getRow() == 0 ? ipo : pool;
  		
  }
   
   /**
    * @return Returns the companySelected.
    */
   public String getCompanySelected()
   {
      return companySelected;
   }
   
   public Portfolio getFromPortfolio() {
       return buyFrom;
   }
   
}

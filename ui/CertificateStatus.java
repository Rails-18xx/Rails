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
public class CertificateStatus extends JPanel implements ActionListener
{
   private JComponent[][] statusArray;
   private ArrayList companies;
   private ArrayList players;
   private MyButton selectedLabel;
   private ButtonGroup sellGroup = new ButtonGroup();
   private String selectedCompany;
   private GridLayout layout;

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
                  statusArray[i][j] = new MyLabel(((PublicCompany)companies.get(i-1)).getName());
            }
            else if (i==0)
            {
                  statusArray[i][j] = new MyLabel(((Player)players.get(j-1)).getName());
            }
            else
            {
            	/*
               statusArray[i][j] = new MyButton(Integer.toString(((Player)players.get(j-1)).getPortfolio().ownsShare((PublicCompany)companies.get(i-1))));               
               statusArray[i][j].addMouseListener(this);
               statusArray[i][j].setBackground(Color.WHITE);
               */
            	int share = ((Player)players.get(j-1)).getPortfolio().ownsShare((PublicCompany)companies.get(i-1));
            	if (share == 0 || players.get(j-1) != GameManager.getCurrentPlayer()) {
            		statusArray[i][j] = new MyLabel(Integer.toString(share), i-1, j-1);
            		statusArray[i][j].setBackground(Color.WHITE);
            	} else {
            		MyButton button = new MyButton (Integer.toString(share), i-1, j-1);
            		statusArray[i][j] = button;
            		sellGroup.add(button);
            		button.setToolTipText("Click to sell");
            		button.setActionCommand("SelectForSell");
            		button.addActionListener (this);
             	}
            	            	
            }
                        
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
      statusArray = new JComponent[companies.size()+1][players.size()+1];
      
      this.layout = new GridLayout(companies.size()+1, players.size()+1, 1, 1);
      this.setLayout(layout);
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setOpaque(true);
           
      updateStatus();
   }
 
   public void actionPerformed (ActionEvent action) {
   		selectedLabel = (MyButton) action.getSource();
   		selectedCompany = ((PublicCompanyI)companies.get(selectedLabel.getRow())).getName();
   	}
   
   public int[] findLabelPosition(MyButton label)
   {
       return new int[] {label.getRow(), label.getCol()};
   }
   
   /**
    * @return Returns the selectedLabel.
    */
   public MyButton getSelectedLabel()
   {
      return selectedLabel;
   }
   /**
    * @param selectedLabel The selectedLabel to set.
    */
   public void setSelectedLabel(MyButton selectedLabel)
   {
      this.selectedLabel = selectedLabel;
   }
   
   public String getSelectedCompany () {
       return selectedCompany;
   }
   
}

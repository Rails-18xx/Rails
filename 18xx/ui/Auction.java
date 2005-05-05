/*
 * Created on May 5, 2005
 */
package ui;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import game.*;

/**
 * @author blentz
 */
public class Auction extends JDialog
{
   private ArrayList privates;
   private JLabel[] privateLabel, priceLabel;
   private JButton bidButton;
   private JTextField bidAmountField;
   private JPanel bidPanel, buttonPanel;
   private PlayerStatus playerStatus;
   
   public Auction()
   {
      privates = (ArrayList) Game.getCompanyManager().getAllPrivateCompanies();
      
      playerStatus = new PlayerStatus();
      
      bidButton = new JButton("Bid");
      bidAmountField = new JTextField("");
      bidPanel = new JPanel();
      buttonPanel = new JPanel();
      
      bidAmountField.setPreferredSize(new Dimension(75, 25));
      
      bidPanel.setLayout(new GridLayout(privates.size()+1, 2));
      
      privateLabel = new JLabel[privates.size()];
      priceLabel = new JLabel[privates.size()];
      
      bidPanel.setBorder(BorderFactory.createEtchedBorder());
      bidPanel.add(new JLabel("Private"));
      bidPanel.add(new JLabel("Price"));
      for(int i=0; i < privates.size(); i++)
      {
         privateLabel[i] = new JLabel(((PrivateCompany)privates.get(i)).getName());
         priceLabel[i] = new JLabel(Integer.toString(((PrivateCompany)privates.get(i)).getBasePrice()));
         
         privateLabel[i].setBackground(Color.WHITE);
         priceLabel[i].setBackground(Color.WHITE);
         privateLabel[i].setOpaque(true);
         priceLabel[i].setOpaque(true);
         
         bidPanel.add(privateLabel[i]);
         bidPanel.add(priceLabel[i]);
      }
      
      buttonPanel.add(bidButton);
      buttonPanel.add(bidAmountField);
      buttonPanel.setBorder(BorderFactory.createLoweredBevelBorder());

      setLayout(new GridLayout(3,1));
      add(playerStatus);
      add(bidPanel);
      add(buttonPanel);
      
      pack();
      setVisible(true);
   }
}

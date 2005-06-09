/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * @author blentz
 */
public class StatusWindow extends JFrame implements ActionListener
{
   private CertificateStatus certStatus;
   private CompanyStatus companyStatus;
   private PlayerStatus playerStatus;
   private JPanel buttonPanel;
   private JButton buyButton, sellButton, doneButton;
   private Player player;
   private PublicCompanyI company;
   private CompanyManagerI cm;
   String companyName;
   
   /*----*/
   public static StockRound round;
   
   
   public StatusWindow ()
   {
       cm = Game.getCompanyManager();
      companyStatus = new CompanyStatus(Game.getCompanyManager(), Game.getBank());
      playerStatus  = new PlayerStatus();
      certStatus = new CertificateStatus();
      buttonPanel = new JPanel();      
      
      buyButton = new JButton("Buy");
      sellButton = new JButton("Sell");
      doneButton = new JButton("Pass");
      
      buttonPanel.add(buyButton);
      buttonPanel.add(sellButton);
      buttonPanel.add(doneButton);
      
      buyButton.setActionCommand("buy");
      sellButton.setActionCommand("sell");
      doneButton.setActionCommand("done");
      
      buyButton.addActionListener(this);
      sellButton.addActionListener(this);
      doneButton.addActionListener(this);

      updateStatus();
      setSize(800,300);
      setLocation(0,450);
      getContentPane().setLayout(new GridLayout(2,0));
      setTitle("Rails: Game Status");
      setVisible(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      /*----*/
      round = new StockRound();
      playerStatus.setPlayerSelected(GameManager.getCurrentPlayer().getName());
      refreshStatus();

   
   }

   public void updateStatus()
   {
      this.getContentPane().add(companyStatus);
      this.getContentPane().add(playerStatus);	
      this.getContentPane().add(certStatus);
      this.getContentPane().add(buttonPanel);
   }
   public void refreshStatus()
   {
      companyStatus.refreshPanel();
      playerStatus.refreshPanel();
      certStatus.refreshPanel();
      updateStatus();
      //FIXME: Not an ideal fix for various repainting issues, but it works well enough for now.
      this.pack();
      System.out.println("StatusWindow Dimensions: " + this.getWidth() + ", " + this.getHeight());
   }
   
   public void repaint()
   {
      super.repaint();
      refreshStatus();
   }
   
   /* (non-Javadoc)
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   public void actionPerformed(ActionEvent arg0)
   {
       player = GameManager.getCurrentPlayer();
       
      if (arg0.getActionCommand().equalsIgnoreCase("buy"))
      {
         buyButtonClicked();
         doneButton.setText("Done");
      }
      else if (arg0.getActionCommand().equalsIgnoreCase("sell"))
      {
         sellButtonClicked();
         doneButton.setText("Done");
         
      } else if (arg0.getActionCommand().equalsIgnoreCase("done")) {
      	
      	round.done(playerStatus.getPlayerSelected());
      	playerStatus.setPlayerSelected(GameManager.getCurrentPlayer().getName());
      	doneButton.setText("Pass");
      }
      repaint();
      
  }

   private void buyButtonClicked()
   {
      companyName = companyStatus.getCompanySelected();
      if(companyName != null)
      {
      company = cm.getPublicCompany(companyName);

      if (company.hasStarted()) {
          if (!round.buyShare(player.getName(), companyStatus.getFromPortfolio(), companyName, 1)) {
              JOptionPane.showMessageDialog(this,Log.getErrorBuffer(), "", JOptionPane.OK_OPTION);
          }
      } else {
      	startCompany();
      }
      } else {
          JOptionPane.showMessageDialog(this,"Unable to buy share.\r\n" +
					"You must select a company first.", 
						"No share bought.", JOptionPane.OK_OPTION);
         
      }
   }
   
   private void sellButtonClicked()
   {
       String companyName = certStatus.getSelectedCompany();
     
       if(companyName != null)
       {
         if(!round.sellShare(player.getName(), companyName)) {
            JOptionPane.showMessageDialog(this, Log.getErrorBuffer());
         }
      	StockChart.refreshStockPanel();
       } else {
           JOptionPane.showMessageDialog(this,"Unable to sell share.\r\n" +
					"You must select a company first.", 
						"Share not sold.", JOptionPane.OK_OPTION);
           
       }
    }
      
   private void startCompany()
   {
      StockMarket stockMarket = (StockMarket) Game.getStockMarket();
      
      if(companyName != null)
      {
          company = cm.getPublicCompany(companyName);
         StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this, "Start company at what price?", 
               						"What Price?", 
               						JOptionPane.INFORMATION_MESSAGE,
               						null,
               						stockMarket.getStartSpaces().toArray(),
               						stockMarket.getStartSpaces().get(0));
         
         //FIXME: Probably should check the boolean startCompany() returns
         /*
         PublicCompany.startCompany(playerStatus.getPlayerSelected(), companyStatus.getCompanySelected(), sp);
         */
         if (!round.startCompany(player.getName(), company.getName(), sp.getPrice())) {
          JOptionPane.showMessageDialog(this,Log.getErrorBuffer(), "", JOptionPane.OK_OPTION);
         }
        StockChart.refreshStockPanel();
      }
      else
         JOptionPane.showMessageDialog(this,"Unable to start company.\r\n" +
         							"You must select a company first.", 
               						"Company not started.", JOptionPane.OK_OPTION);
   }
}

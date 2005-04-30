/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @author blentz
 */
public class StatusWindow extends JFrame implements ActionListener
{
   private static CertificateStatus certStatus;
   private static CompanyStatus companyStatus;
   private static PlayerStatus playerStatus;
   private JPanel buttonPanel;
   private JButton upButton, downButton, leftButton, rightButton, startCoButton, buyButton;
   
   public StatusWindow ()
   {
      companyStatus = new CompanyStatus(Game.getCompanyManager(), Game.getBank());
      playerStatus  = new PlayerStatus();
      certStatus = new CertificateStatus();
      
      this.getContentPane().setLayout(new GridLayout(2,2));
      
      buttonPanel = new JPanel();
      buttonPanel.setLayout(new FlowLayout());

      upButton = new JButton("up");
      downButton = new JButton("down");
      leftButton = new JButton("left");
      rightButton = new JButton("right");
      startCoButton = new JButton("Start Company");
      buyButton = new JButton("Buy");

      buttonPanel.add(upButton);
      buttonPanel.add(downButton);
      buttonPanel.add(leftButton);
      buttonPanel.add(rightButton);
      buttonPanel.add(startCoButton);
      buttonPanel.add(buyButton);
      
      upButton.setActionCommand("up");
      downButton.setActionCommand("down");
      leftButton.setActionCommand("left");
      rightButton.setActionCommand("right");
      startCoButton.setActionCommand("startCo");
      buyButton.setActionCommand("buy");
      
      upButton.addActionListener(this);
      downButton.addActionListener(this);
      leftButton.addActionListener(this);
      rightButton.addActionListener(this);
      startCoButton.addActionListener(this);
      buyButton.addActionListener(this);

      this.getContentPane().add(companyStatus);
      this.getContentPane().add(playerStatus);
      this.getContentPane().add(certStatus);
      this.getContentPane().add(buttonPanel);
      
      this.pack();
      this.setLocation(0,300);
      this.setVisible(true);
   }
   
   public void refreshStatusWindow()
   {
      playerStatus.refreshStatus();
      companyStatus.refreshStatus();
      certStatus.refreshStatus();
   }

   /* (non-Javadoc)
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   public void actionPerformed(ActionEvent arg0)
   {
      try
      {
         String companySelected = companyStatus.getCompanySelected();
         CompanyManager cm = (CompanyManager) Game.getCompanyManager();
         PublicCompany co = (PublicCompany) cm.getPublicCompany(companySelected);
         StockMarket stockMarket = (StockMarket) Game.getStockMarket();
      
         if(arg0.getActionCommand().equalsIgnoreCase("down"))
         {
            stockMarket.sell((PublicCompanyI) co, 1);
            StockChart.refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("left"))
         {
            stockMarket.withhold((PublicCompanyI) co);
            StockChart.refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("up"))
         {
            stockMarket.soldOut((PublicCompanyI) co);
            StockChart.refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("right"))
         {
            stockMarket.payOut((PublicCompanyI) co);
            StockChart.refreshStockPanel();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("startCo"))
         {
            startCompany();
         }
         else if (arg0.getActionCommand().equalsIgnoreCase("buy"))
         {
            JOptionPane.showMessageDialog(this,"This button doesn't work yet.");
            return;
            /*
            int[] x = certStatus.findLabelPosition(certStatus.getSelectedLabel());
            Player p = (Player) Game.getPlayerManager().getPlayersArrayList().get(x[0]-1);
            PublicCompany c = (PublicCompany) Game.getCompanyManager().getAllPublicCompanies().get(x[1]-1);

            try
            {
               p.buyShare((Certificate)c.getCertificates().get(0));
            }
            catch(NullPointerException e)
            {
               startCompany();
            }
            */
         }
      }
      catch (NullPointerException e)
      {   
         JOptionPane.showMessageDialog(this, "Unable to move selected company's token.");
         e.printStackTrace();
      } 
   }
   
   private void startCompany()
   {
      String companySelected = companyStatus.getCompanySelected();
      CompanyManager cm = (CompanyManager) Game.getCompanyManager();
      StockMarket stockMarket = (StockMarket) Game.getStockMarket();
      PublicCompany co = (PublicCompany) cm.getPublicCompany(companySelected);
      
      if(companyStatus.getCompanySelected() != null && playerStatus.getPlayerSelected() != null)
      {
         StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this, "Start company at what price?", 
               						"What Price?", 
               						JOptionPane.INFORMATION_MESSAGE,
               						null,
               						stockMarket.getStartSpaces().toArray(),
               						stockMarket.getStartSpaces().get(0));
         
         PublicCompany.startCompany(playerStatus.getPlayerSelected(), companyStatus.getCompanySelected(), sp);
         
         Player player = Game.getPlayerManager().getPlayerByName(playerStatus.getPlayerSelected());
         
         //Buy Share doesn't completely work yet...
         player.buyShare((Certificate)co.getCertificates().get(0));
         
         companyStatus.setCompanySelected(null);
         playerStatus.setPlayerSelected(null);
         StockChart.refreshStockPanel();
      }
      else
         JOptionPane.showMessageDialog(this,"Unable to start company.\r\nYou must select a player and a company first.", 
               						"Company not started.", JOptionPane.OK_OPTION);
   }
}

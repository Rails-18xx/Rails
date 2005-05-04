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
   private JButton buyButton, sellButton;
   private Player player;
   private PublicCompany company;
   
   public StatusWindow ()
   {
      companyStatus = new CompanyStatus(Game.getCompanyManager(), Game.getBank());
      playerStatus  = new PlayerStatus();
      certStatus = new CertificateStatus();
      buttonPanel = new JPanel();      
      
      buyButton = new JButton("Buy");
      sellButton = new JButton("Sell");
      
      buttonPanel.add(buyButton);
      buttonPanel.add(sellButton);
      
      buyButton.setActionCommand("buy");
      sellButton.setActionCommand("sell");     
      
      buyButton.addActionListener(this);
      sellButton.addActionListener(this);

      updateStatus();
      setSize(800,300);
      setLocation(0,450);
      getContentPane().setLayout(new GridLayout(2,0));
      setTitle("Rails: Game Status");
      setVisible(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
      if (arg0.getActionCommand().equalsIgnoreCase("buy"))
      {
         buyButtonClicked();
      }
      else if (arg0.getActionCommand().equalsIgnoreCase("sell"))
      {
         sellButtonClicked();
      }
   }

   private void setSelectedPlayerAndCompany()
   {
      if(playerStatus.getPlayerSelected() == null && companyStatus.getCompanySelected() == null)
      {
         int[] x = certStatus.findLabelPosition(certStatus.getSelectedLabel());
         player = (Player) Game.getPlayerManager().getPlayersArrayList().get(x[1]-1);
         company = (PublicCompany) Game.getCompanyManager().getAllPublicCompanies().get(x[0]-1);
      }
      else
      {
         player = Game.getPlayerManager().getPlayerByName(playerStatus.getPlayerSelected());
         company = (PublicCompany) Game.getCompanyManager().getPublicCompany(companyStatus.getCompanySelected());
      }
      
      companyStatus.setCompanySelected(company.getName());
      playerStatus.setPlayerSelected(player.getName());
   }
   
   private void buyButtonClicked()
   {
      setSelectedPlayerAndCompany();

      /*
      if(player.hasBoughtStockThisTurn())
      {
         JOptionPane.showMessageDialog(this, "Player has already bought stock this turn.");
         return;
      }
      */
      
      try //Misusing Try/Catch to provide an If/Else condition through the abuse of exceptions.
      {
         player.buyShare((Certificate)company.getNextAvailableCertificate());
      }
      catch(NullPointerException e)
      {
         startCompany();
      }
      
      playerStatus.setPlayerSelected(null);
      companyStatus.setCompanySelected(null);
      repaint();
   }
   
   private void sellButtonClicked()
   {
      setSelectedPlayerAndCompany();
      
      ArrayList certs = (ArrayList) player.getPortfolio().getCertificatesPerCompany(company.getName());
      
      try
      {
         //Just sell the last cert in the stack first.
         if(!((Certificate)certs.get(certs.size()-1)).isPresident())
            player.sellShare((Certificate)certs.get(certs.size()-1));
         else
            JOptionPane.showMessageDialog(this, "You can't sell the President's share.");
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
         JOptionPane.showMessageDialog(this, "You have no shares of this company to sell");
      }
      
      playerStatus.setPlayerSelected(null);
      companyStatus.setCompanySelected(null);
      StockChart.refreshStockPanel();
      repaint();
   }
      
   private void startCompany()
   {
      StockMarket stockMarket = (StockMarket) Game.getStockMarket();
      
      if(companyStatus.getCompanySelected() != null && playerStatus.getPlayerSelected() != null)
      {
         StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this, "Start company at what price?", 
               						"What Price?", 
               						JOptionPane.INFORMATION_MESSAGE,
               						null,
               						stockMarket.getStartSpaces().toArray(),
               						stockMarket.getStartSpaces().get(0));
         
         //FIXME: Probably should check the boolean startCompany() returns
         PublicCompany.startCompany(playerStatus.getPlayerSelected(), companyStatus.getCompanySelected(), sp);
         StockChart.refreshStockPanel();
      }
      else
         JOptionPane.showMessageDialog(this,"Unable to start company.\r\n" +
         							"You must select a player and a company first.", 
               						"Company not started.", JOptionPane.OK_OPTION);
   }
}

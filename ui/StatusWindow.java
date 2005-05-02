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
   private CertificateStatus certStatus;
   private CompanyStatus companyStatus;
   private PlayerStatus playerStatus;
   private JPanel buttonPanel;
   private JButton buyButton;
   
   public StatusWindow ()
   {
      companyStatus = new CompanyStatus(Game.getCompanyManager(), Game.getBank());
      playerStatus  = new PlayerStatus();
      certStatus = new CertificateStatus();
      buttonPanel = new JPanel();      
      
      buyButton = new JButton("Buy");
      
      buttonPanel.add(buyButton);
      
      buyButton.setActionCommand("buy");
      
      buyButton.addActionListener(this);

      updateStatus();
      setSize(600,400);
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
      //FIXME: Not an ideal fix for various repainting issues, but it works.
      this.pack();
      System.out.println(this.getWidth() + ", " + this.getHeight());
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
      try
      {
         String companySelected = companyStatus.getCompanySelected();
         CompanyManager cm = (CompanyManager) Game.getCompanyManager();
         PublicCompany co = (PublicCompany) cm.getPublicCompany(companySelected);
         StockMarket stockMarket = (StockMarket) Game.getStockMarket();
      
         if (arg0.getActionCommand().equalsIgnoreCase("buy"))
         {
            Player p;
            PublicCompany c;
            int[] x;
            
            if(playerStatus.getPlayerSelected() == null && companyStatus.getCompanySelected() == null)
            {
               x = certStatus.findLabelPosition(certStatus.getSelectedLabel());
               p = (Player) Game.getPlayerManager().getPlayersArrayList().get(x[1]-1);
               c = (PublicCompany) Game.getCompanyManager().getAllPublicCompanies().get(x[0]-1);
            }
            else
            {
               p = Game.getPlayerManager().getPlayerByName(playerStatus.getPlayerSelected());
               c = (PublicCompany) Game.getCompanyManager().getPublicCompany(companyStatus.getCompanySelected());
            }

            if(p.hasBoughtStockThisTurn())
            {
               JOptionPane.showMessageDialog(this, "Player has already bought stock this turn.");
               return;
            }
            
            try
            {
               p.buyShare((Certificate)c.getPortfolio().getNextAvailableCertificate(c));
            }
            catch(NullPointerException e)
            {
               companyStatus.setCompanySelected(c.getName());
               playerStatus.setPlayerSelected(p.getName());
               startCompany();
            }
            
            playerStatus.setPlayerSelected(null);
            companyStatus.setCompanySelected(null);
            repaint();
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
         
         player.buyShare((Certificate)co.getCertificates().get(0));
         StockChart.refreshStockPanel();
      }
      else
         JOptionPane.showMessageDialog(this,"Unable to start company.\r\n" +
         							"You must select a player and a company first.", 
               						"Company not started.", JOptionPane.OK_OPTION);
   }
}

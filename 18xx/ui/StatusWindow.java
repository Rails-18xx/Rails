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
   private JPanel buttonPanel;
   private GameStatus gameStatus;
   private JButton buyButton, sellButton, doneButton;
   private Player player;
   private PublicCompanyI[] companies;
   private PublicCompanyI company;
   private CompanyManagerI cm;
   private Portfolio ipo, pool;
   private int compIndex, playerIndex;

   /*----*/
   private GameManager gmgr;
   private Round currentRound;
   private StockRound stockRound;
   private StartRound startRound;
   private StartRoundWindow startRoundWindow;
   private OperatingRound operatingRound;
   private ORWindow orWindow;
   private int np = GameManager.getNumberOfPlayers();
   private int nc;

   JPanel pane = new JPanel(new BorderLayout());

   public StatusWindow()
   {
      cm = Game.getCompanyManager();
      companies = (PublicCompanyI[]) cm.getAllPublicCompanies().toArray(
            new PublicCompanyI[0]);
      ipo = Bank.getIpo();
      pool = Bank.getPool();

      gameStatus = new GameStatus(this);
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

      //updateStatus();
      setSize(800, 300);
      setLocation(25, 450);

      buttonPanel.setBorder(BorderFactory.createEtchedBorder());
      buttonPanel.setOpaque(false);

      setTitle("Rails: Game Status");
      pane.setLayout(new BorderLayout());
      //pane.setPreferredSize(new Dimension (800, 300));
      init();
      pane.add(gameStatus, BorderLayout.NORTH);
      pane.add(buttonPanel, BorderLayout.CENTER);
      pane.setOpaque(true);
      setContentPane(pane);
      refreshStatus();
      //messagePanel.setMinimumSize(new Dimension(0, 80));
      setVisible(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      /*----*/
      gmgr = GameManager.getInstance();
      currentRound = gmgr.getCurrentRound();
      updateStatus();
      pack();
   }

   private void init()
   {
      PublicCompanyI[] companies = (PublicCompanyI[]) Game.getCompanyManager()
            .getAllPublicCompanies().toArray(new PublicCompanyI[0]);
      nc = companies.length;
   }

   public void updateStatus()
   {
      if (currentRound instanceof StartRound)
      {

         doneButton.setEnabled(false);
         startRound = (StartRound) currentRound;
         startRoundWindow = new StartRoundWindow(startRound, this);

         startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());

      }
      else if (currentRound instanceof StockRound)
      {

         doneButton.setEnabled(true);
         stockRound = (StockRound) currentRound;
         gameStatus.setSRPlayerTurn(GameManager.getCurrentPlayerIndex());
         refreshStatus();

      }
      else if (currentRound instanceof OperatingRound)
      {

         doneButton.setEnabled(false);
         operatingRound = (OperatingRound) currentRound;
         orWindow = new ORWindow(operatingRound, this);

      }

   }

   public void resume(JFrame previous)
   {
      this.requestFocus();
      if (previous instanceof StartRoundWindow)
         startRoundWindow = null;
      else if (previous instanceof ORWindow)
         orWindow = null;
      currentRound = GameManager.getInstance().getCurrentRound();
      updateStatus();
   }

   public void refreshStatus()
   {
      gameStatus.repaint();
      //FIXME: Not an ideal fix for various repainting issues, but it works
      // well enough for now.
      this.pack();
   }

   public void repaint()
   {
      super.repaint();
      refreshStatus();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   public void actionPerformed(ActionEvent actor)
   {
      player = GameManager.getCurrentPlayer();

      if (actor.getActionCommand().equalsIgnoreCase("buy"))
      {
         buyButtonClicked();
         doneButton.setText("Done");
      }
      else if (actor.getActionCommand().equalsIgnoreCase("sell"))
      {
         sellButtonClicked();
         doneButton.setText("Done");

      }
      else if (actor.getActionCommand().equalsIgnoreCase("done"))
      {

         stockRound.done(gameStatus.getSRPlayer());
         doneButton.setText("Pass");
      }
      LogWindow.addLog();
      pack();

      currentRound = GameManager.getInstance().getCurrentRound();
      if (currentRound instanceof StockRound)
      {

         gameStatus.setSRPlayerTurn(GameManager.getCurrentPlayerIndex());

      }
      else if (currentRound instanceof OperatingRound)
      {

         gameStatus.setSRPlayerTurn(-1);
         updateStatus();

      }

   }

   private void buyButtonClicked()
   {
      playerIndex = GameManager.getCurrentPlayerIndex();

      if ((compIndex = gameStatus.getCompIndexToBuyFromIPO()) >= 0)
      {
         company = companies[compIndex];
         if (company.hasStarted())
         {
            if (!stockRound.buyShare(player.getName(), ipo, company.getName(),
                  1))
            {
               JOptionPane.showMessageDialog(this, Log.getErrorBuffer(), "",
                     JOptionPane.OK_OPTION);
            }
            else
            {
               gameStatus.updatePlayer(compIndex, playerIndex);
               gameStatus.updateIPO(compIndex);
            }
         }
         else
         {
            startCompany();
         }
         if (company.hasFloated())
            gameStatus.updateCompany(compIndex);

      }
      else if ((compIndex = gameStatus.getCompIndexToBuyFromPool()) >= 0)
      {
         company = companies[compIndex];
         if (company.hasStarted())
         {
            if (!stockRound.buyShare(player.getName(), pool, company.getName(),
                  1))
            {
               JOptionPane.showMessageDialog(this, Log.getErrorBuffer(), "",
                     JOptionPane.OK_OPTION);
            }
            else
            {
               gameStatus.updatePlayer(compIndex, playerIndex);
               gameStatus.updatePool(compIndex);
               gameStatus.updateBank();
            }
         }

      }
      else
      {
         JOptionPane.showMessageDialog(this, "Unable to buy share.\r\n"
               + "You must select a company first.", "No share bought.",
               JOptionPane.OK_OPTION);

      }
   }

   private void sellButtonClicked()
   {
      int compIndex;
      int playerIndex = GameManager.getCurrentPlayerIndex();
      if ((compIndex = gameStatus.getCompIndexToSell()) >= 0)
      {
         company = companies[compIndex];
         if (!stockRound.sellShare(player.getName(), company.getName()))
         {
            JOptionPane.showMessageDialog(this, Log.getErrorBuffer());
         }
         else
         {
            gameStatus.updatePlayer(compIndex, playerIndex);
            gameStatus.updatePool(compIndex);
            gameStatus.updateBank();
            StockChart.refreshStockPanel();
         }
      }
      else
      {
         JOptionPane.showMessageDialog(this, "Unable to sell share.\r\n"
               + "You must select a company first.", "Share not sold.",
               JOptionPane.OK_OPTION);

      }
   }

   private void startCompany()
   {
      StockMarket stockMarket = (StockMarket) Game.getStockMarket();

      if (company != null)
      {
         StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this,
               "Start company at what price?", "What Price?",
               JOptionPane.INFORMATION_MESSAGE, null, stockMarket
                     .getStartSpaces().toArray(), stockMarket.getStartSpaces()
                     .get(0));
         //repaint();
         //FIXME: Probably should check the boolean startCompany() returns
         //PublicCompany.startCompany(playerStatus.getPlayerSelected(),
         // companyStatus.getCompanySelected(), sp);
         if (!stockRound.startCompany(player.getName(), company.getName(), sp
               .getPrice()))
         {
            JOptionPane.showMessageDialog(this, Log.getErrorBuffer(), "",
                  JOptionPane.OK_OPTION);
         }
         else
         {

            gameStatus.updatePlayer(compIndex, playerIndex);
            gameStatus.updateIPO(compIndex);
            gameStatus.updateBank();
            StockChart.refreshStockPanel();
         }
      }
      else
         JOptionPane.showMessageDialog(this, "Unable to start company.\r\n"
               + "You must select a company first.", "Company not started.",
               JOptionPane.OK_OPTION);

   }

   public void enableBuyButton(boolean enable)
   {
      buyButton.setEnabled(enable);
      if (enable)
         sellButton.setEnabled(!enable);
   }

   public void enableSellButton(boolean enable)
   {
      sellButton.setEnabled(enable);
      if (enable)
         buyButton.setEnabled(!enable);
   }

   public GameStatus getGameStatus()
   {
      return gameStatus;
   }
}

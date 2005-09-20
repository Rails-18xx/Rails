/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;
import ui.elements.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @author blentz
 */
public class StartRoundWindow extends JFrame implements ActionListener
{
   private static final int NARROW_GAP = 1;
   private static final int WIDE_GAP = 3;
   private static final int WIDE_LEFT = 1;
   private static final int WIDE_RIGHT = 2;
   private static final int WIDE_TOP = 4;
   private static final int WIDE_BOTTOM = 8;
   
   private static StartRoundWindow startRoundPanel;
   private JPanel statusPanel;
   private JPanel buttonPanel;

    //public static StockRound round;
   
   private GridBagLayout gb;
   private GridBagConstraints gbc;
   //private Color buttonColour = new Color (255, 220, 150);
   //private Color buttonHighlight = new Color (255, 160, 80);
   //private Insets buttonInsets = new Insets (0, 1, 0, 1);
   //private Color captionColour = new Color (240, 240, 240);
   //private Color highlightColour = new Color (255, 255, 80);
   //private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
   
   
   // Grid elements per function
   private Caption itemName[];
   private ClickField itemNameButton[];
   private int itemNameXOffset, itemNameYOffset;
   private Field basePrice[];
   private int basePriceXOffset, basePriceYOffset;
   private Field minBid[];
   private int minBidXOffset, minBidYOffset;
   private Field bidPerPlayer[][];
   private int bidPerPlayerXOffset, bidPerPlayerYOffset;
   private Field playerBids[];
   private int playerBidsXOffset, playerBidsYOffset;
   private Field playerFree[];
   private int playerFreeXOffset, playerFreeYOffset;
   
   private Caption[] upperPlayerCaption;
   private Caption[] lowerPlayerCaption;
   
   private JButton bid5Button;
   private JButton bidButton;
   private JButton buyButton;
   private JSpinner bidAmount;
   private SpinnerNumberModel spinnerModel;
   private JButton passButton;

   private int np; // Number of players
   private int ni; // Number of start items
   private Player[] players;
   private StartItem[] items;
   private StartPacket packet;
   private StartRoundI round;
   private StatusWindow statusWindow;
    
   private Player p;
   private StartItem si;
   private JComponent f;
   
   // Current state
   private int playerIndex = -1;
   private int itemIndex = -1;
   
   private ButtonGroup itemGroup = new ButtonGroup();
   private ClickField dummyButton; // To be selected if none else is.
   
   public StartRoundWindow (StartRound round, StatusWindow parent)
   {
   	  super ();
   	  this.round = round;
   	  statusWindow = parent;
   	  startRoundPanel = this;
 	  setTitle("Rails: Start Round");
 	  getContentPane().setLayout(new BorderLayout());
 	  //UIManager.put("ToggleButton.select", buttonHighlight);
 	  
 	  statusPanel = new JPanel();
 	  gb = new GridBagLayout();
 	  statusPanel.setLayout(gb);
      statusPanel.setBorder(BorderFactory.createEtchedBorder());
      statusPanel.setOpaque(true);
   	  
   	  buttonPanel = new JPanel();
   	  
   	  buyButton = new JButton("Buy");
   	  buyButton.setActionCommand("Buy");
   	  buyButton.addActionListener(this);
   	  buyButton.setEnabled(false);
   	  buttonPanel.add(buyButton);

   	  bidButton = new JButton("Bid:");
   	  bidButton.setActionCommand("Bid");
   	  bidButton.addActionListener(this);
   	  bidButton.setEnabled(false);
   	  buttonPanel.add(bidButton);

   	  spinnerModel = new SpinnerNumberModel (new Integer(999), new Integer(0), null, new Integer(1));
   	  bidAmount = new JSpinner(spinnerModel);
   	  bidAmount.setPreferredSize(new Dimension(50, 28));
   	  bidAmount.setEnabled(false);
   	  buttonPanel.add (bidAmount);
   	  
  	  passButton = new JButton("Pass");
   	  passButton.setActionCommand("Pass");
   	  passButton.addActionListener(this);
   	  passButton.setEnabled(true);
   	  buttonPanel.add(passButton);

   	  buttonPanel.setOpaque(true);
   	  
      gbc = new GridBagConstraints();

      players = Game.getPlayerManager().getPlayersArray();
      np = GameManager.getNumberOfPlayers();
	  packet = round.getStartPacket();
	  items = (StartItem[]) packet.getItems().toArray(new StartItem[0]);
	  ni = items.length;
      
      init();

   	  getContentPane().add(statusPanel, BorderLayout.NORTH);
   	  getContentPane().add(buttonPanel, BorderLayout.SOUTH);
   	  setTitle ("Start Round");
      setLocation(600, 150);
      setSize(400, 500);
      pack();
      setVisible(true);
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      
      LogWindow.addLog();

   }
   
   private void init () {
   	
        itemName = new Caption[ni];
        itemNameButton = new ClickField[ni];
        basePrice = new Field[ni];
        minBid = new Field[ni];
	    bidPerPlayer = new Field[ni][np];
	    upperPlayerCaption = new Caption[np];
	    lowerPlayerCaption = new Caption[np];
	    playerBids = new Field[np];
	    playerFree = new Field[np];

	    itemNameXOffset = 0;
	    itemNameYOffset = 2;
	    basePriceXOffset = 1;
	    basePriceYOffset = 2;
	    minBidXOffset = 2;
	    minBidYOffset = 2;
        bidPerPlayerXOffset = 3;
        bidPerPlayerYOffset = 2;
        playerBidsXOffset = 3;
        playerBidsYOffset = ni+2;
        playerFreeXOffset = 3;
        playerFreeYOffset = ni+3;
       
        addField (new Caption("Item"), 0, 0, 1, 2, WIDE_RIGHT+WIDE_BOTTOM);
        addField (new Caption("<html>Base<br>Price</html>"), basePriceXOffset, 0, 1, 2, WIDE_BOTTOM);
        addField (new Caption("<html>Min.<br>Bid</html>"), minBidXOffset, 0, 1, 2, WIDE_BOTTOM+WIDE_RIGHT);
        addField (new Caption("Players"), bidPerPlayerXOffset, 0, np, 1, 0);
        for (int i=0; i<np; i++) {
        	f = upperPlayerCaption[i] = new Caption(players[i].getName());
            addField (f, bidPerPlayerXOffset+i, 1, 1, 1, WIDE_BOTTOM);
        }
        
        for (int i=0; i<ni; i++) {
            si = items[i];
            f = itemName[i] = new Caption(si.getName());
            addField (f, itemNameXOffset, itemNameYOffset+i, 1, 1, WIDE_RIGHT);
            f = itemNameButton[i] = new ClickField(si.getName(), "", "", this, itemGroup);
            addField (f, itemNameXOffset, itemNameYOffset+i, 1, 1, WIDE_RIGHT);
            
            f = basePrice[i] = new Field(Bank.format(si.getBasePrice()));
            addField (f, basePriceXOffset, basePriceYOffset+i, 1, 1, 0);
            
            f = minBid[i] = new Field("");
            addField (f, minBidXOffset, minBidYOffset+i, 1, 1, WIDE_RIGHT);
            
             for (int j=0; j<np; j++) {
             	f = bidPerPlayer[i][j] = new Field(""); 
                addField (f, bidPerPlayerXOffset+j, bidPerPlayerYOffset+i, 1, 1, 0);
            }
        }
        
        // Player money
        addField (new Caption("Bids"), playerBidsXOffset-1, playerBidsYOffset, 1, 1, WIDE_TOP+WIDE_RIGHT);
        for (int i=0; i<np; i++) {
         	f = playerBids[i] = new Field (Bank.format(players[i].getBlockedCash()));
            addField (f, playerBidsXOffset+i, playerBidsYOffset, 1, 1, WIDE_TOP);
        }
        
        addField (new Caption("Free"), playerBidsXOffset-1, playerFreeYOffset, 1, 1, WIDE_RIGHT);
        for (int i=0; i<np; i++) {
        	f = playerFree[i] = new Field (Bank.format(players[i].getUnblockedCash()));
            addField (f, playerFreeXOffset+i, playerFreeYOffset, 1, 1, 0);
        }
        
        for (int i=0; i<np; i++) {
        	f = lowerPlayerCaption[i] = new Caption(players[i].getName());
            addField (f, playerFreeXOffset+i, playerFreeYOffset+1, 1, 1, WIDE_TOP);
        }
        
        dummyButton = new ClickField ("", "", "", this, itemGroup);
        
    }

   
   private void addField (JComponent comp, int x, int y, int width, int height,
   		int wideGapPositions) {
   	
   		int padTop,padLeft,padBottom,padRight;
       gbc.gridx = x;
       gbc.gridy = y;
       gbc.gridwidth = width;
       gbc.gridheight = height;
       gbc.weightx = gbc.weighty = 0.5;
       gbc.fill = GridBagConstraints.BOTH;
       padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
       padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
       padBottom = (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP : NARROW_GAP;
       padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
       gbc.insets = new Insets (padTop,padLeft,padBottom,padRight);

       statusPanel.add (comp, gbc);
       
   }
   
   public void updateStatus()
   {
       StartItem item;
       Player p;
       int i, j, lastBid;

       for (i=0; i<items.length; i++) {
           item = items[i];
           if (round.isBuyable(item)) {
               itemNameButton[i].setToolTipText("Click to select for buying");
               itemNameButton[i].setActionCommand("Buy");
               setItemNameButton (i, true);
               minBid[i].setText("");
           } else if (round.isBiddable(item)) {
               itemNameButton[i].setToolTipText("Click to select for bidding");
               itemNameButton[i].setActionCommand("Bid");
               setItemNameButton (i, true);
               minBid[i].setText(Bank.format(item.getMinimumBid()));
               if (round.nextStep() == StartRound.BID_OR_PASS) {
                   itemIndex = i;
                   itemNameButton[i].setSelected(true);
                   itemNameButton[i].setEnabled(false);
                   bidButton.setEnabled(true);
                   bidAmount.setEnabled(true);
                   int minBid = items[itemIndex].getMinimumBid();
                   spinnerModel.setMinimum (new Integer(minBid));
                   spinnerModel.setValue(new Integer(minBid));
               }
           } else if (item.isSold()) {
               setItemNameButton (i, false);
               minBid[i].setText("");
               p = (Player) item.getPrimary().getPortfolio().getOwner();
               for (j=0; j<np; j++) {
                   if (p == players[j]) {
                       bidPerPlayer[i][j].setText(Bank.format(item.getBuyPrice()));
                   } else {
                       bidPerPlayer[i][j].setText("");
                   }
               }
           }
       }
       
       for (j=0; j<np; j++) {
           p = players[j];
           playerBids[j].setText(Bank.format(p.getBlockedCash()));
           playerFree[j].setText(Bank.format(p.getUnblockedCash()));
       }
}
   public void refreshStatus()
   {
      //companyStatus.refreshPanel();
      //playerStatus.refreshPanel();
      //certStatus.refreshPanel();
      //updateStatus();
      //FIXME: Not an ideal fix for various repainting issues, but it works well enough for now.
      //this.pack();
      //System.out.println("StatusWindow Dimensions: " + this.getWidth() + ", " + this.getHeight());
   }
   
   public void repaint()
   {
      super.repaint();
      //refreshStatus();
   }
   
   /* (non-Javadoc)
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   public void actionPerformed(ActionEvent actor)
   {
       JComponent source = (JComponent)actor.getSource();
       String command = actor.getActionCommand();
       if (source instanceof ClickField) {
           gbc = gb.getConstraints(source);
           itemIndex = gbc.gridy - bidPerPlayerYOffset;
           if (command.equals("Buy")) {
               buyButton.setEnabled(true);
               bidButton.setEnabled(false);
               bidAmount.setEnabled(false);
           } else if (command.equals("Bid")) {
               buyButton.setEnabled(false);
               bidButton.setEnabled(true);
               bidAmount.setEnabled(true);
               int minBid = items[itemIndex].getMinimumBid();
               spinnerModel.setMinimum (new Integer(minBid));
               spinnerModel.setValue(new Integer(minBid));
           }
       } else if (source instanceof JButton) {
           if (command.equals("Buy")) {
               round.buy(players[playerIndex].getName(), items[itemIndex].getName());
               GameStatus.getInstance().updatePlayers();
               if (round.hasCompanyJustStarted()) {
                   StockChart.refreshStockPanel();
                   round.resetCompanyJustStarted();
               }
           } else if (command.equals("Bid")) {
               round.bid(players[playerIndex].getName(), items[itemIndex].getName(), 
                       ((Integer)bidAmount.getValue()).intValue());
               bidPerPlayer[itemIndex][playerIndex].setText(Bank.format(items[itemIndex].getBid()));
           } else if (command.equals("Pass")) {
               round.pass(players[playerIndex].getName());
           }
           buyButton.setEnabled(false);
           bidButton.setEnabled(false);
           bidAmount.setEnabled(false);
           setSRPlayerTurn(((StartRound)round).getCurrentPlayerIndex());
       }
       LogWindow.addLog ();
       if (round.nextStep() == StartRound.SET_PRICE) {
       	PublicCompanyI company = round.getCompanyNeedingPrice();
       	StockMarketI stockMarket = Game.getStockMarket();
        StockSpace sp = (StockSpace) 
			JOptionPane.showInputDialog(this, "Start "+company.getName()+" at what price?", 
					"What Price?", 
					JOptionPane.INFORMATION_MESSAGE,
					null,
					stockMarket.getStartSpaces().toArray(),
					stockMarket.getStartSpaces().get(0));
        if (!round.setPrice(players[playerIndex].getName(), company.getName(), sp.getPrice())) {
        	JOptionPane.showMessageDialog(this,Log.getErrorBuffer(), "", JOptionPane.OK_OPTION);
        } else {
        	int compIndex = company.getPublicNumber();
        	GameStatus.getInstance().updateIPO(compIndex);
        	StockChart.refreshStockPanel();
        }

       }
       if (round.nextStep() == StartRound.CLOSED) {
       		statusWindow.resume (this);
       		this.dispose();
       } else {
       	updateStatus();
        pack();
       }
      
   }
   
   public int getItemIndex () {
       return itemIndex;
   }
   /*
   public void updatePlayer (int compIndex, int playerIndex) {
       int share = players[playerIndex].getPortfolio().ownsShare(companies[compIndex]);
       String text = share > 0 ? share+"%" : "";
       bidPerPlayer[compIndex][playerIndex].setText(text);
       bidPerPlayerButton[compIndex][playerIndex].setText(text);
       if (share == 0) setPlayerBidButton(compIndex, playerIndex, false);
       playerBids[playerIndex].setText(Bank.format(players[playerIndex].getCash()));
       playerWorth[playerIndex].setText(Bank.format(players[playerIndex].getWorth()));
   }
   
   public void updateIPO (int compIndex) {
       int share = Bank.getIpo().ownsShare(companies[compIndex]);
       String text = share > 0 ? share+"%" : "";
       certInIPO[compIndex].setText(text);
       certInIPOButton[compIndex].setText(text);
       if (share == 0) setIPOCertButton(compIndex, false);
       parPrice[compIndex].setText(Bank.format(companies[compIndex].getParPrice().getPrice()));
       currPrice[compIndex].setText(Bank.format(companies[compIndex].getCurrentPrice().getPrice()));
   }
   
   public void updatePool (int compIndex) {
       int share = Bank.getPool().ownsShare(companies[compIndex]);
       String text = share > 0 ? share+"%" : "";
       certInPool[compIndex].setText(text);
       certInPoolButton[compIndex].setText(text);
       if (share == 0) setPoolCertButton(compIndex, false);
       currPrice[compIndex].setText(Bank.format(companies[compIndex].getCurrentPrice().getPrice()));
   }
   
   public void updateBank () {
       bankCash.setText(Bank.format(Bank.getInstance().getCash()));
   }
   */
    public void setSRPlayerTurn (int selectedPlayerIndex) {
   	
   		int i, j, share;
   		
   		dummyButton.setSelected(true);
   		
   		if ((j = this.playerIndex) >= 0) {
   			upperPlayerCaption[j].setHighlight(false);
   			lowerPlayerCaption[j].setHighlight(false);
   		}
   		this.playerIndex = selectedPlayerIndex;
   		if ((j = this.playerIndex) >= 0) {
   			upperPlayerCaption[j].setHighlight(true);
   			lowerPlayerCaption[j].setHighlight(true);
   		}
   		updateStatus();
 		repaint();
   }
   
   public String getSRPlayer () {
   		if (playerIndex >= 0) 
   			return players[playerIndex].getName();
   		else
   			return "";
   }
   
   private void setItemNameButton (int i, boolean clickable) {
       itemName[i].setVisible (!clickable);
       itemNameButton[i].setVisible(clickable);
   }
   
}

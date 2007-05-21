package rails.ui.swing;

import rails.game.*;
import rails.ui.swing.elements.*;
import rails.util.LocalText;
import rails.util.Util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * This displays the Auction Window
 */
public class StartRoundWindow extends JFrame implements ActionListener, KeyListener
{

	private static final int NARROW_GAP = 1;
	private static final int WIDE_GAP = 3;
	private static final int WIDE_LEFT = 1;
	private static final int WIDE_RIGHT = 2;
	private static final int WIDE_TOP = 4;
	private static final int WIDE_BOTTOM = 8;
	
	protected static final String BUY_CMD = "Buy";
	protected static final String BID_CMD = "Bid";
	protected static final String PASS_CMD = "Pass";

	//private static StartRoundWindow startRoundPanel;
	private JPanel statusPanel;
	private JPanel buttonPanel;

	private GridBagLayout gb;
	private GridBagConstraints gbc;

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
	private int playerFreeCashXOffset, playerFreeCashYOffset;

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
	
	private boolean includeBidding;

	public StartRoundWindow(StartRound round, StatusWindow parent)
	{
		super();
		this.round = round;
		includeBidding = round.hasBidding();
		statusWindow = parent;
		//startRoundPanel = this;
		setTitle(LocalText.getText("START_ROUND_TITLE"));
		getContentPane().setLayout(new BorderLayout());

		statusPanel = new JPanel();
		gb = new GridBagLayout();
		statusPanel.setLayout(gb);
		statusPanel.setBorder(BorderFactory.createEtchedBorder());
		statusPanel.setOpaque(true);

		buttonPanel = new JPanel();

		buyButton = new JButton(LocalText.getText("BUY"));
		buyButton.setActionCommand(BUY_CMD);
		buyButton.setMnemonic(KeyEvent.VK_B);
		buyButton.addActionListener(this);
		buyButton.setEnabled(false);
		buttonPanel.add(buyButton);

		if (includeBidding) {
			bidButton = new JButton(LocalText.getText("BID") + ":");
			bidButton.setActionCommand(BID_CMD);
			bidButton.setMnemonic(KeyEvent.VK_D);
			bidButton.addActionListener(this);
			bidButton.setEnabled(false);
			buttonPanel.add(bidButton);
	
			spinnerModel = new SpinnerNumberModel(new Integer(999),
					new Integer(0),
					null,
					new Integer(1));
			bidAmount = new JSpinner(spinnerModel);
			bidAmount.setPreferredSize(new Dimension(50, 28));
			bidAmount.setEnabled(false);
			buttonPanel.add(bidAmount);
		}

		passButton = new JButton(LocalText.getText("PASS"));
		passButton.setActionCommand(PASS_CMD);
		passButton.setMnemonic(KeyEvent.VK_P);
		passButton.addActionListener(this);
		passButton.setEnabled(true);
		buttonPanel.add(passButton);

		buttonPanel.setOpaque(true);

		gbc = new GridBagConstraints();

		players = Game.getPlayerManager().getPlayersArray();
		np = GameManager.getNumberOfPlayers();
		packet = round.getStartPacket();
		//items = (StartItem[]) packet.getItems().toArray(new StartItem[0]);
		items = (StartItem[]) round.getStartItems().toArray(new StartItem[0]);
		ni = items.length;

		init();

		getContentPane().add(statusPanel, BorderLayout.NORTH);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		setTitle("Rails: Start Round");
		setLocation(300, 150);
		setSize(275, 325);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		requestFocus();

		ReportWindow.addLog();
		
		addKeyListener(this);


		pack();
	}

	private void init()
	{
		int lastX = -1;
		int lastY = 1;

		itemName = new Caption[ni];
		itemNameButton = new ClickField[ni];
		basePrice = new Field[ni];
		minBid = new Field[ni];
		bidPerPlayer = new Field[ni][np];
		upperPlayerCaption = new Caption[np];
		lowerPlayerCaption = new Caption[np];
		playerBids = new Field[np];
		playerFree = new Field[np];

		itemNameXOffset = ++lastX;
		itemNameYOffset = ++lastY;
		basePriceXOffset = ++lastX;
		basePriceYOffset = lastY;
		if (includeBidding) {
			minBidXOffset = ++lastX;
			minBidYOffset = lastY;
		}
		bidPerPlayerXOffset = ++lastX;
		bidPerPlayerYOffset = lastY;

		// Bottom rows
		lastY += (ni - 1);
		if (includeBidding) {
			playerBidsXOffset = bidPerPlayerXOffset;
			playerBidsYOffset = ++lastY;
		}
		playerFreeCashXOffset = bidPerPlayerXOffset;
		playerFreeCashYOffset = ++lastY;

		addField(new Caption(LocalText.getText("ITEM")), 0, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM);
		addField(new Caption(LocalText.getText(includeBidding ? "BASE_PRICE" : "PRICE")),
				basePriceXOffset,
				0,
				1,
				2,
				WIDE_BOTTOM);
		if (includeBidding) {
			addField(new Caption(LocalText.getText("MINIMUM_BID")),
				minBidXOffset,
				0,
				1,
				2,
				WIDE_BOTTOM + WIDE_RIGHT);
		}
		addField(new Caption(LocalText.getText("PLAYERS")), bidPerPlayerXOffset, 0, np, 1, 0);
		for (int i = 0; i < np; i++)
		{
			f = upperPlayerCaption[i] = new Caption(players[i].getName());
			addField(f, bidPerPlayerXOffset + i, 1, 1, 1, WIDE_BOTTOM);
		}

		for (int i = 0; i < ni; i++)
		{
			si = items[i];
			f = itemName[i] = new Caption(si.getName());
			addField(f, itemNameXOffset, itemNameYOffset + i, 1, 1, WIDE_RIGHT);
			f = itemNameButton[i] = new ClickField(si.getName(),
					"",
					"",
					this,
					itemGroup);
			addField(f, itemNameXOffset, itemNameYOffset + i, 1, 1, WIDE_RIGHT);
			// Prevent row height resizing after every buy action 
			itemName[i].setPreferredSize(itemNameButton[i].getPreferredSize());

			f = basePrice[i] = new Field(Bank.format(si.getBasePrice()));
			addField(f, basePriceXOffset, basePriceYOffset + i, 1, 1, 0);

			if (includeBidding) {
				f = minBid[i] = new Field("");
				addField(f, minBidXOffset, minBidYOffset + i, 1, 1, WIDE_RIGHT);
			}

			for (int j = 0; j < np; j++)
			{
				f = bidPerPlayer[i][j] = new Field("");
				addField(f,
						bidPerPlayerXOffset + j,
						bidPerPlayerYOffset + i,
						1,
						1,
						0);
			}
		}

		// Player money
		boolean firstBelowTable = true;
		if (includeBidding) {
			addField(new Caption(LocalText.getText("BID")),
					playerBidsXOffset - 1,
					playerBidsYOffset,
					1,
					1,
					WIDE_TOP + WIDE_RIGHT);
			for (int i = 0; i < np; i++)
			{
				f = playerBids[i] = new Field(Bank.format(players[i].getBlockedCash()));
				addField(f,
						playerBidsXOffset + i,
						playerBidsYOffset,
						1,
						1,
						WIDE_TOP);
			}
			firstBelowTable = false;
		}

		addField(new Caption(LocalText.getText(includeBidding ? "FREE" : "CASH")),
				playerFreeCashXOffset - 1,
				playerFreeCashYOffset,
				1,
				1,
				WIDE_RIGHT + (firstBelowTable ? WIDE_TOP : 0));
		for (int i = 0; i < np; i++)
		{
			f = playerFree[i] = new Field(Bank.format(players[i].getUnblockedCash()));
			addField(f, playerFreeCashXOffset + i, playerFreeCashYOffset, 1, 1, firstBelowTable ? WIDE_TOP : 0);
		}

		for (int i = 0; i < np; i++)
		{
			f = lowerPlayerCaption[i] = new Caption(players[i].getName());
			addField(f,
					playerFreeCashXOffset + i,
					playerFreeCashYOffset + 1,
					1,
					1,
					WIDE_TOP);
		}

		dummyButton = new ClickField("", "", "", this, itemGroup);

	}

	private void addField(JComponent comp, int x, int y, int width, int height,
			int wideGapPositions)
	{

		int padTop, padLeft, padBottom, padRight;
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.gridheight = height;
		gbc.weightx = gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.BOTH;
		padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
		padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
		padBottom = (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP
				: NARROW_GAP;
		padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
		gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

		statusPanel.add(comp, gbc);

	}

	public void updateStatus()
	{
		StartItem item;
		StartItem auctionedItem = round.getAuctionedItem();
		Player p;
		int i, j, status;
		items = (StartItem[]) round.getStartItems().toArray(new StartItem[0]);

		for (i = 0; i < items.length; i++)
		{
			item = items[i];
			status = item.getStatus();
			//if (round.isBuyable(item))
			if (status == StartItem.BUYABLE)
			{
				itemNameButton[i].setToolTipText(LocalText.getText("ClickToSelectForBuying"));
				itemNameButton[i].setActionCommand(BUY_CMD);
				setItemNameButton(i, true);
				if (includeBidding) minBid[i].setText("");
			}
			//else if (round.isBiddable(item))
			else if (status == StartItem.BIDDABLE)
			{
				itemNameButton[i].setToolTipText(LocalText.getText("ClickToSelectForBidding"));
				itemNameButton[i].setActionCommand(BID_CMD);
				setItemNameButton(i, true);
				minBid[i].setText(Bank.format(item.getMinimumBid()));
			}
			else if (status == StartItem.AUCTIONED)
			{
				itemNameButton[i].setToolTipText(LocalText.getText("ThisItemIsAuctionedNow"));
				itemNameButton[i].setActionCommand(BID_CMD);
				setItemNameButton(i, true);
				minBid[i].setText(Bank.format(item.getMinimumBid()));
				itemIndex = i;
				itemNameButton[i].setSelected(true);
				itemNameButton[i].setEnabled(false);
				bidButton.setEnabled(true);
				bidAmount.setEnabled(true);
				int minBid = items[itemIndex].getMinimumBid();
				spinnerModel.setMinimum(new Integer(minBid));
				spinnerModel.setValue(new Integer(minBid));
			}
			//else if (item.isSold())
			else if (status == StartItem.SOLD)
			{
				setItemNameButton(i, false);
				if (includeBidding) minBid[i].setText("");
				p = (Player) item.getPrimary().getPortfolio().getOwner();
				for (j = 0; j < np; j++)
				{
					if (p == players[j])
					{
						bidPerPlayer[i][j].setText(Bank.format(item.getBuyPrice()));
					}
					else
					{
						bidPerPlayer[i][j].setText("");
					}
				}
			}
			else if (status == StartItem.UNAVAILABLE) {
				
				if (item == auctionedItem) {
					// Mark the auctioned item, even if the current player 
					// does not have enough free money to continue bidding.
					itemNameButton[i].setSelected(true);
					itemNameButton[i].setEnabled(false);
					setItemNameButton(i, true);
				} else {
					setItemNameButton(i, false);
				}
			}
		}

		for (j = 0; j < np; j++)
		{
			p = players[j];
			if (includeBidding) playerBids[j].setText(Bank.format(p.getBlockedCash()));
			playerFree[j].setText(Bank.format(p.getUnblockedCash()));
		}
	}

	/*
	public void repaint()
	{
		super.repaint();
	}
	*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent actor)
	{
		JComponent source = (JComponent) actor.getSource();
		String command = actor.getActionCommand();
		if (source instanceof ClickField)
		{
			gbc = gb.getConstraints(source);
			itemIndex = gbc.gridy - bidPerPlayerYOffset;
			if (command.equals(BUY_CMD))
			{
				buyButton.setEnabled(true);
				if (includeBidding) {
					bidButton.setEnabled(false);
					bidAmount.setEnabled(false);
				}
			}
			else if (command.equals(BID_CMD))
			{
				buyButton.setEnabled(false);
				if (includeBidding) {
					bidButton.setEnabled(true);
					bidAmount.setEnabled(true);
					int minBid = items[itemIndex].getMinimumBid();
					spinnerModel.setMinimum(new Integer(minBid));
					spinnerModel.setValue(new Integer(minBid));
				}
			}
		}
		else if (source instanceof JButton)
		{
			if (command.equals(BUY_CMD))
			{
				round.buy(players[playerIndex].getName(),
						items[itemIndex].getName());
				//GameStatus.getInstance().updatePlayers();
				if (round.hasCompanyJustStarted())
				{
					//StockChart.refreshStockPanel();
					round.resetCompanyJustStarted();
				}
			}
			else if (command.equals(BID_CMD))
			{
				round.bid(players[playerIndex].getName(),
						items[itemIndex].getName(),
						((Integer) bidAmount.getValue()).intValue());
				bidPerPlayer[itemIndex][playerIndex].setText(Bank.format(items[itemIndex].getBid()));
			}
			else if (command.equals(PASS_CMD))
			{
				round.pass(players[playerIndex].getName());
			}
			buyButton.setEnabled(false);
			if (includeBidding) {
				bidButton.setEnabled(false);
				bidAmount.setEnabled(false);
			}
			
			// Unselect the selected private
			dummyButton.setSelected(true);
		}
		
		ReportWindow.addLog();
		if (round.nextStep() == StartRound.SET_PRICE)
		{
			PublicCompanyI company = round.getCompanyNeedingPrice();
			StockMarketI stockMarket = Game.getStockMarket();
			StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this,
					LocalText.getText("WHICH_START_PRICE", company.getName()),
					LocalText.getText("WHICH_PRICE"),
					JOptionPane.INFORMATION_MESSAGE,
					null,
					stockMarket.getStartSpaces().toArray(),
					stockMarket.getStartSpaces().get(0));
			if (!round.setPrice(players[playerIndex].getName(),
					company.getName(),
					sp.getPrice()))
			{
				JOptionPane.showMessageDialog(this,
						ReportBuffer.get(),
						LocalText.getText("ERROR"),
						JOptionPane.OK_OPTION);
			}
			else
			{
				//int compIndex = company.getPublicNumber();
				//GameStatus.getInstance().updateIPO(compIndex);
				//GameUILoader.stockChart.refreshStockPanel();
			}

		}
		if (round.nextStep() == StartRound.CLOSED)
		{
			statusWindow.resume(this);
			this.dispose();
		}
		else
		{
			setSRPlayerTurn(((StartRound) round).getCurrentPlayerIndex());
		    displayError();
			updateStatus();
			pack();
		}

	}
	
	public void close() {
	    
	}

	public int getItemIndex()
	{
		return itemIndex;
	}

	public void setSRPlayerTurn(int selectedPlayerIndex)
	{

		int i, j, share;

		//dummyButton.setSelected(true);

		if ((j = this.playerIndex) >= 0)
		{
			upperPlayerCaption[j].setHighlight(false);
			lowerPlayerCaption[j].setHighlight(false);
		}
		this.playerIndex = selectedPlayerIndex;
		if ((j = this.playerIndex) >= 0)
		{
			upperPlayerCaption[j].setHighlight(true);
			lowerPlayerCaption[j].setHighlight(true);
		}
		updateStatus();
		//repaint();
	}

	public String getSRPlayer()
	{
		if (playerIndex >= 0)
			return players[playerIndex].getName();
		else
			return "";
	}
	
    public void displayError() {
    	String message = DisplayBuffer.get();
    	if (Util.hasValue(message)) {
    		JOptionPane.showMessageDialog(this, message);
    	}
    }

	private void setItemNameButton(int i, boolean clickable)
	{
		itemName[i].setVisible(!clickable);
		itemNameButton[i].setVisible(clickable);
	}
	
	public void keyPressed(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_F1) {
	        HelpWindow.displayHelp(GameManager.getInstance().getHelp());
	        e.consume();
	    }
	}
	
	public void keyReleased(KeyEvent e) {}

	public void keyTyped (KeyEvent e) {}
}

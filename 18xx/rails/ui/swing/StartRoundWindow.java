package rails.ui.swing;

import rails.game.*;
import rails.game.action.BuyOrBidStartItem;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.model.CalculatedMoneyModel;
import rails.game.model.MoneyModel;
import rails.ui.swing.elements.*;
import rails.util.LocalText;

import java.util.Iterator;
import java.util.List;
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
    protected static final String UNDO_CMD = "Undo";
    protected static final String REDO_CMD = "Redo";
    protected static final String CLOSE_CMD = "Close";

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

	private JButton bidButton;
	private JButton buyButton;
	private JSpinner bidAmount;
	private SpinnerNumberModel spinnerModel;
	private JButton passButton;

	private int np; // Number of players
	private int ni; // Number of start items
	private Player[] players;
	private StartItem[] items;
	private BuyOrBidStartItem[] actionableItems;
	private BuyOrBidStartItem currentActiveItem;
	private StartPacket packet;
	private int[] crossIndex;
	private StartRoundI round;
	private StatusWindow statusWindow;

	private JMenuBar menuBar;
	private static JMenu moveMenu;
	private JMenuItem undoItem, redoItem;

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

		menuBar = new JMenuBar();
		moveMenu = new JMenu(LocalText.getText("MOVE"));
		moveMenu.setMnemonic(KeyEvent.VK_M);

		undoItem = new JMenuItem(LocalText.getText("UNDO"));
		//undoItem.setName(LocalText.getText("UNDO"));
		undoItem.setActionCommand(UNDO_CMD);
		undoItem.setMnemonic(KeyEvent.VK_U);
		undoItem.addActionListener(this);
		undoItem.setEnabled(false);
		moveMenu.add(undoItem);

		redoItem = new JMenuItem(LocalText.getText("REDO"));
		//redoItem.setName(LocalText.getText("REDO"));
		redoItem.setActionCommand(REDO_CMD);
		redoItem.setMnemonic(KeyEvent.VK_R);
		redoItem.addActionListener(this);
		redoItem.setEnabled(false);
		moveMenu.add(redoItem);
		
		menuBar.add (moveMenu);
		setJMenuBar(menuBar);
		
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
		crossIndex = new int [packet.getNumberOfItems()];

		items = (StartItem[]) round.getStartItems().toArray(new StartItem[0]);
		ni = items.length;
		StartItem item;
		for (int i = 0; i < ni; i++)
		{
			item = items[i];
			crossIndex[item.getIndex()] = i;
		}
		
		actionableItems = new BuyOrBidStartItem[ni];
		currentActiveItem = null;

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

		updateStatus(); //??
		
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

			f = basePrice[i] = new Field(si.getBasePriceModel());
			addField(f, basePriceXOffset, basePriceYOffset + i, 1, 1, 0);

			if (includeBidding) {
				f = minBid[i] = new Field(round.getMinimumBidModel(i).option(MoneyModel.SUPPRESS_ZERO));
				addField(f, minBidXOffset, minBidYOffset + i, 1, 1, WIDE_RIGHT);
			}

			for (int j = 0; j < np; j++)
			{
				f = bidPerPlayer[i][j] = new Field(round.getBidModel(i, j).option(MoneyModel.SUPPRESS_ZERO));
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
				f = playerBids[i] = new Field(round.getBlockedCashModel(i)
						.option(CalculatedMoneyModel.SUPPRESS_ZERO));
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
			f = playerFree[i] = new Field(includeBidding 
					? round.getFreeCashModel(i)
					: players[i].getCashModel());
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
		//StartItem auctionedItem = round.getAuctionedItem();
		int i, j, status;
		BuyOrBidStartItem activeItem;
		
		for (i = 0; i < ni; i++)
		{
			setItemNameButton(i, false);
			actionableItems[i] = null;
		}
		
		List activeItems = PossibleActions.getInstance().getType (BuyOrBidStartItem.class);
		
		if (activeItems == null || activeItems.isEmpty()) {
			//passButton.setText(LocalText.getText("CLOSE"));
			//passButton.setEnabled(true);
			//passButton.setActionCommand(CLOSE_CMD);
			//passButton.setMnemonic(KeyEvent.VK_C);
			closeWindow();
			return;
		}
		
		int nextPlayerIndex = ((PossibleAction)activeItems.get(0)).getPlayerIndex();
		setSRPlayerTurn (nextPlayerIndex);

		for (Iterator it = activeItems.iterator(); it.hasNext(); )
		{
			activeItem = (BuyOrBidStartItem) it.next();
			j = activeItem.getItemIndex();
			i = crossIndex[j];
			actionableItems[i] = activeItem;
			item = activeItem.getStartItem();
			status = activeItem.getStatus();
			
			if (status == StartItem.BUYABLE)
			{
				itemNameButton[i].setToolTipText(LocalText.getText("ClickToSelectForBuying"));
				itemNameButton[i].setActionCommand(BUY_CMD);
				setItemNameButton(i, true);
				if (includeBidding) minBid[i].setText("");
			}
			else if (status == StartItem.BIDDABLE)
			{
				itemNameButton[i].setToolTipText(LocalText.getText("ClickToSelectForBidding"));
				itemNameButton[i].setActionCommand(BID_CMD);
				setItemNameButton(i, true);
				minBid[i].setText(Bank.format(item.getMinimumBid()));
			}
			else if (status == StartItem.AUCTIONED) //??
			{
				currentActiveItem = activeItem;
				
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
			else if (status == StartItem.NEEDS_SHARE_PRICE) {
				
				currentActiveItem = activeItem;
				requestStartPrice();
				if (!round.process (currentActiveItem)) {
					JOptionPane.showMessageDialog(this,
							ReportBuffer.get(),
							LocalText.getText("ERROR"),
							JOptionPane.OK_OPTION);
				} else {
					updateStatus();
					break;
				}
				
			}
		}

		passButton.setEnabled(false);
		undoItem.setEnabled(false);
		redoItem.setEnabled(false);
		
		List inactiveItems = PossibleActions.getInstance().getType (NullAction.class);
		if (inactiveItems != null) {
			
			NullAction na;
			for (Iterator it = inactiveItems.iterator();
					it.hasNext(); ) {
				na = (NullAction) it.next();
				switch (na.getMode()) {
				case NullAction.PASS:
					passButton.setText(LocalText.getText("PASS"));
					passButton.setEnabled (true);
					passButton.setActionCommand(PASS_CMD);
					passButton.setMnemonic(KeyEvent.VK_P);
					break;
				case NullAction.UNDO:
					undoItem.setEnabled(true);
					break;
				case NullAction.REDO:
					redoItem.setEnabled(true);
					break;
				case NullAction.CLOSE:
					passButton.setText(LocalText.getText("CLOSE"));
					passButton.setEnabled (true);
					passButton.setActionCommand(CLOSE_CMD);
					passButton.setMnemonic(KeyEvent.VK_C);
					break;
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent actor)
	{
		JComponent source = (JComponent) actor.getSource();
		String command = actor.getActionCommand();
		PossibleAction action;
		if (actor.getActionCommand().equals(UNDO_CMD)) {
			if ((action = getNullAction (UNDO_CMD)) != null) {
				process (action);
			}
		} else if (actor.getActionCommand().equals(REDO_CMD)) {
			if ((action = getNullAction (REDO_CMD)) != null) {
				process (action);
			}
		} else if (source instanceof ClickField)
		{
			gbc = gb.getConstraints(source);
			itemIndex = gbc.gridy - bidPerPlayerYOffset;
			currentActiveItem = actionableItems[itemIndex];
						
			if (currentActiveItem.getStatus() == StartItem.BUYABLE)
			{
				buyButton.setEnabled(true);
				if (includeBidding) {
					bidButton.setEnabled(false);
					bidAmount.setEnabled(false);
				}
			}
			else if (currentActiveItem.getStatus() == StartItem.BIDDABLE
					|| currentActiveItem.getStatus() == StartItem.AUCTIONED)
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
				if (currentActiveItem.hasSharePriceToSet()) {
					requestStartPrice();
				}

				/* Process the buy action */
				if (!process (currentActiveItem)) {
					JOptionPane.showMessageDialog(this,
							ReportBuffer.get(),
							LocalText.getText("ERROR"),
							JOptionPane.OK_OPTION);
				}
			}
			else if (command.equals(BID_CMD))
			{
				currentActiveItem.setActualBid(((Integer) bidAmount.getValue()).intValue());
				process (currentActiveItem);
				bidPerPlayer[itemIndex][playerIndex].setText(Bank.format(items[itemIndex].getBid()));
			}
			else if (command.equals(PASS_CMD))
			{
				if ((action = getNullAction (PASS_CMD)) != null) {
					process (action);
				}
			}
			else if (command.equals(CLOSE_CMD))
			{
				if ((action = getNullAction (CLOSE_CMD)) != null) {
					process (action);
				} else {
					// Not included in possibleActions, still do it.
					// (This will only happen if there are no active items)
					// TODO See above, possibleActions should not be empty!?
					process (new NullAction (NullAction.CLOSE));
				}
				closeWindow();
				return;
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
		
	    displayError();
		updateStatus();
		pack();

	}
	
	private boolean process (PossibleAction action) {
		return round.process(action);
	}
	
	private void requestStartPrice () {		
		if (currentActiveItem.hasSharePriceToSet()) {
			String compName = currentActiveItem.getCompanyToSetPriceFor();
			StockMarketI stockMarket = Game.getStockMarket();
			StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this,
					LocalText.getText("WHICH_START_PRICE", compName),
					LocalText.getText("WHICH_PRICE"),
					JOptionPane.INFORMATION_MESSAGE,
					null,
					stockMarket.getStartSpaces().toArray(),
					stockMarket.getStartSpaces().get(0));
			int price = sp.getPrice();
			currentActiveItem.setSharePrice(price);
		}

	}
	
	public void closeWindow() {
		statusWindow.resume(this);
		this.dispose();
	}
	
	public void close() {
		this.dispose();
	}
	
	private NullAction getNullAction (String command) {
		
		List nullActions = PossibleActions.getInstance().getType (NullAction.class);
		NullAction action;
		for (Iterator it = nullActions.iterator(); it.hasNext(); ) {
			action = (NullAction)it.next();
			switch (action.getMode()) {
			case NullAction.PASS:
				if (command.equals(PASS_CMD)) return action;
				break;
			//case NullAction.DONE:
			//	if (command.equals(DONE_CMD)) return action;
			//	break;
			case NullAction.UNDO:
				if (command.equals(UNDO_CMD)) return action;
				break;
			case NullAction.REDO:
				if (command.equals(REDO_CMD)) return action;
				break;
			case NullAction.CLOSE:
				if (command.equals(CLOSE_CMD)) return action;
				break;
			}
		}
		return null;
	}

	public int getItemIndex()
	{
		return itemIndex;
	}

	public void setSRPlayerTurn(int selectedPlayerIndex)
	{
		int j;

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
		//updateStatus();
	}

	public String getSRPlayer()
	{
		if (playerIndex >= 0)
			return players[playerIndex].getName();
		else
			return "";
	}
	
    public void displayError() {
    	String[] message = DisplayBuffer.get();
    	if (message != null) {
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

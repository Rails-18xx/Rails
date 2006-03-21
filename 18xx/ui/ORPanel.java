package ui;

import game.*;
import game.model.*;
import ui.elements.*;
import util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;
import java.util.List;
import java.util.regex.*;

public class ORPanel extends JPanel implements ActionListener, KeyListener
{

	private static final int NARROW_GAP = 1;
	private static final int WIDE_GAP = 3;
	private static final int WIDE_LEFT = 1;
	private static final int WIDE_RIGHT = 2;
	private static final int WIDE_TOP = 4;
	private static final int WIDE_BOTTOM = 8;

	private JPanel statusPanel;
	private JPanel buttonPanel;

	private GridBagLayout gb;
	private GridBagConstraints gbc;

	// Grid elements per function
	private Caption leftCompName[];
	private int leftCompNameXOffset, leftCompNameYOffset;
	private Caption rightCompName[];
	private int rightCompNameXOffset, rightCompNameYOffset;
	private Field president[];
	private int presidentXOffset, presidentYOffset;
	private Field sharePrice[];
	private int sharePriceXOffset, sharePriceYOffset;
	private Field cash[];
	private int cashXOffset, cashYOffset;
	private Field privates[];
	private int privatesXOffset, privatesYOffset;
	private Field newPrivatesCost[];
	private Field tiles[];
	private int tilesXOffset, tilesYOffset;
	private Field tileCost[];
	private Field tokens[];
	private Field tokenCost[];
	private int tokensXOffset, tokensYOffset;
	private Field revenue[];
	private Spinner revenueSelect[];
	private Field decision[];
	private int revXOffset, revYOffset;
	private Field trains[];
	private int trainsXOffset, trainsYOffset;
	private Field newTrainCost[];

	private boolean privatesCanBeBought = false;
	private boolean privatesCanBeBoughtNow = false;

	private Caption tileCaption, tokenCaption, revenueCaption, trainCaption,
			privatesCaption;

	private JButton button1;
	private JButton button2;
	private JButton button3;

	private int np = 0; // Number of players
	private int nc = 0; // Number of companies
	private Player[] players;
	private PublicCompanyI[] companies;
	private Round round, previousRound;

	private Player p;
	private PublicCompanyI c;
	private JComponent f;

	// Current state
	private int playerIndex = -1;
	private int orCompIndex = -1;
	private PublicCompanyI orComp = null;
	private String orCompName = "";

	private Pattern buyTrainPattern = Pattern.compile("(.+)-train from (\\S+)( \\(exchanged\\))?.*");
	private int[] newTrainTotalCost;
	List trainsBought;
	
	//Strings
	public static final String LAY_TILES = "Lay Tiles";
	public static final String LAY_TRACK = "Lay Track";
	public static final String LAY_TOKEN = "Lay Token";
	public static final String BUY_PRIVATE = "Buy Private";
	public static final String DONE = "Done";
	public static final String SET_REVENUE = "Set Revenue";
	public static final String WITHHOLD = "Withhold";
	public static final String SPLIT = "Split";
	public static final String PAYOUT = "Pay out";
	public static final String BUY_TRAIN = "Buy Train";

	public ORPanel()
	{
		super();

		statusPanel = new JPanel();
		gb = new GridBagLayout();
		statusPanel.setLayout(gb);
		statusPanel.setBorder(BorderFactory.createEtchedBorder());
		statusPanel.setOpaque(true);

		round = GameManager.getInstance().getCurrentRound();
		privatesCanBeBought = GameManager.getCompaniesCanBuyPrivates();

		initButtonPanel();
		gbc = new GridBagConstraints();

		players = Game.getPlayerManager().getPlayersArray();
		np = GameManager.getNumberOfPlayers();

		if (round instanceof OperatingRound)
		{
			companies = ((OperatingRound) round).getOperatingCompanies();
			nc = companies.length;
		}

		initFields();

		setLayout(new BorderLayout());
		add(statusPanel, BorderLayout.NORTH);
		add(buttonPanel, BorderLayout.SOUTH);

		setVisible(true);

		updateStatus();

		addKeyListener(this);
	}

	public void repaint()
	{
		round = GameManager.getInstance().getCurrentRound();
		if (round instanceof OperatingRound)
		{
			companies = ((OperatingRound) round).getOperatingCompanies();
			nc = companies.length;
			
			initFields();
			updateStatus();
		}
	}

	private void initButtonPanel()
	{
		buttonPanel = new JPanel();

		button1 = new JButton(LAY_TILES);
		button1.setActionCommand(LAY_TILES);
		button1.setMnemonic(KeyEvent.VK_T);
		button1.addActionListener(this);
		button1.setEnabled(true);
		buttonPanel.add(button1);

		button2 = new JButton(BUY_PRIVATE);
		button2.setActionCommand(BUY_PRIVATE);
		button2.setMnemonic(KeyEvent.VK_V);
		button2.addActionListener(this);
		button2.setEnabled(false);
		buttonPanel.add(button2);

		button3 = new JButton(DONE);
		button3.setActionCommand(DONE);
		button3.setMnemonic(KeyEvent.VK_D);
		button3.addActionListener(this);
		button3.setEnabled(true);
		buttonPanel.add(button3);

		buttonPanel.setOpaque(true);
	}

	private void initFields()
	{

		leftCompName = new Caption[nc];
		rightCompName = new Caption[nc];
		president = new Field[nc];
		sharePrice = new Field[nc];
		cash = new Field[nc];
		trains = new Field[nc];
		privates = new Field[nc];
		tiles = new Field[nc];
		tileCost = new Field[nc];
		tokens = new Field[nc];
		tokenCost = new Field[nc];
		revenue = new Field[nc];
		revenueSelect = new Spinner[nc];
		decision = new Field[nc];
		newTrainCost = new Field[nc];
		newPrivatesCost = new Field[nc];

		leftCompNameXOffset = 0;
		leftCompNameYOffset = 2;
		int currentXOffset = leftCompNameXOffset;
		int lastXWidth = 0;

		/* Top titles */
		addField(new Caption("Company"), 0, 0, lastXWidth = 1, 2, WIDE_BOTTOM
				+ WIDE_RIGHT);

		presidentXOffset = currentXOffset += lastXWidth;
		presidentYOffset = leftCompNameYOffset;
		addField(new Caption("President"),
				presidentXOffset,
				0,
				lastXWidth = 1,
				2,
				WIDE_BOTTOM);

		sharePriceXOffset = currentXOffset += lastXWidth;
		sharePriceYOffset = leftCompNameYOffset;
		addField(new Caption("<html>Share<br>value</html>"),
				sharePriceXOffset,
				0,
				lastXWidth = 1,
				2,
				WIDE_BOTTOM);

		cashXOffset = currentXOffset += lastXWidth;
		cashYOffset = leftCompNameYOffset;
		addField(new Caption("Treasury"),
				cashXOffset,
				0,
				lastXWidth = 1,
				2,
				WIDE_BOTTOM + WIDE_RIGHT);

		if (privatesCanBeBought)
		{
			privatesXOffset = currentXOffset += lastXWidth;
			privatesYOffset = leftCompNameYOffset;
			addField(privatesCaption = new Caption("Privates"),
					privatesXOffset,
					0,
					lastXWidth = 2,
					1,
					WIDE_RIGHT);
			addField(new Caption("owned"),
					privatesXOffset,
					1,
					1,
					1,
					WIDE_BOTTOM);
			addField(new Caption("cost"),
					privatesXOffset + 1,
					1,
					1,
					1,
					WIDE_BOTTOM + WIDE_RIGHT);
		}

		tilesXOffset = currentXOffset += lastXWidth;
		tilesYOffset = leftCompNameYOffset;
		addField(tileCaption = new Caption("Tiles"),
				tilesXOffset,
				0,
				lastXWidth = 2,
				1,
				WIDE_RIGHT);
		addField(new Caption("laid"), tilesXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("cost"), tilesXOffset + 1, 1, 1, 1, WIDE_BOTTOM
				+ WIDE_RIGHT);

		tokensXOffset = currentXOffset += lastXWidth;
		tokensYOffset = leftCompNameYOffset;
		addField(tokenCaption = new Caption("Tokens"),
				tokensXOffset,
				0,
				lastXWidth = 2,
				1,
				WIDE_RIGHT);
		addField(new Caption("laid"), tokensXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("cost"), tokensXOffset + 1, 1, 1, 1, WIDE_BOTTOM
				+ WIDE_RIGHT);

		revXOffset = currentXOffset += lastXWidth;
		revYOffset = leftCompNameYOffset;
		addField(revenueCaption = new Caption("Revenue"),
				revXOffset,
				0,
				lastXWidth = 2,
				1,
				WIDE_RIGHT);
		addField(new Caption("earned"), revXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("payout"), revXOffset + 1, 1, 1, 1, WIDE_BOTTOM
				+ WIDE_RIGHT);

		trainsXOffset = currentXOffset += lastXWidth;
		trainsYOffset = leftCompNameYOffset;
		addField(trainCaption = new Caption("Trains"),
				trainsXOffset,
				0,
				lastXWidth = 2,
				1,
				WIDE_RIGHT);
		addField(new Caption("owned"), trainsXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("cost"), trainsXOffset + 1, 1, 1, 1, WIDE_BOTTOM
				+ WIDE_RIGHT);

		rightCompNameXOffset = currentXOffset += lastXWidth;
		rightCompNameYOffset = leftCompNameYOffset;
		addField(new Caption("Company"),
				rightCompNameXOffset,
				0,
				1,
				2,
				WIDE_BOTTOM);

		for (int i = 0; i < nc; i++)
		{
			c = companies[i];
			f = leftCompName[i] = new Caption(c.getName());
			f.setBackground(c.getBgColour());
			f.setForeground(c.getFgColour());
			addField(f,
					leftCompNameXOffset,
					leftCompNameYOffset + i,
					1,
					1,
					WIDE_RIGHT);

			f = president[i] = new Field(c.hasStarted() ? c.getPresident()
					.getName() : "");
			addField(f, presidentXOffset, presidentYOffset + i, 1, 1, 0);

			f = sharePrice[i] = new Field(c.getCurrentPriceModel());
			addField(f, sharePriceXOffset, sharePriceYOffset + i, 1, 1, 0);

			f = cash[i] = new Field(c.getCashModel());
			addField(f, cashXOffset, cashYOffset + i, 1, 1, WIDE_RIGHT);

			if (privatesCanBeBought)
			{
				f = privates[i] = new Field(c.getPortfolio()
						.getPrivatesModel()
						.option(PrivatesModel.SPACE));
				addField(f,
						privatesXOffset,
						privatesYOffset + i,
						1,
						1,
						WIDE_RIGHT);

				f = newPrivatesCost[i] = new Field("");
				addField(f,
						privatesXOffset + 1,
						privatesYOffset + i,
						1,
						1,
						WIDE_RIGHT);
			}

			f = tiles[i] = new Field("");
			addField(f, tilesXOffset, tilesYOffset + i, 1, 1, 0);

			f = tileCost[i] = new Field("");
			addField(f, tilesXOffset + 1, tilesYOffset + i, 1, 1, WIDE_RIGHT);

			f = tokens[i] = new Field("");
			addField(f, tokensXOffset, tokensYOffset + i, 1, 1, 0);

			f = tokenCost[i] = new Field("");
			addField(f, tokensXOffset + 1, tokensYOffset + i, 1, 1, WIDE_RIGHT);

			f = revenue[i] = new Field("");
			addField(f, revXOffset, revYOffset + i, 1, 1, 0);
			f = revenueSelect[i] = new Spinner(0, 0, 0, 10);
			addField(f, revXOffset, revYOffset + i, 1, 1, 0);

			f = decision[i] = new Field("");
			addField(f, revXOffset + 1, revYOffset + i, 1, 1, WIDE_RIGHT);

			f = trains[i] = new Field(c.getPortfolio()
					.getTrainsModel()
					.option(TrainsModel.FULL_LIST));
			addField(f, trainsXOffset, trainsYOffset + i, 1, 1, 0);

			f = newTrainCost[i] = new Field("");
			addField(f, trainsXOffset + 1, trainsYOffset + i, 1, 1, WIDE_RIGHT);

			f = rightCompName[i] = new Caption(c.getName());
			f.setBackground(companies[i].getBgColour());
			f.setForeground(companies[i].getFgColour());
			addField(f, rightCompNameXOffset, rightCompNameYOffset + i, 1, 1, 0);

		}

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
		/* End of game checks */
		if (GameManager.isGameOver())
		{
			JOptionPane.showMessageDialog(this, "GAME OVER");
			/* Further wrap-up to be added */
		}
		else if (Bank.isJustBroken())
		{
			/* The message must become configuration-depedent */
			JOptionPane.showMessageDialog(this,
					"Bank is broken. The game will be over after the current set of ORs.");
		}

		if (GameManager.getInstance().getCurrentRound() instanceof OperatingRound)
		{
			OperatingRound round = (OperatingRound) GameManager.getInstance()
					.getCurrentRound();

			/* Reorder the companies if the round has changed */
			if (round != previousRound)
				reorderCompanies();
			previousRound = round;

			int step = round.getStep();
			if (round.getOperatingCompanyIndex() != orCompIndex)
			{
				setORCompanyTurn(round.getOperatingCompanyIndex());
			}

			setHighlightsOff();
			privatesCanBeBought = GameManager.getCurrentPhase()
					.isPrivateSellingAllowed();

			if (step == OperatingRound.STEP_LAY_TRACK)
			{
				tileCaption.setHighlight(true);
				tileCost[orCompIndex].setText("");
				button1.setVisible(false);

				GameUILoader.orWindow.requestFocus();
				GameUILoader.orWindow.enableTileLaying(true);
				GameUILoader.getMapPanel()
						.setSpecialTileLays((ArrayList) round.getSpecialProperties());

				button2.setText(BUY_PRIVATE);
				button2.setActionCommand(BUY_PRIVATE);
				button2.setMnemonic(KeyEvent.VK_V);
				button2.setEnabled(privatesCanBeBought);
				privatesCaption.setHighlight(privatesCanBeBought);

				button3.setText(DONE);
				button3.setActionCommand(DONE);
				button3.setMnemonic(KeyEvent.VK_D);
				button3.setEnabled(false);

			}
			else if (step == OperatingRound.STEP_LAY_TOKEN)
			{
				GameUILoader.orWindow.requestFocus();
				GameUILoader.orWindow.enableTileLaying(false);
				GameUILoader.orWindow.enableBaseTokenLaying(true);

				tokenCaption.setHighlight(true);
				tokenCost[orCompIndex].setText("");

				button1.setEnabled(false);
				button1.setVisible(false);
				button3.setEnabled(false);
			}
			else if (step == OperatingRound.STEP_CALC_REVENUE)
			{
				if (round.isActionAllowed())
				{
					revenueCaption.setHighlight(true);
					revenueSelect[orCompIndex].setValue(new Integer(companies[orCompIndex].getLastRevenue()));
					setSelect(revenue[orCompIndex],
							revenueSelect[orCompIndex],
							true);

					button1.setText(SET_REVENUE);
					button1.setActionCommand(SET_REVENUE);
					button1.setMnemonic(KeyEvent.VK_R);
					button1.setEnabled(true);
					button1.setVisible(true);
				}
				else
				{
					displayMessage(round.getActionNotAllowedMessage());
					setRevenue(0);
					updateStatus();
					return;
				}

			}
			else if (step == OperatingRound.STEP_PAYOUT)
			{

				revenueCaption.setHighlight(true);
				button1.setText(WITHHOLD);
				button1.setActionCommand(WITHHOLD);
				button1.setMnemonic(KeyEvent.VK_W);
				button1.setEnabled(true);
				button1.setVisible(true);

				button2.setText(SPLIT);
				button2.setActionCommand(SPLIT);
				button2.setMnemonic(KeyEvent.VK_S);
				button2.setEnabled(companies[orCompIndex].isSplitAllowed());

				button3.setText(PAYOUT);
				button3.setActionCommand(PAYOUT);
				button3.setMnemonic(KeyEvent.VK_P);
				button3.setEnabled(true);
			}
			else if (step == OperatingRound.STEP_BUY_TRAIN)
			{
				trainCaption.setHighlight(true);

				button1.setText(BUY_TRAIN);
				button1.setActionCommand(BUY_TRAIN);
				button1.setMnemonic(KeyEvent.VK_T);
				button1.setEnabled(true);
				button1.setVisible(true);

				button2.setText(BUY_PRIVATE);
				button2.setActionCommand(BUY_PRIVATE);
				button2.setMnemonic(KeyEvent.VK_V);
				button2.setEnabled(privatesCanBeBought);
				privatesCaption.setHighlight(privatesCanBeBought);

				button3.setText(DONE);
				button3.setActionCommand(DONE);
				button3.setMnemonic(KeyEvent.VK_D);
				button3.setEnabled(true);

			}
			else if (step == OperatingRound.STEP_FINAL)
			{
				button1.setEnabled(false);
			}
		}
		else
		{
			setORCompanyTurn(-1);
		}
	}

	private void reorderCompanies()
	{
		if (!(round instanceof OperatingRound))
			return;

		companies = ((OperatingRound) round).getOperatingCompanies();
		nc = companies.length;

		for (int i = 0; i < nc; i++)
		{
			c = companies[i];
			leftCompName[i].setText(c.getName());
			leftCompName[i].setBackground(c.getBgColour());
			leftCompName[i].setForeground(c.getFgColour());

			president[i].setText(c.hasStarted() ? c.getPresident().getName()
					: "");

			sharePrice[i].setModel(c.getCurrentPriceModel());

			cash[i].setModel(c.getCashModel());

			if (privatesCanBeBought)
			{
				privates[i].setModel(c.getPortfolio().getPrivatesModel());

				newPrivatesCost[i].setText("");
			}

			tiles[i].setText("");

			tileCost[i].setText("");

			tokens[i].setText("");

			tokenCost[i].setText("");

			trains[i].setModel(c.getPortfolio().getTrainsModel());

			newTrainCost[i].setText("");

			rightCompName[i].setText(c.getName());
			rightCompName[i].setBackground(c.getBgColour());
			rightCompName[i].setForeground(c.getFgColour());
		}

	}

	//FIXME:  This needs to be moved somewhere else. Perhaps ORWindow or HexMap.
	// Having this method located in this class does not make sense. 
	public void layTile(MapHex hex, TileI tile, int orientation)
	{
		if (!(round instanceof OperatingRound))
			return;
		OperatingRound oround = (OperatingRound) round;

		if (tile == null)
		{
			oround.skip(orCompName);
		}
		else
		{
			// Let model process this first
			if (oround.layTile(orCompName, hex, tile, orientation))
			{
				// Display the results
				int cost = oround.getLastTileLayCost();
				tileCost[orCompIndex].setText(cost > 0 ? Bank.format(cost) : "");
				tiles[orCompIndex].setText(oround.getLastTileLaid());
			}
			else
			{
				displayError();
			}
			button3.setEnabled(true);

		}

		LogWindow.addLog();

		if (oround.getStep() != OperatingRound.STEP_LAY_TRACK)
		{
			this.requestFocus();
		}

		updateStatus();
	}

	//FIXME:  This needs to be moved somewhere else. Perhaps ORWindow or HexMap.
	// Having this method located in this class does not make sense. 
	public void layBaseToken(MapHex hex, int station)
	{
		if (!(round instanceof OperatingRound))
			return;
		OperatingRound oround = (OperatingRound) round;

		if (hex == null)
		{
			oround.skip(orCompName);
		}
		else if (oround.layBaseToken(orCompName, hex, station))
		{
			// Let model process this first
			int cost = oround.getLastBaseTokenLayCost();
			tokenCost[orCompIndex].setText(cost > 0 ? Bank.format(cost) : "");
			tokens[orCompIndex].setText(oround.getLastBaseTokenLaid());
		}
		else
		{
			displayError();
		}
		button3.setEnabled(true);

		LogWindow.addLog();

		if (oround.getStep() != OperatingRound.STEP_LAY_TOKEN)
		{
			GameUILoader.orWindow.enableBaseTokenLaying(false);
			this.requestFocus();
		}
		updateStatus();

	}

	private void setRevenue(int amount)
	{
		if (!(round instanceof OperatingRound))
			return;
		OperatingRound oround = (OperatingRound) round;

		revenue[orCompIndex].setText(Bank.format(amount));
		oround.setRevenue(orCompName, amount);
		setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);
		// gameStatus.updateRevenue(orComp.getPublicNumber());
		if (oround.getStep() != OperatingRound.STEP_PAYOUT)
		{
			// The next step is skipped, so update all cash and the share
			// price
			//StockChart.refreshStockPanel();
			repaint();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent actor)
	{
		if (!(round instanceof OperatingRound))
			return;
		OperatingRound oround = (OperatingRound) round;

		String command = actor.getActionCommand();
		int step = oround.getStep();
		boolean done = command.equals(DONE);
		int amount;

		if (command.equals(LAY_TRACK) || done
				&& step == OperatingRound.STEP_LAY_TRACK)
		{
			// This is just a No-op. Tile handling is now all done in
			// layTrack().
		}
		else if (command.equals(LAY_TOKEN) || done
				&& step == OperatingRound.STEP_LAY_TOKEN)
		{
			// This is just a No-op. Tile handling is now all done in
			// layBaseToken().
		}
		else if (command.equals(SET_REVENUE) || done
				&& step == OperatingRound.STEP_CALC_REVENUE)
		{
			amount = done ? 0
					: ((Integer) revenueSelect[orCompIndex].getValue()).intValue();
			setRevenue(amount);

		}
		else if (command.equals(PAYOUT))
		{
			decision[orCompIndex].setText(PAYOUT);
			oround.fullPayout(orCompName);
			//StockChart.refreshStockPanel();
			repaint();
		}
		else if (command.equals(SPLIT))
		{
			decision[orCompIndex].setText(SPLIT);
			oround.splitPayout(orCompName);
			//StockChart.refreshStockPanel();
			repaint();
		}
		else if (command.equals(WITHHOLD))
		{
			decision[orCompIndex].setText(WITHHOLD);
			oround.withholdPayout(orCompName);
			//StockChart.refreshStockPanel();
			repaint();
		}
		else if (command.equals(BUY_TRAIN))
		{
			ArrayList trainsForSale = new ArrayList();
			trainsForSale.add("None");
			TrainI train;
			int i;

			TrainI[] trainsList = (TrainI[]) TrainManager.get()
					.getAvailableNewTrains()
					.toArray(new TrainI[0]);
			for (i = 0; i < trainsList.length; i++)
			{
				trainsForSale.add(trainsList[i].getName()
						+ "-train from IPO at "
						+ Bank.format(trainsList[i].getCost()));
				if (trainsList[i].canBeExchanged()
						&& orComp.getPortfolio().getTrains().length > 0)
				{
					trainsForSale.add(trainsList[i].getName()
							+ "-train from IPO (exchanged) at "
							+ Bank.format(trainsList[i].getType()
									.getFirstExchangeCost()));
				}
			}
			trainsList = Bank.getPool().getUniqueTrains();
			for (i = 0; i < trainsList.length; i++)
			{
				trainsForSale.add(trainsList[i].getName()
						+ "-train from Pool at "
						+ Bank.format(trainsList[i].getCost()));
			}
			for (int j = 0; j < nc; j++)
			{
				c = companies[j];
				if (c == orComp)
					continue;
				trainsList = c.getPortfolio().getUniqueTrains();
				for (i = 0; i < trainsList.length; i++)
				{
					trainsForSale.add(trainsList[i].getName() + "-train from "
							+ c.getName());
				}
			}

			String boughtTrain = (String) JOptionPane.showInputDialog(this,
					"Buy which train?",
					"Which train",
					JOptionPane.QUESTION_MESSAGE,
					null,
					trainsForSale.toArray(),
					trainsForSale.get(1));
			if (Util.hasValue(boughtTrain))
			{
				Matcher m = buyTrainPattern.matcher(boughtTrain);
				if (m.matches()) // Why does this sometimes give a
				// NullPointerException?
				{
					String trainType = m.group(1);
					String sellerName = m.group(2);
					boolean exchanging = (m.group(3) != null);
					TrainTypeI type = TrainManager.get()
							.getTypeByName(trainType);
					train = null;
					TrainI exchangedTrain = null;
					Portfolio seller = null;
					int price = 0;
					if (type != null)
					{
						if (sellerName.equals("IPO"))
						{
							seller = Bank.getIpo();
						}
						else if (sellerName.equals("Pool"))
						{
							seller = Bank.getPool();
						}
						else if (sellerName != null)
						{
							PublicCompanyI sellingComp = Game.getCompanyManager()
									.getPublicCompany(sellerName);
							if (sellingComp != null)
							{
								seller = sellingComp.getPortfolio();

								String sPrice = JOptionPane.showInputDialog(this,
										orComp.getName() + " buys "
												+ boughtTrain
												+ " for which price from "
												+ sellerName + "?",
										"Which price?",
										JOptionPane.QUESTION_MESSAGE);
								try
								{
									price = Integer.parseInt(sPrice);
								}
								catch (NumberFormatException e)
								{
									price = 0; // FIXME: Need better error handling!
								}
							}
						}
					}

					if (seller != null)
					{
						train = seller.getTrainOfType(type);
					}

					if (train != null && exchanging)
					{

						TrainI[] oldTrains = orComp.getPortfolio()
								.getUniqueTrains();
						String[] options = new String[oldTrains.length + 1];
						options[0] = "None";
						for (int j = 0; j < oldTrains.length; j++)
						{
							options[j + 1] = oldTrains[j].getName();
						}
						String exchangedTrainName = (String) JOptionPane.showInputDialog(this,
								"Which train to exchange for "
										+ Bank.format(type.getFirstExchangeCost()),
								"Which train to exchange",
								JOptionPane.QUESTION_MESSAGE,
								null,
								options,
								options[1]);
						if (exchangedTrainName != null
								&& !exchangedTrainName.equalsIgnoreCase("None"))
						{
							price = type.getFirstExchangeCost();
							exchangedTrain = orComp.getPortfolio()
									.getTrainOfType(exchangedTrainName);
						}

					}

					if (train != null)
					{
						if (!oround.buyTrain(orComp.getName(),
								train,
								price,
								exchangedTrain))
						{
							JOptionPane.showMessageDialog(this,
									Log.getErrorBuffer());
						}
						else
						{
							/*
							if (seller.getOwner() instanceof PublicCompanyI)
							{
								for (int k = 0; k < companies.length; k++)
								{
									if (companies[i] == seller.getOwner())
									{
										updateCash(k);
										break;
									}
								}
							}
							else */ if (seller == Bank.getIpo())
							{

								if (TrainManager.get().hasAvailabilityChanged())
								{
									TrainManager.get()
											.resetAvailabilityChanged();
								}
							}
							trainsBought.add(train);
							newTrainCost[orCompIndex].setText(Bank.format(oround.getLastTrainBuyCost()));

							// Check if any trains must be discarded
							if (TrainManager.get().hasPhaseChanged())
							{
								Iterator it = Game.getCompanyManager()
										.getCompaniesWithExcessTrains()
										.iterator();
								while (it.hasNext())
								{
									PublicCompanyI c = (PublicCompanyI) it.next();
									TrainI[] oldTrains = c.getPortfolio()
											.getUniqueTrains();
									String[] options = new String[oldTrains.length];
									for (int j = 0; j < oldTrains.length; j++)
									{
										options[j] = oldTrains[j].getName();
									}
									String discardedTrainName = (String) JOptionPane.showInputDialog(this,
											"Company "
													+ c.getName()
													+ " has too many trains. Which train to discard?",
											"Which train to exchange",
											JOptionPane.QUESTION_MESSAGE,
											null,
											options,
											options[0]);
									if (discardedTrainName != null)
									{
										TrainI discardedTrain = c.getPortfolio()
												.getTrainOfType(discardedTrainName);
										c.getPortfolio()
												.discardTrain(discardedTrain);
									}
								}
							}
						}

					}
				}
			}

		}
		else if (command.equals(BUY_PRIVATE))
		{

			Iterator it = Game.getCompanyManager()
					.getAllPrivateCompanies()
					.iterator();
			ArrayList privatesForSale = new ArrayList();
			String privName;
			PrivateCompanyI priv;
			int minPrice = 0, maxPrice = 0;

			while (it.hasNext())
			{
				priv = (PrivateCompanyI) it.next();
				if (priv.getPortfolio().getOwner() instanceof Player)
				{
					minPrice = (int) (priv.getBasePrice() * orComp.getLowerPrivatePriceFactor());
					maxPrice = (int) (priv.getBasePrice() * orComp.getUpperPrivatePriceFactor());
					privatesForSale.add(priv.getName() + " ("
							+ Bank.format(minPrice) + " - "
							+ Bank.format(maxPrice) + ")");
				}
			}

			if (privatesForSale.size() > 0)
			{
				try
				{
					privName = (String) JOptionPane.showInputDialog(this,
							"Buy which private?",
							"Which Private?",
							JOptionPane.QUESTION_MESSAGE,
							null,
							privatesForSale.toArray(),
							privatesForSale.get(0));
					privName = privName.split(" ")[0];
					priv = Game.getCompanyManager().getPrivateCompany(privName);
					minPrice = (int) (priv.getBasePrice() * orComp.getLowerPrivatePriceFactor());
					maxPrice = (int) (priv.getBasePrice() * orComp.getUpperPrivatePriceFactor());
					String price = (String) JOptionPane.showInputDialog(this,
							"Buy " + privName + " for what price (range "
									+ Bank.format(minPrice) + " - "
									+ Bank.format(maxPrice) + ")?",
							"What price?",
							JOptionPane.QUESTION_MESSAGE);
					try
					{
						amount = Integer.parseInt(price);
					}
					catch (NumberFormatException e)
					{
						amount = 0; // This will generally be refused.
					}
					//Player prevOwner = (Player) priv.getPortfolio().getOwner();
					if (!oround.buyPrivate(orComp.getName(),
							priv.getName(),
							amount))
					{
						displayError();
					}
					else
					{
						repaint();
						newPrivatesCost[orCompIndex].setText(Bank.format(oround.getLastPrivateBuyCost()));
					}
				}
				catch (NullPointerException e)
				{
					// Null Pointer means user hit cancel. 
					// Nothing to do.
				}
			}

		}
		else if (command.equals(DONE))
		{
			oround.done(orComp.getName());
		}

		LogWindow.addLog();

		ORWindow.updateORWindow();
	}

	private void setHighlightsOff()
	{
		tileCaption.setHighlight(false);
		tokenCaption.setHighlight(false);
		revenueCaption.setHighlight(false);
		trainCaption.setHighlight(false);
		privatesCaption.setHighlight(false);
	}

	public int getOrCompIndex()
	{
		return orCompIndex;
	}

	public void setORCompanyTurn(int orCompIndex)
	{
		int j;

		if ((j = this.orCompIndex) >= 0)
		{
			president[j].setBackground(Color.WHITE);
			setSelect(revenue[j], revenueSelect[j], false);
		}

		this.orCompIndex = orCompIndex;
		orComp = orCompIndex >= 0 ? companies[orCompIndex] : null;
		orCompName = orComp != null ? orComp.getName() : "";

		if ((j = this.orCompIndex) >= 0)
		{
			// Give a new company the turn.
			this.playerIndex = companies[orCompIndex].getPresident().getIndex();
			president[j].setHighlight(true);
		}

		trainsBought = new ArrayList();
	}

	public String getORPlayer()
	{
		if (playerIndex >= 0)
			return players[playerIndex].getName();
		else
			return "";
	}

	private void setSelect(JComponent f, JComponent s, boolean active)
	{
		f.setVisible(!active);
		s.setVisible(active);
	}

	private void displayMessage(String text)
	{
		JOptionPane.showMessageDialog(this, text);
	}

	private void displayError()
	{
		JOptionPane.showMessageDialog(this, Log.getErrorBuffer());
	}

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_F1)
		{
			HelpWindow.displayHelp(GameManager.getInstance().getHelp());
			e.consume();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

}

package ui;

import game.*;
import game.model.PrivatesModel;
import game.model.TrainsModel;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import ui.StatusWindow;
import ui.elements.*;

import java.util.*;
import java.util.List;

import util.LocalText;

/**
 * This class is incorporated into StatusWindow and displays the bulk of game
 * status information.
 */
public class GameStatus extends JPanel implements ActionListener
{

	private static final int NARROW_GAP = 1;
	private static final int WIDE_GAP = 3;
	private static final int WIDE_LEFT = 1;
	private static final int WIDE_RIGHT = 2;
	private static final int WIDE_TOP = 4;
	private static final int WIDE_BOTTOM = 8;

	private static GameStatus gameStatus;
	private JFrame parent;

	private GridBagLayout gb;
	private GridBagConstraints gbc;
	private Color buttonHighlight = new Color(255, 160, 80);

	// Grid elements per function
	private Field certPerPlayer[][];
	private ClickField certPerPlayerButton[][];
	private int certPerPlayerXOffset, certPerPlayerYOffset;
	private Field certInIPO[];
	private ClickField certInIPOButton[];
	private int certInIPOXOffset, certInIPOYOffset;
	private Field certInPool[];
	private ClickField certInPoolButton[];
	private int certInPoolXOffset, certInPoolYOffset;
	private Field parPrice[];
	private int parPriceXOffset, parPriceYOffset;
	private Field currPrice[];
	private int currPriceXOffset, currPriceYOffset;
	private Field compCash[];
	private int compCashXOffset, compCashYOffset;
	private Field compRevenue[];
	private int compRevenueXOffset, compRevenueYOffset;
	private Field compTrains[];
	private int compTrainsXOffset, compTrainsYOffset;
	private Field compPrivates[];
	private int compPrivatesXOffset, compPrivatesYOffset;
	private Field playerCash[];
	private int playerCashXOffset, playerCashYOffset;
	private Field playerPrivates[];
	private int playerPrivatesXOffset, playerPrivatesYOffset;
	private Field playerWorth[];
	private int playerWorthXOffset, playerWorthYOffset;
	private Field playerCertCount[];
	private int playerCertCountXOffset, playerCertCountYOffset;
	private Field certLimit;
	private int certLimitXOffset, certLimitYOffset;
	private Field bankCash;
	private int bankCashXOffset, bankCashYOffset;
	private Field poolTrains;
	private int poolTrainsXOffset, poolTrainsYOffset;
	private Field newTrains;
	private int newTrainsXOffset, newTrainsYOffset;
	private Field futureTrains;
	private int futureTrainsXOffset, futureTrainsYOffset;

	private Caption[] upperPlayerCaption;
	private Caption[] lowerPlayerCaption;

	private int np; // Number of players
	private int nc; // NUmber of companies
	private Player[] players;
	private PublicCompanyI[] companies;
	private CompanyManagerI cm;

	private Player p;
	private PublicCompanyI c;
	private JComponent f;

	// Current state
	private int srPlayerIndex = -1;
	private int compSellIndex = -1;
	private int compBuyIPOIndex = -1;
	private int compBuyPoolIndex = -1;
	private List buyableCertificates, sellableCertificates;

	private ButtonGroup buySellGroup = new ButtonGroup();
	private ClickField dummyButton; // To be selected if none else is.

	private Map companyIndex = new HashMap();
	private Map playerIndex = new HashMap();

	public GameStatus(JFrame parent)
	{
		super();
		gameStatus = this;
		this.parent = parent;

		gb = new GridBagLayout();
		this.setLayout(gb);
		UIManager.put("ToggleButton.select", buttonHighlight);

		gbc = new GridBagConstraints();
		// updateStatus();
		setSize(800, 300);
		setLocation(0, 450);
		setBorder(BorderFactory.createEtchedBorder());
		setOpaque(false);

		players = Game.getPlayerManager().getPlayersArray();
		np = GameManager.getNumberOfPlayers();
		cm = Game.getCompanyManager();
		companies = (PublicCompanyI[]) cm.getAllPublicCompanies()
				.toArray(new PublicCompanyI[0]);
		nc = companies.length;

		init();

	}

	private void init()
	{

		certPerPlayer = new Field[nc][np];
		certPerPlayerButton = new ClickField[nc][np];
		certInIPO = new Field[nc];
		certInIPOButton = new ClickField[nc];
		certInPool = new Field[nc];
		certInPoolButton = new ClickField[nc];
		parPrice = new Field[nc];
		currPrice = new Field[nc];
		compCash = new Field[nc];
		compRevenue = new Field[nc];
		compTrains = new Field[nc];
		compPrivates = new Field[nc];
		playerCash = new Field[np];
		playerPrivates = new Field[np];
		playerWorth = new Field[np];
		playerCertCount = new Field[np];
		upperPlayerCaption = new Caption[np];
		lowerPlayerCaption = new Caption[np];

		certPerPlayerXOffset = 1;
		certPerPlayerYOffset = 2;
		certInIPOXOffset = np + 1;
		certInIPOYOffset = 2;
		certInPoolXOffset = np + 2;
		certInPoolYOffset = 2;
		parPriceXOffset = np + 3;
		parPriceYOffset = 2;
		currPriceXOffset = np + 4;
		currPriceYOffset = 2;
		compCashXOffset = np + 5;
		compCashYOffset = 2;
		compRevenueXOffset = np + 6;
		compRevenueYOffset = 2;
		compTrainsXOffset = np + 7;
		compTrainsYOffset = 2;
		compPrivatesXOffset = np + 8;
		compPrivatesYOffset = 2;
		playerCashXOffset = 1;
		playerCashYOffset = nc + 2;
		playerPrivatesXOffset = 1;
		playerPrivatesYOffset = nc + 3;
		playerWorthXOffset = 1;
		playerWorthYOffset = nc + 4;
		playerCertCountXOffset = 1;
		playerCertCountYOffset = nc + 5;
		certLimitXOffset = np + 2;
		certLimitYOffset = playerCertCountYOffset;
		bankCashXOffset = np + 2;
		bankCashYOffset = nc + 3;
		poolTrainsXOffset = np + 3;
		poolTrainsYOffset = nc + 3;
		newTrainsXOffset = np + 5;
		newTrainsYOffset = nc + 3;
		futureTrainsXOffset = np + 6;
		futureTrainsYOffset = nc + 3;

		addField(new Caption(LocalText.getText("COMPANY")),
				0,
				0,
				1,
				2,
				WIDE_RIGHT + WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("PLAYERS")),
				certPerPlayerXOffset,
				0,
				np,
				1,
				0);
		for (int i = 0; i < np; i++)
		{
			playerIndex.put(players[i], new Integer(i));
			f = upperPlayerCaption[i] = new Caption(players[i].getName());
			addField(f, certPerPlayerXOffset + i, 1, 1, 1, WIDE_BOTTOM);
		}
		addField(new Caption(LocalText.getText("BANK_SHARES")),
				certInIPOXOffset,
				0,
				2,
				1,
				WIDE_LEFT + WIDE_RIGHT);
		addField(new Caption(LocalText.getText("IPO")),
				certInIPOXOffset,
				1,
				1,
				1,
				WIDE_LEFT + WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("POOL")),
				certInPoolXOffset,
				1,
				1,
				1,
				WIDE_RIGHT + WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("PRICES")),
				parPriceXOffset,
				0,
				2,
				1,
				WIDE_RIGHT);
		addField(new Caption(LocalText.getText("PAR")),
				parPriceXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("CURRENT")),
				currPriceXOffset,
				1,
				1,
				1,
				WIDE_RIGHT + WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("COMPANY_DETAILS")),
				compCashXOffset,
				0,
				4,
				1,
				0);
		addField(new Caption(LocalText.getText("CASH")),
				compCashXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("REVENUE")),
				compRevenueXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("TRAINS")),
				compTrainsXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("PRIVATES")),
				compPrivatesXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		addField(new Caption(LocalText.getText("COMPANY")),
				compPrivatesXOffset + 1,
				0,
				1,
				2,
				WIDE_LEFT + WIDE_BOTTOM);

		for (int i = 0; i < nc; i++)
		{
			c = companies[i];
			companyIndex.put(c, new Integer(i));
			f = new Caption(c.getName());
			f.setForeground(c.getFgColour());
			f.setBackground(c.getBgColour());
			addField(f, 0, certPerPlayerYOffset + i, 1, 1, WIDE_RIGHT);

			for (int j = 0; j < np; j++)
			{
				f = certPerPlayer[i][j] = new Field(players[j].getPortfolio()
						.getShareModel(c));
				addField(f,
						certPerPlayerXOffset + j,
						certPerPlayerYOffset + i,
						1,
						1,
						0);
				f = certPerPlayerButton[i][j] = new ClickField("",
						LocalText.getText("SELL"),
						LocalText.getText("ClickForSell"),
						this,
						buySellGroup);
				addField(f,
						certPerPlayerXOffset + j,
						certPerPlayerYOffset + i,
						1,
						1,
						0);
			}
			f = certInIPO[i] = new Field(Bank.getIpo().getShareModel(c));
			addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, WIDE_LEFT);
			f = certInIPOButton[i] = new ClickField(certInIPO[i].getText(),
					LocalText.getText("BUY") + LocalText.getText("IPO"),
					LocalText.getText("ClickToSelectForBuy"),
					this,
					buySellGroup);
			f.setVisible(false);
			addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, WIDE_LEFT);
			certInIPO[i].setPreferredSize(certInIPOButton[i].getPreferredSize());

			f = certInPool[i] = new Field(Bank.getPool().getShareModel(c));
			addField(f,
					certInPoolXOffset,
					certInPoolYOffset + i,
					1,
					1,
					WIDE_RIGHT);
			f = certInPoolButton[i] = new ClickField(certInPool[i].getText(),
					LocalText.getText("BUY") + LocalText.getText("POOL"),
					LocalText.getText("ClickToBuy"),
					this,
					buySellGroup);
			f.setVisible(false);
			addField(f,
					certInPoolXOffset,
					certInPoolYOffset + i,
					1,
					1,
					WIDE_RIGHT);
			certInPool[i].setPreferredSize(certInIPOButton[i].getPreferredSize());/* sic */

			f = parPrice[i] = new Field(c.getParPriceModel());
			addField(f, parPriceXOffset, parPriceYOffset + i, 1, 1, 0);

			f = currPrice[i] = new Field(c.getCurrentPriceModel());
			addField(f,
					currPriceXOffset,
					currPriceYOffset + i,
					1,
					1,
					WIDE_RIGHT);

			f = compCash[i] = new Field(c.getCashModel());
			addField(f, compCashXOffset, compCashYOffset + i, 1, 1, 0);

			f = compRevenue[i] = new Field(c.getLastRevenueModel());
			addField(f, compRevenueXOffset, compRevenueYOffset + i, 1, 1, 0);

			f = compTrains[i] = new Field(c.getPortfolio()
					.getTrainsModel()
					.option(TrainsModel.FULL_LIST));
			addField(f, compTrainsXOffset, compTrainsYOffset + i, 1, 1, 0);

			f = compPrivates[i] = new Field(c.getPortfolio()
					.getPrivatesModel()
					.option(PrivatesModel.SPACE));
			addField(f, compPrivatesXOffset, compPrivatesYOffset + i, 1, 1, 0);
			f = new Caption(c.getName());
			f.setForeground(c.getFgColour());
			f.setBackground(c.getBgColour());
			addField(f,
					compPrivatesXOffset + 1,
					compPrivatesYOffset + i,
					1,
					1,
					WIDE_LEFT);
		}

		// Player possessions
		addField(new Caption(LocalText.getText("CASH")),
				0,
				playerCashYOffset,
				1,
				1,
				WIDE_TOP + WIDE_RIGHT);
		for (int i = 0; i < np; i++)
		{
			f = playerCash[i] = new Field(players[i].getCashModel());
			addField(f,
					playerCashXOffset + i,
					playerCashYOffset,
					1,
					1,
					WIDE_TOP);
		}

		addField(new Caption("Privates"),
				0,
				playerPrivatesYOffset,
				1,
				1,
				WIDE_RIGHT);
		for (int i = 0; i < np; i++)
		{
			f = playerPrivates[i] = new Field(players[i].getPortfolio()
					.getPrivatesModel()
					.option(PrivatesModel.BREAK));
			addField(f,
					playerPrivatesXOffset + i,
					playerPrivatesYOffset,
					1,
					1,
					0);
		}

		addField(new Caption(LocalText.getText("WORTH")),
				0,
				playerWorthYOffset,
				1,
				1,
				WIDE_RIGHT);
		for (int i = 0; i < np; i++)
		{
			f = playerWorth[i] = new Field(players[i].getWorthModel(), true);
			addField(f, playerWorthXOffset + i, playerWorthYOffset, 1, 1, 0);
		}

		addField(new Caption("Certs"),
				0,
				playerCertCountYOffset,
				1,
				1,
				WIDE_RIGHT + WIDE_TOP);
		for (int i = 0; i < np; i++)
		{
			f = playerCertCount[i] = new Field(players[i].getCertCountModel(),
					true);
			addField(f,
					playerCertCountXOffset + i,
					playerCertCountYOffset,
					1,
					1,
					WIDE_TOP);
		}

		for (int i = 0; i < np; i++)
		{
			f = lowerPlayerCaption[i] = new Caption(players[i].getName());
			addField(f, i + 1, playerCertCountYOffset + 1, 1, 1, WIDE_TOP);
		}

		// Certificate Limit
		addField(new Caption(LocalText.getText("LIMIT")),
				certLimitXOffset - 1,
				certLimitYOffset,
				1,
				1,
				WIDE_TOP + WIDE_LEFT);
		addField(new Field("" + Player.getCertLimit()),
				certLimitXOffset,
				certLimitYOffset,
				1,
				1,
				WIDE_TOP);

		// Bank
		addField(new Caption(LocalText.getText("BANK")),
				bankCashXOffset - 1,
				bankCashYOffset - 1,
				1,
				2,
				WIDE_TOP + WIDE_LEFT);
		addField(new Caption("Cash"),
				bankCashXOffset,
				bankCashYOffset - 1,
				1,
				1,
				WIDE_TOP);
		bankCash = new Field(Bank.getInstance().getCashModel());
		addField(bankCash, bankCashXOffset, bankCashYOffset, 1, 1, 0);

		addField(new Caption("Used trains"),
				poolTrainsXOffset,
				poolTrainsYOffset - 1,
				2,
				1,
				WIDE_TOP + WIDE_RIGHT);
		poolTrains = new Field(Bank.getPool()
				.getTrainsModel()
				.option(TrainsModel.FULL_LIST));
		addField(poolTrains,
				poolTrainsXOffset,
				poolTrainsYOffset,
				2,
				1,
				WIDE_RIGHT);

		// New trains
		addField(new Caption("New tr."),
				newTrainsXOffset,
				newTrainsYOffset - 1,
				1,
				1,
				WIDE_TOP);
		newTrains = new Field(Bank.getIpo()
				.getTrainsModel()
				.option(TrainsModel.ABBR_LIST));
		addField(newTrains, newTrainsXOffset, newTrainsYOffset, 1, 1, 0);

		dummyButton = new ClickField("", "", "", this, buySellGroup);

		// Future trains
		addField(new Caption("Future trains"),
				futureTrainsXOffset,
				futureTrainsYOffset - 1,
				3,
				1,
				WIDE_LEFT + WIDE_TOP);
		futureTrains = new Field(Bank.getUnavailable()
				.getTrainsModel()
				.option(TrainsModel.ABBR_LIST));
		addField(futureTrains,
				futureTrainsXOffset,
				futureTrainsYOffset,
				3,
				1,
				WIDE_LEFT);

		dummyButton = new ClickField("", "", "", this, buySellGroup);

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

		add(comp, gbc);

	}

	public static GameStatus getInstance()
	{
		return gameStatus;
	}

	public void repaint()
	{
		super.repaint();
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
		if (source instanceof ClickField)
		{
			gbc = gb.getConstraints(source);
			if (command.equals(LocalText.getText("SELL")))
			{
				compSellIndex = gbc.gridy - certPerPlayerYOffset;
				compBuyIPOIndex = compBuyPoolIndex = -1;
				((StatusWindow) parent).enableSellButton(true);
			}
			else if (command.equals(LocalText.getText("BUY")
					+ LocalText.getText("IPO")))
			{
				compBuyIPOIndex = gbc.gridy - certInIPOYOffset;
				compSellIndex = compBuyPoolIndex = -1;
				((StatusWindow) parent).enableBuyButton(true);
			}
			else if (command.equals(LocalText.getText("BUY")
					+ LocalText.getText("POOL")))
			{
				compBuyPoolIndex = gbc.gridy - certInPoolYOffset;
				compSellIndex = compBuyIPOIndex = -1;
				((StatusWindow) parent).enableBuyButton(true);
			}
		}
		repaint();

	}

	public int getCompIndexToSell()
	{
		return compSellIndex;
	}

	public int getCompIndexToBuyFromIPO()
	{
		return compBuyIPOIndex;
	}

	public int getCompIndexToBuyFromPool()
	{
		return compBuyPoolIndex;
	}

	public List getBuyOrSellOptions()
	{
		if (compBuyIPOIndex >= 0)
		{
			return this.certInIPOButton[compBuyIPOIndex].getOptions();
		}
		else if (compBuyPoolIndex >= 0)
		{
			return certInPoolButton[compBuyPoolIndex].getOptions();
		}
		else if (compSellIndex >= 0)
		{
			return this.certPerPlayerButton[srPlayerIndex][compSellIndex].getOptions();
		}
		else
		{
			return new ArrayList();
		}
	}

	public void setSRPlayerTurn(int selectedPlayerIndex)
	{
		int i, j, share;

		dummyButton.setSelected(true);

		if ((j = this.srPlayerIndex) >= 0)
		{
			upperPlayerCaption[j].setHighlight(false);
			lowerPlayerCaption[j].setHighlight(false);
			for (i = 0; i < nc; i++)
			{
				setPlayerCertButton(i, j, false);
			}
		}

		this.srPlayerIndex = selectedPlayerIndex;

		if ((j = this.srPlayerIndex) >= 0)
		{

			StockRound stockRound = (StockRound) GameManager.getInstance()
					.getCurrentRound();

			upperPlayerCaption[j].setHighlight(true);
			lowerPlayerCaption[j].setHighlight(true);

			for (i = 0; i < nc; i++)
			{
				setIPOCertButton(i, false);
				setPoolCertButton(i, false);
				setPlayerCertButton(i, j, false);
			}
			TradeableCertificate tCert;
			PublicCertificateI cert;
			int index;

			for (Iterator it = buyableCertificates.iterator(); it.hasNext();)
			{
				tCert = (TradeableCertificate) it.next();
				cert = tCert.getCert();
				index = cert.getCompany().getPublicNumber();
				if (cert.getPortfolio() == Bank.getIpo())
				{
					setIPOCertButton(index, true, tCert);
				}
				else
				{
					setPoolCertButton(index, true, tCert);
				}
			}

			for (Iterator it = sellableCertificates.iterator(); it.hasNext();)
			{
				tCert = (TradeableCertificate) it.next();
				cert = tCert.getCert();
				index = cert.getCompany().getPublicNumber();
				setPlayerCertButton(index, j, true, tCert);
			}

		}
		else
		{
			for (i = 0; i < nc; i++)
			{
				setIPOCertButton(i, false);
				setPoolCertButton(i, false);
			}
		}

		((StatusWindow) parent).enableBuyButton(false);
		((StatusWindow) parent).enableSellButton(false);
		repaint();
	}

	public void setBuyableCertificates(List certs)
	{
		buyableCertificates = certs;
	}

	public void setSellableCertificates(List certs)
	{
		sellableCertificates = certs;
	}

	public String getSRPlayer()
	{
		if (srPlayerIndex >= 0)
			return players[srPlayerIndex].getName();
		else
			return "";
	}

	private void setPlayerCertButton(int i, int j, boolean clickable, Object o)
	{

		setPlayerCertButton(i, j, clickable);
		if (clickable)
			certPerPlayerButton[i][j].addOption(o);
	}

	private void setPlayerCertButton(int i, int j, boolean clickable)
	{
		if (clickable)
		{
			certPerPlayerButton[i][j].setText(certPerPlayer[i][j].getText());
		}
		certPerPlayer[i][j].setVisible(!clickable);
		certPerPlayerButton[i][j].setVisible(clickable);
	}

	private void setIPOCertButton(int i, boolean clickable, Object o)
	{

		setIPOCertButton(i, clickable);
		if (clickable)
			certInIPOButton[i].addOption(o);
	}

	private void setIPOCertButton(int i, boolean clickable)
	{
		if (clickable)
		{
			certInIPOButton[i].setText(certInIPO[i].getText());
		}
		else
		{
			certInIPOButton[i].clearOptions();
		}
		certInIPO[i].setVisible(!clickable);
		certInIPOButton[i].setVisible(clickable);
	}

	private void setPoolCertButton(int i, boolean clickable, Object o)
	{

		setPoolCertButton(i, clickable);
		if (clickable)
			certInPoolButton[i].addOption(o);
	}

	private void setPoolCertButton(int i, boolean clickable)
	{
		if (clickable)
		{
			certInPoolButton[i].setText(certInPool[i].getText());
		}
		else
		{
			certInPoolButton[i].clearOptions();
		}
		certInPool[i].setVisible(!clickable);
		certInPoolButton[i].setVisible(clickable);
	}

}

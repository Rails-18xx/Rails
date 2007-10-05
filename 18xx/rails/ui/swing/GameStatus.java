package rails.ui.swing;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import rails.game.*;
import rails.game.action.BuyCertificate;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.ui.swing.StatusWindow;
import rails.ui.swing.elements.*;
import rails.util.LocalText;


import java.util.*;
import java.util.List;


/**
 * This class is incorporated into StatusWindow and displays the bulk of rails.game
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
	
	private static final String BUY_FROM_IPO_CMD = "BuyFromIPO";
	private static final String BUY_FROM_POOL_CMD = "BuyFromPool";
	private static final String SELL_CMD = "Sell";

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
	private Field compTokens[];
	private int compTokensXOffset, compTokensYOffset;
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
	private int certLimitXOffset, certLimitYOffset;
	private Field bankCash;
	private int bankCashXOffset, bankCashYOffset;
	private Field poolTrains;
	private int poolTrainsXOffset, poolTrainsYOffset;
	private Field newTrains;
	private int newTrainsXOffset, newTrainsYOffset;
	private Field futureTrains;
	private int futureTrainsXOffset, futureTrainsYOffset;
	private int rightCompCaptionXOffset;

	private Caption[] upperPlayerCaption;
	private Caption[] lowerPlayerCaption;

	private int np; // Number of players
	private int nc; // NUmber of companies
	private Player[] players;
	private PublicCompanyI[] companies;
	private CompanyManagerI cm;
	
    private PossibleActions possibleActions = PossibleActions.getInstance();
    
    private boolean hasParPrices = false;
	private boolean compCanBuyPrivates = false;

	private PublicCompanyI c;
	private JComponent f;

	// Current state
	private int srPlayerIndex = -1;

	private ButtonGroup buySellGroup = new ButtonGroup();
	private ClickField dummyButton; // To be selected if none else is.

	private Map<PublicCompanyI, Integer> companyIndex 
		= new HashMap<PublicCompanyI, Integer>();
	private Map<Player, Integer> playerIndex 
		= new HashMap<Player, Integer>();

	public GameStatus(JFrame parent)
	{
		super();
		gameStatus = this;
		this.parent = parent;

		gb = new GridBagLayout();
		this.setLayout(gb);
		UIManager.put("ToggleButton.select", buttonHighlight);

		gbc = new GridBagConstraints();
		setSize(800, 300);
		setLocation(0, 450);
		setBorder(BorderFactory.createEtchedBorder());
		setOpaque(false);

		players = Game.getPlayerManager().getPlayers().toArray(new Player[0]);
		np = GameManager.getNumberOfPlayers();
		cm = Game.getCompanyManager();
		companies = (PublicCompanyI[]) cm.getAllPublicCompanies()
				.toArray(new PublicCompanyI[0]);
		nc = companies.length;
		
		hasParPrices = GameManager.hasAnyParPrice();
		compCanBuyPrivates = GameManager.canAnyCompBuyPrivates();

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
		compTokens = new Field[nc];
		compPrivates = new Field[nc];
		playerCash = new Field[np];
		playerPrivates = new Field[np];
		playerWorth = new Field[np];
		playerCertCount = new Field[np];
		upperPlayerCaption = new Caption[np];
		lowerPlayerCaption = new Caption[np];

		int lastX = 0;
		int lastY = 1;
		certPerPlayerXOffset = ++lastX;
		certPerPlayerYOffset = ++lastY;
		certInIPOXOffset = (lastX += np);
		certInIPOYOffset = lastY;
		certInPoolXOffset = ++lastX;
		certInPoolYOffset = lastY;
		if (hasParPrices) {
			parPriceXOffset = ++lastX;
			parPriceYOffset = lastY;
		}
		currPriceXOffset = ++lastX;
		currPriceYOffset = lastY;
		compCashXOffset = ++lastX;
		compCashYOffset = lastY;
		compRevenueXOffset = ++lastX;
		compRevenueYOffset = lastY;
		compTrainsXOffset = ++lastX;
		compTrainsYOffset = lastY;
		compTokensXOffset = ++lastX;
		compTokensYOffset = lastY;
		if (compCanBuyPrivates) {
			compPrivatesXOffset = ++lastX;
			compPrivatesYOffset = lastY;
		}
		rightCompCaptionXOffset = ++lastX;
		
		playerCashXOffset = certPerPlayerXOffset;
		playerCashYOffset = (lastY += nc);
		playerPrivatesXOffset = certPerPlayerXOffset;
		playerPrivatesYOffset = ++lastY;
		playerWorthXOffset = certPerPlayerXOffset;
		playerWorthYOffset = ++lastY;
		playerCertCountXOffset = certPerPlayerXOffset;
		playerCertCountYOffset = ++lastY;
		certLimitXOffset = certInPoolXOffset;
		certLimitYOffset = playerCertCountYOffset;
		bankCashXOffset = certInPoolXOffset;
		bankCashYOffset = playerPrivatesYOffset;
		poolTrainsXOffset = bankCashXOffset + 1;
		poolTrainsYOffset = playerPrivatesYOffset;
		newTrainsXOffset = compCashXOffset;
		newTrainsYOffset = playerPrivatesYOffset;
		futureTrainsXOffset = compRevenueXOffset;
		futureTrainsYOffset = playerPrivatesYOffset;

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
		if (this.hasParPrices) {
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
		} else {
			addField(new Caption(LocalText.getText("CURRENT_PRICE")),
					currPriceXOffset,
					0,
					1,
					2,
					WIDE_RIGHT + WIDE_BOTTOM);

		}
		addField(new Caption(LocalText.getText("COMPANY_DETAILS")),
				compCashXOffset,
				0,
				this.compCanBuyPrivates ? 5 : 4,
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
		addField(new Caption(LocalText.getText("TOKENS")),
				compTokensXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		if (compCanBuyPrivates) {
			addField(new Caption(LocalText.getText("PRIVATES")),
				compPrivatesXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		}
		addField(new Caption(LocalText.getText("COMPANY")),
				rightCompCaptionXOffset,
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
						SELL_CMD,
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
					BUY_FROM_IPO_CMD,
					LocalText.getText("ClickToSelectForBuying"),
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
					BUY_FROM_POOL_CMD,
					LocalText.getText("ClickToSelectForBuying"),
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

			if (this.hasParPrices) {
				f = parPrice[i] = new Field(c.getParPriceModel());
				addField(f, parPriceXOffset, parPriceYOffset + i, 1, 1, 0);
			}

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
					.getTrainsModel());
			addField(f, compTrainsXOffset, compTrainsYOffset + i, 1, 1, 0);

			f = compTokens[i] = new Field(c.getBaseTokensModel());
			addField(f, compTokensXOffset, compTokensYOffset + i, 1, 1, 0);

			if (this.compCanBuyPrivates) {
				f = compPrivates[i] = new Field(c.getPortfolio()
						.getPrivatesOwnedModel());
				addField(f, compPrivatesXOffset, compPrivatesYOffset + i, 1, 1, 0);
			}
			
			f = new Caption(c.getName());
			f.setForeground(c.getFgColour());
			f.setBackground(c.getBgColour());
			addField(f,
					rightCompCaptionXOffset,
					certPerPlayerYOffset + i,
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
					.getPrivatesOwnedModel());
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
			f = playerWorth[i] = new Field(players[i].getWorthModel()/*, true*/);
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
				hasParPrices ? 2 : 1,
				1,
				WIDE_TOP + WIDE_RIGHT);
		poolTrains = new Field(Bank.getPool()
				.getTrainsModel());
		addField(poolTrains,
				poolTrainsXOffset,
				poolTrainsYOffset,
				hasParPrices ? 2 : 1,
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
				.getTrainsModel());
		addField(newTrains, newTrainsXOffset, newTrainsYOffset, 1, 1, 0);

		dummyButton = new ClickField("", "", "", this, buySellGroup);

		// Future trains
		addField(new Caption("Future trains"),
				futureTrainsXOffset,
				futureTrainsYOffset - 1,
				this.compCanBuyPrivates ? 3 : 2,
				1,
				WIDE_LEFT + WIDE_TOP);
		futureTrains = new Field(Bank.getUnavailable()
				.getTrainsModel());
		addField(futureTrains,
				futureTrainsXOffset,
				futureTrainsYOffset,
				this.compCanBuyPrivates ? 3 : 2,
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

	public void actionPerformed(ActionEvent actor)
	{
		JComponent source = (JComponent) actor.getSource();
		String command = actor.getActionCommand();
		List<PossibleAction> actions;
		
		if (source instanceof ClickField)
		{
			gbc = gb.getConstraints(source);
			if (command.equals(SELL_CMD))
			{
				actions = ((ClickField)source).getPossibleActions();
				List<String> options = new ArrayList<String>();
				List<SellShares> sellActions = new ArrayList<SellShares>();
				List<Integer> sellAmounts = new ArrayList<Integer>();
				SellShares sale;
				for (PossibleAction action : actions) {
					sale = (SellShares) action;
					
					for (int i=1; i<=sale.getMaximumNumber(); i++) {
						options.add(LocalText.getText("SellShares", new String[] {
								String.valueOf(i * sale.getShare()),
								sale.getCompanyName(),
								Bank.format(i * sale.getShareUnits() * sale.getPrice())
						}));
						sellActions.add (sale);
						sellAmounts.add (i);
					}
				}
				int index = 0;
				if (options.size() > 1) {
					String message = LocalText.getText("PleaseSelect");
					String sp = (String) JOptionPane.showInputDialog(this,
							message,
							message,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options.toArray(new String[0]),
							options.get(0));
					index = options.indexOf(sp);
				} else if (options.size() == 1) {
					String message = LocalText.getText("PleaseConfirm");
					int result = JOptionPane.showConfirmDialog(this,
							options.get(0),
							message,
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					index = (result == JOptionPane.OK_OPTION ? 0 : -1);
				}
				if (index < 0) {
					// cancelled
				} else {
					SellShares chosenAction = sellActions.get(index);
					((SellShares)chosenAction).setNumberSold(sellAmounts.get(index));
					((StatusWindow) parent).process (chosenAction);
				}
			}
			else if (command.equals(BUY_FROM_IPO_CMD)
					|| command.equals(BUY_FROM_POOL_CMD)) {

                boolean startCompany = false;
				
				actions = ((ClickField)source).getPossibleActions();
				List<String> options = new ArrayList<String>();
				List<BuyCertificate> buyActions = new ArrayList<BuyCertificate>();
				List<Integer> buyAmounts = new ArrayList<Integer>();
				BuyCertificate buy;
				PublicCertificateI cert;
				String companyName = "";
				
				for (PossibleAction action : actions) {
					buy = (BuyCertificate) action;
					cert = buy.getCertificate();
					
					if (buy instanceof StartCompany) {
						
						startCompany = true;
						int[] startPrices = ((StartCompany)buy).getStartPrices();
						PublicCompanyI company = cert.getCompany();
						companyName = company.getName();
						for (int i=0; i<startPrices.length; i++) {
							options.add(LocalText.getText("StartCompany", new String[] {
									Bank.format(startPrices[i]),
									String.valueOf(cert.getShare()),
									Bank.format(cert.getShares() * startPrices[i])
							}));
							buyActions.add (buy);
							buyAmounts.add (startPrices[i]);
						}
						
					} else {
					
						options.add (LocalText.getText("BuyCertificate", new String[] {
								String.valueOf(cert.getShare()),
								cert.getCompany().getName(),
								cert.getPortfolio().getName(),
								Bank.format(cert.getShares() * buy.getPrice())
						}));
						buyActions.add (buy);
						buyAmounts.add (1);
						for (int i=2; i<=buy.getMaximumNumber(); i++) {
							options.add(LocalText.getText("BuyCertificates", new String[] {
									String.valueOf(i),
									String.valueOf(cert.getShare()),
									cert.getCompany().getName(),
									cert.getPortfolio().getName(),
									Bank.format(cert.getShares() * buy.getPrice())
							}));
							buyActions.add (buy);
							buyAmounts.add (i);
						}
					}
				}
				int index = 0;
				if (options.size() > 1) {
					if (startCompany) {
						index = new RadioButtonDialog (this, 
				                LocalText.getText("PleaseSelect"),
				                LocalText.getText("WHICH_START_PRICE", companyName),
				                (String[]) options.toArray(new String[0]),
				                -1).getSelectedOption();
					} else {
					String sp = (String) JOptionPane.showInputDialog(this,
							LocalText.getText(startCompany ? "WHICH_PRICE" : "HOW_MANY_SHARES"),
							LocalText.getText("PleaseSelect"),
							JOptionPane.QUESTION_MESSAGE,
							null,
							options.toArray(new String[0]),
							options.get(0));
					index = options.indexOf(sp);
					}
				} else if (options.size() == 1) {
					int result = JOptionPane.showConfirmDialog(this,
							options.get(0),
							LocalText.getText("PleaseConfirm"),
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					index = (result == JOptionPane.OK_OPTION ? 0 : -1);
				}
				if (index < 0) {
					// cancelled
				} else if (startCompany) {
					StartCompany chosenAction = (StartCompany) buyActions.get(index);
					chosenAction.setStartPrice(buyAmounts.get(index));
					chosenAction.setNumberBought(chosenAction.getCertificate().getShares());
					((StatusWindow) parent).process (chosenAction);
				} else {
					BuyCertificate chosenAction = buyActions.get(index);
					chosenAction.setNumberBought(buyAmounts.get(index));
					((StatusWindow) parent).process (chosenAction);
				}
			}
		}
		repaint();

	}

    public void setSRPlayerTurn(int selectedPlayerIndex)
	{
		int i, j;

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
		for (i = 0; i < nc; i++)
		{
			setIPOCertButton(i, false);
			setPoolCertButton(i, false);
		}

		this.srPlayerIndex = selectedPlayerIndex;

		if ((j = this.srPlayerIndex) >= 0)
		{
			upperPlayerCaption[j].setHighlight(true);
			lowerPlayerCaption[j].setHighlight(true);

			PublicCertificateI cert;
			int index;

			List<BuyCertificate> buyableCerts = possibleActions.getType(BuyCertificate.class);
			if (buyableCerts != null) {
				for (BuyCertificate bCert : buyableCerts)
				{
					//tCert = (TradeableCertificate) it.next();
					cert = bCert.getCertificate();
					index = cert.getCompany().getPublicNumber();
					if (cert.getPortfolio() == Bank.getIpo())
					{
						setIPOCertButton(index, true, bCert);
					}
					else
					{
						setPoolCertButton(index, true, bCert);
					}
				}
			}

			PublicCompanyI company;
			List<SellShares> sellableShares = possibleActions.getType(SellShares.class);
			if (sellableShares != null) {
				for (SellShares share : sellableShares)
				{
					company = share.getCompany();
					index = company.getPublicNumber();
					setPlayerCertButton(index, j, true, share);
				}
			}
			
			List<NullAction> nullActions = possibleActions.getType(NullAction.class);
			if (nullActions != null) {
				for (NullAction na : nullActions) {
					((StatusWindow) parent).setPassButton(na);
				}
			}
			
		}

		repaint();
	}
	
	public void setPriorityPlayer (int index) {
	    
	    for (int j=0; j<np; j++) {
			upperPlayerCaption[j].setText (players[j].getName()
					+ (j == index ? " PD" : ""));
		}
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
		if (clickable && o != null) {
			if (o instanceof PossibleAction) certPerPlayerButton[i][j].addPossibleAction((PossibleAction)o);
		}
	}

	private void setPlayerCertButton(int i, int j, boolean clickable)
	{
		if (clickable)
		{
			certPerPlayerButton[i][j].setText(certPerPlayer[i][j].getText());
		} else {
			certPerPlayerButton[i][j].clearPossibleActions();
		}
		certPerPlayer[i][j].setVisible(!clickable);
		certPerPlayerButton[i][j].setVisible(clickable);
	}

	private void setIPOCertButton(int i, boolean clickable, Object o)
	{

		setIPOCertButton(i, clickable);
		if (clickable && o != null) {
			if (o instanceof PossibleAction) certInIPOButton[i].addPossibleAction((PossibleAction)o);
		}
	}

	private void setIPOCertButton(int i, boolean clickable)
	{
		if (clickable)
		{
			certInIPOButton[i].setText(certInIPO[i].getText());
		}
		else
		{
			certInIPOButton[i].clearPossibleActions();
		}
		certInIPO[i].setVisible(!clickable);
		certInIPOButton[i].setVisible(clickable);
	}

	private void setPoolCertButton(int i, boolean clickable, Object o)
	{

		setPoolCertButton(i, clickable);
		if (clickable && o != null) {
			if (o instanceof PossibleAction) certInPoolButton[i].addPossibleAction((PossibleAction)o);
		}
	}

	private void setPoolCertButton(int i, boolean clickable)
	{
		if (clickable)
		{
			certInPoolButton[i].setText(certInPool[i].getText());
		}
		else
		{
			certInPoolButton[i].clearPossibleActions();
		}
		certInPool[i].setVisible(!clickable);
		certInPoolButton[i].setVisible(clickable);
	}

}

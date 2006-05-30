package ui;

import game.*;
import game.special.ExchangeForShare;
import game.special.SpecialSRProperty;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import ui.elements.ClickField;

import java.util.*;
import java.util.List;

/**
 * This is the Window used for displaying nearly all of the game status. This is
 * also from where the ORWindow and StartRoundWindow are triggered.
 */
public class StatusWindow extends JFrame implements ActionListener, KeyListener
{

	private JPanel buttonPanel;
	private GameStatus gameStatus;
	private JButton buyButton, sellButton, passButton, extraButton;
	private Player player;
	private PublicCompanyI[] companies;
	private PublicCompanyI company;
	private CompanyManagerI cm;
	private Portfolio ipo, pool;
	private int compIndex, playerIndex;

	/*----*/
	private GameManager gmgr;
	private Round currentRound;
	private Round previousRound = null;
	private StockRound stockRound;
	private List buyableCertificates;
	private List sellableCertificates;
	private StartRound startRound;
	private StartRoundWindow startRoundWindow;
	private OperatingRound operatingRound;
	//private ORWindow orWindow;
	private int np = GameManager.getNumberOfPlayers();
	private int nc;

	JPanel pane = new JPanel(new BorderLayout());

	private JMenuBar menuBar;
	private static JMenu fileMenu, optMenu;
	private JMenuItem menuItem;

	/* Static Strings */
	public final static String MAP = "Map";
	public final static String MARKET = "Stock Market";
	public final static String LOG = "Log Window";
	public final static String QUIT = "Quit";
	public final static String SAVE = "Save";
	public final static String BUY = "Buy";
	public final static String SELL = "Sell";
	public final static String PASS = "Pass";
	public final static String DONE = "Done";
	public final static String SWAP = "Swap";

	/**
	 * Selector for the pattern to be used in keeping the individual UI fields
	 * up-to-date: 
	 * <br> - true: push changes (via the Observer/Observable pattern),
	 * <br> - false: pull everything on repaint.
	 * NOTE: currently, 'false' does not work for the stock chart. 
	 */
	public static boolean useObserver = true;

	public void initMenu()
	{
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		optMenu = new JMenu("Options");

		fileMenu.setMnemonic(KeyEvent.VK_F);
		optMenu.setMnemonic(KeyEvent.VK_O);

		menuItem = new JMenuItem(SAVE);
		menuItem.setName(SAVE);
		menuItem.setActionCommand(SAVE);
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		fileMenu.addSeparator();

		menuItem = new JMenuItem(QUIT);
		menuItem.setName(QUIT);
		menuItem.setActionCommand(QUIT);
		menuItem.setMnemonic(KeyEvent.VK_Q);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
				ActionEvent.ALT_MASK));
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		menuBar.add(fileMenu);

		menuItem = new JMenuItem("Set Scale");
		menuItem.setName("Set Scale");
		menuItem.setMnemonic(KeyEvent.VK_S);
		menuItem.addActionListener(this);
		optMenu.add(menuItem);

		optMenu.addSeparator();

		menuItem = new JCheckBoxMenuItem(MARKET);
		menuItem.setName(MARKET);
		menuItem.setActionCommand(MARKET);
		menuItem.setMnemonic(KeyEvent.VK_K);
		menuItem.addActionListener(this);
		optMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem(MAP);
		menuItem.setName(MAP);
		menuItem.setActionCommand(MAP);
		menuItem.setMnemonic(KeyEvent.VK_M);
		menuItem.addActionListener(this);
		optMenu.add(menuItem);

		menuItem = new JCheckBoxMenuItem(LOG);
		menuItem.setName(LOG);
		menuItem.setActionCommand(LOG);
		menuItem.setMnemonic(KeyEvent.VK_L);
		menuItem.addActionListener(this);
		optMenu.add(menuItem);

		menuBar.add(optMenu);

		setJMenuBar(menuBar);
	}

	public StatusWindow()
	{
		cm = Game.getCompanyManager();
		companies = (PublicCompanyI[]) cm.getAllPublicCompanies()
				.toArray(new PublicCompanyI[0]);
		ipo = Bank.getIpo();
		pool = Bank.getPool();

		gameStatus = new GameStatus(this);
		buttonPanel = new JPanel();

		extraButton = new JButton(""); // Normally invisible, for special properties.
		extraButton.setVisible(false);
		buyButton = new JButton(BUY);
		sellButton = new JButton(SELL);
		passButton = new JButton(PASS);

		buyButton.setMnemonic(KeyEvent.VK_B);
		sellButton.setMnemonic(KeyEvent.VK_S);
		passButton.setMnemonic(KeyEvent.VK_P);

		buttonPanel.add(extraButton);
		buttonPanel.add(buyButton);
		buttonPanel.add(sellButton);
		buttonPanel.add(passButton);

		buyButton.setActionCommand(BUY);
		sellButton.setActionCommand(SELL);
		passButton.setActionCommand(DONE);

		extraButton.addActionListener(this);
		buyButton.addActionListener(this);
		sellButton.addActionListener(this);
		passButton.addActionListener(this);

		setSize(800, 300);
		setLocation(25, 450);

		buttonPanel.setBorder(BorderFactory.createEtchedBorder());
		buttonPanel.setOpaque(false);

		setTitle("Rails: Game Status");
		pane.setLayout(new BorderLayout());
		init();
		initMenu();
		pane.add(gameStatus, BorderLayout.NORTH);
		pane.add(buttonPanel, BorderLayout.CENTER);
		pane.setOpaque(true);
		setContentPane(pane);
		//refreshStatus();
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/*----*/
		gmgr = GameManager.getInstance();
		currentRound = gmgr.getCurrentRound();

		updateStatus();

		gameStatus.addKeyListener(this);
		buttonPanel.addKeyListener(this);
		addKeyListener(this);
	}

	private void init()
	{
		PublicCompanyI[] companies = (PublicCompanyI[]) Game.getCompanyManager()
				.getAllPublicCompanies()
				.toArray(new PublicCompanyI[0]);
		nc = companies.length;
	}

	public void updateStatus()
	{
		currentRound = GameManager.getInstance().getCurrentRound();
		
		if (currentRound instanceof StartRound)
		{
			passButton.setEnabled(false);
			startRound = (StartRound) currentRound;
			if (startRoundWindow == null)
				startRoundWindow = new StartRoundWindow(startRound, this);
			startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());

			if (currentRound != previousRound) {
				GameUILoader.stockChart.setVisible(false);
				GameUILoader.orWindow.setVisible(false);
	
				disableCheckBoxMenuItem(MAP);
				disableCheckBoxMenuItem(MARKET);
			}
		}
		else if (currentRound instanceof StockRound)
		{

		    if ((currentRound instanceof ShareSellingRound)) {
				passButton.setEnabled(false);
				int cash = ((ShareSellingRound)currentRound).getRemainingCashToRaise();
				JOptionPane.showMessageDialog(this,
						"You must raise "+Bank.format(cash)+" by selling shares",
						"",
						JOptionPane.OK_OPTION);
		    } else {
				passButton.setEnabled(true);
		    }
			passButton.setEnabled(!(currentRound instanceof ShareSellingRound));
			stockRound = (StockRound) currentRound;
			buyableCertificates = stockRound.getBuyableCerts();
			gameStatus.setBuyableCertificates (buyableCertificates);
			gameStatus.setSRPlayerTurn(GameManager.getCurrentPlayerIndex());
			
			if (currentRound != previousRound) {

			    GameUILoader.stockChart.setVisible(true);
				GameUILoader.orWindow.setVisible(false);

				enableCheckBoxMenuItem(MARKET);
				disableCheckBoxMenuItem(MAP);
			}

			
			/* Any special properties in force? */
			player = GameManager.getCurrentPlayer();
			java.util.List specialProperties = stockRound.getSpecialProperties();
			if (specialProperties != null && specialProperties.size() > 0) {
			    /* Assume there will only one special property at a time
			     * (because we have only one extra button)
			     */
			    SpecialSRProperty sp = (SpecialSRProperty) specialProperties.get(0);
			    if (sp instanceof ExchangeForShare) {
			        extraButton.setText(((ExchangeForShare)sp).getPrivateCompany().getName()
			                +"/"+((ExchangeForShare)sp).getPublicCompanyName());
			        extraButton.setActionCommand(SWAP);
			        extraButton.setVisible(true);
			        extraButton.setEnabled(true);
			    }
			} else {
		        extraButton.setEnabled(false);
		        extraButton.setVisible(false);
			}
//System.out.println("Window: SpecProp#="+specialProperties.size());
			toFront();
		}
		else if (currentRound instanceof OperatingRound)
		{
			passButton.setEnabled(false);
			operatingRound = (OperatingRound) currentRound;

			if (currentRound != previousRound) {
				GameUILoader.stockChart.setVisible(false);
				//GameUILoader.orWindow.updateUpgradePanel();
				//GameUILoader.orWindow.updateORPanel();
				//GameUILoader.orWindow.setVisible(true);
				
				//GameUILoader.orWindow.requestFocus();
				GameUILoader.orWindow.activate();

				enableCheckBoxMenuItem(MAP);
				disableCheckBoxMenuItem(MARKET);
			}
		}
		
		pack();
		
		previousRound = currentRound;
	}

	private void enableCheckBoxMenuItem(String name)
	{
		for (int x = 0; x < optMenu.getMenuComponentCount(); x++)
		{
			try
			{
				if (optMenu.getMenuComponent(x).getName().equals(name))
				{
					((JCheckBoxMenuItem) optMenu.getMenuComponent(x)).setSelected(true);
				}
			}
			catch (NullPointerException e)
			{
				// The separator has null name. Har Har Har.
			}
		}
	}

	private void disableCheckBoxMenuItem(String name)
	{
		for (int x = 0; x < optMenu.getMenuComponentCount(); x++)
		{
			try
			{
				if (optMenu.getMenuComponent(x).getName().equals(name))
				{
					((JCheckBoxMenuItem) optMenu.getMenuComponent(x)).setSelected(false);
				}
			}
			catch (NullPointerException e)
			{
				// The separator has null name. Har Har Har.
			}
		}
	}

	public void resume(JFrame previous)
	{
		this.requestFocus();
		if (previous instanceof StartRoundWindow)
		{
			startRoundWindow.close();
			startRoundWindow = null;
		}
		
		currentRound = GameManager.getInstance().getCurrentRound();
		updateStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent actor)
	{
		int returnVal = 0;
		player = GameManager.getCurrentPlayer();

		if (actor.getActionCommand().equalsIgnoreCase(BUY))
		{
			buyButtonClicked();
			passButton.setText(DONE);
			passButton.setMnemonic(KeyEvent.VK_D);
		}
		else if (actor.getActionCommand().equalsIgnoreCase(SELL))
		{
			sellButtonClicked();
			passButton.setText(DONE);
			passButton.setMnemonic(KeyEvent.VK_D);
		}
		else if (actor.getActionCommand().equalsIgnoreCase(DONE))
		{
			stockRound.done(gameStatus.getSRPlayer());
			passButton.setText(PASS);
			passButton.setMnemonic(KeyEvent.VK_P);
		}
		else if (actor.getActionCommand().equalsIgnoreCase(SWAP)) {
		    /* Execute a special property (i.e. swap M&H for NYC) */
		    SpecialSRProperty sp = (SpecialSRProperty)stockRound.getSpecialProperties().get(0);
		    if (sp instanceof ExchangeForShare) {
		        ((ExchangeForShare)sp).execute();
				extraButton.setText("");
				extraButton.setEnabled(false);
				extraButton.setVisible(false);
		    }
		}
		else if (actor.getActionCommand().equalsIgnoreCase(QUIT))
			System.exit(0);
		// We're not going to actually DO anything with the selected file
		// until the infrastructure for saved games is built
		else if (actor.getActionCommand().equalsIgnoreCase(SAVE))
			returnVal = new JFileChooser().showSaveDialog(this);
		else if (actor.getActionCommand().equalsIgnoreCase(LOG))
		{
			GameUILoader.messageWindow.setVisible(
					((JMenuItem) actor.getSource()).isSelected());
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(MARKET))
		{
			GameUILoader.stockChart.setVisible(
					((JMenuItem) actor.getSource()).isSelected());
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(MAP))
		{
			GameUILoader.orWindow.setVisible(
					((JMenuItem) actor.getSource()).isSelected());
			return;
		}

		LogWindow.addLog();

		currentRound = GameManager.getInstance().getCurrentRound();
		if (currentRound instanceof StockRound)
			gameStatus.setSRPlayerTurn(GameManager.getCurrentPlayerIndex());
		else if (currentRound instanceof OperatingRound)
		{
			gameStatus.setSRPlayerTurn(-1);
		}
		
		updateStatus();
	}

	private void buyButtonClicked()
	{
		playerIndex = GameManager.getCurrentPlayerIndex();
		

		if ((compIndex = gameStatus.getCompIndexToBuyFromIPO()) >= 0)
		{
			company = companies[compIndex];
			if (company.hasStarted())
			{
				if (!stockRound.buyShare(player.getName(),
						ipo,
						company.getName(),
						1))
				{
					JOptionPane.showMessageDialog(this,
							Log.getErrorBuffer(),
							"",
							JOptionPane.OK_OPTION);
				}
				else
				{
					//gameStatus.updatePlayer(compIndex, playerIndex);
					//gameStatus.updateIPO(compIndex);
				}
			}
			else
			{
				startCompany();
			}
		}
		else if ((compIndex = gameStatus.getCompIndexToBuyFromPool()) >= 0)
		{
			company = companies[compIndex];
			if (company.hasStarted())
			{
				if (!stockRound.buyShare(player.getName(),
						pool,
						company.getName(),
						1))
				{
					JOptionPane.showMessageDialog(this,
							Log.getErrorBuffer(),
							"",
							JOptionPane.OK_OPTION);
				}
				else
				{
					//gameStatus.updatePlayer(compIndex, playerIndex);
					//gameStatus.updatePool(compIndex);
					 //gameStatus.updateBank();
				}
			}

		}
		else
		{
			JOptionPane.showMessageDialog(this,
					"Unable to buy share.\r\n"
							+ "You must select a company first.",
					"No share bought.",
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
				//GameUILoader.stockChart.refreshStockPanel();
			}
		}
		else
		{
			JOptionPane.showMessageDialog(this,
					"Unable to sell share.\r\n"
							+ "You must select a company first.",
					"Share not sold.",
					JOptionPane.OK_OPTION);

		}
	}

	private void startCompany()
	{
		StockMarket stockMarket = (StockMarket) Game.getStockMarket();
		List startOptions = gameStatus.getBuyOrSellOptions();
		String[] options = new String[startOptions.size()];
		for (int i=0; i<options.length; i++) {
		    options[i] = Bank.format(((TradeableCertificate)startOptions.get(i)).getPrice());
		}

		if (company != null)
		{
			String sp = (String) JOptionPane.showInputDialog(this,
					"Start company at what price?",
					"What Price?",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					options,
					options[0]);
					//stockMarket.getStartSpaces().toArray(),
					//stockMarket.getStartSpaces().get(0));
			int startPrice = Integer.parseInt(sp.replaceAll("\\D",""));
			if (!stockRound.startCompany(player.getName(),
					company.getName(),
					startPrice))
			{
				JOptionPane.showMessageDialog(this,
						Log.getErrorBuffer(),
						"",
						JOptionPane.OK_OPTION);
			}
			else
			{
				//GameUILoader.stockChart.refreshStockPanel((ArrayList) Game.getStockMarket().getStartSpaces());
				//GameUILoader.stockChart.refreshStockPanel();
			}
		}
		else
			JOptionPane.showMessageDialog(this,
					"Unable to start company.\r\n"
							+ "You must select a company first.",
					"Company not started.",
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

	public static void uncheckMenuItemBox(String itemName)
	{
		int count = optMenu.getMenuComponentCount();

		for (int i = 0; i < count; i++)
		{
			try
			{
				if (optMenu.getMenuComponent(i)
						.getName()
						.equalsIgnoreCase(itemName))
				{
					((JCheckBoxMenuItem) optMenu.getMenuComponent(i)).setSelected(false);
					optMenu.invalidate();
				}
			}
			catch (NullPointerException e)
			{
				// Seperators are null
			}
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

    public void keyPressed(KeyEvent e)
    {
    	if (e.getKeyCode() == KeyEvent.VK_F1)
    	{
    		HelpWindow.displayHelp(gmgr.getHelp());
    		e.consume();
    	}
    }

	public void keyTyped(KeyEvent e)
	{
	}
}

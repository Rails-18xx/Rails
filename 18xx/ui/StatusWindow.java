/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;
import game.special.ExchangeForShare;
import game.special.SpecialSRProperty;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * This is the Window used for displaying nearly all of the game status. This is
 * also from where the ORWindow and StartRoundWindow are triggered.
 * 
 * @Author Erik Vos
 * @author Brett
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
	private StockRound stockRound;
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
		refreshStatus();
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/*----*/
		gmgr = GameManager.getInstance();
		currentRound = gmgr.getCurrentRound();

		updateStatus();
		pack();
		
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
		if (currentRound instanceof StartRound)
		{

			passButton.setEnabled(false);
			startRound = (StartRound) currentRound;
			if (startRoundWindow == null)
				startRoundWindow = new StartRoundWindow(startRound, this);
			startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());

			GameUILoader.stockChart.setVisible(false);
			GameUILoader.orWindow.setVisible(false);

			disableCheckBoxMenuItem(MAP);
			disableCheckBoxMenuItem(MARKET);
		}
		else if (currentRound instanceof StockRound)
		{

			passButton.setEnabled(true);
			stockRound = (StockRound) currentRound;
			gameStatus.setSRPlayerTurn(GameManager.getCurrentPlayerIndex());

			GameUILoader.stockChart.setVisible(true);
			GameUILoader.orWindow.setVisible(false);

			enableCheckBoxMenuItem(MARKET);
			disableCheckBoxMenuItem(MAP);
			
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
			refreshStatus();
			toFront();
		}
		else if (currentRound instanceof OperatingRound)
		{
			passButton.setEnabled(false);
			operatingRound = (OperatingRound) currentRound;
			//if (orWindow == null)
			//	orWindow = new ORWindow(operatingRound, this);

			GameUILoader.stockChart.setVisible(false);
			GameUILoader.orWindow.setVisible(true);
			
			GameUILoader.orWindow.requestFocus();

			enableCheckBoxMenuItem(MAP);
			disableCheckBoxMenuItem(MARKET);
		}

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
		else if (previous instanceof ORWindow)
		{
			//orWindow.getORPanel().close();
			//orWindow = null;
		}
		currentRound = GameManager.getInstance().getCurrentRound();
		updateStatus();
	}

	public void refreshStatus()
	{
		gameStatus.repaint();
		// FIXME: Not an ideal fix for various repainting issues, but it works
		// well enough for now.
		this.pack();
	}

	public void repaint()
	{
		super.repaint();
		// refreshStatus();
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
		else if (actor.getActionCommand().equalsIgnoreCase(LOG)
				&& ((JMenuItem) actor.getSource()).isSelected())
		{
			GameUILoader.messageWindow.setVisible(true);
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(LOG)
				&& !((JMenuItem) actor.getSource()).isSelected())
		{
			GameUILoader.messageWindow.setVisible(false);
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(MARKET)
				&& ((JMenuItem) actor.getSource()).isSelected())
		{
			GameUILoader.stockChart.setVisible(true);
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(MARKET)
				&& !((JMenuItem) actor.getSource()).isSelected())
		{
			GameUILoader.stockChart.setVisible(false);
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(MAP)
				&& ((JMenuItem) actor.getSource()).isSelected())
		{
			GameUILoader.orWindow.setVisible(true);
			return;
		}
		else if (actor.getActionCommand().equalsIgnoreCase(MAP)
				&& !((JMenuItem) actor.getSource()).isSelected())
		{
			GameUILoader.orWindow.setVisible(false);
			return;
		}

		LogWindow.addLog();
		pack();

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
					///gameStatus.updateIPO(compIndex);
				}
			}
			else
			{
				startCompany();
			}
			// if (company.hasFloated())
			// gameStatus.updateCompany(compIndex);

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
					// gameStatus.updateBank();
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
				//gameStatus.updatePlayer(compIndex, playerIndex);
				//gameStatus.updatePool(compIndex);
				// gameStatus.updateBank();
				StockChart.refreshStockPanel();
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

		if (company != null)
		{
			StockSpace sp = (StockSpace) JOptionPane.showInputDialog(this,
					"Start company at what price?",
					"What Price?",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					stockMarket.getStartSpaces().toArray(),
					stockMarket.getStartSpaces().get(0));
			// repaint();
			// FIXME: Probably should check the boolean startCompany() returns
			// PublicCompany.startCompany(playerStatus.getPlayerSelected(),
			// companyStatus.getCompanySelected(), sp);
			if (!stockRound.startCompany(player.getName(),
					company.getName(),
					sp.getPrice()))
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
				// gameStatus.updateBank();
				StockChart.refreshStockPanel();
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

	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_F1)
		{
			HelpWindow.displayHelp(gmgr.getHelp());
			e.consume();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

	/*
	public void setOrWindow(ORWindow orWindow)
	{
		this.orWindow = orWindow;
	}

	public ORWindow getOrWindow()
	{
		return orWindow;
	}*/
}

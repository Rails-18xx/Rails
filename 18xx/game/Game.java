package game;

import org.w3c.dom.*;
import util.XmlUtils;

import java.io.File;
import java.util.*;

import util.LocalText;

public class Game
{
	/**
	 * Game is a singleton class.
	 */
	protected static Game instance;

	/** The component Manager */
	protected static ComponentManager componentManager;
	protected static GameManager gameManager;
	protected static CompanyManagerI companyManager;
	protected static PlayerManager playerManager;
	protected static StockMarketI stockMarket;
	protected static Bank bank;
	protected static ArrayList companyList;
	protected static String name;
	
	public static String[] getGames()
	{
		File dataDir = new File("./data/");
		return dataDir.list();
	}

	public static void NewGame(String gameName, ArrayList playerNames)
	{
		name = gameName;
		initialise(gameName);
		companyManager = getCompanyManager();
		companyList = (ArrayList) companyManager.getAllPublicCompanies();
		stockMarket = getStockMarket();
		playerManager = getPlayerManager(playerNames);
	}

	/**
	 * Protected constructor.
	 * 
	 * @param name
	 *            Name of the game (e.g. "1830").
	 */
	public Game()
	{

	}

	public static void initialise(String name)
	{
		Log.write("Game is " + name);
		String file = "data/" + name + "/Game.xml";
		try
		{
			// Have the ComponentManager work through the other game files
			Element elem = XmlUtils.findElementInFile(file,
					ComponentManager.ELEMENT_ID);
			ComponentManager.configureInstance(name, elem);

			componentManager = ComponentManager.getInstance();

			bank = (Bank) componentManager.findComponent("Bank");
			companyManager = (CompanyManagerI) componentManager.findComponent(CompanyManagerI.COMPONENT_NAME);
			stockMarket = (StockMarketI) componentManager.findComponent(StockMarketI.COMPONENT_NAME);
			gameManager = (GameManager) componentManager.findComponent("GameManager");

			/*
			 * Initialisations that involve relations between components can
			 * only be done after all XML has been processed.
			 */
			Bank.initIpo();
			StartPacket.init();
			companyManager.initCompanies();
			stockMarket.init();
		}
		catch (Exception e)
		{
			System.out.println(LocalText.getText("GameSetupFailed1") + file + LocalText.getText("GameSetupFailed2"));
			e.printStackTrace();
		}
		
		//We need to do this assignment after we've loaded all the XML data. 
		MapManager.assignHomesAndDestinations();
		
		System.out.println(LocalText.getText("SelectATile"));
	}

	/**
	 * Public instance creator and getter.
	 * 
	 * @param name
	 *            Name of the game (e.g. "1830").
	 * @return The instance.
	 */
	public static Game getInstance()
	{
		if (instance == null)
		{
			instance = new Game();
		}
		return instance;
	}
	
	/*----- Getters -----*/

	/**
	 * @return The company manager
	 */
	public static CompanyManagerI getCompanyManager()
	{
		return companyManager;
	}

	/**
	 * @return The company manager
	 */
	public static StockMarketI getStockMarket()
	{
		return stockMarket;
	}

	/**
	 * @return The compinent manager (maybe this getter is not needed)
	 */
	public static ComponentManager getComponentManager()
	{
		return componentManager;
	}

	/* Do the Bank properly later */
	public static Bank getBank()
	{
		return bank;
	}

	/**
	 * @return Returns the playerManager.
	 */
	public static PlayerManager getPlayerManager(ArrayList playerNames)
	{
		playerManager = new PlayerManager(playerNames);
		return playerManager;
	}

	/**
	 * @return Returns the playerManager.
	 */
	public static PlayerManager getPlayerManager()
	{
		return playerManager;
	}

	/**
	 * @return Game Name
	 */
	public static String getName()
	{
		return name;
	}
}

package rails.game;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import rails.util.LocalText;
import rails.util.XmlUtils;


import java.io.File;
import java.util.*;


public class Game
{
	public static final String version = "1.0.1";

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

	protected static Logger log = Logger.getLogger(Game.class.getPackage().getName());

	public static String[] getGames()
	{
		File dataDir = new File("./data/");
		return dataDir.list();
	}

	/**
	 * Protected constructor.
	 * 
	 * @param name
	 *            Name of the rails.game (e.g. "1830").
	 */
	protected Game()
	{

	}

	public static void initialise(String name)
	{
		Game.name = name;
		ReportBuffer.add(LocalText.getText("GameIs", name));
		log.info("========== Start of rails.game " + name + " ==========");
		String file = "data/" + name + "/Game.xml";
		try
		{
			// Have the ComponentManager work through the other rails.game files
			Element elem = XmlUtils.findElementInFile(file,
					ComponentManager.ELEMENT_ID);
			if (elem == null) {
				throw new ConfigurationException ("No Game XML element found in file "+file);
			}

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
			log.fatal (LocalText.getText("GameSetupFailed", file), e);
		}
		
		//We need to do this assignment after we've loaded all the XML data. 
		MapManager.assignHomesAndDestinations();
		
	}

	/**
	 * Public instance creator and getter.
	 * 
	 * @param name
	 *            Name of the rails.game (e.g. "1830").
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

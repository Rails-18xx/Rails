package rails.game;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import rails.util.LocalText;
import rails.util.XmlUtils;


import java.io.File;
import java.util.*;


public class Game
{
	public static final String version = "1.0.3";
	public static final String jarName = "./rails-" + version + ".jar";

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
	protected static Element componentManagerElement;
	protected static String gameFilename;

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
	
	public static void prepare(String name) {
		
		Game.name = name;
		ReportBuffer.add(LocalText.getText("GameIs", name));

		gameFilename = "data/" + name + "/Game.xml";
		try
		{
			// Have the ComponentManager work through the other rails.game files
			componentManagerElement = XmlUtils.findElementInFile(gameFilename,
					ComponentManager.ELEMENT_ID);
			if (componentManagerElement == null) {
				throw new ConfigurationException ("No Game XML element found in file "+gameFilename);
			}

			ComponentManager.configureInstance(name, componentManagerElement);

			componentManager = ComponentManager.getInstance();
			
		}
		catch (Exception e)
		{
			log.fatal (LocalText.getText("GameSetupFailed", gameFilename), e);
		}
	}

	public static void initialise()
	{
		log.info("========== Start of rails.game " + name + " ==========");

		try
		{
			// Have the ComponentManager work through the other rails.game files
			componentManager.finishPreparation();

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
			log.fatal (LocalText.getText("GameSetupFailed", gameFilename), e);
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
	public static PlayerManager getPlayerManager(List<String> playerNames)
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
	
	public static Logger getLogger () {
		return log;
	}
}

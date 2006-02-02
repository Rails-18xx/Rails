/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package game;

import org.w3c.dom.*;
import util.XmlUtils;

import java.io.File;
import java.util.*;

public class Game
{

	/*
	 * EV, 12mar2005: Generic game startup code, mostly copied from Iain Adam's
	 * TestApp class.
	 */

	/**
	 * Game is a singleton class.
	 * 
	 * @author Erik Vos
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
	
	protected static String language = "en";
	protected static String country = "";
	protected static String localeCode = language;
	protected static Locale locale;
	protected static ResourceBundle localisedText;

	public static String[] getGames()
	{
		File dataDir = new File("./data/");
		return dataDir.list();
	}

	public static void NewGame(String gameName, ArrayList playerNames)
	{
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
			System.out.println("Game setup from file " + file + " failed");
			e.printStackTrace();
		}
		
		//We need to do this assignment after we've loaded all the XML data. 
		MapManager.assignHomesAndDestinations();
		
		System.out.println(Game.getText("SelectATile"));
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
	
	public static void setLocale (String localeCode) {
	    
	    Game.localeCode = localeCode;
	    String[] codes = localeCode.split("_");
	    if (codes.length > 0) language = codes[0];
	    if (codes.length > 1) country = codes[1];
	    
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
	
	public static String getText (String key) {

	    if (key == null || key.length() == 0) return "";
	    
	    /* Load the texts */
	    if (localisedText == null) {
	        locale = new Locale (language, country);
	        localisedText =
	            ResourceBundle.getBundle("LocalisedText", locale);
	    }
	    
	    /* If the key contains a space, something is wrong, check who did that! */
	    if (key.indexOf(" ") > -1) {
	        try {
	            throw new Exception ("Invalid resource key '"+key+"'");
	        } catch (Exception e) {
	            //System.out.println(e.getMessage());
	            e.printStackTrace();
	        }
	    }
	    /* Find the text */
	    try {
	        return (localisedText.getString(key));
	    } catch (MissingResourceException e) {
	        System.out.println("Missing text for key "+key+" in locale "
	                +locale.getDisplayName()+" ("+localeCode+")");
	        /* If the text is not found, return the key in brackets */
	        return "<"+key+">";
	    }

	}
}

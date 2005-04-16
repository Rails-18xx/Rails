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

import org.w3c.dom.Element;

import util.XmlUtils;

public class Game
{
	/* EV, 12mar2005:
	 * Generic game startup code, mostly copied from Iain Adam's TestApp class. 
	 */
	
	/**
	 * Game is a singleton class.
	 * @author Erik Vos
	 */
	protected static Game instance;
	
	/** The component Manager */
	protected ComponentManager componentMan;
	protected CompanyManagerI companyManager;
	protected StockMarketI stockMarket;
	protected Bank bank;
	
	protected String name;
	
	/**
	 * Protected constructor.
	 * @param name Name of the game (e.g. "1830").
	 */
	public Game () {
		
	}
	
	public void initialise (String name) {
		
		this.name = name;
		
		bank = new Bank (); // The short way for now
		
		String file = "data/"+name+"/Game.xml";
		try{
			Element elem = XmlUtils.findElementInFile(file, ComponentManager.ELEMENT_ID);
			ComponentManager.configureInstance(name, elem);
	
			componentMan = ComponentManager.getInstance();
			companyManager = (CompanyManagerI)componentMan.findComponent(CompanyManagerI.COMPONENT_NAME);
			stockMarket = (StockMarketI)componentMan.findComponent(StockMarketI.COMPONENT_NAME);
		} catch (Exception e) {
			System.out.println ("Game setup from file " + file + " failed");
			e.printStackTrace();
		}
	}
	
	/**
	 * Public instance creator and getter.
	 * @param name Name of the game (e.g. "1830").
	 * @return The instance.
	 */
	public static Game getInstance () {
		if (instance == null) {
			instance = new Game ();
		} 
		return instance;
	}
	
	/*----- Getters -----*/
	
	/**
	 * @return The company manager
	 */
	public CompanyManagerI getCompanyManager() {
		return companyManager;
	}

	/**
	 * @return The company manager
	 */
	public StockMarketI getStockMarket() {
		return stockMarket;
	}

	/**
	 * @return The compinent manager (maybe this getter is not needed)  
	 */
	public ComponentManager getComponentMan() {
		return componentMan;
	}
	
	/* Do the Bank properly later */
	public Bank getBank() {
		return bank;
	}
	
	/* EV, 12mar2005:
	 * The below was the original code by Brett Lentz.
	 * I suppose these things will get moved elsewhere, 
	 * but I'll leave all of it now as a reminder. 
	 */
	/*
   private static int MAX_NUM_PLAYERS;

   private static int MAX_NUM_SHARES;

   private static int numPlayers;

   private static int fastestAvailableTrain;

   private static int maxNumTrains;

   private Player addPlayer()
   {
      Player player = new Player();
      return player;
   }

   private Player[] getPlayers(int numPlayers)
   {
      int count = 0;
      Player[] players;
      players = new Player[numPlayers];

      while (count < numPlayers)
      {
         players[count] = addPlayer();
         count++;
      }
      return players;
   }

   private int getMaxNumShares(int numPlayers)
   {
      switch (numPlayers)
      {
         case 2:
            return 24;
         case 3:
            return 21;
         case 4:
            return 18;
         case 5:
            return 15;
         case 6:
            return 13;
         default:
            return 0;
      }
   }

   public static int getNumPlayers()
   {
      return numPlayers;
   }

   public static int getMaxNumShares()
   {
      return MAX_NUM_SHARES;
   }

   public static int getFastestAvailableTrain()
   {
      return fastestAvailableTrain;
   }

   public static int getMaxNumTrains()
   {
      return maxNumTrains;
   }

   public Game()
   {
      MAX_NUM_PLAYERS = 0;
      numPlayers = 0;
      MAX_NUM_SHARES = 0;
      fastestAvailableTrain = 0;
      Bank bank = new Bank();
   }

   public Game(String gameName)
   {
      if (gameName.matches("1830"))
      {
         MAX_NUM_PLAYERS = 6;
         numPlayers = getNumPlayers();
         MAX_NUM_SHARES = getMaxNumShares(numPlayers);
         fastestAvailableTrain = 2;
         Bank bank = new Bank(numPlayers);
         maxNumTrains = 4;
      } else
      {
         MAX_NUM_PLAYERS = 0;
         numPlayers = 0;
         MAX_NUM_SHARES = 0;
         fastestAvailableTrain = 0;
         Bank bank = new Bank();
         maxNumTrains = 0;
      }
   }
   */

}

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
   protected ComponentManager componentMan;

   protected CompanyManagerI companyManager;

   protected StockMarketI stockMarket;

   protected Bank bank;

   protected Player player;

   protected String name;

   /**
    * Protected constructor.
    * 
    * @param name
    *           Name of the game (e.g. "1830").
    */
   public Game()
   {

   }

   public void initialise(String name)
   {

      this.name = name;

      String file = "data/" + name + "/Game.xml";
      try
      {
         // Have the ComponentManager work through the other game files
         Element elem = XmlUtils.findElementInFile(file,
               ComponentManager.ELEMENT_ID);
         ComponentManager.configureInstance(name, elem);

         componentMan = ComponentManager.getInstance();

         bank = (Bank) componentMan.findComponent("Bank");
         companyManager = (CompanyManagerI) componentMan
               .findComponent(CompanyManagerI.COMPONENT_NAME);
         stockMarket = (StockMarketI) componentMan
               .findComponent(StockMarketI.COMPONENT_NAME);

         bank.initIpo();

      }
      catch (Exception e)
      {
         System.out.println("Game setup from file " + file + " failed");
         e.printStackTrace();
      }

   }

   /**
    * Public instance creator and getter.
    * 
    * @param name
    *           Name of the game (e.g. "1830").
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
   public CompanyManagerI getCompanyManager()
   {
      return companyManager;
   }

   /**
    * @return The company manager
    */
   public StockMarketI getStockMarket()
   {
      return stockMarket;
   }

   /**
    * @return The compinent manager (maybe this getter is not needed)
    */
   public ComponentManager getComponentMan()
   {
      return componentMan;
   }

   /* Do the Bank properly later */
   public Bank getBank()
   {
      return bank;
   }

   public Player getPlayers()
   {
      return player;
   }

}

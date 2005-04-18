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

   protected CompanyManager companyManager;

   protected StockMarket stockMarket;

   protected Bank bank;

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

   public Game(String whichGame)
   {
      this.initialise(whichGame);
   }

   public void initialise(String name)
   {

      this.name = name;

      bank = new Bank(); // The short way for now

      String file = "data/" + name + "/Game.xml";

      try
      {
         Element elem = XmlUtils.findElementInFile(file,
               ComponentManager.ELEMENT_ID);
         ComponentManager.configureInstance(name, elem);
         componentMan = ComponentManager.getInstance();
         companyManager = (CompanyManager) componentMan.findComponent(CompanyManager.COMPONENT_NAME);
         stockMarket = (StockMarket) componentMan.findComponent(StockMarket.COMPONENT_NAME);
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
   public CompanyManager getCompanyManager()
   {
      return companyManager;
   }

   /**
    * @return The company manager
    */
   public StockMarket getStockMarket()
   {
      return stockMarket;
   }

   /**
    * @return The component manager (maybe this getter is not needed)
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
}
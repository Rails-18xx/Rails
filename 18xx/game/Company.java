/*
 Rails: an 18xx game system.
 Copyright (C) 2005 Brett Lentz

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Erik Vos
 */
public class Company
{

   protected static int numberOfCompanies = 0;

   protected String name;

   protected String fgcolour;

   protected String bgcolour;

   protected int number; // For internal use

   protected StockPrice parPrice = null;

   protected StockPrice currentPrice = null;

   protected int treasury;

   protected boolean hasFloated = false;

   protected boolean closed = false;

   protected boolean canBuyStock;

   protected ArrayList trainsOwned;

   protected ArrayList portfolio;

   protected ArrayList littleCoOwned;

   /* List of companies */
   protected static HashMap companies = new HashMap();

   /* Initialiser */
   static public void initialise(String game)
   {
      CompanyLoader cl = new CompanyLoader(game);
   }

   public Company(String name, String fgColour, String bgColour)
   {
      this.name = name;
      this.fgcolour = fgColour;
      this.bgcolour = bgColour;
      this.number = numberOfCompanies++;
   }

   public static void addCompany(Company company)
   {
      companies.put(company.getName(), company);
   }

   public static Company get(String companyName)
   {
      return (Company) companies.get(companyName);
   }

   public static Iterator getIterator()
   {
      return companies.values().iterator();
   }

   public void start(StockPrice startPrice)
   {
      parPrice = currentPrice = startPrice;
      hasFloated = true;
      parPrice.addToken(this);
   }

   /**
    * @return
    */
   public String getBgColour()
   {
      return bgcolour;
   }

   /**
    * @return
    */
   public boolean canBuyStock()
   {
      return canBuyStock;
   }

   /**
    * @return
    */
   public String getFgColour()
   {
      return fgcolour;
   }

   /**
    * @return
    */
   public boolean hasFloated()
   {
      return hasFloated;
   }

   /**
    * @return
    */
   public String getName()
   {
      return name;
   }

   /**
    * @return
    */
   public ArrayList getPortfolio()
   {
      return portfolio;
   }

   /**
    * @return
    */
   public StockPrice getParPrice()
   {
      return parPrice;
   }

   /**
    * @return
    */
   public ArrayList getTrainsOwned()
   {
      return trainsOwned;
   }

   /**
    * @return
    */
   public int getTreasury()
   {
      return treasury;
   }

   /**
    * @param list
    */
   public void setTrainsOwned(ArrayList list)
   {
      trainsOwned = list;
   }

   /**
    * @param i
    */
   public void setTreasury(int i)
   {
      treasury = i;
   }

   /**
    * @return
    */
   public StockPrice getCurrentPrice()
   {
      return currentPrice;
   }

   /**
    * @param price
    */
   public void setCurrentPrice(StockPrice price)
   {
      currentPrice = price;
   }

   /**
    * @param b
    */
   public void setFloated(boolean b)
   {
      hasFloated = b;
   }

   /**
    * @return
    */
   public static int getNumberOfCompanies()
   {
      return numberOfCompanies;
   }

   /**
    * @return
    */
   public int getNumber()
   {
      return number;
   }

   /**
    * @param i
    */
   public static void setNumberOfCompanies(int i)
   {
      numberOfCompanies = i;
   }

   /**
    * @return
    */
   public boolean isClosed()
   {
      return closed;
   }

   /**
    * @param b
    */
   public void setClosed(boolean b)
   {
      closed = b;
      if (closed)
         currentPrice = null;
   }

}
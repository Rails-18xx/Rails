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

 * Changes: 
 * 05mar2005 EV: Changed some names.
 */

package game;

import java.util.ArrayList;

/**
 * Objects of this class represent a square on the StockMarket.
 * 
 * @author Erik Vos
 */
public class StockPrice
{

   /*--- Class attributes ---*/

   /*--- Instance attributes ---*/
   protected String name;
   protected String colour;
   
   protected int row;
   protected int column;
   protected int price;

   protected boolean belowLedge = false; // For 1870
   protected boolean leftOfLedge = false; // For 1870
   protected boolean closesCompany = false;// For 1856 and other games
   protected boolean endsGame = false; // For 1841 and other games
   protected boolean start = false; // Company may start here

   protected ArrayList tokens = new ArrayList();

   /*--- Constants ---*/

   /*--- Contructors ---*/
   public StockPrice(String name, int price)
   {
      this(name, price, "white");
   }

   public StockPrice(String name, int price, String colour)
   {
      this.name = name;
      this.price = price;
      this.colour = colour;
      this.row = Integer.parseInt(name.substring(1)) - 1;
      this.column = (int) (name.toUpperCase().charAt(0) - '@') - 1;
   }

   // No constructors (yet) for the booleans, which are rarely needed. Use the
   // setters.

   /*--- Getters ---*/
   /**
    * @return TRUE is the square is just above a ledge.
    */
   public boolean isBelowLedge()
   {
      return belowLedge;
   }

   /**
    * @return TRUE if the square closes companies landing on it.
    */
   public boolean closesCompany()
   {
      return closesCompany;
   }

   /**
    * @return The square's colour.
    */
   public String getColour()
   {
      return colour;
   }

   /**
    * @return TRUE if the game ends if a company lands on this square.
    */
   public boolean endsGame()
   {
      return endsGame;
   }

   /**
    * @return The stock price associated with the square.
    */
   public int getPrice()
   {
      return price;
   }

   /**
    * @return
    */
   public int getColumn()
   {
      return column;
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
   public int getRow()
   {
      return row;
   }

   /*--- Setters ---*/
   /**
    * @param b
    *           See isAboveLedge.
    */
   public void setBelowLedge(boolean b)
   {
      belowLedge = b;
   }

   /**
    * @param b
    *           See isClosesCompany.
    */
   public void setClosesCompany(boolean b)
   {
      closesCompany = b;
   }

   /**
    * @param b
    *           See isEndsGame.
    */
   public void setEndsGame(boolean b)
   {
      endsGame = b;
   }

   /**
    * @return
    */
   public boolean isStart()
   {
      return start;
   }

   /**
    * @param b
    */
   public void setStart(boolean b)
   {
      start = b;
   }

   /**
    * Add a token at the end of the array (i.e. at the bottom of the pile)
    * 
    * @param company
    *           The company object to add.
    */
   public void addToken(Company company)
   {
      tokens.add(company);
   }

   /**
    * Remove a token from the pile.
    * 
    * @param company
    *           The company object to remove.
    * @return False if the token was not found.
    */
   public boolean removeToken(Company company)
   {
      int index = tokens.indexOf(company);
      if (index >= 0)
      {
         tokens.remove(index);
         return true;
      } else
      {
         return false;
      }
   }

   /**
    * @return
    */
   public ArrayList getTokens()
   {
      return tokens;
   }

   /**
    * @return
    */
   public boolean isLeftOfLedge()
   {
      return leftOfLedge;
   }

   /**
    * @param b
    */
   public void setLeftOfLedge(boolean b)
   {
      leftOfLedge = b;
   }

}
/*
 * Created on 24-Feb-2005
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
public class StockSpace implements StockSpaceI
{

   /*--- Class attributes ---*/

   /*--- Instance attributes ---*/
   protected String name;
   protected int row;
   protected int column;
   protected int price;
   protected String colour;
   protected boolean belowLedge = false; // For 1870
   protected boolean leftOfLedge = false; // For 1870
   protected boolean closesCompany = false;// For 1856 and other games
   protected boolean endsGame = false; // For 1841 and other games
   protected boolean start = false; // Company may start here
   protected boolean hasTokens = false;
   protected StockSpaceTypeI type = null;
   protected ArrayList tokens = new ArrayList();

   /*--- Contructors ---*/
   public StockSpace(String name, int price, StockSpaceTypeI type)
   {
      this.name = name;
      this.price = price;
      this.type = type;
      this.row = Integer.parseInt(name.substring(1)) - 1;
      this.column = (int) (name.toUpperCase().charAt(0) - '@') - 1;
      this.hasTokens = false;
   }

   public StockSpace(String name, int price)
   {
      this(name, price, null);
   }

   // No constructors for the booleans. Use the setters.

   /*--- Token handling methods ---*/
   /**
    * Add a token at the end of the array (i.e. at the bottom of the pile)
    * 
    * @param company
    *           The company object to add.
    */
   public void addToken(CompanyI company)
   {
      tokens.add(company);
      this.setHasTokens(true);
   }

   /**
    * Remove a token from the pile.
    * 
    * @param company
    *           The company object to remove.
    * @return False if the token was not found.
    */
   public boolean removeToken(CompanyI company)
   {
      int index = tokens.indexOf(company);
      if (index >= 0)
      {
         tokens.remove(index);
         
         if(tokens.size() < 1)
         {
            this.setHasTokens(false);
         }
         
         return true;
      }
      else
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
      return type.getColour();
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
   public StockSpaceTypeI getType()
   {
      return type;
   }

   /**
    * @return
    */
   public int getRow()
   {
      return row;
   }

   /**
    * @return
    */
   public boolean isStart()
   {
      return start;
   }

   /**
    * @return
    */
   public boolean isLeftOfLedge()
   {
      return leftOfLedge;
   }

   /**
    * @return
    */
   public boolean isNoBuyLimit()
   {
      return type != null && type.isNoBuyLimit();
   }

   /**
    * @return
    */
   public boolean isNoCertLimit()
   {
      return type != null && type.isNoCertLimit();
   }

   /**
    * @return
    */
   public boolean isNoHoldLimit()
   {
      return type != null && type.isNoHoldLimit();
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
    * @param b
    */
   public void setStart(boolean b)
   {
      start = b;
   }

   /**
    * @param b
    */
   public void setLeftOfLedge(boolean b)
   {
      leftOfLedge = b;
   }

   /**
    * @return Returns the hasTokens.
    */
   public boolean hasTokens()
   {
      return hasTokens;
   }

   /**
    * @param hasTokens
    *           The hasTokens to set.
    */
   public void setHasTokens(boolean b)
   {
      hasTokens = b;
   }
   
   public String toString()
   {
     return "Location: " + row + "," + column + " Price: " + price; 
   }
}
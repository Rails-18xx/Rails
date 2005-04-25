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

import java.util.*;

public final class Player implements CashHolder
{

   public static final int MAX_PLAYERS = 10; //this ought to be read from XML.
   private static ArrayList players = new ArrayList();
   private static int[] playerStartCash = new int[MAX_PLAYERS + 1];
   private static int[] playerCertificateLimits = new int[MAX_PLAYERS + 1];
   private static int playerCertificateLimit;
   String name;
   int wallet = 0;
   boolean hasPriority;

   /* ArrayList littleCoOwned; */
   Portfolio portfolio = null;
   ArrayList companiesSoldThisTurn;

   public static void addPlayer(String name)
   {
      players.add(new Player(name));
      Log.write("Player " + players.size() + " is " + name);
   }

   public static List getPlayers()
   {
      return players;
   }

   public static int numberOfPlayers()
   {
      return players.size();
   }

   public static Player getPlayer(int index)
   {
      return (Player) players.get(index);
   }

   public static void setLimits(int number, int cash, int certLimit)
   {
      if (number > 1 && number <= MAX_PLAYERS)
      {
         playerStartCash[number] = cash;
         playerCertificateLimits[number] = certLimit;
      }
   }

   /**
    * Initialises each Player's parameters which depend on the number of
    * players. To be called when all Players have been added.
    *  
    */
   public static void initPlayers()
   {
      Player player;
      int numberOfPlayers = players.size();
      int startCash = playerStartCash[numberOfPlayers];

      // Give each player the initial cash amount
      for (int i = 0; i < numberOfPlayers; i++)
      {
         player = (Player) players.get(i);
         Bank.transferCash(null, player, startCash);
         Log.write("Player " + player.getName() + " receives " + startCash);
      }

      // Set the sertificate limit
      playerCertificateLimit = playerCertificateLimits[numberOfPlayers];
   }

   public static int getCertLimit()
   {
      return playerCertificateLimit;
   }

   public Player(String name)
   {
      this.name = name;
      portfolio = new Portfolio(name, this);

   }

   public void buyShare(Stock share)
   {
      //if we haven't already bought a share since our last turn
      //if we haven't sold a share of this company's stock during this stock
      // round
      //then remove share from particular pile
      //add share to player's portfolio
      //deduct cost of share from wallet
      //add cost of share to bank
      //update player who will have priority
      //set flag that we've bought a share and can't buy again until player's
      // next turn
   }

   public void sellShare(Stock share)
   {
      //remove share from player's portfolio
      //add share to bank's pile
      //get current value of share
      //deduct current value of share from bank
      //add current value of share to player's wallet
      //if company's chit is not on a ledge
      //move the company's chit down one square on the stock chart
   }

   /**
    * @return Returns the hasPriority.
    */
   public boolean hasPriority()
   {
      return hasPriority;
   }

   /**
    * @param hasPriority
    *           The hasPriority to set.
    */
   public void setHasPriority(boolean hasPriority)
   {
      this.hasPriority = hasPriority;
   }

   /**
    * @return Returns the portfolio.
    */
   public Portfolio getPortfolio()
   {
      return portfolio;
   }

   /**
    * @return Returns the name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * @return Returns the wallet.
    */
   public int getCash()
   {
      return wallet;
   }

   public void addCash(int amount)
   {
      wallet += amount;
   }
}
/*
 * Created on Feb 22, 2005
 *
 */
package game;

import java.util.ArrayList;

/**
 * @author Brett Lentz
 */
public class Player
{
   String name;
   int wallet;
   boolean hasPriority;
   ArrayList portfolio;
   ArrayList littleCoOwned;
   ArrayList companiesSoldThisTurn;
   
   public void buyShare(Stock share)
   {
      //if we haven't already bought a share since our last turn
      //if we haven't sold a share of this company's stock during this stock round
      //then remove share from particular pile
      //add share to player's portfolio
      //deduct cost of share from wallet
      //add cost of share to bank
      //update player who will have priority
      //set flag that we've bought a share and can't buy again until player's next turn
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
    * @param hasPriority The hasPriority to set.
    */
   public void setHasPriority(boolean hasPriority)
   {
      this.hasPriority = hasPriority;
   }
   /**
    * @return Returns the portfolio.
    */
   public ArrayList getPortfolio()
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
   public int getWallet()
   {
      return wallet;
   }
   public Player()
   {
      this("Default Player Name");
   }
   public Player(String n)
   {
      name = n;
      
      switch (Game.getNumPlayers())
      {
         case 3:
         case 4:
         case 5:
         case 6:
         default:
            wallet = 0;
      }
      
      portfolio = new ArrayList(Game.getMaxNumShares());
   }
}

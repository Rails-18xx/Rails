/*
 * Created on Feb 22, 2005
 *
 */
package game;

/**
 * @author Brett Lentz
 */

public class Game
{  
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
      switch(numPlayers)
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
   /**
    * @return Returns the maxNumTrains.
    **/
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
      }
      else
      {
         MAX_NUM_PLAYERS = 0;
         numPlayers = 0;      
         MAX_NUM_SHARES = 0;
         fastestAvailableTrain = 0;      
         Bank bank = new Bank();
         maxNumTrains = 0;
      }
   }
}
/*
 * Created on Feb 22, 2005
 *
 */
package game;
import java.util.ArrayList;

/**
 * @author Brett Lentz
 */
public class Bank
{
   private int money;
   private int gameType;
   ArrayList forSalePile;
   
   public Bank()
   {
      this(0,0);
   }
   public Bank(int numPlayers)
   {
      this(numPlayers,0);
   }
   public Bank(int numPlayers, int gameType)
   {
      switch(numPlayers)
      {
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         default:
            money = 25000;
            break;
      }
      
      forSalePile = new ArrayList();
   }
}

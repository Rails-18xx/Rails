/*
 * Created on Feb 23, 2005
 *
 */
package game;

/**
 * @author a-blentz
 * 
 * This may need to be converted to an abstract class or interface for 
 * fleshing out specific little companies due to their varied and specific 
 * game mechanics.
 */
public class LittleCompany
{
   int cost;
   int perTurnPayout;
   
   /**
    * @return Returns the cost.
    */
   public int getCost()
   {
      return cost;
   }
   /**
    * @return Returns the perTurnPayout.
    */
   public int getPerTurnPayout()
   {
      return perTurnPayout;
   }
   public LittleCompany()
   {
      this(0,0);
   }
   public LittleCompany(int c, int p)
   {
      cost = c;
      perTurnPayout = p;
   }
}

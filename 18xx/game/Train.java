/*
 * Created on Feb 22, 2005
 *
 */
package game;

/**
 * @author Brett Lentz
 */
public class Train
{
   private int speed;
   private int cost;
   
   /**
    * @return Returns the cost.
    */
   public int getCost()
   {
      return cost;
   }
   /**
    * @return Returns the speed.
    */
   public int getSpeed()
   {
      return speed;
   }   
   public Train()
   {
      speed = 0;
      cost = 0;
   }
   public Train(int sp, int co)
   {
      speed = sp;
      cost = co;
   }

}

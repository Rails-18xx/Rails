/*
 * Created on Feb 22, 2005
 *
 */
package game;

/**
 * @author Brett Lentz
 */

public class Turn extends Game
{
   private int turnNumber;
   private int operatingRoundsPerStockRound;
   
   private void doStockRound()
   {
      StockRound currentStockRound = new StockRound();
   }
   private void doOperatingRound()
   {
      OperatingRound currentOperatingRound = new OperatingRound();
   }
   
   public Turn()
   {
      turnNumber = 1;
      operatingRoundsPerStockRound = 1;
   }
   public Turn(int turnNo)
   {
      turnNumber = turnNo;      
      operatingRoundsPerStockRound = super.getFastestAvailableTrain();
   }
}
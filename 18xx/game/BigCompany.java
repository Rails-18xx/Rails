/*
 * Created on Feb 23, 2005
 *
 */
package game;

/**
 * @author a-blentz
 */
public class BigCompany
{
   int strikePrice;
   int treasury;
   boolean hasFloated;
   boolean canBuyStock;
   Train[] trainsOwned;
   Stock[] portfolio;
   LittleCompany[] littleCoOwned;
   
   public BigCompany(int s, boolean cbs)
   {
      strikePrice = s;
      treasury = 10 * strikePrice;
      hasFloated = false;
      canBuyStock = cbs;
      trainsOwned = new Train[Game.getMaxNumTrains()];
   }
}

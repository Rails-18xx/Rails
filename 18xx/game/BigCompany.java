/*
 * Created on Feb 23, 2005
 *
 */
package game;

import java.util.ArrayList;

/**
 * @author a-blentz
 */
public class BigCompany
{
   int strikePrice;
   int treasury;
   boolean hasFloated;
   boolean canBuyStock;
   ArrayList trainsOwned;
   ArrayList portfolio;
   ArrayList littleCoOwned;
   
   public BigCompany(int s, boolean cbs)
   {
      strikePrice = s;
      treasury = 10 * strikePrice;
      hasFloated = false;
      canBuyStock = cbs;
      trainsOwned = new ArrayList(Game.getMaxNumTrains());
   }
}

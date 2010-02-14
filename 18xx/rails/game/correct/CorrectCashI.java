package rails.game.correct;

import rails.game.*;


/**
 * Interface to action that changes the cash position of a cashholder.
 * 
 * @author Stefan Frey
 */
public interface CorrectCashI  {

   /**
    * Gets preconditions.
    */
   public CashHolder getCashHolder();
   
   public String getCashHolderName();
   
   
   /**
    * Gets and sets postconditions.
    */
   public int getAmount();
   
   public void setAmount(int amount);
    
}
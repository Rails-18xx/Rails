/*
 * Created on Feb 22, 2005
 *
 */
package game;

/**
 * @author Brett Lentz
 */
public class Stock
{
   private String companyName;
   private int initialValue;
   private int currentValue;
   private boolean presidentShare;
   
   /**
    * @return Returns the currentValue.
    */
   public int getCurrentValue()
   {
      return currentValue;
   }
   /**
    * @param currentValue The currentValue to set.
    */
   public void setCurrentValue(int currentValue)
   {
      this.currentValue = currentValue;
   }
   /**
    * @return Returns the presidentShare.
    */
   public boolean isPresidentShare()
   {
      return presidentShare;
   }
   /**
    * @param presidentShare The presidentShare to set.
    */
   public void setPresidentShare(boolean presidentShare)
   {
      this.presidentShare = presidentShare;
   }
   /**
    * @return Returns the companyName.
    */
   public String getCompanyName()
   {
      return companyName;
   }
   /**
    * @return Returns the initialValue.
    */
   public int getInitialValue()
   {
      return initialValue;
   }
      
   public Stock()
   {
      companyName = "Default";
      initialValue = 0;
      currentValue = 0;
      presidentShare = false;
   }
   public Stock(String coName, int val, boolean presShare)
   {
      companyName = coName;
      initialValue = val;
      currentValue = val;
      presidentShare = presShare;
   }
}

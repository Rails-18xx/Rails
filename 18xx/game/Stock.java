/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package game;

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
    * @param currentValue
    *           The currentValue to set.
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
    * @param presidentShare
    *           The presidentShare to set.
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
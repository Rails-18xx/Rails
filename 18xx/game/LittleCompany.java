/*
 Rails: an 18xx game system.
 Copyright (C) 2005 Brett Lentz

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package game;

/**
 * This may need to be converted to an abstract class or interface for fleshing
 * out specific little companies due to their varied and specific game
 * mechanics.
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
      this(0, 0);
   }

   public LittleCompany(int c, int p)
   {
      cost = c;
      perTurnPayout = p;
   }
}
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
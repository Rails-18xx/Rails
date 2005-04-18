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
//      trainsOwned = new Train[Game.getMaxNumTrains()];
   }
}
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
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

package test;

import ui.StockChart;
import game.*;

public class StockTest
{

   public static void StockChartTest()
   {
      int row, col, price;
      game.StockChart chart = new game.StockChart("1830");
      StockPrice square;

      System.out.print(" ");
      for (col = 0; col < chart.getNumCols(); col++)
      {
         System.out.print("   " + Character.toString((char) ('A' + col)));
      }
      System.out.println();
      for (row = 0; row < chart.getNumRows(); row++)
      {
         System.out.print((row < 9 ? " " : "") + (row + 1));
         for (col = 0; col < chart.getNumCols(); col++)
         {
            square = chart.getStockPrice(row, col);
            if (square != null)
            {
               price = square.getPrice();
               System.out.print(" " + (price < 100 ? " " : "") + price);
            } else
            {
               System.out.print("    ");
            }
         }
         System.out.println();
      }
   }

   public static void StockUITest()
   {
      StockChart sc = new ui.StockChart();
   }

   public static void main(String[] args)
   {

      StockUITest();
   }
}
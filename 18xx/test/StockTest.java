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

package test;

import java.util.List;
import ui.*;
import game.*;

public class StockTest
{

   public static void StockChartTest()
   {
      int row, col, price;
	  Game game = new Game("1830");
	  StockMarketI chart = game.getStockMarket();
      StockSpaceI square;

      System.out.print(" ");
      for (col = 0; col < chart.getNumberOfColumns(); col++)
      {
         System.out.print("   " + Character.toString((char) ('A' + col)));
      }
      System.out.println();
      for (row = 0; row < chart.getNumberOfRows(); row++)
      {
         System.out.print((row < 9 ? " " : "") + (row + 1));
         for (col = 0; col < chart.getNumberOfColumns(); col++)
         {
            square = chart.getStockSpace(row, col);
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

   public void testIt(String[] args) throws Exception{
      if (args.length<1){
          throw new ConfigurationException("No config file specified.");
      }
      //Element elem = XmlUtils.findElementInFile(args[0], ComponentManager.ELEMENT_ID);
      //ComponentManager.configureInstance(elem);

      ComponentManager componentMan = ComponentManager.getInstance();
      CompanyManagerI companyManager = (CompanyManagerI)componentMan.findComponent(CompanyManagerI.COMPONENT_NAME);

      List companies = companyManager.getAllCompanies();
      System.out.println(companies.size()+" companies are registered");
      for (int i = 0; i< companies.size(); i++){
          CompanyI company = (CompanyI) companies.get(i);
          System.out.println("Company " + i + " is called " + company.getName() +
                  ", and is of type " + company.getType());
          if (company instanceof PrivateCompanyI) {
          	System.out.println("  Base price: "+((PrivateCompanyI)company).getBasePrice()
          		+ " revenue: " + ((PrivateCompanyI)company).getRevenue());
          } else {
			System.out.println("  Foreground colour: "+((PublicCompanyI)company).getFgColour()
				+ " background colour: " + ((PublicCompanyI)company).getBgColour());
         	
          }
      }
  }
   
   public static void StockUITest()
   {
      Game game = Game.getInstance();
      game.initialise("1830");
      
      StockMarket sm = (StockMarket) game.getStockMarket();
      StockChart sc = new ui.StockChart(sm);
   }

   public static void main(String[] args)
   {
      StockUITest();
   }
}
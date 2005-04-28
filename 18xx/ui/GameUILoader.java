/*
 * Created on Apr 25, 2005
 */
package ui;

import game.*;

/**
 * @author blentz
 */
public class GameUILoader
{
   private static CompanyStatus companyStatus;
   private static PlayerStatus playerStatus;
   private static StockChart stockChart;
   
   public static void gameUIInit()
   {
      companyStatus = new CompanyStatus(Game.getCompanyManager(), Game.getBank());
      playerStatus = new PlayerStatus();         
      stockChart = new ui.StockChart((StockMarket) Game.getStockMarket(), companyStatus, playerStatus);
   }
}

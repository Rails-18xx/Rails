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
      playerStatus = new PlayerStatus(Game.getPlayers()); //might need to be here for access to certain objects
      											// We'll know more after the Player class is fleshed out            
      stockChart = new ui.StockChart((StockMarket) Game.getStockMarket(), companyStatus, playerStatus);
   }
}

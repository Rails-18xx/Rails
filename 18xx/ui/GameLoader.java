/*
 * Created on Apr 25, 2005
 */
package ui;

import game.*;
import java.util.*;

/**
 * @author blentz
 */
public class GameLoader
{
   static Game game;
   static CompanyManager companyManager;
   static CompanyStatus companyStatus;
   static PlayerStatus playerStatus;
   static StockMarket stockMarket;
   static StockChart stockChart;
   static ArrayList companyList;
   static Player[] players;
   static Bank bank;
   
   public static void NewGame(String gameName, ArrayList playerNames)
   {
      game = Game.getInstance();
      game.initialise(gameName);
      companyManager = (CompanyManager) game.getCompanyManager();
      companyList = (ArrayList) companyManager.getAllPublicCompanies();
      stockMarket = (StockMarket) game.getStockMarket();
      players = new Player[playerNames.size()];
      bank = game.getBank();
      
      
      for(int i=0; i < playerNames.size(); i++)
      {
         players[i] = new Player(playerNames.get(i).toString());
      }
      
      Player.initPlayers(players);
      
      for(int i=0; i < companyList.size(); i++)
      {
         //Put all the tokens on the stock market for testing.
         ((PublicCompany) companyList.get(i)).setParPrice((StockSpace) stockMarket.getStartSpaces().get(0));
      }
      
      playerStatus = new PlayerStatus(players); //might need to be here for access to certain objects
      											// We'll know more after the Player class is fleshed out
      companyStatus = new CompanyStatus(companyManager, game.getBank());      
      stockChart = new ui.StockChart(stockMarket, companyStatus, playerStatus);
   }
}

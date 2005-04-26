/*
 * Created on Apr 25, 2005
 */
package ui;

import game.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

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
      
      for(int i=0; i < playerNames.size(); i++)
      {
         players[i] = new Player(playerNames.get(i).toString());
      }
      
      Player.initPlayers(players);
      
      playerStatus = new PlayerStatus(players); //might need to be here for access to certain objects
      											// We'll know more after the Player class is fleshed out
      companyStatus = new CompanyStatus(companyManager, bank);      
      stockChart = new ui.StockChart(stockMarket, companyStatus, playerStatus);
   }
   
   public static boolean StartCompany(Player player, String companyName, int parValue)
   {
      if(player.getCash() >= parValue*6)
      {
         PublicCompany co = (PublicCompany) companyManager.getPublicCompany(companyName);
         co.setParPrice(stockMarket.getStartSpace(parValue));
         co.setClosed(false);
         Bank.transferCash(player, co, parValue*6);
         
         return true;
      }
      else
         return false;
   }
}

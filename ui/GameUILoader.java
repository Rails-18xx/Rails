/*
 * Created on Apr 25, 2005
 */
package ui;

/**
 * @author blentz
 */
public class GameUILoader
{
   public static StockChart stockChart;
   public static StatusWindow statusWindow;
   public static LogWindow messageWindow;
   public static MapWindow mapWindow;
   public static Options options;
   
   public GameUILoader()
   {
      options = new Options();
   }
   
   public static void gameUIInit()
   {
      stockChart = new StockChart();
      mapWindow = new MapWindow();
      messageWindow = new LogWindow();
      statusWindow = new StatusWindow();
   }
}

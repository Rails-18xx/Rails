/*
 * Created on Apr 25, 2005
 */
package ui;

/**
 * @author blentz
 */
public class GameUILoader
{
   private static StockChart stockChart;
   private static StatusWindow statusWindow;
   private static LogWindow messageWindow;
   private static MapWindow mapWindow;
   private static Options options;
   
   public GameUILoader()
   {
      options = new Options();
   }
   
   public static void gameUIInit()
   {
      stockChart = new StockChart();
      messageWindow = new LogWindow();
      statusWindow = new StatusWindow();
      mapWindow = new MapWindow();
   }
}

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
   private static Options options;
   
   public GameUILoader()
   {
      options = new Options();
   }
   
   public static void gameUIInit()
   {
      stockChart = new StockChart();
      statusWindow = new StatusWindow();
   }
}

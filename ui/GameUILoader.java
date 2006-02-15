/*
 * Created on Apr 25, 2005
 */
package ui;

/**
 * This class is called by main() and loads all of the UI components
 * 
 * @author blentz
 */
public class GameUILoader
{
   public static StockChart stockChart;
   public static StatusWindow statusWindow;
   public static LogWindow messageWindow;
   //public static MapPanel mapPanel;
   private static MapPanel mapPanel;
   public static ORWindow orWindow;
   public static Options options;
   
   public GameUILoader()
   {
      options = new Options();
   }
   
   public static void gameUIInit()
   {
      stockChart = new StockChart();
      //mapPanel = new MapPanel();
      messageWindow = new LogWindow();
      orWindow = new ORWindow();
      mapPanel = orWindow.getMapPanel();
      statusWindow = new StatusWindow();
   }
   
   public static MapPanel getMapPanel()
   {
	   return mapPanel;
   }
}

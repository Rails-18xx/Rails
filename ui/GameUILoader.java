package ui;

/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUILoader
{
   public static StockChart stockChart;
   public static StatusWindow statusWindow;
   public static LogWindow messageWindow;
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

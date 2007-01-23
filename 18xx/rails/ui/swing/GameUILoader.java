package rails.ui.swing;

/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUILoader
{
   public static StockChart stockChart;
   public static StatusWindow statusWindow;
   public static ReportWindow reportWindow;
   private static MapPanel mapPanel;
   public static ORWindow orWindow;
   public static Options options;
   public static ImageLoader imageLoader;
   
   public GameUILoader()
   {
      options = new Options();
   }
   
   public static void gameUIInit()
   {
	  imageLoader = new ImageLoader();
      stockChart = new StockChart();
      reportWindow = new ReportWindow();
      orWindow = new ORWindow();
      mapPanel = orWindow.getMapPanel();
      statusWindow = new StatusWindow();
   }
   
   public static MapPanel getMapPanel()
   {
	   return mapPanel;
   }
   
   public static ImageLoader getImageLoader()
   {
	   return imageLoader;
   }
}

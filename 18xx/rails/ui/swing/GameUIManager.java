package rails.ui.swing;

import org.apache.log4j.Logger;

import rails.game.Bank;
import rails.game.Game;
import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.RoundI;
import rails.game.StartRound;
import rails.game.StockRound;
import rails.game.action.PossibleAction;

/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUIManager
{
   public StockChart stockChart;
   public StatusWindow statusWindow;
   public ReportWindow reportWindow;
   public static ORWindow orWindow;
   public static MapPanel mapPanel;
   private StartRoundWindow startRoundWindow;
   public Options options;
   public static ImageLoader imageLoader;
   
    private GameManager gameManager;
    private PossibleAction lastAction;
    private ActionPerformer activeWindow = null;
    private RoundI currentRound;
    private RoundI previousRound = null;
    private StartRound startRound;

    protected static Logger log = Logger.getLogger(GameUIManager.class.getPackage().getName());

    public GameUIManager()
   {
      options = new Options(this);
      
   }
   
   public void gameUIInit()
   {
       gameManager = GameManager.getInstance();
	  imageLoader = new ImageLoader();
      stockChart = new StockChart();
      reportWindow = new ReportWindow();
      orWindow = new ORWindow(this);
      mapPanel = orWindow.getMapPanel();
      statusWindow = new StatusWindow(this);
      
      orWindow.setVisible(false);

      updateUI();

   }
   
   public boolean processOnServer (PossibleAction action) {
       
    // In some cases an Undo requires a different follow-up
    lastAction = action;

    log.debug ("==Passing to server: "+action);
       boolean result = gameManager.process (action);
    log.debug ("==Result from server: "+result);
       
       ReportWindow.addLog();
       
       // End of game checks
       if (GameManager.isGameOver()) {
           
           statusWindow.reportGameOver();

           return true;
           
       } else if (Bank.isJustBroken()) {
           
           statusWindow.reportBankBroken();
           
       }

       // Check in which round we are now,
       // and make sure that the right window is active.
       updateUI();
       
       statusWindow.setUndoRedo();

       if (result) {
           return activeWindow.processImmediateAction();
       } else {
           return false;
       }
   }
   
   public void updateUI() {
       
       currentRound = gameManager.getCurrentRound();

       log.debug("Current round="+currentRound+", previous round="+previousRound);
       // Process consequences of a round type change to the UI
       //if (previousRound != currentRound) {
       if (previousRound == null || !previousRound.equals(currentRound)) {
           
           // Finish the previous round UI aspects
           if (previousRound instanceof StockRound) {
               log.debug("Finishing Stock Round UI");
               statusWindow.finishRound();
           } else if (previousRound instanceof StartRound) {
               log.debug("Finishing Start Round UI");
               if (startRoundWindow != null) {
                   startRoundWindow.close();
                   startRoundWindow = null;
                   log.debug("~~~~Closing StartRoundWindow");
               }
           } else if (previousRound instanceof OperatingRound) {
               log.debug("Finishing Operating Round UI");
               orWindow.finish();
               orWindow.setVisible(false);
           }
           
           // Start the new round UI aspects
           if (currentRound instanceof StartRound) {
               
               log.debug ("Entering Start Round UI");
               startRound = (StartRound) currentRound;
               if (startRoundWindow == null) {
                   startRoundWindow = new StartRoundWindow(startRound, this);
                   log.debug("~~~~Creating new StartRoundWindow");
               } else {
                   log.debug("~~~~NOT creating new StartRoundWindow");
               }
               
               stockChart.setVisible(false);
               
           } else if (currentRound instanceof StockRound) {
               
               log.debug("Entering Stock Round UI");
               stockChart.setVisible(true);
               statusWindow.setVisible(true);
               
           } else if (currentRound instanceof OperatingRound) {
               
               log.debug("Entering Operating Round UI");
               stockChart.setVisible(false);
               orWindow.activate();
           }
       }
 
       statusWindow.setupFor(currentRound);

       // Update the current round window
       if (currentRound instanceof StartRound)
       {
           activeWindow = startRoundWindow;
           
 
           startRoundWindow.updateStatus("StatusWindow.updateUI");
           startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());
           
       }
       else if (currentRound instanceof StockRound)
       {
           activeWindow = statusWindow;

           //stockRound = (StockRound) currentRound;
           
           statusWindow.updateStatus ("GameUIManager.updateUI");
           
      }
       else if (currentRound instanceof OperatingRound)
       {
           activeWindow = orWindow;

           orWindow.updateStatus();
       }

       previousRound = currentRound;

   }
   
   public PossibleAction getLastAction () {
    return lastAction;
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

package rails.ui.swing;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import rails.game.Bank;
import rails.game.DisplayBuffer;
import rails.game.Game;
import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.Player;
import rails.game.RoundI;
import rails.game.StartRound;
import rails.game.StockRound;
import rails.game.action.GameAction;
import rails.game.action.PossibleAction;
import rails.util.Config;
import rails.util.Util;

/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUIManager
{
	public static GameUIManager instance = null;
	
   public StockChart stockChart;
   public StatusWindow statusWindow;
   public ReportWindow reportWindow;
   public ORUIManager orUIManager;  
   public /*static*/ ORWindow orWindow;  //TEMPORARY
   private StartRoundWindow startRoundWindow;
   public GameSetupWindow gameSetupWindow;
   public static ImageLoader imageLoader;
   
    private GameManager gameManager;
    private PossibleAction lastAction;
    private ActionPerformer activeWindow = null;
    private RoundI currentRound;
    private RoundI previousRound = null;
    private StartRound startRound;

    protected static final String DEFAULT_SAVE_DIRECTORY = "save";
    protected static final String DEFAULT_SAVE_PATTERN = "yyyyMMdd_HHmm";
    protected static final String DEFAULT_SAVE_EXTENSION = "rails";
    
    protected String saveDirectory;
    protected String savePattern;
    protected String saveExtension;
    protected String providedName = null;
    protected SimpleDateFormat saveDateTimeFormat;
    protected File lastFile, lastDirectory;
    
    protected static Logger log = Logger.getLogger(GameUIManager.class.getPackage().getName());

    public GameUIManager()
   {
    	instance = this;
    	
        saveDirectory = Config.get("save.directory");
        if (!Util.hasValue(saveDirectory)) {
            saveDirectory = DEFAULT_SAVE_DIRECTORY;
        }
        savePattern = Config.get("save.filename.date_time_pattern");
        if (!Util.hasValue(savePattern)) {
            savePattern = DEFAULT_SAVE_PATTERN;
        }
        saveDateTimeFormat = new SimpleDateFormat(savePattern);
        saveExtension = Config.get("save.filename.extension");
        if (!Util.hasValue(saveExtension)) {
            saveExtension = DEFAULT_SAVE_EXTENSION;
        }
        
      gameSetupWindow = new GameSetupWindow(this);
      
   }
   
   public void gameUIInit()
   {
      gameManager = GameManager.getInstance();
	  imageLoader = new ImageLoader();
      stockChart = new StockChart();
      reportWindow = new ReportWindow();
      orWindow = new ORWindow(this);
      orUIManager = orWindow.getORUIManager();
      //mapPanel = orWindow.getMapPanel();
      statusWindow = new StatusWindow(this);
      
      updateUI();

   }
   
   public boolean processOnServer (PossibleAction action) {
       
    // In some cases an Undo requires a different follow-up
    lastAction = action;

    log.debug ("==Passing to server: "+action);

    Player player = GameManager.getCurrentPlayer();
    if (action != null && player != null) {
    	action.setPlayerName(player.getName());
    }
    boolean result = gameManager.process (action);
    log.debug ("==Result from server: "+result);
    activeWindow.displayServerMessage();
       
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
       
       statusWindow.setGameActions();

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
               }
           } else if (previousRound instanceof OperatingRound) {
               log.debug("Finishing Operating Round UI");
               orUIManager.finish();
          }
           
           // Start the new round UI aspects
           if (currentRound instanceof StartRound) {
               
               log.debug ("Entering Start Round UI");
               startRound = (StartRound) currentRound;
               if (startRoundWindow == null) {
                   startRoundWindow = new StartRoundWindow(startRound, this);
               }
               
               stockChart.setVisible(false);
               
           } else if (currentRound instanceof StockRound) {
               
               log.debug("Entering Stock Round UI");
               stockChart.setVisible(true);
               statusWindow.setVisible(true);
               
           } else if (currentRound instanceof OperatingRound) {
               
               log.debug("Entering Operating Round UI");
               stockChart.setVisible(false);
               orUIManager.initOR((OperatingRound)currentRound);
           }
       }
 
       statusWindow.setupFor(currentRound);

       // Update the current round window
       if (currentRound instanceof StartRound)
       {
           activeWindow = startRoundWindow;
           
 
           startRoundWindow.updateStatus();
           startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());
           
       }
       else if (currentRound instanceof StockRound)
       {
           activeWindow = statusWindow;

           //stockRound = (StockRound) currentRound;
           
           statusWindow.updateStatus ();
           
      }
       else if (currentRound instanceof OperatingRound)
       {
           activeWindow = orUIManager.getORWindow();

           orUIManager.updateStatus();
       }

       previousRound = currentRound;

   }
   
   public void saveGame (GameAction saveAction) {
       
       JFileChooser jfc = new JFileChooser();
       String filename;
       if (providedName != null) {
           filename = providedName;
       } else {
           filename = saveDirectory + "/"
           + Game.getName() + "_"
           + saveDateTimeFormat.format (new Date())
           + "." + saveExtension;
       }

       File proposedFile = new File (filename);
       jfc.setSelectedFile(proposedFile);
       if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
           File selectedFile = jfc.getSelectedFile();
           String filepath = selectedFile.getPath();
           saveDirectory = selectedFile.getParent();
           if (!selectedFile.getName().equalsIgnoreCase(proposedFile.getName())) {
               providedName = filepath;
           }
           saveAction.setFilepath(filepath);
           processOnServer (saveAction);
       }
   }
	
   public boolean loadGame () {
       
	   JFileChooser jfc = new JFileChooser();
       if (providedName != null) {
           jfc.setSelectedFile(new File(providedName));
       } else {
           jfc.setCurrentDirectory(new File(saveDirectory));
       }
       
       if (jfc.showOpenDialog(gameSetupWindow.getContentPane()) == JFileChooser.APPROVE_OPTION) {
           File selectedFile = jfc.getSelectedFile();
           String filepath = selectedFile.getPath();
           saveDirectory = selectedFile.getParent();
           
           if (!Game.load(filepath)) {
               JOptionPane.showMessageDialog(gameSetupWindow,
                       DisplayBuffer.get(), "", JOptionPane.ERROR_MESSAGE);
               return false;
           }
           DisplayBuffer.clear();

		   gameUIInit();
		   processOnServer (null);
		   //updateUI();
	       statusWindow.setGameActions();
       }
       
       return true;
   }
	

   
   public PossibleAction getLastAction () {
    return lastAction;
   }
   

   /*
   public static MapPanel getMapPanel()
   {
	   return instance.mapPanel;
   }
   */
   
   public static ImageLoader getImageLoader()
   {
	   return imageLoader;
   }
   
   public GameManager getGameManager () {
	   return gameManager;
   }
   
}

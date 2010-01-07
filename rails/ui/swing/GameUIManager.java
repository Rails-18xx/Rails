package rails.ui.swing;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.Defs;
import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.CheckBoxDialog;
import rails.ui.swing.elements.DialogOwner;
import rails.util.*;

/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUIManager implements DialogOwner {
    public static GameUIManager instance = null;

    public StockChart stockChart;
    public StatusWindow statusWindow;
    public ReportWindow reportWindow;
    public ORUIManager orUIManager;
    public ORWindow orWindow; // TEMPORARY
    private StartRoundWindow startRoundWindow;

    protected JDialog currentDialog = null;
    protected PossibleAction currentDialogAction = null;

    public static ImageLoader imageLoader;

    private GameManagerI gameManager;
    private PossibleAction lastAction;
    private ActionPerformer activeWindow = null;
    private RoundI currentRound;
    private RoundI previousRound = null;
    private StartRound startRound;

    protected static final String DEFAULT_SAVE_DIRECTORY = "save";
    protected static final String DEFAULT_SAVE_PATTERN = "yyyyMMdd_HHmm";
    protected static final String DEFAULT_SAVE_EXTENSION = "rails";
    protected static final String NEXT_PLAYER_SUFFIX = "NEXT_PLAYER";

    protected String saveDirectory;
    protected String savePattern;
    protected String saveExtension;
    protected String saveSuffixSpec = "";
    protected String saveSuffix = "";
    protected String providedName = null;
    protected SimpleDateFormat saveDateTimeFormat;
    protected File lastFile, lastDirectory;

    protected static Logger log =
            Logger.getLogger(GameUIManager.class.getPackage().getName());

    public GameUIManager() {

    }

    public void init (GameManagerI gameManager) {

        instance = this;
        this.gameManager = gameManager;

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
        String timezone = Config.get("save.filename.date_time_zone");
        // Default is local time
        if (Util.hasValue(timezone)) {
            saveDateTimeFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        }
        saveSuffixSpec = Config.get("save.filename.suffix");
        if (Util.hasValue(saveSuffixSpec) && !saveSuffixSpec.equals(NEXT_PLAYER_SUFFIX)) {
            saveSuffix = "_" + saveSuffixSpec;
        }

    }

    public void gameUIInit() {
        imageLoader = new ImageLoader();
        stockChart = new StockChart(this);
        reportWindow = new ReportWindow(gameManager);
        orWindow = new ORWindow(this);
        orUIManager = orWindow.getORUIManager();

        String statusWindowClassName = getClassName(Defs.ClassName.STATUS_WINDOW);
        try {
            Class<? extends StatusWindow> statusWindowClass =
                Class.forName(statusWindowClassName).asSubclass(StatusWindow.class);
            statusWindow = statusWindowClass.newInstance();
            statusWindow.init(this);
        } catch (Exception e) {
            log.fatal("Cannot instantiate class " + statusWindowClassName, e);
            System.exit(1);
        }

        updateUI();

        reportWindow.scrollDown();
    }

    public void startLoadedGame() {
        gameUIInit();
        processOnServer(null);
        statusWindow.setGameActions();
    }

    public boolean processOnServer(PossibleAction action) {

        // In some cases an Undo requires a different follow-up
        lastAction = action;
        if (action != null) action.setActed();

        log.debug("==Passing to server: " + action);

        Player player = getCurrentPlayer();
        if (action != null && player != null) {
            action.setPlayerName(player.getName());
        }

        // Process the action on the server
        boolean result = gameManager.process(action);

        // Follow-up the result
        log.debug("==Result from server: " + result);
        if (DisplayBuffer.getAutoDisplay()) activeWindow.displayServerMessage();

        reportWindow.addLog();

        // End of game checks
        if (gameManager.isGameOver()) {

            statusWindow.reportGameOver();

            return true;

        } else if (gameManager.getBank().isJustBroken()) {

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

        log.debug("Current round=" + currentRound + ", previous round="
                  + previousRound);
        // Process consequences of a round type change to the UI

        Class<? extends RoundI> previousRoundType
                = previousRound == null ? null : previousRound.getClass();
        Class<? extends RoundI> currentRoundType
                = currentRound.getClass();
        Class<? extends RoundI> previousRoundUItype
                = previousRound == null ? null : previousRound.getRoundTypeForUI();
        Class<? extends RoundI> currentRoundUItype
                = currentRound.getRoundTypeForUI();

        /* Distinguish actual round type from visible round type.
         * Actual round type is the class of the active Round subclass.
         * Visible round type is the class of one of the three 'basic'
         * round types: Start, Stock or Operating.
         * The latter type determines what UI windows will become visible.
         */

        /* Process actual round type changes */
        if (previousRound == null || !previousRound.equals(currentRound)) {

            /* Finish previous round UI processing */
            if (previousRound != null) {

                if (StockRound.class.isAssignableFrom(previousRoundType)) {
                    log.debug("UI leaving Stock Round");
                    statusWindow.finishRound();
                } else if (StartRound.class.isAssignableFrom(previousRoundType)) {
                    log.debug("UI leaving Start Round");
                    if (startRoundWindow != null) {
                        startRoundWindow.close();
                        startRoundWindow = null;
                    }
                } else if (OperatingRound.class.isAssignableFrom(previousRoundType)) {
                    log.debug("UI leaving Operating Round");
                    orUIManager.finish();
                } else if (SwitchableUIRound.class.isAssignableFrom(previousRoundType) ) {
                    log.debug("UI leaving switchable round type");
                }
            }

            // Start the new round UI processing
            if (StartRound.class.isAssignableFrom(currentRoundType)) {

                log.debug("UI entering Start Round");
                startRound = (StartRound) currentRound;
                if (startRoundWindow == null) {
                    startRoundWindow = new StartRoundWindow(startRound, this);
                }

            } else if (StockRound.class.isAssignableFrom(currentRoundType)) {

                log.debug("UI entering Stock Round");
                orWindow.setVisible(false);
                stockChart.setVisible(true);

            } else if (OperatingRound.class.isAssignableFrom(currentRoundType)) {

                log.debug("UI entering Operating Round");
                orUIManager.initOR((OperatingRound) currentRound);
                orWindow.setVisible(true);
                stockChart.setVisible(false);

            } else if (SwitchableUIRound.class.isAssignableFrom(currentRoundType) ) {
                log.debug("UI entering switchable round type");
                orWindow.setVisible(true);
                statusWindow.pack();
                stockChart.setVisible(true);
            }
        }

        /* Process visible round type changes */
        if (previousRoundUItype == null || !previousRoundUItype.equals(currentRoundUItype)) {

            if (previousRoundUItype != null) {
                // Finish the previous round UI aspects
                if (StockRound.class.isAssignableFrom(previousRoundUItype)) {
                    log.debug("Leaving Stock Round UI type");
                } else if (StartRound.class.isAssignableFrom(previousRoundUItype)) {
                    log.debug("Leaving Start Round UI type");
                } else if (OperatingRound.class.isAssignableFrom(previousRoundUItype)) {
                    log.debug("Leaving Operating Round UI type");
                    //orWindow.setVisible(false);
                }
            }

            // Start the new round UI aspects
             if (StartRound.class.isAssignableFrom(currentRoundUItype)) {

                log.debug("Entering Start Round UI type");
                activeWindow = startRoundWindow;
                //stockChart.setVisible(false);

            } else if (StockRound.class.isAssignableFrom(currentRoundUItype)) {

                log.debug("Entering Stock Round UI type");
                activeWindow = statusWindow;
                //stockChart.setVisible(true);
                //statusWindow.setVisible(true);

            } else if (OperatingRound.class.isAssignableFrom(currentRoundUItype)) {

                log.debug("Entering Operating Round UI type ");
                activeWindow = orWindow;
                //stockChart.setVisible(false);
                //orWindow.setVisible(true);
            }
        }

        statusWindow.setupFor(currentRound);
        previousRound = currentRound;

        // Update the currently visible round window
        // "Switchable" rounds will be handled from subclasses of this class.
        if (StartRound.class.isAssignableFrom(currentRoundUItype)) {

            log.debug("Updating Start round window");
            startRoundWindow.updateStatus();
            startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());

        } else if (StockRound.class.isAssignableFrom(currentRoundUItype)) {

            log.debug("Updating Stock (status) round window");
            statusWindow.updateStatus();

        } else if (OperatingRound.class.isAssignableFrom(currentRoundUItype)) {

            log.debug("Updating Operating round window");
            orUIManager.updateStatus();

        } else {
            // Handle special rounds that do not fall in a standard category
            // The round must indicate which main window to raise
            if (StockRound.class.isAssignableFrom(currentRoundUItype)) {
                log.debug("Updating switched Stock (status) round window");
                activeWindow = statusWindow;
            } else if (OperatingRound.class.isAssignableFrom(currentRoundUItype)) {
                log.debug("Updating switched Operating round window");
                activeWindow = orWindow;
            }
            updateStatus(activeWindow);
        }
    }

    /** Stub, to be overridden in subclasses for special round types */
    protected void updateStatus(ActionPerformer activeWindow) {

    }

    public void discardTrains (DiscardTrain dt) {

        PublicCompanyI c = dt.getCompany();
        String playerName = dt.getPlayerName();
        List<TrainI> trains = dt.getOwnedTrains();
        int size = trains.size() + (dt.isForced() ? 0 : 1);
        List<String> trainOptions =
                new ArrayList<String>(size);
        String[] options = new String[size];
        String prompt = null;

        int j = 0;
        if (!dt.isForced()) {
            trainOptions.add(
                    options[j++] = LocalText.getText("None")
            );
            prompt = LocalText.getText("MayDiscardTrain",
                    c.getName());
        }
        int offset = j;
        for (int i = 0; i < trains.size(); i++) {
            trainOptions.add(
                    options[j++] = LocalText.getText("N_Train",
                            trains.get(i).getName())
            );
        }
        if (prompt == null) prompt = LocalText.getText(
                "HAS_TOO_MANY_TRAINS",
                playerName,
                c.getName() );

        String discardedTrainName =
                (String) JOptionPane.showInputDialog(orWindow,
                        prompt,
                        LocalText.getText("WhichTrainToDiscard"),
                        JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);
        if (discardedTrainName != null) {
            int index = trainOptions.indexOf(discardedTrainName);
            if (index >= offset) {
                TrainI discardedTrain =
                        trains.get(trainOptions.indexOf(discardedTrainName)-offset);
                dt.setDiscardedTrain(discardedTrain);
            }

            orWindow.process(dt);
        }
    }

    public void exchangeTokens (ExchangeTokens action) {

        int cityNumber;
        String prompt, cityName, hexName, oldCompName;
        String[] ct;
        MapHex hex;
        List<String> options = new ArrayList<String>();
        City city;
        List<ExchangeableToken> oldTokens = action.getTokensToExchange();

        for (ExchangeableToken t : oldTokens) {
            cityName = t.getCityName();
            ct = cityName.split("/");
            hexName = ct[0];
            try {
                cityNumber = Integer.parseInt(ct[1]);
            } catch (NumberFormatException e) {
                cityNumber = 1;
            }
            hex = orWindow.getMapPanel().getMap().getHexByName (hexName).getHexModel();
            city = hex.getCity(cityNumber);
            oldCompName = t.getOldCompanyName();
            options.add(LocalText.getText("ExchangeableToken",
                    oldCompName,
                    hexName,
                    hex.getCityName(),
                    cityNumber,
                    city.getTrackEdges()));
        }


        int minNumber = action.getMinNumberToExchange();
        int maxNumber = action.getMaxNumberToExchange();
        if (minNumber == maxNumber) {
            prompt = LocalText.getText("ExchangeTokensPrompt1",
                    minNumber,
                    action.getCompanyName());
        } else {
            prompt = LocalText.getText("ExchangeTokensPrompt2",
                    minNumber, maxNumber,
                    action.getCompanyName());
        }

        if (options.size() > 0) {
            orWindow.setVisible(true);
            orWindow.toFront();

            currentDialogAction = action;
            currentDialog = new CheckBoxDialog(this,
                    LocalText.getText("ExchangeTokens"),
                    prompt,
                    options.toArray(new String[0]));

        }
    }

    public void dialogActionPerformed () {

    	if (currentDialog instanceof CheckBoxDialog
    			&& currentDialogAction instanceof ExchangeTokens) {

    		CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
    		ExchangeTokens action = (ExchangeTokens) currentDialogAction;
            boolean[] exchanged = dialog.getSelectedOptions();
            String[] options = dialog.getOptions();

            for (int index=0; index < options.length; index++) {
                if (exchanged[index]) {
                    action.getTokensToExchange().get(index).setSelected(true);
                }
            }
        } else {
            return;
        }

        processOnServer(currentDialogAction);
    }

    public void saveGame(GameAction saveAction) {

        JFileChooser jfc = new JFileChooser();
        String filename;
        if (providedName != null) {
            filename = providedName;
        } else {
            if (NEXT_PLAYER_SUFFIX.equals(saveSuffixSpec)) {
                saveSuffix = "_" + gameManager.getCurrentPlayer().getName();
            }
            filename =
                    saveDirectory + "/" + gameManager.getGameName() + "_"
                            + saveDateTimeFormat.format(new Date())
                            + saveSuffix + "."
                            + saveExtension;
        }

        File proposedFile = new File(filename);
        jfc.setSelectedFile(proposedFile);
        if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String filepath = selectedFile.getPath();
            saveDirectory = selectedFile.getParent();
            if (!selectedFile.getName().equalsIgnoreCase(proposedFile.getName())) {
                providedName = filepath;
            }
            saveAction.setFilepath(filepath);
            processOnServer(saveAction);
        }
    }

    public void setSaveDirectory(String saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    public PossibleAction getLastAction() {
        return lastAction;
    }

    public static ImageLoader getImageLoader() {
        return imageLoader;
    }

    public GameManagerI getGameManager() {
        return gameManager;
    }

    public void setORUIManager(ORUIManager orUIManager) {
        this.orUIManager = orUIManager;
    }

    public ORUIManager getORUIManager() {
        return orUIManager;
    }

    public RoundI getCurrentRound() {
        return gameManager.getCurrentRound();
    }

    public boolean isGameOver() {
        return gameManager.isGameOver();
    }

    public String getHelp () {
        return gameManager.getHelp();
    }

    public int getNumberOfPlayers() {
        return gameManager.getNumberOfPlayers();
    }

    public List<Player> getPlayers() {
        return gameManager.getPlayers();
    }

    public List<String> getPlayerNames() {
        return gameManager.getPlayerNames();
    }

    public Player getCurrentPlayer() {
        return gameManager.getCurrentPlayer();
    }

    public Player getPriorityPlayer () {
        return gameManager.getPriorityPlayer();
    }

    public PhaseI getCurrentPhase() {
        return gameManager.getCurrentPhase();
    }

    public List<PublicCompanyI> getAllPublicCompanies(){
        return gameManager.getAllPublicCompanies();
    }

    public String getClassName (Defs.ClassName key) {
        return gameManager.getClassName(key);
    }

    public Object getGameParameter (Defs.Parm key) {
        return gameManager.getGameParameter(key);
    }

    public boolean getGameParameterAsBoolean (Defs.Parm key) {
        return (Boolean) getGameParameter(key);
    }

}

package rails.ui.swing;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.GuiDef;
import rails.common.GuiHints;
import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.*;
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

    protected GameManagerI gameManager;
    protected PossibleAction lastAction;
    protected ActionPerformer activeWindow = null;
    protected StartRound startRound;

    protected RoundI currentRound;
    protected RoundI previousRound;
    protected Class<? extends RoundI> previousRoundType = null;
    protected Class<? extends RoundI> currentRoundType = null;
    protected GuiHints uiHints= null;
    protected String previousRoundName;
    protected String currentRoundName;

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

    protected boolean configuredStockChartVisibility = false;

    protected boolean previousStockChartVisibilityHint;
    protected boolean previousStatusWindowVisibilityHint;
    protected boolean previousORWindowVisibilityHint;

    protected boolean previousResult;

    protected static Logger log =
            Logger.getLogger(GameUIManager.class.getPackage().getName());

    public GameUIManager() {

    }

    public void init (GameManagerI gameManager) {

        instance = this;
        this.gameManager = gameManager;
        uiHints = gameManager.getUIHints();

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

        configuredStockChartVisibility = "yes".equalsIgnoreCase(Config.get("stockchart.window.open"));

    }

    public void gameUIInit() {
        imageLoader = new ImageLoader();
        stockChart = new StockChart(this);
        reportWindow = new ReportWindow(gameManager);
        orWindow = new ORWindow(this);
        orUIManager = orWindow.getORUIManager();

        String statusWindowClassName = getClassName(GuiDef.ClassName.STATUS_WINDOW);
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
        processOnServer(new NullAction(NullAction.START_GAME));
        statusWindow.setGameActions();
    }

    public boolean processOnServer(PossibleAction action) {

        boolean result = true;

        // In some cases an Undo requires a different follow-up
        lastAction = action;

        if (action == null) {
            // If the action is null, we can skip processing
            // and continue with following up a previous action.
            // This occurs after a nonmodal Message dialog.
            result = previousResult;

        } else {
            action.setActed();
            action.setPlayerName(getCurrentPlayer().getName());

            log.debug("==Passing to server: " + action);

            Player player = getCurrentPlayer();
            if (player != null) {
                action.setPlayerName(player.getName());
            }

            // Process the action on the server
            result = previousResult = gameManager.process(action);

            // Follow-up the result
            log.debug("==Result from server: " + result);
            reportWindow.addLog();
            /*
            if (DisplayBuffer.getAutoDisplay()) {
                if (displayServerMessage()) {
                    // Interrupt processing.
                    // Will be continued via dialogActionPerformed().
                    return true;
                }
            }*/
        }

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

        // Is this perhaps the right place to display messages...?
        if (DisplayBuffer.getAutoDisplay()) {
            if (displayServerMessage()) {
                // Interrupt processing.
                // Will be continued via dialogActionPerformed().
                return true;
            }
        }

        if (!result) return false;

           return activeWindow.processImmediateAction();
    }

    public boolean displayServerMessage() {
        String[] message = DisplayBuffer.get();
        if (message != null) {
            setCurrentDialog(new MessageDialog(this,
                    LocalText.getText("Message"),
                    "<html>" + Util.joinWithDelimiter(message, "<br>")),
                null);
            return true;
        }
        return false;
    }


    public void updateUI() {

        previousRoundType = currentRoundType;
        previousRoundName = currentRoundName;
        previousRound = currentRound;

        currentRound = gameManager.getCurrentRound();
        currentRoundName = currentRound.toString();

        log.debug("Current round=" + currentRoundName + ", previous round="
                  + previousRoundName);

        currentRoundType = uiHints.getCurrentRoundType();

        /* Process actual round type changes */
        if (previousRoundType != currentRoundType) {

            /* Finish previous round UI processing */
            if (previousRoundType != null) {

                if (StockRound.class.isAssignableFrom(previousRoundType)) {
                    log.debug("UI leaving Stock Round "+previousRoundName);
                    statusWindow.finishRound();
                } else if (StartRound.class.isAssignableFrom(previousRoundType)) {
                    log.debug("UI leaving Start Round "+previousRoundName);
                    if (startRoundWindow != null) {
                        startRoundWindow.close();
                        startRoundWindow = null;
                    }
                } else if (OperatingRound.class.isAssignableFrom(previousRoundType)) {
                    log.debug("UI leaving Operating Round "+previousRoundName);
                    orUIManager.finish();
                } else if (SwitchableUIRound.class.isAssignableFrom(previousRoundType) ) {
                    log.debug("UI leaving switchable round type "+previousRoundName);
                }
            }

        }

        if (currentRound != previousRound) {

            // Start the new round UI processing
            if (StartRound.class.isAssignableFrom(currentRoundType)) {

                log.debug("UI entering Start Round "+currentRoundName);
                startRound = (StartRound) currentRound;
                if (startRoundWindow == null) {
                    startRoundWindow = new StartRoundWindow(startRound, this);
                }

            } else if (StockRound.class.isAssignableFrom(currentRoundType)) {

                log.debug("UI entering Stock Round "+currentRoundName);

            } else if (OperatingRound.class.isAssignableFrom(currentRoundType)) {

                log.debug("UI entering Operating Round "+currentRoundName);
                orUIManager.initOR((OperatingRound) currentRound);

            } else if (SwitchableUIRound.class.isAssignableFrom(currentRoundType) ) {
                log.debug("UI entering switchable round type "+currentRoundName);
                statusWindow.pack();
            }
        }

        /* Process visible round type changes */

        // Visibility settings are handled first.
        // Any window not represented in a setting is left unaffected.
        // Each window set visible or already being visible will be put
        // in front as well.
        // As the settings are handled in which these have been entered,
        // this means that this way the window top-to-bottom sequence
        // can be influenced.
        // To make this work, clearVisbilityHints() should be called
        // before each sequence of settings (usually at the start of a round).
        for (GuiHints.VisibilityHint hint : uiHints.getVisibilityHints()) {
            switch (hint.getType()) {
            case STOCK_MARKET:
                boolean stockChartVisibilityHint = hint.getVisibility()
                        || configuredStockChartVisibility;
                if (stockChartVisibilityHint != previousStockChartVisibilityHint) {
                    stockChart.setVisible(stockChartVisibilityHint);
                    previousStockChartVisibilityHint = stockChartVisibilityHint;
                }
                if (hint.getVisibility()) stockChart.toFront();
               break;
            case STATUS:
                boolean statusWindowVisibilityHint = hint.getVisibility();
                if (statusWindowVisibilityHint != previousStatusWindowVisibilityHint) {
                    statusWindow.setVisible(statusWindowVisibilityHint);
                    previousStatusWindowVisibilityHint = statusWindowVisibilityHint;
                }
                if (statusWindowVisibilityHint) statusWindow.toFront();
                break;
            case MAP:
                boolean orWindowVisibilityHint = hint.getVisibility();
                if (orWindowVisibilityHint != previousORWindowVisibilityHint) {
                    orWindow.setVisible(orWindowVisibilityHint);
                    previousORWindowVisibilityHint = orWindowVisibilityHint;
                }
                if (orWindowVisibilityHint) orWindow.toFront();
                break;
            case START_ROUND:
                // Handled elsewhere
            }
        }

        // Active window settings are handled last.
        // Side effects: the active window is made visible and put on top.
        if (uiHints.getActivePanel() == GuiDef.Panel.START_ROUND) {
            log.debug("Entering Start Round UI type");
            activeWindow = startRoundWindow;
            startRoundWindow.setVisible(true);
            startRoundWindow.toFront();

        } else if (uiHints.getActivePanel() == GuiDef.Panel.STATUS) {

            log.debug("Entering Stock Round UI type");
            activeWindow = statusWindow;
            stockChart.setVisible(true);
            statusWindow.setVisible(true);
            statusWindow.toFront();

        } else if (uiHints.getActivePanel() == GuiDef.Panel.MAP) {

            log.debug("Entering Operating Round UI type ");
            activeWindow = orWindow;
            orWindow.setVisible(true);
            orWindow.toFront();
        }



        statusWindow.setupFor(currentRound);

        // Update the currently visible round window
        // "Switchable" rounds will be handled from subclasses of this class.
        if (StartRoundWindow.class.isAssignableFrom(activeWindow.getClass())) {

            log.debug("Updating Start round window");
            startRoundWindow.updateStatus();
            startRoundWindow.setSRPlayerTurn(startRound.getCurrentPlayerIndex());

        } else if (StatusWindow.class.isAssignableFrom(activeWindow.getClass())) {

            log.debug("Updating Stock (status) round window");
            statusWindow.updateStatus();

        } else if (ORWindow.class.isAssignableFrom(activeWindow.getClass())) {

            log.debug("Updating Operating round window");
            orUIManager.updateStatus();
        }

        updateStatus(activeWindow);
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

            CheckBoxDialog dialog = new CheckBoxDialog(this,
                    LocalText.getText("ExchangeTokens"),
                    prompt,
                    options.toArray(new String[0]));
            setCurrentDialog (dialog, action);

        }
    }

    public void dialogActionPerformed () {
        dialogActionPerformed(false);
    }

    public void dialogActionPerformed (boolean ready) {

        if (!ready) {
            
            if (checkGameSpecificDialogAction()) {
                ;
            } else if (currentDialog instanceof RadioButtonDialog
                    && currentDialogAction instanceof StartCompany) {

                RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                StartCompany action = (StartCompany) currentDialogAction;

                int index = dialog.getSelectedOption();
                if (index >= 0) {
                    int price = action.getStartPrices()[index];
                    action.setStartPrice(price);
                    action.setNumberBought(action.getCertificate().getShares());
                } else {
                    // No selection done - no action
                    return;
                }


            } else if (currentDialog instanceof CheckBoxDialog
                    && currentDialogAction instanceof ExchangeTokens) {

                CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
                ExchangeTokens action = (ExchangeTokens) currentDialogAction;
                boolean[] exchanged = dialog.getSelectedOptions();
                String[] options = dialog.getOptions();

                int numberSelected = 0;
                for (int index=0; index < options.length; index++) {
                    if (exchanged[index]) {
                        numberSelected++;
                    }
                }

                int minNumber = action.getMinNumberToExchange();
                int maxNumber = action.getMaxNumberToExchange();
                if (numberSelected < minNumber
                        || numberSelected > maxNumber) {
                    if (minNumber == maxNumber) {
                        JOptionPane.showMessageDialog(null,
                                LocalText.getText("YouMustSelect1", minNumber));
                    } else {
                        JOptionPane.showMessageDialog(null,
                                LocalText.getText("YouMustSelect2", minNumber, maxNumber));
                    }
                    exchangeTokens (action);
                    return;

                }
                for (int index=0; index < options.length; index++) {
                    if (exchanged[index]) {
                        action.getTokensToExchange().get(index).setSelected(true);
                   }
                }
            } else if (currentDialog instanceof RadioButtonDialog
                        && currentDialogAction instanceof RepayLoans) {

                    RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                    RepayLoans action = (RepayLoans) currentDialogAction;
                    int selected = dialog.getSelectedOption();
                    action.setNumberTaken(action.getMinNumber() + selected);
            } else if (currentDialog instanceof MessageDialog) {
                // Nothing to do
                currentDialogAction = null; // Should already be null
            } else {
                return;
            }
        }

        processOnServer(currentDialogAction);
    }

    /** Stub, can be overridden by subclasses */
    protected boolean checkGameSpecificDialogAction() {
        return false;
    }

    public JDialog getCurrentDialog() {
        return currentDialog;
    }

    public PossibleAction getCurrentDialogAction () {
        return currentDialogAction;
    }

    public void setCurrentDialog (JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = dialog;
        currentDialogAction = action;
    }

    public void saveGame(GameAction saveAction) {

        JFileChooser jfc = new JFileChooser();
        String filename;
        if (providedName != null) {
            filename = providedName;
        } else {
            if (NEXT_PLAYER_SUFFIX.equals(saveSuffixSpec)) {
                saveSuffix = "_" + gameManager.getCurrentPlayer().getName().replaceAll("[^-\\w\\.]", "_");
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

    public String getClassName (GuiDef.ClassName key) {
        return gameManager.getClassName(key);
    }

    public Object getGameParameter (GuiDef.Parm key) {
        return gameManager.getGuiParameter(key);
    }

    public boolean getGameParameterAsBoolean (GuiDef.Parm key) {
        return (Boolean) getGameParameter(key);
    }

}

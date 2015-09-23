package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.Observer;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;

import com.google.common.collect.Iterables;


/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUIManager implements DialogOwner {
    public static GameUIManager instance = null;

    public StockChart stockChart;
    public StatusWindow statusWindow;
    public ReportWindow reportWindow;
    public ConfigWindow configWindow;
    public ORUIManager orUIManager;
    public ORWindow orWindow; // TEMPORARY
    private StartRoundWindow startRoundWindow;

    protected JDialog currentDialog = null;
    protected PossibleAction currentDialogAction = null;

    public static ImageLoader imageLoader;

    protected RailsRoot railsRoot;
    protected PossibleAction lastAction;
    protected ActionPerformer activeWindow = null;
    protected StartRound startRound;

    protected RoundFacade currentRound;
    protected RoundFacade previousRound;
    protected Class<? extends RoundFacade> previousRoundType = null;
    protected Class<? extends RoundFacade> currentRoundType = null;
    protected GuiHints uiHints= null;
    protected String previousRoundName;
    protected String currentRoundName;

    protected static final String DEFAULT_SAVE_DIRECTORY = "save";
    protected static final String DEFAULT_SAVE_PATTERN = "yyyyMMdd_HHmm";
    public static final String DEFAULT_SAVE_EXTENSION = "rails";
    protected static final String NEXT_PLAYER_SUFFIX = "NEXT_PLAYER";
    protected static final String CURRENT_ROUND_SUFFIX = "CURRENT_ROUND";

    protected String saveDirectory;
    protected String savePattern;
    protected String saveExtension;
    protected String savePrefix;
    protected String saveSuffixSpec = "";
    protected String saveSuffix = "";
    protected String providedName = null;
    protected SimpleDateFormat saveDateTimeFormat;
    protected File lastFile, lastDirectory;

    protected boolean autoSaveLoadInitialized = false;
    protected int autoSaveLoadStatus = 0;
    protected int autoSaveLoadPollingInterval = 30;
    protected AutoLoadPoller autoLoadPoller = null;
    protected boolean myTurn = true;
    protected String lastSavedFilenameFilepath;
    protected String lastSavedFilename = "";
    protected String localPlayerName = "";

    protected boolean gameWasLoaded = false;

    protected WindowSettings windowSettings;

    protected boolean configuredStockChartVisibility = false;

    protected boolean previousStockChartVisibilityHint;
    protected boolean previousStatusWindowVisibilityHint;
    protected boolean previousORWindowVisibilityHint;

    protected boolean previousResult;
    
    // Player order
//    protected PlayerOrderView playerOrderView;
    /** Player names set at time of initialisation or after reordering.
     *<p> To be used as a reference to the current player order as shown in the UI.
     * Note, that getPlayers() currently calls the game engine directly, and
     * therefore updates before the UI gets notice via the playerOrderView.*/
    protected List<String> currentGuiPlayerNames;


    /* Keys of dialogs owned by this class */
    public static final String COMPANY_START_PRICE_DIALOG = "CompanyStartPrice";
    public static final String SELECT_COMPANY_DIALOG = "SelectCompany";
    public static final String REPAY_LOANS_DIALOG = "RepayLoans";
    public static final String EXCHANGE_TOKENS_DIALOG = "ExchangeTokens";
    
    

    protected static Logger log =
            LoggerFactory.getLogger(GameUIManager.class);

    private SplashWindow splashWindow = null;


    public GameUIManager() {

    }

    public void init (RailsRoot root, boolean wasLoaded, SplashWindow splashWindow) {

        this.splashWindow = splashWindow;
        splashWindow.notifyOfStep(SplashWindow.STEP_INIT_UI);

        instance = this;
        this.railsRoot = root;
        uiHints = railsRoot.getGameManager().getUIHints();
        savePrefix = railsRoot.getGameName();
        gameWasLoaded = wasLoaded;

        initWindowSettings();
        initSaveSettings();
        initFontSettings();

        configuredStockChartVisibility = "yes".equalsIgnoreCase(Config.get("stockchart.window.open"));
        
//        playerOrderView = new PlayerOrderView();
        currentGuiPlayerNames = new ArrayList<String>();
        for (Player player : getPlayers()) {
            currentGuiPlayerNames.add (player.getId());
       }
    }

    private void initWindowSettings () {

        windowSettings = new WindowSettings (railsRoot.getGameName());
        windowSettings.load();
    }

    public void terminate () {
        getWindowSettings ().save();
        if (orWindow != null) orWindow.saveLayout();
        System.exit(0);
    }

    public WindowSettings getWindowSettings () {
        return windowSettings;
    }

    private void initSaveSettings() {
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
        if (!Util.hasValue(saveSuffixSpec) || saveSuffixSpec.equals(NEXT_PLAYER_SUFFIX)) {
            saveSuffix = getPlayers().get(0).getId();
        } else if (saveSuffixSpec.equals(CURRENT_ROUND_SUFFIX))  {
            if (currentRound != null) {
                saveSuffix = currentRound.getRoundName();
            } else {
                saveSuffix = "";
            }
        } else { // otherwise use specified suffix
            saveSuffix = saveSuffixSpec;
        }
        log.debug("Initial save suffix: "+saveSuffix);
    }

    private void initFontSettings() {

        // font settings, can be game specific
        String fontType = Config.getGameSpecific("font.ui.name");
        Font font = null;
        if (Util.hasValue(fontType)) {
            boolean boldStyle = true;
            String fontStyle = Config.getGameSpecific("font.ui.style");
            if (Util.hasValue(fontStyle)) {
                if (fontStyle.equalsIgnoreCase("plain")) {
                    boldStyle = false;
                }
            }
            if (boldStyle) {
                font = new Font(fontType, Font.BOLD, 12);
            } else {
                font = new Font(fontType, Font.PLAIN, 12);
            }
            if (font != null) log.debug("Change text fonts globally to " + font.getName() + " / " + (boldStyle ? "Bold" : "Plain"));
        }

        log.debug("Change text fonts to relative scale " + GUIGlobals.getFontsScale());
        changeGlobalFont(font, GUIGlobals.getFontsScale());
    }


    public void gameUIInit(boolean newGame) {

        imageLoader = new ImageLoader();

        splashWindow.notifyOfStep(SplashWindow.STEP_STOCK_CHART);
        stockChart = new StockChart(this);

        splashWindow.notifyOfStep(SplashWindow.STEP_REPORT_WINDOW);
        if (Config.get("report.window.type").equalsIgnoreCase("static")) {
            // reportWindow = new ReportWindowStatic(this);
        } else {
            reportWindow = new ReportWindow(this);
        }

        orWindow = new ORWindow(this, splashWindow);
        orUIManager = orWindow.getORUIManager();

        splashWindow.notifyOfStep(SplashWindow.STEP_STATUS_WINDOW);
        String statusWindowClassName = getClassName(GuiDef.ClassName.STATUS_WINDOW);
        try {
            Class<? extends StatusWindow> statusWindowClass =
                Class.forName(statusWindowClassName).asSubclass(StatusWindow.class);
            statusWindow = statusWindowClass.newInstance();

            //            GraphicsEnvironment ge = GraphicsEnvironment.
            //            getLocalGraphicsEnvironment();
            //            GraphicsDevice[] gs = ge.getScreenDevices();
            //            log.debug("ScreenDevices = " + Arrays.toString(gs));
            //            statusWindow = statusWindowClass.getConstructor(GraphicsConfiguration.class).newInstance(gs[1].getDefaultConfiguration());

            statusWindow.init(this);
        } catch (Exception e) {
            log.error("Cannot instantiate class " + statusWindowClassName, e);
            System.exit(1);
        }

        // removed for reloaded games to avoid double revenue calculation
        if (newGame) {
            splashWindow.notifyOfStep(SplashWindow.STEP_INIT_NEW_GAME);
            updateUI();
        }

        reportWindow.scrollDown();

        // define configWindow
        splashWindow.notifyOfStep(SplashWindow.STEP_CONFIG_WINDOW);
        configWindow = new ConfigWindow(statusWindow);
        configWindow.init(true);

        // notify sound manager of game initialization
        splashWindow.notifyOfStep(SplashWindow.STEP_INIT_SOUND);
        SoundManager.notifyOfGameInit(railsRoot);
    }

    public void startLoadedGame() {
        gameUIInit(false); // false indicates reload

        splashWindow.notifyOfStep(SplashWindow.STEP_INIT_LOADED_GAME);
        processAction(new NullAction(NullAction.Mode.START_GAME));
        statusWindow.setGameActions();
    }

    public boolean processAction(PossibleAction action) {

        boolean result = true;

        // In some cases an Undo requires a different follow-up
        lastAction = action;

        if (action == null) {
            // If the action is null, we can skip processing
            // and continue with following up a previous action.
            // This occurs after a nonmodal Message dialog.
            result = previousResult;

        } else {

            Player oldPlayer = getCurrentPlayer();
            boolean wasMyTurn = oldPlayer.getId().equals(localPlayerName);

            // Notify the Sound Manager about this action, as it could lead to
            // playing sfx or music.
            // Notification has to be done before action processing so that
            // resulting sfx are played in the correct order (first the action
            // related sfx and then model-change related sfx)
            SoundManager.notifyOfActionProcessing(railsRoot, action);

            // Process the action on the server
            result = previousResult = processOnServer (action);

            // Process any autosaving and turn relinquishing, resp. autoloading and turn pickup
            if (autoSaveLoadInitialized && autoSaveLoadStatus != AutoLoadPoller.OFF) {
                Player newPlayer = getCurrentPlayer();
                boolean isMyTurn = newPlayer.getId().equals(localPlayerName);
                if (newPlayer != oldPlayer) {
                    if (wasMyTurn && !isMyTurn) {
                        autoSave (newPlayer.getId());
                        autoLoadPoller.setLastSavedFilename(lastSavedFilename);
                        autoLoadPoller.setActive(true);
                        log.info ("Relinquishing turn to "+newPlayer.getId());
                    } else if (!wasMyTurn && isMyTurn) {
                        autoLoadPoller.setActive(false);
                        setCurrentDialog(new MessageDialog(null, this,
                                (JFrame) activeWindow,
                                LocalText.getText("Message"),
                                LocalText.getText("YourTurn", localPlayerName)),
                                null);
                        log.info ("Resuming turn as "+localPlayerName);
                    } else {
                        log.info(newPlayer.getId()+" now has the turn");
                    }
                    myTurn = isMyTurn;
                } else {
                    log.info(oldPlayer.getId()+" keeps the turn");
                }
            }
        }

        // Check in which round we are now,
        // and make sure that the right window is active.
        updateUI();

        statusWindow.initGameActions();
        if (!myTurn) return true;
        statusWindow.setGameActions();
        statusWindow.setCorrectionMenu();
        reportWindow.setActions();

        // Is this perhaps the right place to display messages...?
        if (getDisplayBuffer().getAutoDisplay()) {
            if (displayServerMessage()) {
                // Interrupt processing.
                // Will be continued via dialogActionPerformed().
                return true;
            }
        }

        // display the end of game report
        if (railsRoot.getGameManager().isGameOver()) statusWindow.endOfGameReport();

        if (!result) return false;

        return activeWindow.processImmediateAction();
    }

    protected boolean processOnServer (PossibleAction action) {

        boolean result;

        action.setActed();
        action.setPlayerName(getCurrentPlayer().getId());

        log.debug("==Passing to server: " + action);

        Player player = getCurrentPlayer();
        if (player != null) {
            action.setPlayerName(player.getId());
        }

        // Process the action on the server
        result = railsRoot.getGameManager().process(action);

        // Follow-up the result
        log.debug("==Result from server: " + result);

        return result;
    }

    public boolean displayServerMessage() {
        String[] message = getDisplayBuffer().get();
        if (message != null) {
            setCurrentDialog(new MessageDialog(null, this,
                    (JFrame) activeWindow,
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

        currentRound = railsRoot.getGameManager().getCurrentRound();
        currentRoundName = currentRound.toString();

        log.debug("Current round=" + currentRoundName + ", previous round="
                + previousRoundName);

        currentRoundType = uiHints.getCurrentRoundType();

        /* Process actual round type changes */
        if (previousRoundType != currentRoundType) {

            /* Finish previous round UI processing */
            if (previousRoundType != null) {

                /* close current dialog */
                setCurrentDialog(null, null);

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
                    //startRoundWindow = new StartRoundWindow(startRound, this);
                    String startRoundWindowClassName = getClassName(GuiDef.ClassName.START_ROUND_WINDOW);
                    try {
                        Class<? extends StartRoundWindow> startRoundWindowClass =
                            Class.forName(startRoundWindowClassName).asSubclass(StartRoundWindow.class);
                        startRoundWindow = startRoundWindowClass.newInstance();
                        startRoundWindow.init(startRound, this);
                    } catch (Exception e) {
                        log.error("Cannot instantiate class " + startRoundWindowClassName, e);
                        System.exit(1);

                    }
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
                    setMeVisible(stockChart,stockChartVisibilityHint);
                    previousStockChartVisibilityHint = stockChartVisibilityHint;
                }
                if (hint.getVisibility()) setMeToFront(stockChart);
                break;
            case STATUS:
                boolean statusWindowVisibilityHint = hint.getVisibility();
                if (statusWindowVisibilityHint != previousStatusWindowVisibilityHint) {
                    setMeVisible(statusWindow,statusWindowVisibilityHint);
                    previousStatusWindowVisibilityHint = statusWindowVisibilityHint;
                }
                if (statusWindowVisibilityHint) setMeToFront(statusWindow);
                break;
            case MAP:
                boolean orWindowVisibilityHint = hint.getVisibility();
                if (orWindowVisibilityHint != previousORWindowVisibilityHint) {
                    setMeVisible(orWindow,orWindowVisibilityHint);
                    previousORWindowVisibilityHint = orWindowVisibilityHint;
                }
                if (orWindowVisibilityHint) setMeToFront(orWindow);
                break;
            case START_ROUND:
                // Handled elsewhere
            }
        }

        boolean correctionOverride = statusWindow.setupFor(currentRound);
        correctionOverride = false;

        if (correctionOverride) {
            log.debug("Correction overrides active window: status window active");
        }

        // Active window settings are handled last.
        // Side effects: the active window is made visible and put on top.
        if (uiHints.getActivePanel() == GuiDef.Panel.START_ROUND) {
            log.debug("Entering Start Round UI type");
            activeWindow = startRoundWindow;
            setMeVisible(startRoundWindow,true);
            setMeToFront(startRoundWindow);

        } else if (uiHints.getActivePanel() == GuiDef.Panel.STATUS || correctionOverride) {

            log.debug("Entering Stock Round UI type");
            activeWindow = statusWindow;
            stockChart.setVisible(true);
            setMeVisible(statusWindow,true);
            setMeToFront(statusWindow);

        } else if (uiHints.getActivePanel() == GuiDef.Panel.MAP  && !correctionOverride) {

            log.debug("Entering Operating Round UI type ");
            activeWindow = orWindow;
            setMeVisible(orWindow,true);
            setMeToFront(orWindow);
        }


        // Update the currently visible round window
        // "Switchable" rounds will be handled from subclasses of this class.
        if (StartRoundWindow.class.isAssignableFrom(activeWindow.getClass())) {

            log.debug("Updating Start round window");
            startRoundWindow.updateStatus(myTurn);
            startRoundWindow.setSRPlayerTurn(getRoot().getPlayerManager().getCurrentPlayer().getIndex());

        } else if (StatusWindow.class.isAssignableFrom(activeWindow.getClass())) {
            //        } else {

            log.debug("Updating Stock (status) round window");
            statusWindow.updateStatus(myTurn);

        } else if (ORWindow.class.isAssignableFrom(activeWindow.getClass())) {

            log.debug("Updating Operating round window");
            orUIManager.updateStatus(myTurn);
        }

        updateStatus(activeWindow);

    }
    
    protected void updatePlayerOrder (List<String> newPlayerOrder) {
            if (startRoundWindow != null) startRoundWindow.updatePlayerOrder (newPlayerOrder);
            if (statusWindow != null) statusWindow.updatePlayerOrder (newPlayerOrder);
            currentGuiPlayerNames = newPlayerOrder;
        }
    
    /** Stub, to be overridden in subclasses for special round types */
    protected void updateStatus(ActionPerformer activeWindow) {

    }

    public void discardTrains (DiscardTrain dt) {

        PublicCompany c = dt.getCompany();
        String playerName = dt.getPlayerName();
        Set<Train> trains = dt.getOwnedTrains();
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
                    c.getId());
        }
        int offset = j;
        for (int i = 0; i < trains.size(); i++) {
            trainOptions.add(
                    options[j++] = LocalText.getText("N_Train",
                            Iterables.get(trains, i).toText())
            );
        }
        if (prompt == null) prompt = LocalText.getText(
                "HAS_TOO_MANY_TRAINS",
                playerName,
                c.getId() );

        String discardedTrainName =
            (String) JOptionPane.showInputDialog(orWindow,
                    prompt,
                    LocalText.getText("WhichTrainToDiscard"),
                    JOptionPane.QUESTION_MESSAGE, null,
                    options, options[0]);
        if (discardedTrainName != null) {
            int index = trainOptions.indexOf(discardedTrainName);
            // FIXME: Does this work with the new Set defined?
            if (index >= offset) {
                Train discardedTrain =
                        Iterables.get(trains, trainOptions.indexOf(discardedTrainName)-offset);
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
        Station station;
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
            hex = railsRoot.getMapManager().getHex(hexName);
            station = hex.getStation(cityNumber);
            oldCompName = t.getOldCompanyName();
            options.add(LocalText.getText("ExchangeableToken",
                    oldCompName,
                    hex.toText(),
                    cityNumber,
                    hex.getConnectionString(station)));
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

            CheckBoxDialog dialog = new CheckBoxDialog(EXCHANGE_TOKENS_DIALOG,
                    this,
                    orWindow,
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

            String key = "";
            if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog)currentDialog).getKey();

            if (currentDialog instanceof AutoSaveLoadDialog) {
                // Not yet a NonModalDialog subclass
                autoSaveLoadGame2 ((AutoSaveLoadDialog)currentDialog);

            } else if (!(currentDialog instanceof NonModalDialog)) {

                log.warn("Unknown dialog action: dialog=["+currentDialog+"] action=["+currentDialogAction+"]");
                currentDialogAction = null;

            } else if (currentDialog instanceof MessageDialog) {
                // Nothing to do.
                currentDialogAction = null;
                // This cancels the currently incomplete user action.
                // WARNING: always do this if dialog processing terminates in a context
                // where an action is aborted and the UI must return to its previous state.
                // This will normally be the case after a CANCEL (but not after a NO).

            } else if (COMPANY_START_PRICE_DIALOG.equals(key)) {

                RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                StartCompany action = (StartCompany) currentDialogAction;

                int index = dialog.getSelectedOption();
                if (index >= 0) {
                    int price = action.getStartPrices()[index];
                    action.setStartPrice(price);
                    action.setNumberBought(action.getSharesPerCertificate());
                } else {
                    // No selection done - no action
                    currentDialogAction = null;
                }

            } else if (EXCHANGE_TOKENS_DIALOG.equals(key)) {

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
            } else if (REPAY_LOANS_DIALOG.equals(key)) {

                RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                RepayLoans action = (RepayLoans) currentDialogAction;
                int selected = dialog.getSelectedOption();
                action.setNumberTaken(action.getMinNumber() + selected);

            } else {
                log.warn("Unknown NonModal dialog action: dialog=["+currentDialog+"] action=["+currentDialogAction+"]");
                currentDialogAction = null;
            }
        }

        processAction(currentDialogAction);

    }

    protected void autoSave (String newPlayer) {
        lastSavedFilename = savePrefix + "_"
        + saveDateTimeFormat.format(new Date()) + "_"
        + newPlayer + "."
        + saveExtension;
        GameAction saveAction = new GameAction(GameAction.Mode.SAVE);
        saveAction.setFilepath(saveDirectory + "/" + lastSavedFilename);
        log.debug("Autosaving to "+lastSavedFilename);
        processOnServer (saveAction);

        saveAutoSavedFilename (lastSavedFilename);
    }

    protected void saveAutoSavedFilename (String lastSavedFilename) {

        try {
            File f = new File (lastSavedFilenameFilepath);
            PrintWriter out = new PrintWriter (new FileWriter (f));
            out.println (lastSavedFilename);
            out.close();
        } catch (IOException e) {
            log.error ("Exception whilst autosaving file '"+lastSavedFilenameFilepath+"'", e);
        }

    }

    protected boolean pollingIsOn () {
        return autoLoadPoller != null && autoLoadPoller.getStatus() == AutoLoadPoller.ON;
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

    /**
     * Change global font size
     * @param scale
     */
    public void changeGlobalFont(Font replaceFont, double scale) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while(keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if(value != null && value instanceof Font) {
                UIManager.put(key, null);
                Font font;
                if (replaceFont != null) {
                    font = replaceFont;
                } else {
                    font = UIManager.getFont(key);
                }
                if(font != null) {
                    float newSize = font.getSize2D() * (float)scale;
                    UIManager.put(key, new FontUIResource(font.deriveFont(newSize)));
                }
            }
        }
    }


    public void exportGame(GameAction exportAction) {
        JFileChooser jfc = new JFileChooser();
        String filename;
        if (providedName != null) {
            filename = providedName;
        } else {
            filename = saveDirectory + "/" + savePrefix + "_"
            + saveDateTimeFormat.format(new Date())+ "_"
            + saveSuffix + ".txt";
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
            exportAction.setFilepath(filepath);
            processAction(exportAction);
        }
    }


    public void saveGame(GameAction saveAction) {

        // copy latest report buffer entries to clipboard
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection reportText = new StringSelection(
                getRoot().getReportManager().getReportBuffer().getRecentPlayer());
        clipboard.setContents(reportText, null);

        JFileChooser jfc = new JFileChooser();
        String filename;
        if (providedName != null) {
            filename = providedName;
        } else {
            String currentSuffix;
            if (NEXT_PLAYER_SUFFIX.equals(saveSuffixSpec)) {
                currentSuffix = getCurrentPlayer().getId().replaceAll("[^-\\w\\.]", "_");
            } else if (CURRENT_ROUND_SUFFIX.equals(saveSuffixSpec)) {
                if (currentRound != null) {
                    currentSuffix = currentRound.getRoundName();
                } else {
                    currentSuffix = "";
                }
            } else {
                currentSuffix = saveSuffix;
            }
            filename =
                saveDirectory + "/" + savePrefix + "_"
                + saveDateTimeFormat.format(new Date()) + "_"
                + currentSuffix + "."
                + saveExtension;
        }

        File proposedFile = new File(filename);
        jfc.setSelectedFile(proposedFile);

        // allows adjustment of the save dialog title, to add hint about copy to clipboard
        jfc.setDialogTitle(LocalText.getText("SaveDialogTitle"));

        if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String filepath = selectedFile.getPath();
            saveDirectory = selectedFile.getParent();
            if (!selectedFile.getName().equalsIgnoreCase(proposedFile.getName())) {
                // User has not accepted the default name but entered a different one.
                // Check the new name. If only the prefix has changed, only remember that part.
                String[] proposedParts = proposedFile.getName().split("_", 2);
                String[] selectedParts = selectedFile.getName().split("_", 2);
                if (!proposedParts[0].equals(selectedParts[0])
                        && proposedParts[1].equals(selectedParts[1])) {
                    savePrefix = selectedParts[0];
                } else {
                    // Otherwise, remember and keep using the whole filename.
                    providedName = filepath;
                }
            }
            saveAction.setFilepath(filepath);
            processAction(saveAction);
        }

    }

    public void reloadGame(GameAction reloadAction) {

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(saveDirectory));

        if (jfc.showOpenDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            saveDirectory = selectedFile.getParent();
            reloadAction.setFilepath(selectedFile.getPath());
            processAction(reloadAction);
        } else { // cancel pressed
            return;
        }

    }

    public void autoSaveLoadGame () {

        localPlayerName = System.getProperty("local.player.name");
        if (!Util.hasValue(localPlayerName)) {
            localPlayerName = Config.get("local.player.name");
        }
        if (!Util.hasValue(localPlayerName)) {
            // FIXME (Rails2.0) Replace this with something better (DisplayBuffer is not available so far
            // DisplayBuffer.add(this, "You cannot activate AutoSave/Load without setting local.player.name");
            return;
        }
        log.debug("Polling local player name: "+localPlayerName);

        AutoSaveLoadDialog dialog = new AutoSaveLoadDialog (this,
                autoSaveLoadStatus,
                autoSaveLoadPollingInterval);
        setCurrentDialog(dialog, null);
    }

    public void autoSaveLoadGame2 (AutoSaveLoadDialog dialog) {

        autoSaveLoadStatus = dialog.getStatus();
        autoSaveLoadPollingInterval = dialog.getInterval();

        if (autoLoadPoller == null && autoSaveLoadStatus > 0) {

            autoLoadPoller = new AutoLoadPoller (this, saveDirectory, savePrefix,
                    localPlayerName, autoSaveLoadStatus, autoSaveLoadPollingInterval);
            autoLoadPoller.start();
        } else if (autoLoadPoller != null) {
            autoLoadPoller.setStatus(autoSaveLoadStatus);
            autoLoadPoller.setPollingInterval(autoSaveLoadPollingInterval);
        }
        log.debug("AutoSaveLoad parameters: status="+autoSaveLoadStatus
                +" interval="+autoSaveLoadPollingInterval);

        if (gameWasLoaded) {
            autoSaveLoadInitialized = true;
            lastSavedFilenameFilepath = saveDirectory + "/" + savePrefix + ".last_rails";
            saveAutoSavedFilename (lastSavedFilename);
        }

        if (autoLoadPoller != null && autoSaveLoadStatus != AutoLoadPoller.OFF
                && !autoSaveLoadInitialized && !gameWasLoaded) {

            /* The first time (only) we use the normal save process,
             * so the player can select a directory, and change
             * the prefix if so desired.
             */
            GameAction saveAction = new GameAction(GameAction.Mode.SAVE);
            saveSuffix = localPlayerName;
            saveGame (saveAction);
            File lastSavedFile = new File (saveAction.getFilepath());
            saveDirectory = lastSavedFile.getParentFile().getPath();

            /* Now also save the "last saved file" file */
            String lastSavedFilename = lastSavedFile.getName();
            lastSavedFilenameFilepath = saveDirectory + "/" + savePrefix + ".last_rails";
            try {
                File f = new File (lastSavedFilenameFilepath);
                PrintWriter out = new PrintWriter (new FileWriter (f));
                out.println (lastSavedFilename);
                out.close();
                autoSaveLoadInitialized = true;
            } catch (IOException e) {
                log.error ("Exception whilst creating .last_rails file '"
                        + lastSavedFilenameFilepath + "'", e);
            }
        }

        myTurn = getCurrentPlayer().getId().equals(localPlayerName);

        if (!myTurn) {
            // Start autoload polling
            autoLoadPoller.setActive(autoSaveLoadStatus == AutoLoadPoller.ON && !myTurn);
            log.debug("MyTurn="+myTurn+" poller status="+autoLoadPoller.getStatus()
                    +" active="+autoLoadPoller.isActive());

        } else {
            myTurn = true;
            log.debug("MyTurn="+myTurn);
        }

    }

    public void saveGameStatus() {

        List<String> status = statusWindow.getGameStatus().getTextContents();

        JFileChooser jfc = new JFileChooser();
        String filename = saveDirectory + "/" + savePrefix + "_"
        + saveDateTimeFormat.format(new Date())+ ".status";

        File proposedFile = new File(filename);
        jfc.setSelectedFile(proposedFile);

        if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            try {
                PrintWriter pw = new PrintWriter(selectedFile);

                for (String line : status) pw.println(line) ;

                pw.close();

            } catch (IOException e) {
                log.error("Save failed", e);
                getDisplayBuffer().add(LocalText.getText("SaveFailed", e.getMessage()));
            }
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

    public RailsRoot getRoot() {
        return railsRoot;
    }
    
    public GameManager getGameManager() {
        return railsRoot.getGameManager();
    }
    
    public DisplayBuffer getDisplayBuffer() {
        return railsRoot.getReportManager().getDisplayBuffer();
    }
    
    public void setORUIManager(ORUIManager orUIManager) {
        this.orUIManager = orUIManager;
    }

    public ORUIManager getORUIManager() {
        return orUIManager;
    }

    public RoundFacade getCurrentRound() {
        return railsRoot.getGameManager().getCurrentRound();
    }

    public boolean isGameOver() {
        return railsRoot.getGameManager().isGameOver();
    }

    public int getNumberOfPlayers() {
        return railsRoot.getPlayerManager().getNumberOfPlayers();
    }

    public List<Player> getPlayers() {
        return railsRoot.getPlayerManager().getPlayers();
    }
    
    public Player getCurrentPlayer() {
        return railsRoot.getPlayerManager().getCurrentPlayer();
    }

    public Player getPriorityPlayer () {
        return railsRoot.getPlayerManager().getPriorityPlayer();
    }

    public Phase getCurrentPhase() {
        return railsRoot.getPhaseManager().getCurrentPhase();
    }

    public List<PublicCompany> getAllPublicCompanies(){
        return railsRoot.getCompanyManager().getAllPublicCompanies();
    }

    public String getClassName (GuiDef.ClassName key) {
        return railsRoot.getGameManager().getClassName(key);
    }

    public Object getGameParameter (GuiDef.Parm key) {
        return railsRoot.getGameManager().getGuiParameter(key);
    }

    public boolean getGameParameterAsBoolean (GuiDef.Parm key) {
        return (Boolean) getGameParameter(key);
    }

    private void setEnabledWindow(boolean enabled, JFrame window, JFrame exceptionWindow) {

        if (window != null && window != exceptionWindow) {
            window.setEnabled(enabled);
        }
    }
    /**
     * deactivate all game windows, except the argument one
     */
    public void setEnabledAllWindows(boolean enabled, JFrame exceptionWindow) {
        setEnabledWindow(enabled, stockChart, exceptionWindow);
        setEnabledWindow(enabled, reportWindow, exceptionWindow);
        setEnabledWindow(enabled, configWindow, exceptionWindow);
        setEnabledWindow(enabled, orWindow, exceptionWindow);
        setEnabledWindow(enabled, startRoundWindow, exceptionWindow);
        setEnabledWindow(enabled, statusWindow, exceptionWindow);
    }


    private void updateWindowsLookAndFeel() {
        SwingUtilities.updateComponentTreeUI(statusWindow);
        statusWindow.pack();
        SwingUtilities.updateComponentTreeUI(orWindow);
        orWindow.pack();
        SwingUtilities.updateComponentTreeUI(reportWindow);
        reportWindow.pack();
        SwingUtilities.updateComponentTreeUI(configWindow);
        configWindow.pack();
        SwingUtilities.updateComponentTreeUI(stockChart);
        stockChart.pack();
    }
    
    // Forwards the format() method to the server
    public String format(int amount) {
        return Bank.format(railsRoot, amount);
    }

    /** update fonts settings
     * (after configuration changes)
     */
    public static void updateUILookAndFeel() {
        GUIGlobals.initFontsScale();
        instance.initFontSettings();
        instance.updateWindowsLookAndFeel();
    }

    /**
     * Only set frame directly to visible if the splash phase is already over.
     * Otherwise, the splash framework remembers this visibility request and
     * postpones the setVisible to the point in time where the splash is completed.
     */
    public void setMeVisible(JFrame frame, boolean setToVisible) {
        if (splashWindow == null) {
            frame.setVisible(setToVisible);
        } else {
            splashWindow.registerFrameForDeferredVisibility(frame,setToVisible);
        }
    }

    /**
     * Only set frame directly to front if the splash phase is already over.
     * Otherwise, the splash framework remembers this toFront request and
     * postpones the toFront to the point in time where the splash is completed.
     */
    public void setMeToFront(JFrame frame) {
        if (splashWindow == null) {
            frame.toFront();
        } else {
            splashWindow.registerFrameForDeferredToFront(frame);
        }
    }

    /**
     * called when the splash process is completed
     * (and visibility changes are not to be deferred any more)
     */
    public void notifyOfSplashFinalization() {
        splashWindow = null;
    }

    /**
     * Packs specified frame and tries to apply user defined size afterwards.
     * These actions are performed within the EDT as the caller is assumed to
     * be run by a non-EDT thread.
     */
    public void packAndApplySizing(JFrame frame) {
        final JFrame finalFrame = frame;
        SwingUtilities.invokeLater(new Thread() {
            @Override
            public void run() {
                finalFrame.pack();

                WindowSettings ws = getWindowSettings();
                Rectangle bounds = ws.getBounds(finalFrame);
                if (bounds.x != -1 && bounds.y != -1) finalFrame.setLocation(bounds.getLocation());
                if (bounds.width != -1 && bounds.height != -1) finalFrame.setSize(bounds.getSize());
                ws.set(finalFrame);
            }
        });
    }
    
    public List<String> getCurrentGuiPlayerNames() {
        return currentGuiPlayerNames;
    }
    
   public class PlayerOrderView implements Observer {
        PlayerOrderView () {
            railsRoot.getPlayerManager().getPlayerOrderModel().addObserver(this);
        }

        public void update(Observable o, Object arg) {
               List<String> newPlayerNames = Arrays.asList(((String)arg).split(";"));
                updatePlayerOrder (newPlayerNames);
        }

        public void deRegister() {
            // TODO Auto-generated method stub

        }

        @Override
        public void update(String text) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public net.sf.rails.game.state.Observable getObservable() {
            // TODO Auto-generated method stub
            return null;
        }
   }

}

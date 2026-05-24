package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import net.sf.rails.ui.swing.WindowSettings;

import net.sf.rails.ui.swing.hexmap.HexMap;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import net.sf.rails.game.*;
import net.sf.rails.game.GameManager.TimeConsequence;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.read.CyclicBufferAppender;
import net.sf.rails.common.Config;
import net.sf.rails.common.ConfigManager;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.GuiHints;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.notify.Discord;
import net.sf.rails.common.notify.Slack;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.round.I_MapRenderableRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Observer;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.CheckBoxDialog;
import net.sf.rails.ui.swing.elements.DialogOwner;
import net.sf.rails.ui.swing.elements.MessageDialog;
import net.sf.rails.ui.swing.elements.NonModalDialog;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;
import net.sf.rails.ui.swing.elements.StartPriceGridDialog;
import net.sf.rails.util.Util;
import rails.game.action.*;
import rails.game.correct.TrainCorrectionAction;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Timer; // Correct Timer for Swing
import java.awt.event.ActionListener; // Correct Listener interface
import java.awt.event.ActionEvent; // Correct Event class
import net.sf.rails.game.ai.AIPlayer; //
import net.sf.rails.game.ai.TileLayOption; // 
import rails.game.action.*;
import net.sf.rails.game.specific._1835.PrussianFormationRound;

/**
 * This class is called by main() and loads all of the UI components
 */
public class GameUIManager implements DialogOwner {
    protected StatusWindow statusWindow;
    protected ReportWindow reportWindow;
    protected ConfigWindow configWindow;
    protected ORUIManager orUIManager;
    protected ORWindow orWindow; // TEMPORARY -- EV: Why?
    private StartRoundWindow startRoundWindow;

    
    protected JDialog currentDialog = null;

    protected StockChartWindow stockChartWindow;

    protected PossibleAction currentDialogAction = null;

    protected RailsRoot railsRoot;
    protected PossibleAction lastAction;
    protected ActionPerformer activeWindow = null;
    protected StartRound startRound;

    protected RoundFacade currentRound;
    protected RoundFacade previousRound;
    protected Class<? extends RoundFacade> previousRoundType = null;
    protected Class<? extends RoundFacade> currentRoundType = null;
    protected GuiHints uiHints = null;
    protected String previousRoundName;
    protected String currentRoundName;

    protected static final String DEFAULT_SAVE_DIRECTORY = "save";
    protected static final String DEFAULT_SAVE_PATTERN = "yyyyMMdd_HHmm";
    public static final String DEFAULT_SAVE_EXTENSION = "rails";

    public static final String DEFAULT_SAVE_POLLING_EXTENSION = "lrails";
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

    protected boolean autoSaveLoadInitialized = false;
    protected int autoSaveLoadStatus = 0;
    protected int autoSaveLoadPollingInterval = 30;
    protected AutoLoadPoller autoLoadPoller = null;
    protected boolean myTurn = true;
    protected String lastSavedFilename = null;
    protected String localPlayerName = "";
private static GameUIManager instance; 
    protected boolean gameWasLoaded = false;

    protected WindowSettings windowSettings;

    protected boolean configuredStockChartVisibility = false;

    protected boolean previousStockChartVisibilityHint;
    protected boolean previousStatusWindowVisibilityHint;
    protected boolean previousORWindowVisibilityHint;

    protected boolean previousResult;
    private boolean isJsonLoad = false;

    // Game Timer Management
    private Timer gameTimer = null;
    private Player currentPlayerOnTimer = null; // No longer strictly needed but fine to keep null for now
    private boolean isTimerPaused = false;
    private double currentFontScale = 1.0;

    private boolean isHistoryNavigation = false;

    // Player order
    // protected PlayerOrderView playerOrderView;
    /**
     * Player names set at time of initialisation or after reordering.
     * <p>
     * To be used as a reference to the current player order as shown in the UI.
     * Note, that getPlayers() currently calls the game engine directly, and
     * therefore updates before the UI gets notice via the playerOrderView.
     */
    protected List<String> currentGuiPlayerNames;

    /* Keys of dialogs owned by this class */
    public static final String COMPANY_START_PRICE_DIALOG = "CompanyStartPrice";
    public static final String SELECT_COMPANY_DIALOG = "SelectCompany";
    public static final String REPAY_LOANS_DIALOG = "RepayLoans";
    public static final String EXCHANGE_TOKENS_DIALOG = "ExchangeTokens";
    public static final String ADJUST_SHARE_PRICE_DIALOG = "AdjustSharePrice";

    private static final Logger log = LoggerFactory.getLogger(GameUIManager.class);

    private SplashWindow splashWindow = null;

    // LOCAL UI STOPWATCH
    private long turnStartTimestamp = System.currentTimeMillis();
    private long accumulatedPauseTime = 0;
    private long currentPauseStart = 0;
    private boolean isStopwatchPaused = false;

    public void resetUIStopwatch() {
        turnStartTimestamp = System.currentTimeMillis();
        accumulatedPauseTime = 0;
        currentPauseStart = 0;
        isStopwatchPaused = false;
    }

    /**
     * Calculates the exact ticking visual time for a player without mutating the engine's state.
     */
    public int getDisplayedTime(Player p) {
        if (p == null) return 0;
        int time = p.getTimeBankModel().value();
        
        if (railsRoot != null && railsRoot.getGameManager() != null) {
            time -= railsRoot.getGameManager().getPendingTimePenalty(p.getName());
        }
        
        // Only deduct active stopwatch time if this is the currently operating player
        if (p.equals(getCurrentPlayer()) && railsRoot.getGameManager().isTimeManagementEnabled()) {
            long now = isStopwatchPaused ? currentPauseStart : System.currentTimeMillis();
            long elapsedMs = now - turnStartTimestamp - accumulatedPauseTime;
            int elapsedSec = Math.max(0, (int) (elapsedMs / 1000));
            time -= elapsedSec;
        }
        
        return time;
    }

    public GameUIManager() {
        instance = this; 
    }

    public void init(RailsRoot root, boolean wasLoaded, SplashWindow splashWindow) {
        this.splashWindow = splashWindow;
        splashWindow.notifyOfStep(SplashWindow.STEP_INIT_UI);

        this.railsRoot = root;
        uiHints = railsRoot.getGameManager().getUIHints();

        // Force the StartRoundWindow to be recreated.
        // This ensures it links to the new GameManager's PossibleActions list.
        if (startRoundWindow != null) {
            startRoundWindow.dispose();
            startRoundWindow = null;
        }

        savePrefix = railsRoot.getGameName();
        gameWasLoaded = wasLoaded;

        initWindowSettings();
        initSaveSettings();
        initFontSettings();

        configuredStockChartVisibility = "yes".equalsIgnoreCase(Config.get("stockchart.window.open"));

        // playerOrderView = new PlayerOrderView();
        currentGuiPlayerNames = new ArrayList<>();
        for (Player player : getPlayers()) {
            currentGuiPlayerNames.add(player.getId());
        }

        localPlayerName = System.getProperty("local.player.name");
        if (!Util.hasValue(localPlayerName)) {
            localPlayerName = Config.get("local.player.name");
        }
        if (autoSaveLoadStatus > 0) {
            myTurn = getCurrentPlayer().getId().equals(localPlayerName);
        }

        OpenGamesManager.getInstance().addGame(this);
    }

    private void initWindowSettings() {
        windowSettings = new WindowSettings(railsRoot.getGameName());
        windowSettings.load();
    }

    public static boolean confirmQuit(JFrame parent) {
        if (StringUtils.isNotBlank(Config.get("skip_confirm_quit"))
                && Util.parseBoolean(Config.get("skip_confirm_quit"))) {
            // user has a confirm_quit preference and it's not set
            return true;
        }
        return JOptionPane.showConfirmDialog(parent, LocalText.getText("CLOSE_WINDOW"),
                LocalText.getText("Select"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    public void closeGame() {
        if (myTurn) {
            // TODO: confirm game close if in turn and polling?
        }
        OpenGamesManager.getInstance().removeGame(this);

        if (windowSettings != null) {
            log.info("GameUIManager: Closing game. Saving font.ui.scale '{}' to WindowSettings.",
                    this.currentFontScale);
            windowSettings.setProperty("font.ui.scale", String.valueOf(this.currentFontScale));
        }
        getWindowSettings().save();

        if (startRoundWindow != null) {
            startRoundWindow.close();
        }
        if (statusWindow != null) {
            statusWindow.dispose();
        }
        if (reportWindow != null) {
            reportWindow.dispose();
        }
        if (orWindow != null) {
            orWindow.dispose();
        }
        if (configWindow != null) {
            configWindow.dispose();
        }
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        if (autoLoadPoller != null) {
            autoLoadPoller.setActive(false);
            autoLoadPoller.close();
        }
        // TODO: terminate things like Discord

        // clean up config items that are game play specific (ie like Discord)
        ConfigManager.getInstance().clearTransientConfig();
    }

    public void terminate() {
        // Save Window Positions
        if (windowSettings != null) {
            windowSettings.setProperty("font.ui.scale", String.valueOf(this.currentFontScale));
        }
        getWindowSettings().save();

        // Save Font Scale
        Config.set("font.ui.scale", String.valueOf(this.currentFontScale));

        if (orWindow != null)
            orWindow.saveLayout();
        System.exit(0);
    }

    public WindowSettings getWindowSettings() {
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
        } else if (saveSuffixSpec.equals(CURRENT_ROUND_SUFFIX)) {
            if (currentRound != null) {
                saveSuffix = currentRound.getRoundName();
            } else {
                saveSuffix = "";
            }
        } else { // otherwise use specified suffix
            saveSuffix = saveSuffixSpec;
        }

        String str = Config.get("save.auto.enabled");
        autoSaveLoadStatus = "yes".equals(str) ? AutoLoadPoller.ON : AutoLoadPoller.OFF;
        str = Config.get("save.auto.interval");
        if (Util.hasValue(str)) {
            autoSaveLoadPollingInterval = Integer.parseInt(str);
        }
    }

    private void initFontSettings() {
        // font settings, can be game specific
        String fontType = Config.getGameSpecific(railsRoot.getGameName(), "font.ui.name");
        Font font = null;

        if (Util.hasValue(fontType)) {
            // --- RESTORED MISSING VARIABLE ---
            boolean boldStyle = true;
            String fontStyle = Config.getGameSpecific(railsRoot.getGameName(), "font.ui.style");
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
        }

        // Check WindowSettings first (preferred storage), then Config (fallback)
        String savedScale = null;
        if (windowSettings != null) {
            savedScale = windowSettings.getProperty("font.ui.scale");
        } else {
            log.warn("GameUIManager: WindowSettings is null during initFontSettings!");
        }

        if (!Util.hasValue(savedScale)) {
            savedScale = Config.get("font.ui.scale");
        }

        if (Util.hasValue(savedScale)) {
            try {
                this.currentFontScale = Double.parseDouble(savedScale);
            } catch (NumberFormatException e) {
                this.currentFontScale = GUIGlobals.getFontsScale();
            }
        } else {
            this.currentFontScale = GUIGlobals.getFontsScale();
        }

        changeGlobalFont(font, this.currentFontScale);
    }

    public StatusWindow getStatusWindow() {
        return statusWindow;
    }

    public void gameUIInit(boolean newGame) {
        splashWindow.notifyOfStep(SplashWindow.STEP_STOCK_CHART);
        stockChartWindow = new StockChartWindow(this);
        stockChartWindow.setName("StockChartWindow");
        stockChartWindow.setTitle("Rails Evolution - " + railsRoot.getGameName() + " - Stock Market");

        // Ensure the layout manager doesn't force a rectangular grid if we are in 1837
        if (getGameManager().getRoot().getStockMarket()
                .getStockChartType() == net.sf.rails.game.financial.StockMarket.ChartType.HEXAGONAL) {
            stockChartWindow.setResizable(true);
        }

        // Register Stock Window for Safe Storage ---
        registerWindowStorage(stockChartWindow);

        if (newGame) {
            List<Player> players = getPlayers();
            if (players == null || players.isEmpty()) {
                getGameManager().setTimeManagementEnabled(false);
            }
        }

        splashWindow.notifyOfStep(SplashWindow.STEP_REPORT_WINDOW);
        boolean staticReportWindow = Config.get("report.window.type").equalsIgnoreCase("static");
        reportWindow = new ReportWindow(this, staticReportWindow);
        reportWindow.setName("ReportWindow");

        // --- FIX: Register Report Window for Safe Storage ---
        registerWindowStorage(reportWindow);
        reportWindow.setTitle("Rails Evolution - " + railsRoot.getGameName() + " - Game Report");

        orWindow = new ORWindow(this, splashWindow);
        orWindow.setName("MapWindow");
        orWindow.setTitle("Rails Evolution - " + railsRoot.getGameName() + " - Map & Operations");
        orUIManager = orWindow.getORUIManager();

        // --- FIX: Register Map Window for Safe Storage ---
        registerWindowStorage(orWindow);

        splashWindow.notifyOfStep(SplashWindow.STEP_STATUS_WINDOW);
        String statusWindowClassName = getClassName(GuiDef.ClassName.STATUS_WINDOW);
        try {
            Class<? extends StatusWindow> statusWindowClass = Class.forName(statusWindowClassName)
                    .asSubclass(StatusWindow.class);
            statusWindow = statusWindowClass.newInstance();
            statusWindow.setName("StatusWindow");

            statusWindow.init(this);
            statusWindow.setTitle("Rails Evolution - " + railsRoot.getGameName() + " - Game Status");

            // Register Status Window for Safe Storage
            registerWindowStorage(statusWindow);

            // Hook into the window closing event to ensure all settings are saved to disk
            statusWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    saveWindowSettings(railsRoot.getGameName());
                }
            });

            initGameTimer();
            if (gameTimer != null) {
                gameTimer.start();
                startTimerForCurrentPlayer();
                if (gameTimer != null && !gameTimer.isRunning()) {
                    gameTimer.start();
                }
            }

        } catch (Exception e) {
            System.exit(1);
        }

        if (newGame) {
            splashWindow.notifyOfStep(SplashWindow.STEP_INIT_NEW_GAME);
            updateUI();
        }

        reportWindow.scrollDown();

        splashWindow.notifyOfStep(SplashWindow.STEP_CONFIG_WINDOW);
        configWindow = new ConfigWindow(statusWindow);
        configWindow.setName("ConfigWindow");

        configWindow.setTitle("Rails Evolution - " + railsRoot.getGameName() + " - Configuration");

        // --- FIX: Register Config Window for Safe Storage ---
        registerWindowStorage(configWindow);

        configWindow.init(true);

        splashWindow.notifyOfStep(SplashWindow.STEP_INIT_SOUND);

        try {
            SoundManager.notifyOfGameInit(railsRoot);
        } catch (Exception soundEx) {
        }
        new Discord(this, railsRoot);
        new Slack(this, railsRoot);
    }

    public void startLoadedGame() {
        gameUIInit(false); // false indicates reload

        splashWindow.notifyOfStep(SplashWindow.STEP_INIT_LOADED_GAME);
        if (isJsonLoad) {
            getRoot().getGameManager().getCurrentRound().setPossibleActions();

            updateUI(); // Now, updateUI() will find the new actions and enable the UI
        } else {
            processAction(new NullAction(getRoot(), NullAction.Mode.START_GAME));
        }

        initGameTimer();
        gameTimer.start(); // Start timer immediately upon load

        statusWindow.setGameActions();
    }

    public void adjustSharePrice(AdjustSharePrice action) {

        int optionsCount = action.getDirections().size();
        String[] options = new String[optionsCount + 1];
        options[0] = LocalText.getText("None");

        Iterator<AdjustSharePrice.Direction> iterator = action.getDirections().iterator();
        int i = 0;
        while (iterator.hasNext()) {
            options[++i] = LocalText.getText("AdjustDirection", iterator.next());
        }

        RadioButtonDialog dialog = new RadioButtonDialog(ADJUST_SHARE_PRICE_DIALOG,
                this, statusWindow,
                LocalText.getText("AdjustSharePrice", action.getCompanyName()),
                LocalText.getText("SelectPriceAdjustment", action.getCompanyName()),
                options, 0);
        setCurrentDialog(dialog, action);

    }

    protected boolean processOnServer(PossibleAction action) {
        boolean result;
        int currentActionCount = getGameManager().getCurrentActionCount(); // Get count *before* processing

        String actionSource = action.isAIAction() ? "AI" : "Human"; // Check the flag

        action.setActed();
        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer != null) {
            action.setPlayerName(currentPlayer.getId());
        } else {
            action.setPlayerName("System"); // Fallback for administrative actions
        }

        // Process the action on the server (this will increment the counter inside
        // GameManager)
        result = railsRoot.getGameManager().process(action);

        // Follow-up the result

        return result;
    }

    public boolean displayServerMessage() {
        String[] message = getDisplayBuffer().get();
        if (message != null) {

            String combinedMessage = Util.join(message, " ");

            // Suppress Modals ---
            if (combinedMessage.contains("Bank is broken") ||
                    combinedMessage.contains("Correction activated") ||
                    combinedMessage.toLowerCase().contains("must raise") ||
                    combinedMessage.toLowerCase().contains("bankrupt")) {

                log.info("SUPPRESSED UI MESSAGE: {}", combinedMessage);
                return false;
            }

            setCurrentDialog(new MessageDialog(null, this,
                    (JFrame) activeWindow,
                    LocalText.getText("Message"),
                    "<html>" + Util.join(message, "<br>")),
                    null);
            return true;
        }
        return false;
    }

    public void updateUI() {

        int actionCount = railsRoot.getGameManager().getPossibleActions().getList().size();

        if (actionCount == 0) {
            System.err.println("!!! CRITICAL: UI RECEIVED EMPTY ACTION LIST !!!");
            // Trace who triggered this update if needed
            // Thread.dumpStack();
        }

        currentRound = railsRoot.getGameManager().getCurrentRound();
        if (currentRound != null) {
            currentRoundName = currentRound.toString();
            currentRoundType = currentRound.getClass();
        } else {
            currentRoundName = "Game Start";
            currentRoundType = null;
        }

        // Derive previous types
        if (previousRound != null) {
            previousRoundType = previousRound.getClass();
            previousRoundName = previousRound.toString();
        } else {
            previousRoundType = null;
            previousRoundName = "";
        }

        // 2. Handle specific Transition Events
        if (previousRoundType != currentRoundType) {
            if (previousRoundType != null) {
                setCurrentDialog(null, null);

                if (StockRound.class.isAssignableFrom(previousRoundType)) {
                    statusWindow.finishRound();
                } else if (StartRound.class.isAssignableFrom(previousRoundType)) {
                    if (startRoundWindow != null) {
                        startRoundWindow.close();
                        startRoundWindow = null;
                    }
                }
            }
        }

        if (currentRound != previousRound && currentRoundType != null) {
            if (StartRound.class.isAssignableFrom(currentRoundType)) {
                startRound = (StartRound) currentRound;
                if (startRoundWindow == null) {
                    String startRoundWindowClassName = getClassName(GuiDef.ClassName.START_ROUND_WINDOW);
                    try {
                        Class<? extends StartRoundWindow> startRoundWindowClass = Class
                                .forName(startRoundWindowClassName).asSubclass(StartRoundWindow.class);
                        startRoundWindow = startRoundWindowClass.newInstance();
                        startRoundWindow.init(startRound, this, orUIManager);
                        startRoundWindow.setTitle("Rails Evolution - " + railsRoot.getGameName() + " - Start Round");
                    } catch (Exception e) {
                        log.error("GUIM: Failed to init StartRoundWindow", e);
                        System.exit(1);
                    }
                }
            } else if (StockRound.class.isAssignableFrom(currentRoundType)) {
                statusWindow.getGameStatus().initGameSpecificActions();
                if (orUIManager != null)
                    orUIManager.finish();
            } else if (OperatingRound.class.isAssignableFrom(currentRoundType)) {
                orUIManager.initOR((OperatingRound) currentRound);
            } else if (SwitchableUIRound.class.isAssignableFrom(currentRoundType)) {
                statusWindow.pack();
            }
        }

        /* Process visible round type changes */
        for (GuiHints.VisibilityHint hint : uiHints.getVisibilityHints()) {
            switch (hint.getType()) {
                case STOCK_MARKET:
                    boolean stockChartVisibilityHint = hint.isVisible() || configuredStockChartVisibility;
                    if (stockChartVisibilityHint != previousStockChartVisibilityHint) {
                        stockChartWindow.setVisible(stockChartVisibilityHint);
                        previousStockChartVisibilityHint = stockChartVisibilityHint;
                    }
                    break;
                case STATUS:
                    boolean statusWindowVisibilityHint = hint.isVisible();
                    if (statusWindowVisibilityHint != previousStatusWindowVisibilityHint) {
                        setMeVisible(statusWindow, statusWindowVisibilityHint);
                        previousStatusWindowVisibilityHint = statusWindowVisibilityHint;
                    }
                    break;
                case MAP:
                    boolean orWindowVisibilityHint = hint.isVisible();
                    if (orWindowVisibilityHint != previousORWindowVisibilityHint) {
                        setMeVisible(orWindow, orWindowVisibilityHint);
                        previousORWindowVisibilityHint = orWindowVisibilityHint;
                    }
                    break;
                case START_ROUND:
                    break;
            }
        }

        // Report hint logic...
        Object reportHint = getGameManager().getGuiParameter(GuiDef.Parm.SHOW_GAME_END_REPORT);
        boolean showReport = (reportHint instanceof Boolean) ? (Boolean) reportHint : false;
        if (showReport) {
            getGameManager().setGuiParameter(GuiDef.Parm.SHOW_GAME_END_REPORT, false);
            showPlayerWorthChart();
        }

        // WRAP IN TRY/CATCH to see if the Refactor broke this
        boolean correctionOverride = false;
        try {
            correctionOverride = statusWindow.setupFor(currentRound);
        } catch (Exception e) {
            log.error("GUIM: CRASH in statusWindow.setupFor()", e);
        }

        boolean isMapRenderable = (currentRound instanceof net.sf.rails.game.round.I_MapRenderableRound);

        // ... (Active Window Selection Logic) ...
        if ((uiHints.getActivePanel() == GuiDef.Panel.MAP || isMapRenderable) && !correctionOverride) {
            activeWindow = orWindow;
            setMeVisible(orWindow, true);
            setMeToFront(orWindow);
        } else if (uiHints.getActivePanel() == GuiDef.Panel.START_ROUND) {
            activeWindow = startRoundWindow;
            setMeVisible(startRoundWindow, true);
            setMeToFront(startRoundWindow);
        } else if (uiHints.getActivePanel() == GuiDef.Panel.STATUS || correctionOverride) {
            activeWindow = statusWindow;
            stockChartWindow.setVisible(true);
            setMeVisible(statusWindow, true);
            setMeToFront(statusWindow);
        } else if (uiHints.getActivePanel() == GuiDef.Panel.MAP && !correctionOverride) {
            activeWindow = orWindow;
            setMeVisible(orWindow, true);
            setMeToFront(orWindow);
        }

        if (startRoundWindow != null) {
            try {
                startRoundWindow.updateStatus(myTurn);
            } catch (Exception e) {
            }
        } else {
        }

        if (statusWindow != null) {
            try {
                statusWindow.updateStatus(myTurn);
            } catch (Exception e) {
                log.error("Recovered from StatusWindow crash during updateUI.", e);
            }
        }

        if (orUIManager != null) {
            try {
                orUIManager.updateStatus(myTurn);
            } catch (Exception e) {
                log.error("Recovered from ORUIManager crash.", e);
            }
        }

        if (StartRoundWindow.class.isAssignableFrom(activeWindow.getClass())) {
            startRoundWindow.setSRPlayerTurn();
        }

        updateStatus(activeWindow);
        updateActivityPanel();

        // Focus restoration...
        if (activeWindow == startRoundWindow && startRoundWindow != null) {
            SwingUtilities.invokeLater(() -> {
                if (startRoundWindow != null && startRoundWindow.isVisible()) {
                    startRoundWindow.toFront();
                    startRoundWindow.requestFocus();
                }
            });
        }

        previousRound = currentRound;
        if (previousRound != null) {
            previousRoundType = previousRound.getClass();
            previousRoundName = previousRound.toString();
        }

    }

    /**
     * Creates the "Game End Report" menu item.
     * Displays the Final Ranking text directly in a simple dialog.
     */
    public JMenuItem getMenuGameEndReport() {
        // Use "Game End Report" for the menu label
        JMenuItem reportItem = new JMenuItem(LocalText.getText("GameEndReportTitle", "Game End Report"));

        reportItem.addActionListener(e -> {
            // 1. Get the raw text report from GameManager
            java.util.List<String> reportLines = getGameManager().getGameReport();

            // 2. Format it as HTML for the dialog to look nice
            StringBuilder html = new StringBuilder("<html><body style='font-family: sans-serif; font-size: 12pt;'>");

            for (String line : reportLines) {
                // Bold the header lines
                if (line.contains("Winner") || line.contains("Final ranking")) {
                    html.append("<b>").append(line).append("</b><br>");
                } else if (line.trim().isEmpty()) {
                    html.append("<br>");
                } else {
                    html.append(line).append("<br>");
                }
            }
            html.append("</body></html>");

            // 3. Determine the Title
            String title = LocalText.getText("EoGFinalRanking", "Final ranking").replace(":", "").trim();

            // 4. Show the "Little Window" (JOptionPane) directly
            // This bypasses the crash because it doesn't use reflection or WorthChartWindow
            JOptionPane.showMessageDialog(
                    statusWindow != null ? statusWindow : orWindow, // Parent window
                    html.toString(), // Content
                    title, // Title
                    JOptionPane.PLAIN_MESSAGE // Clean style (no icon)
            );
        });
        return reportItem;
    }

    /**
     * Checks if the Start Round Window is currently active and should hold
     * exclusive focus.
     * Used by other windows (Map, Status) to prevent focus stealing during
     * auctions.
     */
    public boolean isStartRoundActive() {
        return (startRoundWindow != null && startRoundWindow.isVisible() &&
                activeWindow == startRoundWindow);
    }

    protected void updatePlayerOrder(List<String> newPlayerOrder) {
        if (startRoundWindow != null)
            startRoundWindow.updatePlayerOrder(newPlayerOrder);
        if (statusWindow != null)
            statusWindow.updatePlayerOrder(newPlayerOrder);
        currentGuiPlayerNames = newPlayerOrder;
    }

    public void uncheckMenuItemBox(String itemName) {
        statusWindow.uncheckMenuItemBox(itemName);
    }

    /**
     * Stub, to be overridden in subclasses for special round types
     */
    protected void updateStatus(ActionPerformer activeWindow) {
    }

    // public void discardTrains(DiscardTrain dt) {
    // PublicCompany c = dt.getCompany();
    // String playerName = dt.getPlayerName();
    // String companyDirector = dt.getCompany().getPresident().getId();
    // Set<Train> trains = dt.getOwnedTrains();
    // int size = trains.size() + (dt.isForced() ? 0 : 1);
    // List<String> trainOptions = new ArrayList<>(size);
    // String[] options = new String[size];
    // String prompt = null;

    // int j = 0;
    // if (!dt.isForced()) {
    // trainOptions.add(
    // options[j++] = LocalText.getText("None"));
    // prompt = LocalText.getText("MayDiscardTrain", c.getId());
    // }
    // int offset = j;
    // for (int i = 0; i < trains.size(); i++) {
    // trainOptions.add(
    // options[j++] = LocalText.getText("N_Train",
    // Iterables.get(trains, i).toText()));
    // }
    // // Martin Brumm: 18.7.2017
    // // Need to Check that the player informed here is the director
    // // Underlying problem is that the director might not be the operating player
    // in
    // // the
    // // moment and theres no quick way to change that behaviour..
    // // Only Chance would be to introduce a new Discard Train Round with complete
    // // separate
    // // Mechanics

    // if (prompt == null) {
    // if (playerName.equals(companyDirector)) {
    // prompt = LocalText.getText(
    // "HAS_TOO_MANY_TRAINS",
    // playerName,
    // c.getId());
    // } else {
    // prompt = LocalText.getText(
    // "HAS_TOO_MANY_TRAINS",
    // playerName,
    // c.getId());
    // prompt += "\n Please contact the director of the " + c.getId() + " : " +
    // companyDirector
    // + " for guidance.";
    // }
    // }

    // String discardedTrainName = (String) JOptionPane.showInputDialog(orWindow,
    // prompt,
    // LocalText.getText("WhichTrainToDiscard"),
    // JOptionPane.QUESTION_MESSAGE, null,
    // options, options[0]);
    // if (discardedTrainName != null) {
    // int index = trainOptions.indexOf(discardedTrainName);
    // // FIXME: Does this work with the new Set defined?
    // if (index >= offset) {
    // Train discardedTrain = Iterables.get(trains,
    // trainOptions.indexOf(discardedTrainName) - offset);
    // dt.setDiscardedTrain(discardedTrain);
    // }

    // orWindow.process(dt);
    // }
    // }

    public void exchangeTokens(ExchangeTokens action) {

        int cityNumber;
        String prompt, cityName, hexName, oldCompName;
        String[] ct;
        MapHex hex;
        List<String> options = new ArrayList<>();
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
            setCurrentDialog(dialog, action);

        }
    }

    @Override
    public void dialogActionPerformed() {
        dialogActionPerformed(false);
    }

    public void dialogActionPerformed(boolean ready) {
        if (!ready) {

            String key = "";
            if (currentDialog instanceof NonModalDialog)
                key = ((NonModalDialog) currentDialog).getKey();

            if (currentDialog instanceof AutoSaveLoadDialog) {
                // Not yet a NonModalDialog subclass
                startAutoSaveLoadPoller((AutoSaveLoadDialog) currentDialog);

            } else if (!(currentDialog instanceof NonModalDialog)) {
                currentDialogAction = null;

            } else if (currentDialog instanceof MessageDialog) {
                // Nothing to do.
                currentDialogAction = null;
                // This cancels the currently incomplete user action.
                // WARNING: always do this if dialog processing terminates in a context
                // where an action is aborted and the UI must return to its previous state.
                // This will normally be the case after a CANCEL (but not after a NO).

            } else if (COMPANY_START_PRICE_DIALOG.equals(key)) {

                // RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                StartCompany action = (StartCompany) currentDialogAction;
                StartPriceGridDialog dialog = (StartPriceGridDialog) currentDialog;

                int index = dialog.getSelectedOption();
                if (index >= 0) {
                    int price = action.getStartPrices()[index];
                    action.setStartPrice(price);
                    if (action.getNumberBought() == 0) {
                        // Set bought amount only if it has not been preset
                        // (used in SOH to start a company from phase 3)
                        action.setNumberBought(action.getSharesPerCertificate());
                    }
                } else {
                    // No selection done - no action
                    currentDialogAction = null;
                }

            } else if (EXCHANGE_TOKENS_DIALOG.equals(key)) {

                CheckBoxDialog dialog = (CheckBoxDialog) currentDialog;
                ExchangeTokens action = (ExchangeTokens) currentDialogAction;
                boolean[] exchanged = dialog.getSelectedOptions();
                String[] options = dialog.getOptions();

                int numberSelected = (int) IntStream.range(0, options.length).filter(index -> exchanged[index]).count();

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
                    exchangeTokens(action);
                    return;

                }
                for (int index = 0; index < options.length; index++) {
                    if (exchanged[index]) {
                        action.getTokensToExchange().get(index).setSelected(true);
                    }
                }
            } else if (REPAY_LOANS_DIALOG.equals(key)) {

                RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                RepayLoans action = (RepayLoans) currentDialogAction;
                int selected = dialog.getSelectedOption();
                action.setNumberTaken(action.getMinNumber() + selected);

            } else if (ADJUST_SHARE_PRICE_DIALOG.equals(key)) {
                // RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
                StartPriceGridDialog dialog = (StartPriceGridDialog) currentDialog;
                AdjustSharePrice action = (AdjustSharePrice) currentDialogAction;
                EnumSet<AdjustSharePrice.Direction> directions = action.getDirections();
                int selected = dialog.getSelectedOption();
                if (selected > 0) {
                    action.setChosenDirection(
                            (AdjustSharePrice.Direction) directions.toArray()[selected - 1]);
                }

            } else {

                currentDialogAction = null;
            }
        }

        processAction(currentDialogAction);

    }

    protected void autoSave(String newPlayer) {
        lastSavedFilename = savePrefix + "_" + saveDateTimeFormat.format(new Date()) + "_" + newPlayer + "."
                + saveExtension;
        GameAction saveAction = new GameAction(getRoot(), GameAction.Mode.SAVE);
        saveAction.setFilepath(saveDirectory + "/" + lastSavedFilename);
        processOnServer(saveAction);

        saveAutoSavedFilename(lastSavedFilename);
    }

    protected boolean saveAutoSavedFilename(String lastSavedFilename) {
        String lastSavedFilenameFilepath = saveDirectory + "/" + savePrefix + "."
                + GameUIManager.DEFAULT_SAVE_POLLING_EXTENSION;
        try {
            File f = new File(lastSavedFilenameFilepath);
            PrintWriter out = new PrintWriter(new FileWriter(f));
            out.println(lastSavedFilename);
            out.close();
            return true;
        } catch (IOException e) {

            return false;
        }
    }

    protected boolean pollingIsOn() {
        return autoLoadPoller != null && autoLoadPoller.getStatus() == AutoLoadPoller.ON;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    /**
     * Stub, can be overridden by subclasses
     */
    protected boolean checkGameSpecificDialogAction() {
        return false;
    }

    @Override
    public JDialog getCurrentDialog() {
        return currentDialog;
    }

    @Override
    public PossibleAction getCurrentDialogAction() {
        return currentDialogAction;
    }

    @Override
    public void setCurrentDialog(JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = dialog;
        currentDialogAction = action;
    }

    /**
     * Change global font size
     *
     * @param scale
     */
    public void changeGlobalFont(Font replaceFont, double scale) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font) {
                UIManager.put(key, null);
                Font font;
                if (replaceFont != null) {
                    font = replaceFont;
                } else {
                    font = UIManager.getFont(key);
                }
                if (font != null) {
                    float newSize = font.getSize2D() * (float) scale;
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
                    + saveDateTimeFormat.format(new Date()) + "_"
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
            filename = saveDirectory + "/" + savePrefix + "_" + saveDateTimeFormat.format(new Date()) + "_" +
                    currentSuffix + "." + saveExtension;
        }

        File proposedFile = new File(filename);
        jfc.setSelectedFile(proposedFile);

        // allows adjustment of the save dialog title, to add hint about copy to
        // clipboard
        jfc.setDialogTitle(LocalText.getText("SaveDialogTitle"));

        if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String filepath = selectedFile.getPath();

            String extension = "." + saveExtension;
            if (!filepath.toLowerCase().endsWith(extension)) {
                filepath += extension;
            }

            saveDirectory = selectedFile.getParent();
            if (!selectedFile.getName().equalsIgnoreCase(proposedFile.getName())) {
                // User has not accepted the default name but entered a different one.
                // Check the new name. If only the prefix has changed, only remember that part.
                String[] proposedParts = proposedFile.getName().split("_", 2);
                String[] selectedParts = selectedFile.getName().split("_", 2);
                if (proposedParts.length >= 2 && selectedParts.length >= 2 &&
                        !proposedParts[0].equals(selectedParts[0]) &&
                        proposedParts[1].equals(selectedParts[1])) {
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

    public void autoSaveLoadGame() {
        AutoSaveLoadDialog dialog = new AutoSaveLoadDialog(this,
                autoSaveLoadStatus,
                autoSaveLoadPollingInterval);
        setCurrentDialog(dialog, null);
    }

    public void startAutoSaveLoadPoller(AutoSaveLoadDialog dialog) {
        autoSaveLoadStatus = dialog.getStatus();
        autoSaveLoadPollingInterval = dialog.getInterval();
        startAutoSaveLoadPoller();
    }

    public void startAutoSaveLoadPoller() {
        if (!Util.hasValue(localPlayerName)) {
            // FIXME (Rails2.0) Replace this with something better (DisplayBuffer is not
            // available so far
            // DisplayBuffer.add(this, "You cannot activate AutoSave/Load without setting
            // local.player.name");
            return;
        }

        if (autoSaveLoadStatus != AutoLoadPoller.OFF) {
            if (!gameWasLoaded) {
                /*
                 * The first time (only) we use the normal save process,
                 * so the player can select a directory, and change
                 * the prefix if so desired.
                 */
                GameAction saveAction = new GameAction(getRoot(), GameAction.Mode.SAVE);
                saveSuffix = localPlayerName;
                saveGame(saveAction);
                lastSavedFilename = saveAction.getFilepath();
            }
            if (lastSavedFilename != null) {
                /* Now also save the "last saved file" file */
                autoSaveLoadInitialized = saveAutoSavedFilename(lastSavedFilename);
            }
        }

        if (autoLoadPoller == null && autoSaveLoadStatus > 0) {
            autoLoadPoller = new AutoLoadPoller(this, saveDirectory, savePrefix, lastSavedFilename,
                    localPlayerName, autoSaveLoadStatus, autoSaveLoadPollingInterval);
            autoLoadPoller.start();
        } else if (autoLoadPoller != null) {
            autoLoadPoller.setStatus(autoSaveLoadStatus);
            autoLoadPoller.setPollingInterval(autoSaveLoadPollingInterval);
        }

        if (autoLoadPoller != null) {
            myTurn = getCurrentPlayer().getId().equals(localPlayerName);
            if (!myTurn) {
                // Start autoload polling
                autoLoadPoller.setActive(autoSaveLoadStatus == AutoLoadPoller.ON);
            }

        }
    }

    public void saveGameStatus() {
        List<String> status = statusWindow.getGameStatus().getTextContents();

        JFileChooser jfc = new JFileChooser();
        String filename = saveDirectory + "/" + savePrefix + "_"
                + saveDateTimeFormat.format(new Date()) + ".status";

        File proposedFile = new File(filename);
        jfc.setSelectedFile(proposedFile);

        if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            try {
                PrintWriter pw = new PrintWriter(selectedFile);

                for (String line : status)
                    pw.println(line);

                pw.close();

            } catch (IOException e) {
                getDisplayBuffer().add(LocalText.getText("SaveFailed", e.getMessage()));
            }
        }
    }

    public void setGameFile(File gameFile) {
        saveDirectory = gameFile.getParent();
        lastSavedFilename = gameFile.getName();
    }

    public void setSaveDirectory(String saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    /**
     * Swaps the active RailsRoot and forces all visible UI components to refresh
     * their data references. This enables "Visual Rehydration".
     */
    public void updateAllVisuals(RailsRoot historicalRoot) {
        this.railsRoot = historicalRoot;

        // 1. Refresh the Status Window
        if (statusWindow != null) {
            statusWindow.updateStatus(true);
        }

        // 2. Refresh the Map (Search for MapPanel in the ORWindow)
        if (orWindow != null) {
            // We use a generic approach to find the MapPanel since we are the manager
            MapPanel panel = findMapPanel(orWindow);
            if (panel != null)
                panel.updateData();
        }

        // 3. Refresh the Stock Market
        if (stockChartWindow != null && stockChartWindow.isVisible()) {
            stockChartWindow.repaint();
        }

        // 4. Update the "Thinking" alert in the Activity Panel
        updateActivityPanel();
    }

    private MapPanel findMapPanel(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof MapPanel)
                return (MapPanel) c;
            if (c instanceof Container) {
                MapPanel found = findMapPanel((Container) c);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    public PossibleAction getLastAction() {
        return lastAction;
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

    public PlayerManager getPlayerManager() {
        return railsRoot.getPlayerManager();
    }

    public List<Player> getPlayers() {
        return railsRoot.getPlayerManager().getPlayers();
    }

    public Player getPriorityPlayer() {
        return railsRoot.getPlayerManager().getPriorityPlayer();
    }

    public Phase getCurrentPhase() {
        return railsRoot.getPhaseManager().getCurrentPhase();
    }

    public List<PublicCompany> getAllPublicCompanies() {
        return railsRoot.getCompanyManager().getAllPublicCompanies();
    }

    public String getClassName(GuiDef.ClassName key) {
        return railsRoot.getGameManager().getClassName(key);
    }

    public Object getGameParameter(GuiDef.Parm key) {
        return railsRoot.getGameManager().getGuiParameter(key);
    }

    public boolean getGameParameterAsBoolean(GuiDef.Parm key) {
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
        // setEnabledWindow(enabled, stockChart, exceptionWindow);
        setEnabledWindow(enabled, reportWindow, exceptionWindow);
        setEnabledWindow(enabled, configWindow, exceptionWindow);
        setEnabledWindow(enabled, orWindow, exceptionWindow);
        setEnabledWindow(enabled, startRoundWindow, exceptionWindow);
        setEnabledWindow(enabled, statusWindow, exceptionWindow);
    }

    private void updateWindowsLookAndFeel() {
        // --- gameStatus.initTurn(getCurrentPlayer().getIndex(), true); ---
        // Call our non-flickering resizer for each window
        if (statusWindow != null) {
            SwingUtilities.updateComponentTreeUI(statusWindow);
            packAndApplySizing(statusWindow);
        }
        if (orWindow != null) {
            SwingUtilities.updateComponentTreeUI(orWindow);
            packAndApplySizing(orWindow);
        }
        if (reportWindow != null) {
            SwingUtilities.updateComponentTreeUI(reportWindow);
            packAndApplySizing(reportWindow);
        }
        if (configWindow != null) {
            SwingUtilities.updateComponentTreeUI(configWindow);
            packAndApplySizing(configWindow);
        }
        if (stockChartWindow != null) {
            SwingUtilities.updateComponentTreeUI(stockChartWindow);
            packAndApplySizing(stockChartWindow); // <--- ADD THIS LINE
        }
    }

    /**
     * Called by the StatusWindow menu to adjust the global font size.
     */
    public void adjustGlobalFontScale(double delta) {
        // Set bounds, e.g., 0.8x to 2.0x
        double newScale = this.currentFontScale + delta;
        if (newScale < 0.8)
            newScale = 0.8;
        if (newScale > 2.0)
            newScale = 2.0;

        if (this.currentFontScale == newScale)
            return; // No change

        this.currentFontScale = newScale;

        // Re-get the base font from config
        String fontType = Config.getGameSpecific(railsRoot.getGameName(), "font.ui.name");
        Font font = null;
        if (Util.hasValue(fontType)) {
            boolean boldStyle = true;
            String fontStyle = Config.getGameSpecific(railsRoot.getGameName(), "font.ui.style");
            if (Util.hasValue(fontStyle) && fontStyle.equalsIgnoreCase("plain")) {
                boldStyle = false;
            }
            if (boldStyle) {
                font = new Font(fontType, Font.BOLD, 12);
            } else {
                font = new Font(fontType, Font.PLAIN, 12);
            }
        }

        // Apply the new font globally
        changeGlobalFont(font, this.currentFontScale);

        if (orUIManager != null) {
            orUIManager.updateScale();
        }

        // Refresh all windows to apply the new layout
        updateWindowsLookAndFeel();
    }

    public double getFontScale() {
        return this.currentFontScale;
    }

    // Forwards the format() method to the server
    // EV: Not really. The client also knows about the Bank
    // and all static configuration details. All the complexities
    // built around the money format, including the Currency class,
    // look completely redundant to me.
    public String format(int amount) {
        String formatted = Bank.format(railsRoot, amount);

        // Strip currency symbols if configured centrally to declutter the UI
        if ("yes".equalsIgnoreCase(Config.get("ui.hide_currency_symbols"))) {
            return formatted.replaceAll("[^\\d\\.,-]", "").trim();
        }
        return formatted;
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
            splashWindow.registerFrameForDeferredVisibility(frame, setToVisible);
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

        if (autoSaveLoadStatus > 0) {
            SwingUtilities.invokeLater(this::startAutoSaveLoadPoller);
        }
    }

    /**
     * Packs specified frame and tries to apply user defined size afterwards.
     * These actions are performed within the EDT.
     */
    public void packAndApplySizing(JFrame frame) {
        final JFrame finalFrame = frame;
        SwingUtilities.invokeLater(new Thread(() -> {

            WindowSettings ws = getWindowSettings();
            Rectangle bounds = ws.getBounds(finalFrame);
            boolean hasSavedSize = (bounds.width != -1 && bounds.height != -1);

            // SANITY CHECK: Ignore "Tiny" saved windows (e.g. 10x10 bug)
            if (hasSavedSize && (bounds.width < 200 || bounds.height < 100)) {
                log.warn("Ignoring invalid saved size for {}: {}", finalFrame.getName(), bounds);
                hasSavedSize = false;
            }

            if (hasSavedSize) {
                // --- A. USER'S SAVED SIZE EXISTS & IS VALID ---
                if (bounds.x != -1 && bounds.y != -1) {
                    finalFrame.setLocation(bounds.getLocation());
                }
                finalFrame.setSize(bounds.getSize());
            } else {
                // --- B. NO SAVED SIZE (FIRST RUN) ---
                finalFrame.pack();

                // Ensure we respect the component's minimum size if pack() shrinks it too much
                Dimension minSize = finalFrame.getMinimumSize();
                if (finalFrame.getWidth() < minSize.width || finalFrame.getHeight() < minSize.height) {
                    finalFrame.setSize(
                            Math.max(finalFrame.getWidth(), minSize.width),
                            Math.max(finalFrame.getHeight(), minSize.height));
                }

                // StockChartWindow specific fix
                if (finalFrame instanceof StockChartWindow) {
                    Dimension defaultSize = finalFrame.getPreferredSize();
                    if (defaultSize.width > 0 && defaultSize.height > 0) {
                        finalFrame.setSize(defaultSize);
                    }
                }
            }
        }));
    }

    public List<String> getCurrentGuiPlayerNames() {
        return currentGuiPlayerNames;
    }

    public void saveLogs() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        CyclicBufferAppender<?> buffer = (CyclicBufferAppender<?>) lc.getLogger(Logger.ROOT_LOGGER_NAME)
                .getAppender("buffer");
        if (buffer == null) {
            return;
        }

        PatternLayout layout = new PatternLayout();
        layout.setContext(lc);
        layout.setPattern("%d{MM-dd-yyyy:HH:mm:ss.SSS} [%thread] %-5level %logger{10}->%method\\(\\):%line - %msg%n");
        layout.start();

        try {
            PrintWriter writer = new PrintWriter(saveDirectory + "/" +
                    StringUtils.replace(lastSavedFilename, ".rails", "_" + localPlayerName + ".log"));
            int count = buffer.getLength();
            LoggingEvent le;
            for (int i = 0; i < count; i++) {
                le = (LoggingEvent) buffer.get(i);
                writer.print(layout.doLayout(le));
            }
            writer.close();
        } catch (FileNotFoundException e) {
        }
    }

    public void saveReportFile() {

        List<String> report = getRoot().getReportManager().getReportBuffer().getAsList();

        JFileChooser jfc = new JFileChooser();
        String filename = saveDirectory + "/" + savePrefix + "_"
                + saveDateTimeFormat.format(new Date()) + ".report";

        File proposedFile = new File(filename);
        jfc.setSelectedFile(proposedFile);

        if (jfc.showSaveDialog(statusWindow) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            try {
                PrintWriter pw = new PrintWriter(selectedFile);

                for (String line : report)
                    pw.println(line);

                pw.close();

            } catch (IOException e) {
                getDisplayBuffer().add(LocalText.getText("SaveFailed", e.getMessage()));
            }
        }

    }

    public class PlayerOrderView implements Observer {
        PlayerOrderView() {
            railsRoot.getPlayerManager().getPlayerOrderModel().addObserver(this);
        }

        public void update(Observable o, Object arg) {
            List<String> newPlayerNames = Arrays.asList(((String) arg).split(";"));
            updatePlayerOrder(newPlayerNames);
        }

        public void deRegister() {
        }

        @Override
        public void update(String text) {
        }

        @Override
        public net.sf.rails.game.state.Observable getObservable() {
            return null;
        }
    }

    public JMenuItem getMenuCompanyPayoutChart() {
        // Use LocalText with a default, similar to other menu items
        JMenuItem chartItem = new JMenuItem(LocalText.getText("CompanyPayoutChartTitle", "Company Payout Chart"));
        chartItem.addActionListener(e -> {
            showCompanyPayoutChart();
        });
        return chartItem;
    }

    public void showCompanyPayoutChart() {
        if (statusWindow != null) {
            getGameManager().displayCompanyPayoutChart(statusWindow);
        } else if (orWindow != null) {
            getGameManager().displayCompanyPayoutChart(orWindow);
        }
    }

    public JMenuItem getMenuMultiplierChart() {
        JMenuItem item = new JMenuItem(LocalText.getText("MultiplierChart", "Investment Multiplier"));
        item.addActionListener(e -> showMultiplierChart());
        return item;
    }

    public void showMultiplierChart() {
        if (statusWindow != null) {
            getGameManager().displayMultiplierChart(statusWindow);
        } else if (orWindow != null) {
            getGameManager().displayMultiplierChart(orWindow);
        }
    }

    public StartRoundWindow getStartRoundWindow() {
        return startRoundWindow;
    }

    /**
     * Initializes the master javax.swing.Timer that fires every second
     * to update the current operating player's time bank during ORs.
     */
    private void initGameTimer() {
        if (gameTimer == null) {
            gameTimer = new Timer(1000, new ActionListener() { // 1000 ms = 1 second
                @Override
                public void actionPerformed(ActionEvent e) {
                    // --- Pre-checks ---
                    // Added check for railsRoot.getGameManager().isGamePaused()
                    if (isTimerPaused || railsRoot.getGameManager().isGameOver()
                            || !railsRoot.getGameManager().isTimeManagementEnabled()
                            || railsRoot.getGameManager().isGamePaused()) {

                        return;
                    }

                    RoundFacade currentRound = railsRoot.getGameManager().getCurrentRound();
                    Player playerWhoseTimeTicks = null;
                    int playerIndex = -1;

                    // --- Determine Player based on Round Type --- // *** MODIFIED BLOCK ***
                    if (currentRound instanceof OperatingRound) {
                        OperatingRound or = (OperatingRound) currentRound;
                        PublicCompany operatingCompany = or.getOperatingCompany();
                        if (operatingCompany != null) {
                            playerWhoseTimeTicks = operatingCompany.getPresident(); // Time for company president
                        } else {
                            return; // No tick between OR turns
                        }
                    } else if (currentRound instanceof StockRound || currentRound instanceof StartRound
                            || (currentRound != null && currentRound.getCurrentPlayer() != null)) {
                        // In SR or IR, or any round reporting a current player, time ticks for that
                        // player
                        playerWhoseTimeTicks = railsRoot.getGameManager().getCurrentPlayer();
                    } else {
                        return; // No tick for other round types for now
                    }

                    // --- Get Player Index ---
                    if (playerWhoseTimeTicks != null) {
                        playerIndex = playerWhoseTimeTicks.getIndex();
                        if (playerIndex == -1) {

                            playerWhoseTimeTicks = null; // Invalidate player if index is bad
                        }
                    } else {
                        return; // No player determined, no tick
                    }

                    // --- Process Time Tick ---
                    if (playerWhoseTimeTicks != null) { // Check again after index validation
                        // Fetch the synchronized visual ticking time
                        int newTime = getDisplayedTime(playerWhoseTimeTicks);

                        // CRITICAL: Do NOT mutate timeBank.set() here. Engine handles the payload.

                        final int finalPlayerIndex = playerIndex;
                        final int finalNewTime = newTime;
                        final Player finalPlayer = playerWhoseTimeTicks;

                        SwingUtilities.invokeLater(() -> {
                            if (statusWindow != null && statusWindow.getGameStatus() != null) {
                                statusWindow.getGameStatus().updatePlayerTime(finalPlayerIndex, finalNewTime);
                            }
                        });

                        // --- Apply Consequence if time ran out ---
                        if (newTime < 0) {
                            if (railsRoot.getGameManager()
                                    .getTimeConsequence() == TimeConsequence.SUBTRACT_FINAL_SCORE) {
                                finalPlayer.addTimePenalty(1);
                            } else if (railsRoot.getGameManager()
                                    .getTimeConsequence() == TimeConsequence.SUBTRACT_IMMEDIATE_CASH) {

                            }
                        }
                    }

                    // No 'else' needed here, already handled by return statements above
                } // End actionPerformed
            }); // End ActionListener
            gameTimer.setInitialDelay(1000);
            gameTimer.setRepeats(true);
        } else {
        }
    } // End initGameTimer

    public void fitMapToWidth() {
        if (orWindow != null && orWindow.getMapPanel() != null) {
            orWindow.getMapPanel().fitToWidth();
        }
    }

    public void fitMapToHeight() {
        if (orWindow != null && orWindow.getMapPanel() != null) {
            orWindow.getMapPanel().fitToHeight();
        }
    }

    /**
     * Starts the game timer for the current player if time management is enabled.
     */
public void startTimerForCurrentPlayer() {
        // *** FIX: Use railsRoot.getGameManager() ***
        if (railsRoot.getGameManager().isTimeManagementEnabled() && gameTimer != null && !gameTimer.isRunning()) {
            isTimerPaused = false; // Ensure not paused
            gameTimer.start();
            // *** FIX: Use railsRoot.getGameManager() ***
        } else if (railsRoot.getGameManager().isTimeManagementEnabled() && gameTimer != null && gameTimer.isRunning()) {
            // *** FIX: Use railsRoot.getGameManager() ***
        } else if (!railsRoot.getGameManager().isTimeManagementEnabled()) {
        } else {
        }
    }

   
    /**
     * Pauses the game timer unconditionally.
     */
    public void pauseTimer() {
        isTimerPaused = true;
        if (!isStopwatchPaused) {
            currentPauseStart = System.currentTimeMillis();
            isStopwatchPaused = true;
        }
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop(); // Truly halt the background UI ticker to save CPU
        }
    }

    /**
     * Resumes the game timer unconditionally.
     */
    public void resumeTimer() {
        isTimerPaused = false;
        if (isStopwatchPaused) {
            accumulatedPauseTime += (System.currentTimeMillis() - currentPauseStart);
            isStopwatchPaused = false;
        }
        if (gameTimer != null && !gameTimer.isRunning()) {
            gameTimer.start(); // Revive the background UI ticker
        }
    }
    
    /**
     * Resets the active player being tracked by the timer.
     * This is called by processAction when a turn change is detected.
     */
    public void resetTimerForNewPlayer(Player newPlayer) {
        // This check prevents the NullPointerException at startup
        if (gameTimer == null) {
            return;
        }

        // Stop/start to ensure a clean tick for the new player
        gameTimer.stop();
        currentPlayerOnTimer = newPlayer;
        if (!isTimerPaused) {
            gameTimer.start();
        }

    }

    // Ensure this public access method is here if the GameUIManager uses it:
    // (It typically delegates the call to the GameManager/PlayerManager)
    public Player getCurrentPlayer() {
        return getRoot().getGameManager().getCurrentPlayer();
    }

    /** Checks if the timer is currently paused */
    public boolean isTimerPaused() {
        return isTimerPaused;
    }

    public void performAIMove() {

        RoundFacade currentRound = getCurrentRound();

        // CRITICAL FIX: Zwinge die Engine, die Aktionen zu setzen, bevor wir
        // weitermachen.
        // Dies behebt die Race Condition im 'A'-Tasten-Pfad.
        try {
            currentRound.setPossibleActions();
        } catch (Exception e) {
            return;
        }

        // Jetzt die aktualisierte Liste abrufen (die nun DiscardTrain enthalten
        // sollte).
        PossibleActions currentActions = getGameManager().getPossibleActions();

        // Safety check: The UI calls us, so ORUIManager should not be null
        if (orUIManager == null) {
            return;
        }

        try {

            // --- Delegate based on Round Type ---
            if (currentRound instanceof OperatingRound) {
                // Wir lassen ORUIManager die AI aufrufen, da es den OR-Kontext hat.
                // ORUIManager.processAIMove() enthält den finalen KI-Aufruf, der den Discard
                // wählt.
                orUIManager.processAIMove();

            } else if (currentRound instanceof StockRound || currentRound instanceof StartRound
                    || currentRound instanceof PrussianFormationRound) {

                // --- Keep SR/IR/PFR AI Logic Here ---
                AIPlayer aiBrain = new AIPlayer("AI_Generic_Round", getGameManager());
                PossibleAction chosenAction = aiBrain.chooseMove(
                        null, // No operating company for SR/IR/PFR contexts usually needed for this level
                        currentActions,
                        Collections.emptyList(), // No tiles
                        Collections.emptyList() // No tokens
                );

                if (chosenAction != null) {
                    processAction(chosenAction); // Process normally
                } else {
                    PossibleAction fallback = findFallbackAction(currentActions);
                    if (fallback != null) {
                        processAction(fallback);
                    } else {
                    }
                }
            } else {
            }

        } catch (Exception e) {
        } finally {
            // --- Re-enable Button ---
            if (statusWindow != null) {
                statusWindow.enableAIButton(true);
            }
        }
    }

    // Helper to find Pass/Done/Skip
    private PossibleAction findFallbackAction(PossibleActions actions) {
        if (actions == null)
            return null;
        for (PossibleAction action : actions.getList()) {
            if (action instanceof NullAction) {
                NullAction.Mode mode = ((NullAction) action).getMode();
                if (mode == NullAction.Mode.PASS || mode == NullAction.Mode.DONE || mode == NullAction.Mode.SKIP) {
                    return action;
                }
            }
        }
        return null; // No suitable fallback
    }

    public void setJsonLoad(boolean isJson) {
        this.isJsonLoad = isJson;
    }

    // ... (lines of unchanged context code) ...
    private void updateActivityPanel() {
        if (statusWindow == null)
            return;

        // 1. Safety Checks (Pause state)
        boolean enginePaused = getGameManager().isGamePaused();
        boolean timerPaused = isTimerPaused();

        if (enginePaused || timerPaused) {
            String reason = enginePaused ? "GAME PAUSED" : "TIMER PAUSED";
            statusWindow.updateActivityPanel(
                    "<html><center><h2 style='color:red'>*** " + reason + " ***</h2></center></html>");
            return;
        }

        // 2. Gather Data (Safely)
        String historyText = "";
        String roundStep = "Init";
        String moveCount = "Move: 0";

        try {
            if (getGameManager() != null) {
                historyText = getGameManager().getLastActionSummary();

                moveCount = "Move: " + getGameManager().getActionCountModel().value();

                RoundFacade currentRound = getGameManager().getCurrentRound();
                if (currentRound != null) {
                    if (currentRound instanceof OperatingRound) {
                        roundStep = "OR " + getGameManager().getORId();
                    } else if (currentRound instanceof StockRound) {
                        roundStep = "SR " + getGameManager().getSRNumber();
                    } else {
                        String rId = currentRound.getId();
                        roundStep = (rId != null) ? rId : "Start";
                    }
                }
            }
        } catch (Exception e) {
            log.error("UI Header Error", e);
        }

        if (statusWindow != null) {
            String metaString = roundStep + " | " + moveCount;
            statusWindow.updateMetadata(metaString);
        }

        // 3. Build "Thinking" Content
        String mainAlert = "Waiting...";

        Player currentPlayer = getCurrentPlayer();
        RoundFacade round = getCurrentRound();

        if (currentPlayer != null && round != null) {
            String playerName = currentPlayer.getName(); // Normal capitalization
            String roundDesc = "";

            if (round instanceof OperatingRound) {
                OperatingRound or = (OperatingRound) round;
                String companyName = "";
                if (or.getOperatingCompany() != null) {
                    companyName = or.getOperatingCompany().getId();
                }
                // Shorten step names
                String stepName = or.getStep().toString().replace("_", " ").toLowerCase();
                roundDesc = companyName + " - " + stepName;
            } else if (round instanceof StockRound) {
                roundDesc = "Stock Round";
            } else {
                roundDesc = round.getId();
            }

            mainAlert = "Thinking: <b>" + playerName + "</b> " + roundDesc;
        }

        // 4. Build Footer (Next Only)
        String nextText = "";
        try {
            if (round instanceof OperatingRound) {
                // ... (Existing Next Player Logic) ...
                OperatingRound or = (OperatingRound) round;
                java.util.List<PublicCompany> ops = or.getOperatingCompanies();
                PublicCompany current = or.getOperatingCompany();
                if (ops != null && !ops.isEmpty() && current != null) {
                    int idx = ops.indexOf(current);
                    PublicCompany nextComp = ops.get((idx + 1) % ops.size());
                    if (nextComp != null && nextComp.getPresident() != null) {
                        nextText = "Next: " + nextComp.getPresident().getName() + " (" + nextComp.getId() + ")";
                    }
                }
            } else if (currentPlayer != null) {
                java.util.List<Player> players = getGameManager().getPlayers();
                if (players != null && !players.isEmpty()) {
                    int idx = players.indexOf(currentPlayer);
                    Player next = players.get((idx + 1) % players.size());
                    nextText = "Next: " + next.getName();
                }
            }
        } catch (Exception e) {
        }

        // --- 5. THE CLEAN LAYOUT (Full Width, No Metadata) ---
        StringBuilder html = new StringBuilder("<html>");

        // CSS Style: width 100% to fill panel
        html.append("<table width='100%' cellpadding='0' cellspacing='0' style='padding:0px;'>");

        // ROW 1: History (Blue) - Full Width
        html.append("<tr>");
        html.append("<td width='100%' align='left' style='padding-left:5px;'><font size='4' color='#1976D2'>")
                .append(historyText).append("</font></td>");
        html.append("</tr>");

        // ROW 2: MAIN ALERT (Red) - Full Width, No Wrap
        html.append("<tr>");
        // Added 'white-space:nowrap' to force single line
        html.append(
                "<td width='100%' align='center' style='padding-top:2px; padding-bottom:2px; white-space:nowrap;'>");
        html.append("<font size='6' color='#D32F2F'>").append(mainAlert).append("</font>");
        html.append("</td>");
        html.append("</tr>");

        // ROW 3: Footer (Green) - Full Width
        html.append("<tr>");
        html.append("<td width='100%' align='left' style='padding-left:5px;'><font size='5' color='#388E3C'>")
                .append(nextText).append("</font></td>");
        html.append("</tr>");

        html.append("</table></html>");

        statusWindow.updateActivityPanel(html.toString());
    }

    /**
     * Checks if the current player is controlled by an AI.
     * (Required for ORUIManager's AI Auto-Trigger.)
     * 
     * @return True if the current player's Actor is an AIPlayer, false otherwise.
     */
    public boolean isCurrentPlayerAI() {
        // FIX: This now compiles because Player.java has the getActor() method.
        Player currentPlayer = getGameManager().getCurrentPlayer();
        return currentPlayer != null && currentPlayer.getActor() instanceof net.sf.rails.game.ai.AIPlayer;
    }

    private Set<PossibleAction> verifiedActions = new HashSet<>();

    public boolean processAction(PossibleAction action) {
        boolean result;
        lastAction = action;

        // Detect Undo/Redo
        if (action instanceof GameAction) {
            GameAction.Mode mode = ((GameAction) action).getMode();
            if (mode == GameAction.Mode.UNDO || mode == GameAction.Mode.REDO || mode == GameAction.Mode.FORCED_UNDO) {
                isHistoryNavigation = true;
                resetUIStopwatch();
            } else {
                isHistoryNavigation = false;
            }

            result = previousResult = processOnServer(action);
            if (result) {
                updateUI();
                if (statusWindow != null) {
                    statusWindow.setGameActions();
                }
            }
            return result;
        }

        // Intercept Train Correction Actions and route to Manager
        if (action instanceof TrainCorrectionAction) {
            // We delegate execution to the Manager, which runs the JOptionPane sequence
            rails.game.correct.TrainCorrectionManager mgr = (rails.game.correct.TrainCorrectionManager) getGameManager()
                    .getCorrectionManager(rails.game.correct.CorrectionType.CORRECT_TRAINS);

            if (mgr != null) {
                boolean result2 = mgr.executeCorrection((rails.game.correct.CorrectionAction) action);
                if (result2)
                    updateUI();
                return result2;
            }
        }

        // Intercept Cash Correction Actions to handle UI inputs locally
        if (action instanceof rails.game.correct.CashCorrectionAction) {
            rails.game.correct.CashCorrectionAction cca = (rails.game.correct.CashCorrectionAction) action;

            String amountString = (String) JOptionPane.showInputDialog(statusWindow,
                    LocalText.getText("CorrectCashDialogMessage", cca.getCashHolderName()),
                    LocalText.getText("CorrectCashDialogTitle"),
                    JOptionPane.QUESTION_MESSAGE, null, null, "0");

            if (amountString == null)
                return false;

            if (amountString.length() > 0 && amountString.charAt(0) == '+')
                amountString = amountString.substring(1);

            try {
                int amount = Integer.parseInt(amountString);
                cca.setAmount(amount);
            } catch (NumberFormatException e) {
                return false;
            }
            // Fall through to processOnServer below
        }

        if (action == null) {
            result = previousResult;
        } else {
            Player oldPlayer = getCurrentPlayer();
            SoundManager.notifyOfActionProcessing(railsRoot, action);

            // Local UI Stopwatch: Embed elapsed time into the action payload
            long now = System.currentTimeMillis();
            long elapsedMs = now - turnStartTimestamp - accumulatedPauseTime;
            int elapsedSec = Math.max(0, (int) (elapsedMs / 1000));

            // Isolate standard game moves from structural engine commands
            if (!(action instanceof GameAction) && !(action instanceof NullAction
                    && ((NullAction) action).getMode() == NullAction.Mode.START_GAME)) {
                action.setExecutionTimeSeconds(elapsedSec);
            }
            resetUIStopwatch();

            result = previousResult = processOnServer(action);

            if (result) {
                // A. AutoSave Logic
                if (autoSaveLoadStatus > 0) {
                    String nextPlayerId = "System";
                    if (getCurrentPlayer() != null)
                        nextPlayerId = getCurrentPlayer().getId();
                    autoSave(nextPlayerId);
                }

                // B. CRITICAL: Refresh the Screen
                updateUI();

                // This updates the 'Undo' button availability in the Menu/Key listener
                if (statusWindow != null) {
                    statusWindow.setGameActions();
                }

                // C. Reset Timer if player changed
                Player newPlayer = getCurrentPlayer();
                if (newPlayer != null && !newPlayer.equals(oldPlayer)) {
                    resetTimerForNewPlayer(newPlayer);
                }

                // D. Show Server Messages
                displayServerMessage();

                // E. Mark Game Over
                if (isGameOver() && !getGameManager().getGameOverReportedUI()) {
                    getGameManager().setGameOverReportedUI(true);
                    // The game is over, now display the final worth chart
                    showPlayerWorthChart();
                }
            }

            if (verifiedActions.size() > 10)
                verifiedActions.clear();
        }
        return result;
    }

    /**
     * Creates the "Worth Chart" menu item, which should be added to the
     * StatusWindow menu bar.
     * * @return A JMenuItem configured to call showPlayerWorthChart().
     */
    public JMenuItem getMenuChart() {
        // We use a hardcoded name here since LocalText is unavailable.
        JMenuItem chartItem = new JMenuItem("Player Worth Chart");
        chartItem.addActionListener(e -> {
            showPlayerWorthChart();
        });
        return chartItem;
    }

    /**
     * Checks if any Correction Manager (e.g., CORRECT_MAP, CORRECT_TRAINS)
     * is currently set to active by the user.
     */
    private boolean isAnyCorrectionModeActive() {
        if (railsRoot == null)
            return false;

        for (rails.game.correct.CorrectionType ct : EnumSet.allOf(rails.game.correct.CorrectionType.class)) {
            if (getGameManager().getCorrectionManager(ct).isActive()) {
                return true;
            }
        }
        return false;
    }

    public void fitMapToWindow() {
        if (orWindow != null && orWindow.getMapPanel() != null) {
            orWindow.getMapPanel().fitToWindow();
        }
    }

    public void zoomMap(boolean zoomIn) {
        if (orWindow != null && orWindow.getMapPanel() != null) {
            if (zoomIn) {
                orWindow.getMapPanel().zoomIn();
            } else {
                orWindow.getMapPanel().zoomOut();
            }
        }
    }

    /**
     * Registers a window with the WindowSettings manager.
     * 1. Applies stored size/position immediately (via packAndApplySizing).
     * 2. Adds a listener to save the window state only when it is a VALID size.
     */
    private void registerWindowStorage(JFrame frame) {
        if (frame == null)
            return;

        // 1. Initial Position Restore
        packAndApplySizing(frame);

        // 2. Add Smart Listener
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                saveIfValid(frame);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                saveIfValid(frame);
            }
        });
    }

    /**
     * Helper checks that the window is large enough to be valid before saving.
     * This prevents the "Tiny Window" bug (saving 10x10 or 80x28).
     */
    private void saveIfValid(JFrame frame) {
        // Only save if the window is reasonably sized
        if (frame.isVisible() && frame.getWidth() >= 200 && frame.getHeight() >= 100) {
            windowSettings.set(frame);
        }
    }

    public void correctStockManually() {
        // 1. Select Company
        List<PublicCompany> companies = getAllPublicCompanies();
        if (companies.isEmpty())
            return;

        // Sort by ID for easier finding
        companies.sort((c1, c2) -> c1.getId().compareTo(c2.getId()));

        PublicCompany selectedCompany = (PublicCompany) JOptionPane.showInputDialog(
                statusWindow,
                LocalText.getText("SelectCompany"),
                LocalText.getText("Correction"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                companies.toArray(),
                companies.get(0));

        if (selectedCompany == null)
            return; // Cancelled

        // 2. Select Destination (Text Input)
        String currentLoc = (selectedCompany.getCurrentSpace() != null)
                ? selectedCompany.getCurrentSpace().getId()
                : "None";

        String targetId = JOptionPane.showInputDialog(
                statusWindow,
                "Current: " + currentLoc + "\nEnter New Coordinate (e.g. 'D2'):",
                "Move " + selectedCompany.getId(),
                JOptionPane.PLAIN_MESSAGE);

        if (targetId == null || targetId.trim().isEmpty())
            return;

        targetId = targetId.trim().toUpperCase();

        // 3. Validate and Move
        net.sf.rails.game.financial.StockSpace targetSpace = getRoot().getStockMarket().getStockSpace(targetId);

        if (targetSpace != null) {
            // Use StockMarket logic to properly move tokens (update Chart/Pool)
            getRoot().getStockMarket().correctStockPrice(selectedCompany, targetSpace);

            updateUI();

            // Log for confirmation
            String msg = "Correction: Moved " + selectedCompany.getId() + " to " + targetId;
            log.info(msg);
            JOptionPane.showMessageDialog(statusWindow, msg);
        } else {
            JOptionPane.showMessageDialog(statusWindow, "Invalid Coordinate: " + targetId, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

    }

    public void correctSharesManually() {
        // 1. Gather all possible owners (Bank Pools, Players, Companies)
        List<PortfolioOwner> owners = getAllPortfolioOwners();

        // 2. Select Source
        PortfolioOwner source = (PortfolioOwner) JOptionPane.showInputDialog(
                statusWindow,
                LocalText.getText("SelectSource", "Select Source"),
                LocalText.getText("Correction"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                owners.toArray(),
                owners.get(0));

        if (source == null)
            return;

        // 3. Select Certificate (Shares AND Privates)
        // Use the common interface 'Certificate' (or Ownable) for the list
        List<net.sf.rails.game.financial.Certificate> certs = new ArrayList<>();

        // Add Public Shares
        certs.addAll(source.getPortfolioModel().getCertificates());

        // Add Private Companies
        certs.addAll(source.getPortfolioModel().getPrivateCompanies());

        if (certs.isEmpty()) {
            JOptionPane.showMessageDialog(statusWindow, "No certificates found in " + source.getId());
            return;
        }

        // Sort for readability. Both types implement toText() / toString()
        certs.sort((c1, c2) -> c1.toString().compareTo(c2.toString()));

        // Use the interface type for the selection variable
        net.sf.rails.game.financial.Certificate selectedCert = (net.sf.rails.game.financial.Certificate) JOptionPane
                .showInputDialog(
                        statusWindow,
                        LocalText.getText("SelectCertificate", "Select Certificate"),
                        LocalText.getText("Correction"),
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        certs.toArray(),
                        certs.get(0));

        if (selectedCert == null)
            return;

        // 4. Select Destination
        PortfolioOwner dest = (PortfolioOwner) JOptionPane.showInputDialog(
                statusWindow,
                LocalText.getText("SelectDestination", "Select Destination"),
                LocalText.getText("Correction"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                owners.toArray(),
                owners.get(0));

        if (dest == null)
            return;

        // 5. Execute Move
        if (source.equals(dest)) {
            JOptionPane.showMessageDialog(statusWindow, "Source and Destination are the same.");
            return;
        }

        // Cast to Ownable to ensure we can access moveTo()
        // Both PrivateCompany and PublicCertificate implement Ownable/RailsOwnable
        if (selectedCert instanceof net.sf.rails.game.state.Ownable) {
            ((net.sf.rails.game.state.Ownable) selectedCert).moveTo(dest);
        } else {
            log.error("Selected certificate does not implement Ownable: {}", selectedCert);
            JOptionPane.showMessageDialog(statusWindow, "Error: Item cannot be moved.");
            return;
        }

        // Force GameStatus to rebuild layout (e.g. if Player -> IPO change happened)
        if (statusWindow != null && statusWindow.getGameStatus() != null) {
            statusWindow.getGameStatus().recreate();
        }

        // 6. Refresh and Log
        updateUI();
        String msg = "Correction: Moved " + selectedCert.toString() + " from " + source.getId() + " to " + dest.getId();
        log.info(msg);
        JOptionPane.showMessageDialog(statusWindow, msg);
    }

    private List<PortfolioOwner> getAllPortfolioOwners() {
        List<PortfolioOwner> list = new ArrayList<>();
        Bank bank = getRoot().getBank();

        // Bank Portfolios
        list.add(bank.getIpo());
        list.add(bank.getPool());
        list.add(bank.getUnavailable());
        list.add(bank.getScrapHeap());

        // Players
        list.addAll(getPlayers());

        // Companies (Treasuries)
        // We include all public companies because any of them *might* hold shares (e.g.
        // 1830 redeemable, or errors)
        list.addAll(getAllPublicCompanies());

        return list;
    }

    /**
     * Toggles the visibility of Hex names (A1, B2, etc.) on the map.
     */
    public void toggleHexNames() {
        if (orWindow != null && orWindow.getMapPanel() != null) {
            orWindow.getMapPanel().toggleDisplayHexNames();
        }
    }

    /**
     * Toggles the visibility of Hex build numbers on the map.
     */
    public void toggleHexNumbers() {
        if (orWindow != null && orWindow.getMapPanel() != null) {
            orWindow.getMapPanel().toggleDisplayBuildNumbers();
        }
    }

    /**
     * Called from the UI Menu (e.g., a StatusWindow menu item) to display the
     * Player Worth History Chart. Delegates the call to GameManager, passing the
     * StatusWindow (or MapWindow as fallback) as the parent frame for the dialog.
     */
    public void showPlayerWorthChart() {
        if (statusWindow != null) {
            // statusWindow is a JFrame, which is what the GameManager method expects (via
            // Object)
            getGameManager().displayWorthChart(statusWindow);
        } else if (orWindow != null) {
            // Fallback to orWindow if statusWindow isn't ready
            getGameManager().displayWorthChart(orWindow);
        }
    }

    /**
     * Called by the CorrectionManager to force the entire UI to re-synchronize
     * after a correction action (e.g., Cash or Share movement).
     * This bypasses the normal flow optimizations which can leave "ghost"
     * artifacts.
     */
    public void forceFullUIRefresh() {
        log.info("Forcing full UI refresh after Correction Mode change or Correction execution.");

        // 1. Force the game engine to recalculate its possible actions
        // (Ensures CorrectionModeActions are correctly added/removed)
        RoundFacade currentRound = getGameManager().getCurrentRound();
        if (currentRound != null) {
            currentRound.setPossibleActions();
        }

        // 2. Force the StatusWindow to re-evaluate its possible actions and status
        if (statusWindow != null) {
            statusWindow.updateStatus(true);
        }

        // 3. Force the ORWindow/Map to fully redraw (if applicable, e.g., via
        // refreshAll())
        if (orUIManager != null) {
            // ORUIManager handles map redraw/token update
            orUIManager.updateStatus(true);
        }

        // 4. Force a graphical repaint and focus management
        if (activeWindow instanceof JFrame) {
            ((JFrame) activeWindow).repaint();
            setMeToFront((JFrame) activeWindow);
        }

        // 5. Ensure the correction menu is rebuilt (this is part of
        // statusWindow.updateStatus, but is good redundancy)
        if (statusWindow != null) {
            statusWindow.setCorrectionMenu();
        }

    }

    /**
     * Resets the time tracking history for a player to prevent visual artifacts
     * (fake bonuses) during Undo.
     */
    public void resetTimeHistory(int playerIndex) {
        if (statusWindow != null && statusWindow.getGameStatus() != null) {
            statusWindow.getGameStatus().resetTimeHistory(playerIndex);
        }
    }

    /**
     * Updates the player time display, bypassing all delta-check logic.
     * Used exclusively after an Undo action to prevent Ghost Green Flashes.
     */
    public void setPlayerTimeAfterUndo(int playerIndex, int newTime) {
        if (statusWindow != null && statusWindow.getGameStatus() != null) {
            // Note: This method will also update the lastPlayerTimes array automatically.
            statusWindow.getGameStatus().setPlayerTimeWithoutDeltaCheck(playerIndex, newTime);
        }
    }

    /**
     * Saves the position and size of all active windows.
     * 
     * @param gameName The name of the current game (e.g. "1830") for file naming.
     */
    public void saveWindowSettings(String gameName) {
        // Initialize with the provided game name
        if (windowSettings == null) {
            windowSettings = new WindowSettings(gameName);
        }

        // 1. Update settings for known windows
        if (statusWindow != null)
            windowSettings.set(statusWindow);
        if (orWindow != null)
            windowSettings.set(orWindow);
        if (reportWindow != null)
            windowSettings.set(reportWindow);

        // Include the Stock Chart window which was previously omitted
        if (stockChartWindow != null)
            windowSettings.set(stockChartWindow);

        // Include the Config window for completeness
        if (configWindow != null)
            windowSettings.set(configWindow);

        // Save the font UI scaling
        windowSettings.setProperty("font.ui.scale", String.valueOf(this.currentFontScale));

        // 2. Save Map Zoom Step
        if (orUIManager != null && orUIManager.getMap() != null) {
            HexMap map = orUIManager.getMap();
            int zoomStep = map.getZoomStep();
            windowSettings.setProperty("MapZoomStep", String.valueOf(zoomStep));
        }

        // 3. Write to disk
        windowSettings.save();
    }

    /**
     * Restores the position and size of all active windows.
     * 
     * @param gameName The name of the current game.
     */
    public void loadWindowSettings(String gameName) {
        if (windowSettings == null) {
            windowSettings = new WindowSettings(gameName);
        }

        // 1. Apply positions
        restoreWindow(statusWindow);
        restoreWindow(orWindow);
        restoreWindow(reportWindow);

        // 2. Restore Map Zoom Step
        if (orUIManager != null && orUIManager.getMap() != null) {
            HexMap map = orUIManager.getMap();
            String zoomStr = windowSettings.getProperty("MapZoomStep");
            if (zoomStr != null) {
                try {
                    int zoomStep = Integer.parseInt(zoomStr);
                    map.setZoomStep(zoomStep);
                } catch (NumberFormatException e) {
                    // Ignore invalid zoom data
                }
            }
        }

    }

    public void shutdown() {
        log.info("GameUIManager shutdown triggered.");

        // Force save of window bounds and scale factors
        if (windowSettings != null) {
            windowSettings.save();
            log.info("Window settings flushed to disk.");
        }
        
    }
    public static GameUIManager getInstance() {
        return instance;
    }

    private void restoreWindow(JFrame window) {
        if (window == null)
            return;
        Rectangle r = windowSettings.getBounds(window);
        // If window settings are valid, verify they are visible on the current screen.
        if (r.width > 0 && r.height > 0) {
            boolean visible = false;
            // Check if the top-left corner is within any available screen device
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (GraphicsDevice gd : ge.getScreenDevices()) {
                if (gd.getDefaultConfiguration().getBounds().contains(r.getLocation())) {
                    visible = true;
                    break;
                }
            }

            if (visible) {
                window.setBounds(r);
            } else {
                // If off-screen, center it on the default screen instead
                window.setSize(r.getSize());
                window.setLocationRelativeTo(null);
                log.info("Window restored to center (was off-screen): {}", window.getName());
            }
        }

    }

}
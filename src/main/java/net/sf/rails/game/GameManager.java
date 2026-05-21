package net.sf.rails.game;

// Add this if not present
import net.sf.rails.common.*;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.PlayerManager.PlayerOrderModel;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.LocatedBonus;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.special.SpecialBonusTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.*;
import net.sf.rails.game.state.Currency;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.GameSaver;
import net.sf.rails.util.Util;
import java.io.IOException; // Add this
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;
import rails.game.correct.CorrectionAction;
import rails.game.correct.CorrectionManager;
import rails.game.correct.CorrectionType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.google.common.collect.ComparisonChain;

import javafx.event.ActionEvent;
import net.sf.rails.game.ai.snapshot.JsonStateSerializer;
import java.io.IOException; // Add this

import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.Component;
import java.awt.Window;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager extends RailsManager implements Configurable, Owner {

    protected enum GameEnd {
        UNDEFINED,
        AFTER_THIS_OR,
        AFTER_SET_OF_ORS
    }

    private static final Logger log = LoggerFactory.getLogger(GameManager.class);

    public static final String ARCHIVE_ENABLED = "save.archive.enabled";
    public static final String ARCHIVE_DIRECTORY = "save.archive.dir";
    public static final String ARCHIVE_KEEP_COUNT = "save.archive.keep_count";

    protected Class<? extends StockRound> stockRoundClass = StockRound.class;
    protected Class<? extends OperatingRound> operatingRoundClass = OperatingRound.class;
    protected Class<? extends ShareSellingRound> shareSellingRoundClass = ShareSellingRound.class;
    protected Class<? extends TreasuryShareRound> treasuryShareRoundClass = TreasuryShareRound.class;

    // Variable UI Class names
    protected String gameUIManagerClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.GAME_UI_MANAGER);
    protected String orUIManagerClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.OR_UI_MANAGER);
    protected String gameStatusClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.GAME_STATUS);
    protected String statusWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.STATUS_WINDOW);
    protected String orWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.OR_WINDOW);
    protected String startRoundWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.START_ROUND_WINDOW);

    // map of correctionManagers
    protected final Map<CorrectionType, CorrectionManager> correctionManagers = new HashMap<>();

    protected String gameName;
    protected int currentNumberOfOperatingRounds = 1;
    private transient GameUIManager gameUIManager;

    public void setGameUIManager(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
    }

    public GameUIManager getGameUIManager() {
        if (this.gameUIManager == null) {
            this.gameUIManager = OpenGamesManager.getInstance().getGame(getRoot().getId());
        }
        return this.gameUIManager;
    }

    protected boolean skipFirstStockRound = false;
    protected boolean showCompositeORNumber = true;

    protected boolean forcedSellingCompanyDump = true;
    protected boolean gameEndsWithBankruptcy = false;
    protected int gameEndsWhenBankHasLessOrEqual = 0;
    protected GameEnd gameEndWhen = GameEnd.UNDEFINED;
    private int absoluteActionCounter = 0;

    protected transient String sessionStartTimestamp = null;

    protected boolean dynamicOperatingOrder = true;
    /*
     * If true, companies can only buy trains from other companies
     * if both companies share the same President.
     */
    protected boolean restrictTrainTradingToSameOwner = true;

    // Member variables to support reload "look-ahead"
    private transient List<PossibleAction> actionsBeingReloaded = null;
    private transient int reloadActionIndex = 0;

    // Replaced single major increment with Phase-Specific increments
    protected final IntegerState timeMgmtYellowIncrement = IntegerState.create(this, "timeMgmtYellowIncrement", 35);
    protected final IntegerState timeMgmtGreenIncrement = IntegerState.create(this, "timeMgmtGreenIncrement", 70);
    protected final IntegerState timeMgmtBrownIncrement = IntegerState.create(this, "timeMgmtBrownIncrement", 35);

    public void setTimeMgmtYellowIncrement(int seconds) {
        this.timeMgmtYellowIncrement.set(seconds);
    }

    public int getTimeMgmtYellowIncrement() {
        return this.timeMgmtYellowIncrement.value();
    }

    public void setTimeMgmtGreenIncrement(int seconds) {
        this.timeMgmtGreenIncrement.set(seconds);
    }

    public int getTimeMgmtGreenIncrement() {
        return this.timeMgmtGreenIncrement.value();
    }

    public void setTimeMgmtBrownIncrement(int seconds) {
        this.timeMgmtBrownIncrement.set(seconds);
    }

    public int getTimeMgmtBrownIncrement() {
        return this.timeMgmtBrownIncrement.value();
    }

    public void setTimeMgmtMajorCoIncrement(int seconds) {
        this.timeMgmtYellowIncrement.set(seconds);
    }

    public int getTimeMgmtMajorCoIncrement() {
        return this.timeMgmtYellowIncrement.value();
    }

    /**
     * A transient field to tell the 'processOnReload' hook where to
     * save state snapshot files. If null, logging is disabled.
     */
    private transient File logOutputDirectory = null;

    /**
     * Injects an output directory to enable state-snapshot logging
     * during a replay.
     * 
     * @param directory The directory to save state_XXXX.json files.
     */
    public void setLogOutputDirectory(File directory) {
        this.logOutputDirectory = directory;
    }

    /**
     * Will only be set during game reload
     */
    protected boolean reloading = false;

    protected final EnumMap<GameDef.Parm, Object> gameParameters = new EnumMap<>(GameDef.Parm.class);

    /**
     * Calculate the worth of a private company for player net-worth/bankruptcy.
     * Default behavior is the base price (face value).
     */
    public int getPrivateWorth(PrivateCompany priv) {
        return priv.getBasePrice();
    }

    /**
     * Current round should not be set here but from within the Round classes.
     * This is because in some cases the round has already changed to another
     * one when the constructor terminates. Example: if the privates have not
     * been sold, it finishes by starting an Operating Round, which handles the
     * privates payout and then immediately starts a new Start Round.
     */
    protected final GenericState<RoundFacade> currentRound = new GenericState<>(this, "currentRound");
    private GenericState<RoundFacade> interruptedRound = new GenericState<>(this, "interruptedRound");

    protected final IntegerState startRoundNumber = IntegerState.create(this, "startRoundNumber");
    protected final IntegerState stockRoundNumber = IntegerState.create(this, "srNumber");

    protected final IntegerState absoluteORNumber = IntegerState.create(this, "absoluteORNUmber");
    protected final IntegerState relativeORNumber = IntegerState.create(this, "relativeORNumber");
    protected final IntegerState numOfORs = IntegerState.create(this, "numOfORs");
    // This tracks the limit valid for the CURRENT set of Operating Rounds.
    // It prevents Phase changes (which update numOfORs immediately) from
    // extending the current cycle unexpectedly.
    protected final IntegerState operatingRoundLimit = IntegerState.create(this, "operatingRoundLimit", 1);

    protected int maxRounds; // For SOH

    /**
     * The previous OR or SR, excluding all merge rounds.
     * Needed in 1837 when merge Rounds occur
     */
    protected GenericState<Round> currentSRorOR = new GenericState<>(this, "prevMainRound");

    protected final BooleanState firstAllPlayersPassed = new BooleanState(this, "firstAllPlayersPassed");

    /**
     * GameOver pending, a last OR or set of ORs must still be completed
     */
    protected final BooleanState gameOverPending = new BooleanState(this, "gameOverPending");
    /**
     * GameOver is executed, no more moves
     */
    protected final BooleanState gameOver = new BooleanState(this, "gameOver");
    protected Boolean gameOverReportedUI = false;
    protected final BooleanState endedByBankruptcy = new BooleanState(this, "endedByBankruptcy");

    /**
     * UI display hints
     */
    protected GuiHints guiHints;

    /** Flags to be passed to the UI, aiding the layout definition */
    protected final EnumMap<GuiDef.Parm, Boolean> guiParameters = new EnumMap<>(GuiDef.Parm.class);

    protected GenericState<StartPacket> startPacket = new GenericState<>(this, "startPacket");

    protected PossibleActions possibleActions = PossibleActions.create();

    protected final ArrayListState<PossibleAction> executedActions = new ArrayListState<>(this, "executedActions");

    /**
     * Special properties that can be used by other players or companies
     * than just the owner (such as buyable bonus tokens as in 1856).
     */
    protected Portfolio<SpecialProperty> commonSpecialProperties = null;

    /**
     * indicates that the recoverySave already issued a warning, avoids displaying
     * several warnings
     */
    protected boolean recoverySaveWarning = true;

    /**
     * Flag to skip a subsequent Done action (if present) during reloading.
     * <br>
     * This is a fix to maintain backwards compatibility when redundant
     * actions are skipped in new code versions (such as the bypassing of
     * a treasury trading step if it cannot be executed).
     * <br>
     * This flag must be reset after processing <i>any</i> action (not just Done).
     */
    protected boolean skipNextDone = false;
    /**
     * Step that must be in effect to do an actual Done skip during reloading.
     * <br>
     * This is to ensure that Done actions in different OR steps are
     * considered separately.
     */

    protected GameDef.OrStep skippedStep = null;

    // storage to replace static class variables
    // TODO: Move that to a better place
    protected Map<String, Object> objectStorage = new HashMap<>();
    protected Map<String, Integer> storageIds = new HashMap<>();

    private int revenueSpinnerIncrement = 10;
    // Used for Storing the PublicCompany to be Founded by a formationround
    private PublicCompany nationalToFound;

    private final Map<PublicCompany, Player> NationalFormStartingPlayer = new HashMap<>();

    protected PlayerOrderModel playerNamesModel;

    /**
     * Stores player worth snapshots at the end of each major round.
     * Key: RoundID (e.g., "OR_1.1", "SR_1", "End"). Value: Map<PlayerID, Worth>.
     */
    protected final GenericState<LinkedHashMap<String, Map<String, Double>>> playerWorthHistory = new GenericState<>(
            this, "playerWorthHistory");

    /**
     * Simple DTO to hold historical state for the UI table.
     */
    public static class PlayerAssetSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        public int cash;
        public Map<String, Integer> sharePercents = new HashMap<>(); // Key: CompanyID, Value: %
        public Map<String, Integer> holdingValues = new HashMap<>(); // Key: CompanyID, Value: $
    }

    /**
     * Stores detailed asset snapshots for the detailed table in the Worth Chart.
     * Key: RoundID. Value: Map<PlayerName, PlayerAssetSnapshot>.
     */
    protected final GenericState<LinkedHashMap<String, Map<String, PlayerAssetSnapshot>>> playerAssetHistory = new GenericState<>(
            this, "playerAssetHistory");

            protected final GenericState<LinkedHashMap<String, Map<String, Integer>>> playerTimeHistory = new GenericState<>(
            this, "playerTimeHistory");

    public LinkedHashMap<String, Map<String, Integer>> getPlayerTimeHistory() {
        return playerTimeHistory.value();
    }
    /**
     * Stores cumulative company payouts snapshots.
     * Key: RoundID. Value: Map<CompanyID, CumulativePayout>.
     */
    protected final GenericState<LinkedHashMap<String, Map<String, Integer>>> companyPayoutHistory = new GenericState<>(
            this, "companyPayoutHistory");
    /**
     * Tracks the running total of payouts for each company (Current State).
     */
    protected final GenericState<Map<String, Integer>> cumulativeCompanyPayouts = new GenericState<>(
            this, "cumulativeCompanyPayouts");

    /**
     * Stores instantaneous (per-round) company payouts.
     * Key: RoundID. Value: Map<CompanyID, PayoutInThisRound>.
     */
    protected final GenericState<LinkedHashMap<String, Map<String, Integer>>> instantaneousPayoutHistory = new GenericState<>(
            this, "instantaneousPayoutHistory");

    protected final GenericState<Map<String, Integer>> currentRoundPayouts = new GenericState<>(
            this, "currentRoundPayouts");

    /**
     * Tracks ChangeStack indices where rounds begin.
     * Must NOT be a State variable, so it survives UNDO and allows "Next Round"
     * navigation.
     */
    private final TreeSet<Integer> roundBoundaries = new TreeSet<>();

    public void markRoundBoundary() {
        if (getRoot() == null || getRoot().getStateManager() == null)
            return;
        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
        if (changeStack != null) {
            roundBoundaries.add(changeStack.getCurrentIndex());
        }
    }

    private void truncateRoundBoundaries(int currentIndex) {
        roundBoundaries.tailSet(currentIndex, false).clear();
    }

    public Integer getRoundStart(int currentIndex) {
        return roundBoundaries.floor(currentIndex);
    }

    public Integer getPreviousRoundIndex(int currentIndex) {
        return roundBoundaries.lower(currentIndex);
    }

    public Integer getNextRoundIndex(int currentIndex) {
        return roundBoundaries.higher(currentIndex);
    }

    /**
     * Flag used to track if any CorrectionModeAction is currently active (e.g.,
     * Cash, Train).
     */
    protected final BooleanState isCorrectionModeActive = new BooleanState(this, "isCorrectionModeActive");

    public BooleanState getCorrectionModeActiveModel() {
        return isCorrectionModeActive;
    }

    // ++ START TIME MANAGEMENT ++
    protected final BooleanState timeManagementEnabled = new BooleanState(this, "timeManagementEnabled", true);
    protected final IntegerState timeMgmtStartingSeconds = IntegerState.create(this, "timeMgmtStartingSeconds", 300);
    protected final IntegerState timeMgmtShareRoundIncrement = IntegerState.create(this, "timeMgmtShareRoundIncrement",
            60);

    private final HashSetState<String> awardedBonuses = HashSetState.create(this, "awardedBonuses");
    private transient Map<String, Integer> pendingTimePenalties = new HashMap<>();

    protected final IntegerState timeMgmtUndoPenalty = IntegerState.create(this, "timeMgmtUndoPenalty", 0);
    protected final IntegerState timeMgmtMajorUndoPenalty = IntegerState.create(this, "timeMgmtMajorUndoPenalty", 30);

    public void setTimeMgmtMajorUndoPenalty(int seconds) {
        this.timeMgmtMajorUndoPenalty.set(seconds);
    }

    public int getTimeMgmtMajorUndoPenalty() {
        return this.timeMgmtMajorUndoPenalty.value();
    }

    public void setTimeMgmtUndoPenalty(int seconds) {
        this.timeMgmtUndoPenalty.set(seconds);
    }

    public int getTimeMgmtUndoPenalty() {
        return this.timeMgmtUndoPenalty.value();
    }

    /**
     * Exposes the transient penalty for UI rendering calculations.
     */
    public int getPendingTimePenalty(String playerName) {
        if (pendingTimePenalties != null && pendingTimePenalties.containsKey(playerName)) {
            return pendingTimePenalties.get(playerName);
        }
        return 0;
    }


    public void grantTimeBonus(Player player, String roundId, int amount) {
        if (player == null || amount <= 0)
            return;

        String companyContext = "";
        if (getCurrentRound() instanceof OperatingRound) {
            PublicCompany comp = ((OperatingRound) getCurrentRound()).operatingCompany.value();
            if (comp != null) {
                companyContext = ":" + comp.getId();
            }
        }

        // Create a unique key: "RoundID:CompanyID:PlayerName" (e.g., "OR_1.1:PR:Bjoern" or "SR_1::Bjoern")
        String key = roundId + companyContext + ":" + player.getName();

        if (awardedBonuses.contains(key)) {
            return;
        }

        // Grant Bonus & Mark as Awarded
        player.getTimeBankModel().add(amount);
        awardedBonuses.add(key);
    }
   

    // --- PERSISTENT STATE FIELDS ---
    protected final GenericState<Double> timeMgmtOperatorMultiplier = new GenericState<>(this, "OperatorMultiplier");
    protected final GenericState<String> timeMgmtOperatorName = new GenericState<>(this, "OperatorName");
    // Persistent Global Game Timer (in seconds)
    protected final IntegerState totalGameTime = IntegerState.create(this, "totalGameTime");

    private boolean isPaused = false;

    public boolean isGamePaused() {
        return isPaused;
    }

    public void setGamePaused(boolean paused) {
        this.isPaused = paused;
    }

    public void incrementTotalGameTime() {
        if (!isPaused) {
            totalGameTime.add(1);

            // Force time deduction for Prussian Formation Round (skipped by standard UI
            // timer)
            if (isTimeManagementEnabled() && getCurrentRound() != null) {
                String rName = getCurrentRound().getClass().getSimpleName();
                if (rName.contains("Prussian")) {
                    Player active = getCurrentPlayer();
                    if (active != null) {
                        active.getTimeBankModel().add(-1);
                    }
                }
            }
        }
    }

    public String getFormattedGameTime() {
        int totalSeconds = totalGameTime.value();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // --- PUBLIC SETTERS (Using .set() for state) ---
    public void setTimeManagementEnabled(boolean enabled) {
        this.timeManagementEnabled.set(enabled);
    }

    public void setTimeMgmtStartingSeconds(int seconds) {
        this.timeMgmtStartingSeconds.set(seconds);
    }

    public void setTimeMgmtShareRoundIncrement(int seconds) {
        this.timeMgmtShareRoundIncrement.set(seconds);
    }

    public void setTimeMgmtOperatorName(String name) {
        this.timeMgmtOperatorName.set(name);
        // BACKUP: Save to global config
        Config.set("time.operator.name", name);
    }

    public void setTimeMgmtOperatorMultiplier(double multiplier) {
        this.timeMgmtOperatorMultiplier.set(multiplier);
        // BACKUP: Save to global config
        Config.set("time.operator.multiplier", String.valueOf(multiplier));
    }

    public String getTimeMgmtOperatorName() {
        String val = this.timeMgmtOperatorName.value();
        if (val != null && !val.isEmpty())
            return val;

        // BACKUP: Read from global config
        return Config.get("time.operator.name", "");
    }

    // --- PUBLIC GETTERS (Using .value() for state) ---
    public boolean isTimeManagementEnabled() {
        return this.timeManagementEnabled.value();
    }

    /**
     * DYNAMIC PHASE BONUS
     * Returns the time bonus based on the CURRENT Game Phase.
     * This implements the "Green Wall" logic where the Green phase grants more
     * time.
     */
    public int getOrTimeBonus() {
        Phase phase = getRoot().getPhaseManager().getCurrentPhase();

        // Fallback if phase is null
        if (phase == null)
            return getTimeMgmtYellowIncrement();

        // Use toText() instead of getName()
        String phaseName = phase.toText().toLowerCase();

        // Logic for 1835 Phases
        // Phase 1 is usually Yellow. Phase 2 starts Green. Phase 3+ is Brown.
        if (phaseName.contains("2") || phaseName.contains("green")) {
            return getTimeMgmtGreenIncrement();
        } else if (phaseName.contains("3") || phaseName.contains("brown") || phaseName.contains("gray")) {
            return getTimeMgmtBrownIncrement();
        }

        // Default to Yellow (Early Game)
        return getTimeMgmtYellowIncrement();
    }

    public int getTimeMgmtStartingSeconds() {
        return this.timeMgmtStartingSeconds.value();
    }

    public int getTimeMgmtShareRoundIncrement() {
        return this.timeMgmtShareRoundIncrement.value();
    }

    public double getTimeMgmtOperatorMultiplier() {
        Double val = this.timeMgmtOperatorMultiplier.value();
        if (val != null)
            return val;

        // BACKUP: Read from global config
        String configVal = Config.get("time.operator.multiplier");
        if (configVal != null) {
            try {
                return Double.parseDouble(configVal);
            } catch (NumberFormatException e) {
            }
        }
        return 1.5;
    }

    public int getCurrentActionCount() {
        return absoluteActionCounter;
    }

    private void initializePlayerTimeBanks() {
        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            int startTime = getTimeMgmtStartingSeconds();

            // Use the safe getters
            String opName = getTimeMgmtOperatorName();
            double opMult = getTimeMgmtOperatorMultiplier();

            if (Util.hasValue(opName) &&
                    player.getId().trim().equalsIgnoreCase(opName.trim())) {

                startTime = (int) (startTime * opMult);

            }

            player.getTimeBankModel().set(startTime);
        }
    }

    // Add this enum definition inside the GameManager class
    public enum TimeConsequence {
        NONE("Do nothing"), // Default
        SUBTRACT_FINAL_SCORE("Subtract from final score"),
        SUBTRACT_IMMEDIATE_CASH("Subtract immediately from cash");

        private final String description;

        TimeConsequence(String description) {
            this.description = description;
        }

        public String getDescription() {
            return LocalText.getText(this.name()); // Use enum name as key for LocalText
        }

        public static TimeConsequence fromDescription(String description) {
            for (TimeConsequence consequence : values()) {
                if (consequence.getDescription().equals(description)) {
                    return consequence;
                }
            }
            return NONE; // Default fallback
        }

        @Override
        public String toString() {
            return getDescription(); // Make ComboBox display the localized description
        }
    }

    // ... inside GameManager class body ...

    /**
     * To register the step number of subsequent company releases
     * (i.e. making companies available, as in 1835 and 1837).
     */
    protected final IntegerState companyReleaseStep = IntegerState.create(this, "releaseStep", 0);

    /**
     * @return the revenueSpinnerIncrement
     */
    public int getRevenueSpinnerIncrement() {
        return revenueSpinnerIncrement;
    }

    private IntegerState actionCount = IntegerState.create(this, "actionCount");

    public GameManager(RailsRoot parent, String id) {
        super(parent, id);

        this.guiHints = new GuiHints(this, "guiHints");
        // Install the Focus Spy immediately upon creation to catch startup focus issues
        installFocusSpy();
    }

    /**
     * A diagnostic "Spy" that logs all focus and window activation changes.
     * Use this to identify which component is stealing focus during Stock Rounds.
     */
    private void installFocusSpy() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner",
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        Component oldC = (Component) evt.getOldValue();
                        Component newC = (Component) evt.getNewValue();

                        String oldName = (oldC != null) ? oldC.getClass().getSimpleName() + "[" + oldC.getName() + "]"
                                : "null";
                        String newName = (newC != null) ? newC.getClass().getSimpleName() + "[" + newC.getName() + "]"
                                : "null";

                        // log.info("FOCUS SPY [Component]: LOST={} | GAINED={}", oldName, newName);

                        // Optional: Print stack trace if the focus shift looks suspicious (e.g. to a
                        // map component)
                        // if (newName.contains("MapPanel") || newName.contains("ORPanel")) {
                        // Thread.dumpStack();
                        // }
                    }
                });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow",
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        Window oldW = (Window) evt.getOldValue();
                        Window newW = (Window) evt.getNewValue();

                        String oldName = (oldW != null) ? oldW.getClass().getSimpleName() + "[" + oldW.getName() + "]"
                                : "null";
                        String newName = (newW != null) ? newW.getClass().getSimpleName() + "[" + newW.getName() + "]"
                                : "null";

                        // log.info("FOCUS SPY [Window]: DEACTIVATED={} | ACTIVATED={}", oldName,
                        // newName);
                    }
                });
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Get the rails.game name as configured */
        Tag gameTag = tag.getChild("Game");
        if (gameTag == null)
            throw new ConfigurationException(
                    "No Game tag specified in GameManager tag");

        gameName = gameTag.getAttributeAsString("name");
        // TODO (Rails 2.0): Check if this still works and is still needed
        if (gameName == null) {
            throw new ConfigurationException("No gameName specified in Game tag");
        }
        if (!gameName.equals(getRoot().getGameName())) {
            throw new ConfigurationException("Deviating gameName specified in Game tag");
        }

        initGameParameters();

        Tag gameParmTag = tag.getChild("GameParameters");
        if (gameParmTag != null) {

            // StockRound class and other properties
            Tag srTag = gameParmTag.getChild("StockRound");
            if (srTag != null) {
                // FIXME: Rails 2.0, move this to some default .xml!
                String srClassName = srTag.getAttributeAsString("class", "net.sf.rails.game.financial.StockRound");
                try {
                    stockRoundClass = Class.forName(srClassName).asSubclass(StockRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + srClassName, e);
                }
                String stockRoundSequenceRuleString = srTag.getAttributeAsString("sequence");
                if (Util.hasValue(stockRoundSequenceRuleString)) {
                    if ("SellBuySell".equalsIgnoreCase(stockRoundSequenceRuleString)) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY_SELL);
                    } else if ("SellBuy".equalsIgnoreCase(stockRoundSequenceRuleString)) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY);
                    } else if ("SellBuyOrBuySell".equalsIgnoreCase(stockRoundSequenceRuleString)) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY_OR_BUY_SELL);
                    }
                }

                skipFirstStockRound = srTag.getAttributeAsBoolean("skipFirst",
                        skipFirstStockRound);

                for (String ruleTagName : srTag.getChildren().keySet()) {
                    switch (ruleTagName) {
                        case "NoSaleInFirstSR":
                            setGameParameter(GameDef.Parm.NO_SALE_IN_FIRST_SR, true);
                            break;
                        case "NoSaleIfNotOperated":
                            setGameParameter(GameDef.Parm.NO_SALE_IF_NOT_OPERATED, true);
                            break;
                        case "NoSaleOfJustBoughtShare":
                            setGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT, true);
                            break;
                        case "NoSaleOfJustStartedCompany":
                            setGameParameter(GameDef.Parm.NO_SALE_OF_JUST_STARTED_COMPANY, true);
                            break;
                        case "NoCertificateSplitOnSelling":
                            setGameParameter(GameDef.Parm.NO_CERTIFICATE_SPLIT_ON_SELLING, true);
                            break;
                    }

                }
            }

            // OperatingRound class
            Tag orTag = gameParmTag.getChild("OperatingRound");
            if (orTag != null) {
                // FIXME: Rails 2.0, move this to some default .xml!
                String orClassName = orTag.getAttributeAsString("class", "net.sf.rails.game.OperatingRound");
                try {
                    operatingRoundClass = Class.forName(orClassName).asSubclass(
                            OperatingRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + orClassName, e);
                }

                Tag orderTag = orTag.getChild("OperatingOrder");
                if (orderTag != null) {
                    dynamicOperatingOrder = orderTag.getAttributeAsBoolean("dynamic",
                            dynamicOperatingOrder);
                }

                Tag emergencyTag = orTag.getChild("EmergencyTrainBuying");
                if (emergencyTag != null) {
                    setGameParameter(GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN,
                            emergencyTag.getAttributeAsBoolean("mustBuyCheapestTrain",
                                    GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN,
                            emergencyTag.getAttributeAsBoolean("mayAlwaysBuyNewTrain",
                                    GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY,
                            emergencyTag.getAttributeAsBoolean("mayBuyFromCompany",
                                    GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MAY_ADD_PRES_CASH_FROM_COMPANY,
                            emergencyTag.getAttributeAsBoolean("mayAddPresCashFromCompany",
                                    GameDef.Parm.EMERGENCY_MAY_ADD_PRES_CASH_FROM_COMPANY.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY,
                            emergencyTag.getAttributeAsBoolean("companyBankruptcy",
                                    GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES,
                            emergencyTag.getAttributeAsBoolean("mustSellTreasuryShares",
                                    GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MUST_TAKE_LOANS,
                            emergencyTag.getAttributeAsBoolean("mustTakeLoans",
                                    GameDef.Parm.EMERGENCY_MUST_TAKE_LOANS.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE,
                            emergencyTag.getAttributeAsBoolean("mustBuyTrainEvenIfNoRoute",
                                    GameDef.Parm.MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE.defaultValueAsBoolean()));
                }

                Tag revenueIncrementTag = orTag.getChild("RevenueIncrement");
                if (revenueIncrementTag != null) {
                    revenueSpinnerIncrement = revenueIncrementTag.getAttributeAsInteger("amount", 10);
                }
            }

            // ShareSellingRound class
            Tag ssrTag = gameParmTag.getChild("ShareSellingRound");
            if (ssrTag != null) {
                // FIXME: Rails 2.0, move this to some default .xml!
                String ssrClassName = ssrTag.getAttributeAsString("class",
                        "net.sf.rails.game.financial.ShareSellingRound");
                try {
                    shareSellingRoundClass = Class.forName(ssrClassName).asSubclass(ShareSellingRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + ssrClassName, e);
                }
            }

            // TreasuryShareRound class
            Tag tsrTag = gameParmTag.getChild("TreasuryShareRound");
            if (tsrTag != null) {
                String tsrClassName = tsrTag.getAttributeAsString("class",
                        "net.sf.rails.game.financial.TreasuryShareRound");
                try {
                    treasuryShareRoundClass = Class.forName(tsrClassName).asSubclass(TreasuryShareRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + tsrClassName, e);
                }
            }

            /* Max. % of shares of one company that a player may hold */
            Tag shareLimitTag = gameParmTag.getChild("PlayerShareLimit");
            if (shareLimitTag != null) {
                setGameParameter(GameDef.Parm.PLAYER_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.PLAYER_SHARE_LIMIT.defaultValueAsInt()));
            }

            /* Max. % of shares of one company that the bank pool may hold */
            Tag poolLimitTag = gameParmTag.getChild("BankPoolShareLimit");
            if (poolLimitTag != null) {
                setGameParameter(GameDef.Parm.POOL_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.POOL_SHARE_LIMIT.defaultValueAsInt()));
            }

            /* Max. % of own shares that a company treasury may hold */
            Tag treasuryLimitTag = gameParmTag.getChild("TreasuryShareLimit");
            if (treasuryLimitTag != null) {
                setGameParameter(GameDef.Parm.TREASURY_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.TREASURY_SHARE_LIMIT.defaultValueAsInt()));
            }

            Tag bankruptcyTag = gameParmTag.getChild("Bankruptcy");
            if (bankruptcyTag != null) {
                String bankruptcyStyle = bankruptcyTag.getAttributeAsString("style", "DEFAULT");
                Bankruptcy.Style styleObject = Bankruptcy.Style.valueOf(bankruptcyStyle);
                if (styleObject != null) {
                    setGameParameter(GameDef.Parm.BANKRUPTCY_STYLE, styleObject);
                }
            }
        }

        /* End of rails.game criteria */
        Tag endOfGameTag = tag.getChild("EndOfGame");
        if (endOfGameTag != null) {
            Tag forcedSellingTag = endOfGameTag.getChild("ForcedSelling");
            if (forcedSellingTag != null) {
                forcedSellingCompanyDump = forcedSellingTag.getAttributeAsBoolean("CompanyDump", true);
            }
            if (endOfGameTag.getChild("Bankruptcy") != null) {
                gameEndsWithBankruptcy = true;
            }
            Tag bankBreaksTag = endOfGameTag.getChild("BankBreaks");
            if (bankBreaksTag != null) {
                gameEndsWhenBankHasLessOrEqual = bankBreaksTag.getAttributeAsInteger("limit",
                        gameEndsWhenBankHasLessOrEqual);
                String attr = bankBreaksTag.getAttributeAsString("finish");
                if ("setOfORs".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_SET_OF_ORS;
                } else if ("currentOR".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_THIS_OR;
                }
            }
            if (endOfGameTag.getChild("Bankruptcy") != null) {
                gameEndsWithBankruptcy = true;
            }
            Tag maxPriceTag = endOfGameTag.getChild("MaxPriceReached");
            if (maxPriceTag != null) {
                String attr = maxPriceTag.getAttributeAsString("finish");
                if ("setOfORs".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_SET_OF_ORS;
                } else if ("currentOR".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_THIS_OR;
                }
            }
            Tag maxRoundsTag = endOfGameTag.getChild("MaxRoundsReached");
            if (maxRoundsTag != null) {
                maxRounds = maxRoundsTag.getAttributeAsInteger("number", 0);
            }
        }

        Tag guiClassesTag = tag.getChild("GuiClasses");
        if (guiClassesTag != null) {

            // GameUIManager class
            Tag gameUIMgrTag = guiClassesTag.getChild("GameUIManager");
            if (gameUIMgrTag != null) {
                gameUIManagerClassName = gameUIMgrTag.getAttributeAsString("class", gameUIManagerClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(gameUIManagerClassName);
            }

            // ORUIManager class
            Tag orMgrTag = guiClassesTag.getChild("ORUIManager");
            if (orMgrTag != null) {
                orUIManagerClassName = orMgrTag.getAttributeAsString("class", orUIManagerClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(orUIManagerClassName);
            }

            // GameStatus class
            Tag gameStatusTag = guiClassesTag.getChild("GameStatus");
            if (gameStatusTag != null) {
                gameStatusClassName = gameStatusTag.getAttributeAsString("class", gameStatusClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(gameStatusClassName);
            }

            // StatusWindow class
            Tag statusWindowTag = guiClassesTag.getChild("StatusWindow");
            if (statusWindowTag != null) {
                statusWindowClassName = statusWindowTag.getAttributeAsString("class",
                        statusWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(statusWindowClassName);
            }

            // ORWindow class
            Tag orWindowTag = guiClassesTag.getChild("ORWindow");
            if (orWindowTag != null) {
                orWindowClassName = orWindowTag.getAttributeAsString("class", orWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(orWindowClassName);
            }

            // StartRoundWindow class
            Tag startRoundWindowTag = guiClassesTag.getChild("StartRoundWindow");
            if (startRoundWindowTag != null) {
                startRoundWindowClassName = startRoundWindowTag.getAttributeAsString("class",
                        startRoundWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(startRoundWindowClassName);
            }
        }
    }

    /* WARNING: required but never never called */
    public void finishConfiguration(RailsRoot root) {
    }

    public void init() {
        showCompositeORNumber = !"simple".equalsIgnoreCase(Config.get("or.number_format"));
    }

    public void startGame() {
        setGuiParameters();
        getRoot().getCompanyManager().initStartPackets(this);

        if (isTimeManagementEnabled()) {
            initializePlayerTimeBanks();
        }

        // Initialize session timestamp for autosaves
        this.sessionStartTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        beginStartRound();
        markRoundBoundary();
    }

    public boolean isDynamicOperatingOrder() {
        return dynamicOperatingOrder;
    }

    public PossibleActions getPossibleActions() {
        return possibleActions;
    }

    protected void setGuiParameters() {
        CompanyManager cm = getRoot().getCompanyManager();

        for (PublicCompany company : cm.getAllPublicCompanies()) {
            if (company.hasParPrice())
                guiParameters.put(GuiDef.Parm.HAS_ANY_PAR_PRICE, true);
            if (company.canBuyPrivates())
                guiParameters.put(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES, true);
            if (company.canHoldOwnShares())
                guiParameters.put(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES, true);
            if (company.getMaxNumberOfLoans() != 0)
                guiParameters.put(GuiDef.Parm.HAS_ANY_COMPANY_LOANS, true);
            if (company.getShareUnitSizes().size() > 1)
                guiParameters.put(GuiDef.Parm.HAS_GROWING_NUMBER_OF_SHARES, true);
            if (company.hasBonds())
                guiParameters.put(GuiDef.Parm.HAS_BONDS, true);
        }

        loop: for (PrivateCompany company : cm.getAllPrivateCompanies()) {
            for (SpecialProperty sp : company.getSpecialProperties()) {
                if (sp instanceof SpecialBonusTokenLay || sp instanceof SellBonusToken) {
                    guiParameters.put(GuiDef.Parm.DO_BONUS_TOKENS_EXIST, true);
                    break loop;
                }
            }

        }

        // define guiParameters from gameOptions
        if (GameOption.getAsBoolean(this, "NoMapMode")) {
            guiParameters.put(GuiDef.Parm.NO_MAP_MODE, true);
            guiParameters.put(GuiDef.Parm.ROUTE_HIGHLIGHT, false);
            guiParameters.put(GuiDef.Parm.REVENUE_SUGGEST, false);
        } else {
            if ("Highlight".equalsIgnoreCase(GameOption.getValue(this, "RouteAwareness"))) {
                guiParameters.put(GuiDef.Parm.ROUTE_HIGHLIGHT, true);
            }
            if ("Suggest".equalsIgnoreCase(GameOption.getValue(this, "RevenueCalculation"))) {
                guiParameters.put(GuiDef.Parm.REVENUE_SUGGEST, true);
            }
        }
        // Read the global restriction for train trading
        String restrictOption = GameOption.getValue(this, "RestrictTrainTradingToSameOwner");
        if (restrictOption != null) {
            restrictTrainTradingToSameOwner = GameOption.OPTION_VALUE_YES.equalsIgnoreCase(restrictOption);
            log.info("Game Option: Train Trading restricted to same-owner companies set to: " + restrictOption);
        } else {
            restrictTrainTradingToSameOwner = true;
            log.info("NO OPTION!!! Game Option: Train Trading restricted to same-owner companies set to default: "
                    + restrictTrainTradingToSameOwner);
        }

    }

    public boolean isRestrictTrainTradingToSameOwner() {
        return restrictTrainTradingToSameOwner;
    }

    private void initGameParameters() {
        for (GameDef.Parm parm : GameDef.Parm.values()) {
            gameParameters.put(parm, parm.defaultValue());
        }
    }

    public void setRound(RoundFacade round) {
        String oldRound = (currentRound.value() != null) ? currentRound.value().getId() : "null";
        String newRound = (round != null) ? round.getId() : "null";

        currentRound.set(round);
    }

    public void setInterruptedRound(RoundFacade interruptedRound) {
        this.interruptedRound.set(interruptedRound);
    }

    public RoundFacade getInterruptedRound() {
        return interruptedRound.value();
    }

    private int loopGuardActionId = -1;
    private int loopGuardCounter = 0;

    public void nextRound(Round round) {

        // Infinite Loop Guard ---
        // Check if we are cycling rounds without processing actions.
        // We MUST disable this check during reload/replay, because the action counter
        // logic behaves differently and speed is high, causing false positives.
        if (!isReloading()) {
            if (this.absoluteActionCounter == loopGuardActionId) {
                loopGuardCounter++;
                if (loopGuardCounter > 20) {
                    String error = "CRITICAL: Infinite Loop detected in Round Transition. \n" +
                            "The Game Engine is cycling rounds without accepting input. \n" +
                            "Last Round: " + (round != null ? round.getId() : "null");
                    log.error(error);
                    DisplayBuffer.add(this, error);
                    // Throwing RuntimeException breaks the loop and prevents the UI hang
                    throw new RuntimeException(error);
                }
            } else {
                loopGuardActionId = this.absoluteActionCounter;
                loopGuardCounter = 0;
            }
        }

        if (round instanceof StartRound) {
            if (((StartRound) round).getStartPacket().areAllSold()) { // This start Round was completed
                StartPacket nextStartPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
                if (nextStartPacket == null) {
                    if (skipFirstStockRound) {
                        Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();

                        if (currentPhase != null) {
                            numOfORs.set(currentPhase.getNumberOfOperatingRounds());
                        } else {
                            log.error("CRITICAL: StockRound ending but PhaseManager has NO current phase.");
                            // Fallback to 1 OR if everything is broken
                            numOfORs.set(1);
                        }

                        log.info("Phase={} ORs={}", currentPhase.toText(), numOfORs);

                        // Create a new OperatingRound (never more than one Stock Round)
                        // OperatingRound.resetRelativeORNumber();

                        relativeORNumber.set(0);
                        startOperatingRound(true);
                    } else {
                        startStockRound();
                    }
                } else {
                    beginStartRound();
                }
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
        } else if (round instanceof StockRound) {
            Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
            if (currentPhase == null)
                // log.error("Current Phase is null??", new Exception(""));
                numOfORs.set(currentPhase.getNumberOfOperatingRounds());
            // log.debug("Phase={} ORs={}", currentPhase.toText(), numOfORs);

            // Create a new OperatingRound (never more than one Stock Round)
            // OperatingRound.resetRelativeORNumber();
            capturePlayerWorthSnapshot(round.getId());
            captureCompanyPayoutSnapshot(round.getId());
            relativeORNumber.set(0);
            startOperatingRound(true);

        } else if (round instanceof OperatingRound) {
            if (gameOverPending.value() && gameEndWhen == GameEnd.AFTER_THIS_OR) {

                finishGame();

                // Use operatingRoundLimit instead of numOfORs to prevent mid-cycle extension
            } else if (relativeORNumber.value() < operatingRoundLimit.value()) {
                // There will be another OR
                capturePlayerWorthSnapshot(round.getId());
                captureCompanyPayoutSnapshot(round.getId());
                startOperatingRound(true);
            } else if (getRoot().getCompanyManager().getNextUnfinishedStartPacket() != null) {
                beginStartRound();
            } else {
                // Before starting the new Stock Round
                capturePlayerWorthSnapshot(round.getId());
                captureCompanyPayoutSnapshot(round.getId());
                if (gameOverPending.value() && gameEndWhen == GameEnd.AFTER_SET_OF_ORS) {
                    finishGame();
                } else {

                    ((OperatingRound) round).checkForeignSales();
                    startStockRound();
                }
            }
        } else if (round instanceof net.sf.rails.game.specific._1817.AuctionRound_1817) {
            // Resume the Stock Round that was interrupted by the IPO auction
            net.sf.rails.game.round.RoundFacade roundToResume = getInterruptedRound();
            setInterruptedRound(null);
            setRound(roundToResume);
            if (roundToResume != null) {
                roundToResume.resume();
            }
        }
    }

    protected void beginStartRound() {
        StartPacket startPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();

        // check if there are still unfinished startPackets
        if (startPacket != null) {
            // set this to the current startPacket
            this.startPacket.set(startPacket);
            // start a new StartRound
            createStartRound(startPacket);
        } else {
            // otherwise
            startStockRound();
        }
    }

    protected void createStartRound(StartPacket startPacket) {
        String startRoundClassName = startPacket.getRoundClassName();
        startRoundNumber.add(1);
        StartRound startRound = createRound(startRoundClassName,
                "IR_" + startRoundNumber.value());
        startRound.start();
    }

    /**
     * Stub, to be overridden if companies can run before the Start Packet has been
     * completely sold
     * (as in 1835). Also see further comments at the overriding method in
     * GameManager_18Scan.
     *
     * @return true if companies can run regardless. Default false.
     */
    protected boolean runIfStartPacketIsNotCompletelySold() {
        return false;
    }

    protected void startStockRound() {
        stockRoundNumber.add(1);
        clearStatusMessage(); // Reset Blue Text
        StockRound sr = createRound(stockRoundClass, "SR_" + stockRoundNumber.value());
        currentSRorOR.set(sr);

        // Update numOfORs before the OR cycle starts
        Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
        if (currentPhase != null) {
            numOfORs.set(currentPhase.getNumberOfOperatingRounds());
            operatingRoundLimit.set(numOfORs.value());
        }

if (isTimeManagementEnabled()) {
            if (!isReloading()) {
                for (Player p : getRoot().getPlayerManager().getPlayers()) {
                    grantTimeBonus(p, sr.getId(), getTimeMgmtShareRoundIncrement());
                }
            }
        }
        sr.start();
    }

    protected void startOperatingRound(boolean operate) {
        // log.debug("Operating round started with operate-flag={}", operate);
        String orId;
        clearStatusMessage(); // Reset Blue Text
        if (operate) {
            absoluteORNumber.add(1);
            if (showCompositeORNumber) {
                relativeORNumber.add(1);
                orId = "OR_" + stockRoundNumber.value() + "." + relativeORNumber.value();
            } else {
                orId = "OR_" + absoluteORNumber.value();
            }
        } else {
            relativeORNumber.add(1);
            orId = "OR_0." + relativeORNumber.value();
        }
        OperatingRound or = createRound(operatingRoundClass, orId);
        // if (operate) absoluteORNumber.add(1);
        currentSRorOR.set(or);
        // Explicitly record private company revenue at the start of the OR.
        // This ensures the Payout Chart captures fixed income (e.g. PfB's 15M).
        if (operate) {
            recordPrivateRevenue();
        }

        // log.info("[STATE] Starting Operating Round {} (Abs: {}, Rel: {})", orId,
        // absoluteORNumber.value(), relativeORNumber.value()); // 'essential logging'
        // keep!
        or.start();
    }

    // ... (lines of unchanged context code) ...
    public <T extends RoundFacade> T createRound(String roundClassName, String id) {
        // log.error("--- GM.createRound(String, String) CALLED. ClassName: {}, ID: {}",
        // roundClassName, id);
        T round = null;
        try {
            // 1. Load the class object from the string name
            Class<? extends RoundFacade> roundClass = Class.forName(roundClassName).asSubclass(RoundFacade.class);

            // 2. Call Configure.create with the specific CLASS object,
            // not the abstract StartRound.class
            round = Configure.create((Class<T>) roundClass, GameManager.class, this, id);

        } catch (Exception e) {
            log.error("CRITICAL: Failed to create round " + roundClassName, e);
            throw new RuntimeException("Failed to create round: " + roundClassName, e);
        }
        setRound(round);
        return round;
    }

    // FIXME: We need an ID!
    public <T extends RoundFacade> T createRound(Class<T> roundClass, String id) {

        // Force 1817 specific Operating Round if the game is 1817
        if (getGameName().equals("1817") && roundClass.equals(OperatingRound.class)) {
            roundClass = (Class<T>) net.sf.rails.game.specific._1817.OperatingRound_1817.class;
        }

        T round = null;
        try {
            round = Configure.create(roundClass, GameManager.class, this, id);

        } catch (Exception e) {
            log.error("CRITICAL: Failed to create round " + roundClass.getName(), e);
            throw new RuntimeException("Failed to create round: " + roundClass.getName(), e);
        }
        setRound(round);
        return round;
    }

    public void newPhaseChecks(RoundFacade round) {

    }

    public String getGameName() {
        return gameName;
    }

    public void reportAllPlayersPassed() {
        ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
        firstAllPlayersPassed.set(true);
    }

    public boolean getFirstAllPlayersPassed() {
        return firstAllPlayersPassed.value();
    }

    public String getORId() {
        if (showCompositeORNumber) {
            return getCompositeORNumber();
        } else {
            return String.valueOf(absoluteORNumber.value());
        }
    }

    public int getAbsoluteORNumber() {
        return absoluteORNumber.value();
    }

    public String getCompositeORNumber() {
        return stockRoundNumber.value() + "." + relativeORNumber.value();
    }

    public int getRelativeORNumber() {
        return relativeORNumber.value();
    }

    public String getNumOfORs() {
        return numOfORs.toText();
    }

    public void setNumberOfOperatingRounds(int number) {
        // Use currentSRorOR instead of currentRound.
        // In 1835, buying a 5-train triggers the PFR (Prussian Formation Round)
        // immediately.
        // If we check currentRound, it sees PFR (not an OperatingRound) and allows the
        // update.
        // currentSRorOR remembers we are in an Operating context regardless of
        // interruptions.
        if (currentSRorOR.value() instanceof OperatingRound) {
            return;
        }

        if (numOfORs.value() != number) {
            numOfORs.set(number);
        }
    }

    public int getNumberOfOperatingRounds() {
        return numOfORs.value();
    }

    public int getStartRoundNumber() {
        return startRoundNumber.value();
    }

    public int getSRNumber() {
        return stockRoundNumber.value();
    }

    public void startShareSellingRound(Player player, int cashToRaise,
            PublicCompany cashNeedingCompany, boolean problemDumpOtherCompanies) {

        interruptedRound.set(getCurrentRound());

        // An id based on interruptedRound and company id
        String id = "SSR_" + getInterruptedRound().getId() + "_" + cashNeedingCompany.getId() + "_" + cashToRaise;

        // check if other companies can be dumped
        createRound(shareSellingRoundClass, id).start(
                getInterruptedRound(), player, cashToRaise, cashNeedingCompany,
                !problemDumpOtherCompanies || forcedSellingCompanyDump);
        // the last parameter indicates if the dump of other companies is allowed,
        // either this is explicit or
        // the action does not require that check
    }

    public void startTreasuryShareTradingRound(PublicCompany company) {
        interruptedRound.set(getCurrentRound());
        String id = "TreasuryShareRound_" + getInterruptedRound().getId() + "_" + company.getId();
        createRound(treasuryShareRoundClass, id).start(getInterruptedRound());
    }

    /**
     * EMERGENCY DEBUG TOOL: Forces the Operating Round to jump to the next company.
     * Use this if the game hangs on a closed company (Zombie Company).
     */
    public void forceSkipStuckCompany() {
        RoundFacade current = getCurrentRound();
        if (current == null || !(current instanceof OperatingRound)) {
            return;
        }

        try {
            OperatingRound or = (OperatingRound) current;

            // 1. Access the internal list of operating companies
            // We use the getter provided in OperatingRound to avoid reflection if possible,
            // otherwise we assume 'operatingCompanies' is accessible or use reflection.
            List<PublicCompany> companies = or.getOperatingCompanies();
            PublicCompany stuckComp = or.getOperatingCompany();

            if (companies == null || companies.isEmpty()) {
                return;
            }

            // 2. Find current index
            int currentIndex = companies.indexOf(stuckComp);

            // 3. Find next valid company
            int nextIndex = currentIndex;
            int attempts = 0;
            PublicCompany nextComp = null;

            while (attempts < companies.size()) {
                nextIndex = (nextIndex + 1) % companies.size();
                PublicCompany candidate = companies.get(nextIndex);
                if (!candidate.isClosed() && candidate.hasFloated()) {
                    nextComp = candidate;
                    break;
                }
                attempts++;
            }

            if (nextComp != null) {

                // 4. Force state update using Reflection to bypass 'private' access if needed,
                // or just call the protected method if we were in the same package (we aren't).
                // Since we are in GameManager, we might need Reflection to set
                // 'operatingCompany'
                // if there isn't a public setter. OperatingRound has 'setOperatingCompany' but
                // it is protected.

                java.lang.reflect.Method setMethod = OperatingRound.class.getDeclaredMethod("setOperatingCompany",
                        PublicCompany.class);
                setMethod.setAccessible(true);
                setMethod.invoke(or, nextComp);

                // 5. Reset Step
                java.lang.reflect.Method setStepMethod = OperatingRound.class.getDeclaredMethod("setStep",
                        GameDef.OrStep.class);
                setStepMethod.setAccessible(true);
                setStepMethod.invoke(or, GameDef.OrStep.INITIAL);

                // 6. Init Turn
                java.lang.reflect.Method initTurnMethod = OperatingRound.class.getDeclaredMethod("initTurn");
                initTurnMethod.setAccessible(true);
                initTurnMethod.invoke(or);

                DisplayBuffer.add(this, "FORCE SKIP SUCCESS: Advanced to " + nextComp.getId());
            } else {
            }

        } catch (Exception e) {
            DisplayBuffer.add(this, "Force Skip Failed: " + e.getMessage());
        }
    }

    public boolean process(PossibleAction action) {

        if (action != null && action.getRoot() == null) {
            action.setRoot(this.getRoot());
        }

        // EMERGENCY OVERRIDE: Check for "FORCE_SKIP" signal
        if (action != null && action.toString().contains("FORCE_SKIP")) {
            forceSkipStuckCompany();
            return true;
        }

        getRoot().getReportManager().getDisplayBuffer().clear();

        // -----------------------------------------------------------
        // 1. CRITICAL INTERCEPT: Navigation Actions (Undo/Redo)
        // -----------------------------------------------------------
        // This MUST happen before 'logActionTaken' or 'logAction'.
        // If we let it pass, the engine treats Undo as "Move #267",
        // increments the counter, and creates the "Death Spiral".

        if (action instanceof GameAction) {
            GameAction ga = (GameAction) action;
            GameAction.Mode mode = ga.getMode();

            if (mode == GameAction.Mode.UNDO ||
                    mode == GameAction.Mode.FORCED_UNDO ||
                    mode == GameAction.Mode.REDO) {

                // Clear hints to ensure clean UI rebuild
                guiHints.clearVisibilityHints();

                // Execute the undo/redo logic directly
                // This reloads the old state without incrementing the "Move Counter"
                boolean success = processGameActions(ga);

                // We must update the list of possible moves for the new state immediately.
                // Otherwise, the UI will have stale buttons (e.g., missing Redo).
                if (success) {
                    possibleActions.clear();

                    // Allow action generation check
                    boolean allowActionGeneration = !isGameOver() || (getCurrentRound() instanceof EndOfGameRound);

                    if (allowActionGeneration) {
                        if (getCurrentRound() != null) {
                            getCurrentRound().setPossibleActions();
                        }
                        if (!isGameOver()) {
                            setCorrectionActions();
                        }

                        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
                        if (changeStack.isUndoPossible()) {
                            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.FORCED_UNDO));
                        }
                        if (changeStack.isRedoPossible()) {
                            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.REDO));
                        }
                    }
                }

                // Force a UI refresh cycle immediately after the state change
                if (success && gameUIManager != null && gameUIManager.getStatusWindow() != null) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Object status = gameUIManager.getStatusWindow().getGameStatus();
                            if (status instanceof java.util.Observer) {
                                ((java.util.Observer) status).update(null, "ForceUpdate");
                            }
                        } catch (Exception e) {
                            log.warn("UI ForceUpdate failed after Undo", e);
                        }
                    });
                }

                // RETURN IMMEDIATELY. Do not fall through to logActionTaken().
                return success;
            }
        }
        // -----------------------------------------------------------

        // Capture the active entity (Player or Company) BEFORE processing the action.
        RoundFacade roundBefore = getCurrentRound();
        Object actorBefore = getCurrentPlayer();

        if (roundBefore instanceof OperatingRound) {
            actorBefore = ((OperatingRound) roundBefore).operatingCompany.value();
        }

        // Snapshot logging (for AI/Replay debugging)
        if (this.logOutputDirectory != null) {
            try {
                String filename = String.format("state_%05d.json", actionCount.value());
                File outputFile = new File(this.logOutputDirectory, filename);
                if (getCurrentRound() != null) {
                    net.sf.rails.game.ai.snapshot.JsonStateSerializer.serialize(this, outputFile.getAbsolutePath());
                }
            } catch (Exception e) {
                // Ignore snapshot errors
            }
        }

        guiHints.clearVisibilityHints();
        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
        boolean startGameAction = false;

        // --- INJECT PENALTY BEFORE LOGGING ---
        // Pre-load the accumulated penalty into the action so the log matches the final
        // payload.
        if (!(action instanceof GameAction)
                && !(action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.START_GAME)) {
            if (isTimeManagementEnabled()) {
                String pName = action.getPlayerName();
                if (pName != null && pendingTimePenalties != null && pendingTimePenalties.containsKey(pName)) {
                    int payload = action.getExecutionTimeSeconds();
                    payload += pendingTimePenalties.get(pName);
                    action.setExecutionTimeSeconds(payload);
                }
            }
        }

        // -----------------------------------------------------------
        // 2. STANDARD LOGGING (Only for real moves)
        // -----------------------------------------------------------
        // Since we returned early for Undo/Redo, code reaching here is a REAL move.

        logActionTaken(action); // Increments the official move counter (Move #265 -> #266)
        logAction(action); // Logs "Move #266: Laid Tile..."

        boolean isBuyTrainAction = action instanceof BuyTrain;
        boolean result = false;

        // -----------------------------------------------------------
        // 3. STANDARD PROCESSING
        // -----------------------------------------------------------

        if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.START_GAME) {
            startGameAction = true;
            result = true;

        } else if (action != null) {
            action.setActed();

            // Validation checks
            String actionPlayerName = action.getPlayerName();
            String currentPlayerName = getCurrentPlayer().getId();

            boolean nameMatch = actionPlayerName.equals(currentPlayerName);

            if (!nameMatch) {
                if (actionPlayerName.trim().equalsIgnoreCase(currentPlayerName.trim())) {
                    nameMatch = true;
                } else if (action.isAIAction()) {
                    nameMatch = true;
                }
            }

            if (!nameMatch) {
                DisplayBuffer.add(this, LocalText.getText("WrongPlayer", actionPlayerName, currentPlayerName));
                return false;
            }

            if (!possibleActions.validate(action)) {
                DisplayBuffer.add(this, LocalText.getText("ActionNotAllowed", action.toString()));
                // INSTRUMENTATION: Log failure
                log.error("GM: Action VALIDATION FAILED for: " + action.toString());
                debugLogPossibleActions(); // Show what was actually allowed
                return false;
            }

            // Process the action
            try {
                if (action instanceof GameAction) {
                    // This handles Save/Load/Export (Undo/Redo handled above)
                    GameAction gameAction = (GameAction) action;
                    result = processGameActions(gameAction);
                } else {
                    // Logic/Correction Actions
                    boolean correctionHandled = processCorrectionActions(action);
                    if (correctionHandled) {
                        result = true;
                    } else if (getCurrentRound() != null) {
                        result = getCurrentRound().process(action);
                    } else {
                        result = false;
                    }
                }

                // Increment absolute counter (legacy field)
                if (result && !(action instanceof GameAction)) {
                    this.absoluteActionCounter++;

                    if (action.hasActed()) {
                        if (action.getAbsoluteTimestamp() == 0) {
                            action.setAbsoluteTimestamp(System.currentTimeMillis());
                        }
                        executedActions.add(action);
                    }
                    updatePayoutTracker(action);

                    // Re-log strictly for history consistency if needed
                    logAction(action);

                }
            } catch (Exception e) {
                result = false;
                log.error("GM: process() Exception caught", e);
            }

        } else {
            result = true;
        }

        // BuyTrain logging cleanup
        if (isBuyTrainAction) {
            BuyTrain buyTrainAction = (BuyTrain) action;
            if (buyTrainAction.getCompany() instanceof PublicCompany) {
                PublicCompany company = (PublicCompany) buyTrainAction.getCompany();
            }
        }

        possibleActions.clear();

        // 4. POST-PROCESSING (Autosave, ChangeStack)
        boolean allowActionGeneration = !isGameOver() || (getCurrentRound() instanceof EndOfGameRound);

        if (allowActionGeneration) {

            getCurrentRound().setPossibleActions();

            if (result && !(action instanceof GameAction) && !(startGameAction)) {

                if (isTimeManagementEnabled()) {
                    String pName = action.getPlayerName();
                    Player p = getRoot().getPlayerManager().getPlayerByName(pName);

                    if (p != null) {
                        int payload = action.getExecutionTimeSeconds(); // Already includes pre-loaded penalty //
                                                                        // penalty
                        if (payload > 0) {
                            p.getTimeBankModel().add(-payload);
                        }
                    }
                    // Action succeeded, safe to clear the cache
                    if (pName != null && pendingTimePenalties != null) {
                        pendingTimePenalties.remove(pName);
                    }
                }

                changeStack.close(action);

                // Overwrite timeline: clear future boundaries that are no longer valid
                truncateRoundBoundaries(changeStack.getCurrentIndex());

                // Capture worth and payout snapshots per action
                String moveId = String.valueOf(absoluteActionCounter);
                capturePlayerWorthSnapshot(moveId);
                captureCompanyPayoutSnapshot(moveId);

                // Autosave logic
                RoundFacade roundAfter = getCurrentRound();
                Object actorAfter = getCurrentPlayer();

                if (roundAfter instanceof OperatingRound) {
                    actorAfter = ((OperatingRound) roundAfter).operatingCompany.value();
                }

                boolean roundChanged = (roundBefore != roundAfter);
                if (roundChanged) {
                    markRoundBoundary();
                }
                boolean actorChanged = (actorBefore == null && actorAfter != null) ||
                        (actorBefore != null && !actorBefore.equals(actorAfter));

                if (roundChanged || actorChanged) {
                    // Capture worth and payout snapshots at the end of each turn
                    String rId = (getCurrentRound() != null) ? getCurrentRound().getId() : "Start";
                    String snapId = "Move_" + actionCount.value() + ":" + rId;
                    capturePlayerWorthSnapshot(snapId);
                    captureCompanyPayoutSnapshot(snapId);

                    recoverySave();
                }

                if (Config.getBoolean("ai.save.state.on.move", false)) {
                    File stateDir = new File("logs/state");
                    if (!stateDir.exists())
                        stateDir.mkdirs();
                    try {
                        String filename = String.format("logs/state/state_%05d.json", this.absoluteActionCounter);
                        JsonStateSerializer.serialize(this, filename);
                    } catch (IOException e) {
                    }
                }
            }

            // FORCE UI UPDATE
            if (result && !startGameAction) {
                if (gameUIManager != null && gameUIManager.getStatusWindow() != null) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Object status = gameUIManager.getStatusWindow().getGameStatus();
                            if (status instanceof java.util.Observer) {
                                ((java.util.Observer) status).update(null, "ForceUpdate");
                            }
                        } catch (Exception e) {
                            log.warn("UI ForceUpdate failed", e);
                        }
                    });
                }
            }

            if (!isGameOver())
                setCorrectionActions();

            if (changeStack.isUndoPossible()) {
                possibleActions.add(new GameAction(getRoot(), GameAction.Mode.FORCED_UNDO));
            }
            if (changeStack.isRedoPossible()) {
                possibleActions.add(new GameAction(getRoot(), GameAction.Mode.REDO));
            }

        }

        return result;
    }

    protected void logActionTaken(PossibleAction action) {
        if (action instanceof NullAction
                && ((NullAction) action).getMode() == NullAction.Mode.START_GAME) {
        } else {
            actionCount.add(1);
        }
    }

    /**
     * Adds all Game actions
     * Examples are: undo/redo/corrections
     */
    private void setCorrectionActions() {

        // 1. Check if ANY Correction Manager is currently active.
        boolean anyCorrectionActive = false;
        for (CorrectionType ct : EnumSet.allOf(CorrectionType.class)) {
            CorrectionManager cm = getCorrectionManager(ct);
            if (cm.isActive()) {
                anyCorrectionActive = true;
                break;
            }
        }

        // 2. If a correction is active, clear the normal game actions.
        if (anyCorrectionActive) {
            possibleActions.clear();
        }

        // Correction Actions
        for (CorrectionType ct : EnumSet.allOf(CorrectionType.class)) {
            CorrectionManager cm = getCorrectionManager(ct);
            possibleActions.addAll(cm.createCorrections());
        }
    }

    private boolean processCorrectionActions(PossibleAction a) {

        boolean result = false;

        if (a instanceof CorrectionAction) {
            CorrectionAction ca = (CorrectionAction) a;
            CorrectionType ct = ca.getCorrectionType();
            CorrectionManager cm = getCorrectionManager(ct);
            result = cm.executeCorrection(ca);

            // Force state resynchronization after map corrections mid-turn.
            // This prevents stale route/revenue caches from rejecting subsequent actions.
            if (result) {
                boolean shouldFlush = false;
                if (ca instanceof rails.game.correct.MapCorrectionAction) {
                    shouldFlush = true;
                } else if (ca.getClass().getSimpleName().equals("CorrectionModeAction")) {
                    if (ct != CorrectionType.CORRECT_CASH && ct != CorrectionType.CORRECT_TRAINS) {
                        try {
                            java.lang.reflect.Method isActiveMethod = ca.getClass().getMethod("isActive");
                            Boolean isActive = (Boolean) isActiveMethod.invoke(ca);
                            if (!isActive) {
                                shouldFlush = true;
                            }
                        } catch (Exception e) {
                            shouldFlush = true;

                        }
                    }
                }

                if (shouldFlush) {
                    RoundFacade currentRound = getCurrentRound();
                    if (currentRound instanceof OperatingRound) {
                        // ((OperatingRound) currentRound).resetTransientStateOnLoad();
                        log.info(
                                "MAP CORRECTION: Skipped OperatingRound transient caches flush to prevent prematurely ending the round.");
                    } else if (currentRound instanceof StockRound) {
                        ((StockRound) currentRound).resetTransientStateOnLoad();
                    }
                }
            }

        }

        return result;
    }

    private boolean processGameActions(GameAction gameAction) {
        // Process undo/redo centrally
        boolean result = false;

        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
        int index = gameAction.getmoveStackIndex();

        // The player initiating the action is the one who should pay the penalty.
        String disruptorName = gameAction.getPlayerName();

        switch (gameAction.getMode()) {
            case SAVE:
                result = save(gameAction);
                break;
            case RELOAD:
                result = reload(gameAction);
                break;

            case UNDO:
            case FORCED_UNDO:
                // 1. BLIND THE UI (Pre-Undo)
                if (gameUIManager != null && getCurrentPlayer() != null) {
                    gameUIManager.resetTimeHistory(getCurrentPlayer().getIndex());
                }

                // 2. CAPTURE STATE BEFORE UNDO
                Player playerBefore = getCurrentPlayer();
                Player initiator = gameAction.getPlayer();
                int timeSpent = gameAction.getExecutionTimeSeconds();
                // 2.5 CAPTURE PENALTY FROM ALL ABORTED ACTIONS
                if (pendingTimePenalties == null) {
                    pendingTimePenalties = new HashMap<>();
                }
                if (!executedActions.isEmpty()) {
                    int targetSize = (index == -1) ? executedActions.size() - 1 : index;
                    targetSize = Math.max(0, targetSize);

                    // Aggregate penalties for all moves being undone (handles multi-undo and
                    // bypassed AI moves)
                    for (int i = executedActions.size() - 1; i >= targetSize; i--) {
                        PossibleAction abortedAction = executedActions.get(i);
                        String pName = abortedAction.getPlayerName();
                        int timeSpentOnAborted = abortedAction.getExecutionTimeSeconds();
                        if (pName != null && timeSpentOnAborted > 0) {
                            pendingTimePenalties.put(pName,
                                    pendingTimePenalties.getOrDefault(pName, 0) + timeSpentOnAborted);
                        }
                    }
                }

                // 3. EXECUTE UNDO (Restores state, including Victim's time)
                if (index == -1) {
                    changeStack.undo();
                } else {
                    changeStack.undo(index);
                }

                // Truncate orphaned future states
                truncateStateLogs(actionCount.value());

                // 4. CAPTURE STATE AFTER UNDO
                Player playerAfter = getCurrentPlayer();

                if (gameUIManager != null && playerAfter != null) {

                    // Sync UI to the restored time immediately
                    int restoredTime = playerAfter.getTimeBankModel().value();
                    gameUIManager.setPlayerTimeAfterUndo(playerAfter.getIndex(), restoredTime);
                }

                result = true;
                break;

            case REDO:
                if (changeStack.isRedoPossible()) {

                    if (pendingTimePenalties != null && getCurrentPlayer() != null) {
                        pendingTimePenalties.remove(getCurrentPlayer().getName());
                    }

                    if (index == -1) {
                        changeStack.redo();
                    } else {
                        changeStack.redo(index);
                    }
                    result = true;
                } else {
                    log.warn("Ignored REDO request: ChangeStack indicates Redo is not possible.");
                    result = false;
                }
                break;
            case EXPORT:
                result = export(gameAction);
                break;
            case LOAD:
                break;
            case NEW:
                break;

            default:
                break;
        }

        return result;
    }

    public void finishLoading() {
        guiHints.clearVisibilityHints();
    }

    /**
     * recoverySave method
     * Uses filePath defined in save.recovery.filepath
     */
    protected void recoverySave() {
        if (isGameOver())
            return;

        if (Config.get("save.recovery.active", "yes").equalsIgnoreCase("no"))
            return;

        // Ensure session timestamp exists (generates a new session ID for loaded games)
        if (this.sessionStartTimestamp == null) {
            this.sessionStartTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        }

        // Determine base directory
        String path = Config.get("save.recovery.filepath");
        File baseDir;
        if (Util.hasValue(path)) {
            baseDir = new File(path).getParentFile();
        } else {
            baseDir = new File("autosave");
        }
        if (baseDir == null)
            baseDir = new File(".");

        // Create game-specific subfolder (e.g., autosave/1817)
        File dir = new File(baseDir, getGameName());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Filename uses the session timestamp to group saves from the exact same play
        // session
        String dateStr = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String filename = String.format("%s_%s_%05d.rails", getGameName(), this.sessionStartTimestamp,
                actionCount.value());
        File saveFile = new File(dir, filename);

        GameSaver gameSaver = new GameSaver(getRoot().getGameData(), executedActions.view());
        try {
            // Save the new file
            gameSaver.saveGame(saveFile);

            // Delete old autosave files ONLY for THIS SPECIFIC session within the subfolder
            final String sessionPrefix = getGameName() + "_" + this.sessionStartTimestamp + "_";
            File[] oldFiles = dir.listFiles(
                    (d, name) -> name.startsWith(sessionPrefix) &&
                            (name.endsWith(".rails") || name.endsWith(".json")) &&
                            !name.equals(filename) &&
                            !name.equals(filename + ".state.json"));

            if (oldFiles != null) {
                for (File f : oldFiles) {
                    f.delete();
                }
            }

            // Automatically persist UI window states during the recovery autosave cycle
            if (getGameUIManager() != null) {
                getGameUIManager().saveWindowSettings(getGameName());
            }

            recoverySaveWarning = false;
        } catch (IOException e) {
            // suppress warning after first occurrence
            if (!recoverySaveWarning) {
                DisplayBuffer.add(this, LocalText.getText("RecoverySaveFailed", e.getMessage()));
                recoverySaveWarning = true;
            }
        }
    }

    protected boolean save(GameAction saveAction) {
        GameSaver gameSaver = new GameSaver(getRoot().getGameData(), executedActions.view());
        File file = new File(saveAction.getFilepath());
        try {
            gameSaver.saveGame(file);

            // Save UI Window Positions & Scale
            // Save UI Window Positions & Scale, passing the Game Name
            if (getGameUIManager() != null) {
                getGameUIManager().saveWindowSettings(getGameName());
            }

        } catch (IOException e) {
            DisplayBuffer.add(this, LocalText.getText("SaveFailed", e.getMessage()));
            return false;
        }

        boolean archive = Config.getBoolean(ARCHIVE_ENABLED, false);
        if (archive) {
            int count = Config.getInt(ARCHIVE_KEEP_COUNT, 5);
            if (count < 1) {
                count = 1;
            }

            String archiveDir = Config.get(ARCHIVE_DIRECTORY);
            if (StringUtils.isBlank(archiveDir)) {
                // default to "archive"
                archiveDir = "archive";
            }
            if (!archiveDir.startsWith(File.separator)) {
                // it should be relative to the current saved files
                archiveDir = file.getParent() + File.separator + archiveDir;
            }

            File archiveDirFile = new File(archiveDir);
            if (!archiveDirFile.exists()) {
                // create it
                try {
                    Files.createDirectories(Path.of(archiveDir, File.separator, "dummy"));
                } catch (IOException e) {
                    archive = false;
                }
            } else if (archiveDirFile.exists() && !archiveDirFile.isDirectory()) {
                archive = false;
            }

            if (archive) {
                // iterate through files in current directory
                SortedSet<File> files = new TreeSet<>((a, b) -> ComparisonChain.start()
                        .compare(b.lastModified(), a.lastModified())
                        .result());

                for (File entry : file.getParentFile().listFiles()) {
                    if (entry.isFile()) {
                        String ext = StringUtils.substringAfterLast(entry.getName(), ".");
                        boolean doInclude = GameUIManager.DEFAULT_SAVE_EXTENSION.equals(ext);
                        // TODO: verify it matches out expected file name format
                        if (doInclude) {
                            files.add(entry);
                        }
                    }
                }
                if (files.size() > count) {
                    File[] fileList = files.toArray(new File[] {});
                    for (int i = count; i < fileList.length; i++) {
                        File toMove = fileList[i];
                        File destFile = new File(archiveDir + File.separator + toMove.getName());
                        if (!toMove.renameTo(destFile)) {

                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * tries to reload the current game
     * executes the additional action(s)
     */
    // ... (lines of unchanged context code) ...
    /**
     * tries to reload the current game
     * executes the additional action(s)
     */
    protected boolean reload(GameAction reloadAction) {

        /* Use gameLoader to load the game data */
        GameLoader gameLoader = new GameLoader();
        String filepath = reloadAction.getFilepath();

        File file = new File(filepath);
        if (!file.exists()) {
            System.out.println("file not found");
            return false;
        }

        clearStateLogs();
        if (!gameLoader.reloadGameFromFile(getRoot(), file)) {
            return false;
        }

        // CRITICAL: Reset time penalties on reload (replaying log should be
        // penalty-free)
        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            player.getTimePenaltyModel().set(0);
        }

        /*
         * gameLoader actions get compared to the executed actions of the current game
         */
        List<PossibleAction> savedActions = gameLoader.getActions();

        setReloading(true);

        // This is the fix for the save/load crash
        // We must clean up all *active round references* in the GameManager
        // to clear 'transient' fields of stale data *before* we replay any actions.

        // Clean up the current round
        RoundFacade loadedRound = getCurrentRound();
        if (loadedRound instanceof OperatingRound) {
            ((OperatingRound) loadedRound).resetTransientStateOnLoad();
        } else if (loadedRound instanceof StockRound) {
            ((StockRound) loadedRound).resetTransientStateOnLoad();
        }

        // Clean up the interrupted round
        RoundFacade interrupted = getInterruptedRound();
        if (interrupted instanceof OperatingRound) {
            ((OperatingRound) interrupted).resetTransientStateOnLoad();
        } else if (interrupted instanceof StockRound) {
            ((StockRound) interrupted).resetTransientStateOnLoad();
        }

        // Clean up the other round references
        if (this.currentSRorOR.value() instanceof OperatingRound) {
            ((OperatingRound) this.currentSRorOR.value()).resetTransientStateOnLoad();
        } else if (this.currentSRorOR.value() instanceof StockRound) {
            ((StockRound) this.currentSRorOR.value()).resetTransientStateOnLoad();
        }

        // Store the list and index for getNextActionFromLog()
        this.actionsBeingReloaded = savedActions;

        // Check size
        if (savedActions.size() < executedActions.size()) {

            DisplayBuffer.add(this, LocalText.getText("LOAD_FAILED_MESSAGE",
                    "loaded file has less actions than current game"));
            this.actionsBeingReloaded = null; // Cleanup
            return false;
        }

        // Check action identity
        this.reloadActionIndex = 0; // <-- USE MEMBER VARIABLE
        // save off the current # of executed actions as it will grow as we execute
        // newly loaded
        int executedActionsCount = executedActions.size();
        PossibleAction executedAction;
        try {
            // for (PossibleAction savedAction : savedActions) { // <-- DELETE
            for (this.reloadActionIndex = 0; this.reloadActionIndex < savedActions.size(); this.reloadActionIndex++) {

                // INSTRUMENTATION: Identity Check
                try {
                    GameManager rootGM = getRoot().getGameManager();
                    RoundFacade currentRound = getCurrentRound();
                    Object roundGM = null;

                    // Reflection to get the GM from the Round
                    if (currentRound != null) {
                        try {
                            java.lang.reflect.Field gmField = currentRound.getClass().getSuperclass()
                                    .getDeclaredField("gameManager"); // Assumes Round.java has this
                            gmField.setAccessible(true);
                            roundGM = gmField.get(currentRound);
                        } catch (Exception e) {
                            roundGM = "ReflectionFailed";
                        }
                    }

                    log.info(String.format("DEBUG_ID: Reload Loop Step %d", this.reloadActionIndex));
                    log.info(String.format("DEBUG_ID: GM_Current (this) = %d", System.identityHashCode(this)));
                    log.info(String.format("DEBUG_ID: GM_Root     = %s",
                            (rootGM == null ? "null" : System.identityHashCode(rootGM))));
                    log.info(String.format("DEBUG_ID: GM_Round    = %s",
                            (roundGM instanceof GameManager ? System.identityHashCode(roundGM) : roundGM)));

                    // Attempt Aggressive Sync if we find a mismatch
                    if (roundGM instanceof GameManager && roundGM != this) {
                        log.info("DEBUG_ID: MISMATCH DETECTED! Attempting Sync to GM_Round...");
                        GameManager target = (GameManager) roundGM;
                        target.actionsBeingReloaded = this.actionsBeingReloaded;
                        target.reloadActionIndex = this.reloadActionIndex;
                        target.setReloading(true);
                    }

                } catch (Exception e) {
                    log.error("DEBUG_ID: Error in instrumentation", e);
                }

                PossibleAction savedAction = savedActions.get(this.reloadActionIndex);
                if (this.reloadActionIndex < executedActionsCount) { // <-- USE MEMBER
                    executedAction = executedActions.get(this.reloadActionIndex); // <-- USE MEMBER
                    if (!savedAction.equalsAsAction(executedAction)) {

                        DisplayBuffer.add(this, LocalText.getText("LoadFailed",
                                "loaded action \"" + savedAction
                                        + "\"<br>   is not same as game action \"" + executedAction.toString()
                                        + "\""));
                        this.actionsBeingReloaded = null; // <-- CLEANUP
                        return false;
                    }
                    logAction(savedAction, this.reloadActionIndex + 1);
                } else {
                    if (this.reloadActionIndex == executedActionsCount) { // <-- USE MEMBER
                    }
                    // Found a new action: execute it
                    if (!processOnReload(savedAction)) {
                        DisplayBuffer.add(this, LocalText.getText("LoadFailed",
                                " loaded action \"" + savedAction.toString() + "\" is invalid"));
                        this.actionsBeingReloaded = null; // <-- CLEANUP
                        break;
                    }
                }
            }
        } catch (Exception e) {
            DisplayBuffer.add(this, LocalText.getText("LoadFailed", e.getMessage()));
            this.actionsBeingReloaded = null; // <-- CLEANUP
            return false;
        }

        // Cleanup after reload finishes
        this.actionsBeingReloaded = null;

        setReloading(false);
        finishLoading();

        // use new comments (without checks)
        // FIXME (Rails2.0): CommentItems have to be replaced
        // ReportBuffer.setCommentItems(gameLoader.getComments());

        // Reload Window Positions & Scale, passing the Game Name
        if (gameUIManager != null) {
            gameUIManager.loadWindowSettings(getGameName());
        }

        // 2. Force Pause so players can orient themselves before the clock ticks
        if (isTimeManagementEnabled()) {
            setGamePaused(true);
        }

        return true;
    }

    /**
     * Records fixed revenue for private companies at the start of an OR.
     * This handles the "Silent Payouts" that do not generate SetDividend actions.
     */
    protected void recordPrivateRevenue() {
        for (PrivateCompany priv : getAllPrivateCompanies()) {
            // Only record if the private is active (not closed) and owned by a
            // player/company (not Bank/Null).
            if (!priv.isClosed() && priv.getOwner() != null && !(priv.getOwner() instanceof Bank)) {

                Phase currentPhase = getCurrentPhase();

                // Get revenue specific to the current phase (returns int)
                int rev = priv.getRevenueByPhase(currentPhase);

                if (rev > 0) {
                    String id = priv.getId();

                    // 1. Update Cumulative Payouts
                    Map<String, Integer> cumulative = cumulativeCompanyPayouts.value();
                    if (cumulative == null)
                        cumulative = new HashMap<>();
                    else
                        cumulative = new HashMap<>(cumulative); // Copy-on-write

                    int newCumulative = cumulative.getOrDefault(id, 0) + rev;
                    cumulative.put(id, newCumulative);
                    cumulativeCompanyPayouts.set(cumulative);

                    // 2. Update Instantaneous (Round) Payouts
                    Map<String, Integer> current = currentRoundPayouts.value();
                    if (current == null)
                        current = new HashMap<>();
                    else
                        current = new HashMap<>(current);

                    int newCurrent = current.getOrDefault(id, 0) + rev;
                    current.put(id, newCurrent);
                    currentRoundPayouts.set(current);

                }
            }
        }
    }

    protected boolean export(GameAction exportAction) {
        String filename = exportAction.getFilepath();
        boolean result = false;

        try {
            PrintWriter pw = new PrintWriter(filename);

            // write map information
            for (MapHex hex : getRoot().getMapManager().getHexes()) {
                pw.println(hex.getId() + "," + hex.getCurrentTile().toText() + ","
                        + hex.getCurrentTileRotation() + ","
                        + hex.getOrientationName(hex.getCurrentTileRotation()));
            }
            pw.close();
            result = true;

        } catch (IOException e) {
            DisplayBuffer.add(this, LocalText.getText("SaveFailed", e.getMessage()));
        }

        return result;
    }

    public void finishShareSellingRound() {
        finishShareSellingRound(true);
    }

    public void finishShareSellingRound(boolean resume) {
        // 1. Get the round to resume
        RoundFacade roundToResume = getInterruptedRound();

        // 2. Clear the global state field
        setInterruptedRound(null);

        // 3. Restore the current round
        setRound(roundToResume);

        // getInterruptedRound() is now null, so we must use 'roundToResume'.
        if (roundToResume != null) {
            guiHints.setCurrentRoundType(roundToResume.getClass());
        } else {
        }

        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);

        if (resume && getCurrentRound() != null)
            getCurrentRound().resume();
    }

    public void finishTreasuryShareRound() {
        // Get the round to resume and *then* clear the state field
        RoundFacade roundToResume = getInterruptedRound();
        setInterruptedRound(null);
        setRound(roundToResume);

        guiHints.setCurrentRoundType(roundToResume.getClass());

        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        ((OperatingRound) getCurrentRound()).nextStep();
    }

    public void registerPlayerBankruptcy(Player player) {
        endedByBankruptcy.set(true);
        String message = LocalText.getText("PlayerIsBankrupt",
                player.getId());
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);

        // House Rule: End game immediately instead of attempting player elimination
        String warningMsg = "Game Over by Bankruptcy (House Rule):\n" +
                "Some games might eliminate the player and continue the game.\n" +
                "However, this implementation ends the game here.\n";

        ReportBuffer.add(this, warningMsg);

        // Show warning dialog if UI is active AND we are not reloading
        if (!isReloading() && !java.awt.GraphicsEnvironment.isHeadless()) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    warningMsg,
                    "Game Over (House Rule)",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
        }

        finishGame();

    }

    public void registerCompanyBankruptcy(PublicCompany company) {
        OperatingRound or = (OperatingRound) getInterruptedRound();
        String message = LocalText.getText("CompanyIsBankrupt",
                company.getId());
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);
        if (currentRound.value() instanceof ShareSellingRound) {
            finishShareSellingRound(false);
        }
        or.finishTurn();
    }

    /**
     * Process the effects of a player going bankrupt, without ending the game.
     * This code applies to (most of) the David Hecht games.
     * So far 1826, 18EU, 18VA, 18Scan have been identified to use this code.
     * Also 1835, 1837.
     */
    protected void processPlayerBankruptcy() {

        // Assume default case as in 18EU: all assets to Bank/Pool
        Player bankrupter = getCurrentPlayer();
        Currency.toBankAll(bankrupter); // All money has already gone to company

        PlayerManager plmgr = getRoot().getPlayerManager();
        if (bankrupter == plmgr.getPriorityPlayer()) {
            plmgr.setPriorityPlayerToNext();
        }

        PortfolioModel bpf = bankrupter.getPortfolioModel();
        List<PublicCompany> presidencies = new ArrayList<>();
        Map<PublicCompany, Player> newPresidencies = new HashMap<>();
        for (PublicCertificate cert : bpf.getCertificates()) {
            if (cert.isPresidentShare())
                presidencies.add(cert.getCompany());
        }
        for (PublicCompany company : presidencies) {
            // Check if the presidency is dumped onto someone
            Player newPresident = null;
            int maxShare = 0;
            PlayerManager pm = getRoot().getPlayerManager();
            for (Player player : pm.getNextPlayers(false)) {
                int share = player.getPortfolioModel().getShare(company);
                if (share >= company.getPresidentsShare().getShare()
                        && (share > maxShare)) {
                    maxShare = share;
                    newPresident = player;
                }
            }
            if (newPresident != null) {
                bankrupter.getPortfolioModel().swapPresidentCertificate(company,
                        newPresident.getPortfolioModel());
                ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                        newPresident.getId(),
                        company.getId()));
            } else {
                // This process is game-dependent.
                // WARNING: The default version in this class currently does nothing.
                // Games needing it must override the method called here!
                newPresident = processCompanyAfterPlayerBankruptcy(bankrupter, company);
            }
            newPresidencies.put(company, newPresident);
        }

        // Dump all remaining shares to pool
        // (note: this will reset the company president to null)
        Portfolio.moveAll(PublicCertificate.class, bankrupter,
                getRoot().getBank().getPool());

        // Now we can safely set any new presidencies
        for (PublicCompany company : newPresidencies.keySet()) {
            if (company.getPresident() == null) {
                company.setPresident(newPresidencies.get(company));
            }
        }

        // bankrupter.setBankrupt(); // this is a duplicate

        // Finish the share selling round
        if (getCurrentRound() instanceof ShareSellingRound) {
            finishShareSellingRound();
        }
    }

    /**
     * Should only be called by processPlayerBankruptcy().
     * 
     * @param player  The player having gone bankrupt
     * @param company The company that caused the player going bankrupt
     * @return The new president, if any; else null
     */
    protected Player processCompanyAfterPlayerBankruptcy(Player player, PublicCompany company) {
        return Bankruptcy.processCompanyAfterPlayerBankruptcy(this, player, company,
                (Bankruptcy.Style) getGameParameter(GameDef.Parm.BANKRUPTCY_STYLE));
    }

    public void registerBrokenBank() {
        gameOverPending.set(true);
        ReportBuffer.add(this, LocalText.getText("BankIsBrokenReportText"));
        String msgContinue = "";
        if (gameEndWhen == GameEnd.AFTER_SET_OF_ORS)
            msgContinue = LocalText.getText("gameOverPlaySetOfORs");
        else if (gameEndWhen == GameEnd.AFTER_THIS_OR)
            msgContinue = LocalText.getText("gameOverPlayOnlyOR");
        String msg = LocalText.getText("BankIsBrokenDisplayText", msgContinue);
        DisplayBuffer.add(this, msg);
        addToNextPlayerMessages(msg, true);

        // Use invokeLater to prevent blocking the game loop, which causes duplicate
        // moves (race condition)
        if (!isReloading() && !java.awt.GraphicsEnvironment.isHeadless()) {
            final String finalMsg = msg;
            javax.swing.SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(null,
                    finalMsg,
                    "Bank Broken!",
                    javax.swing.JOptionPane.WARNING_MESSAGE));
        }

    }

    public void registerMaxedSharePrice(PublicCompany company, StockSpace space) {
        gameOverPending.set(true);
        ReportBuffer.add(this, LocalText.getText("MaxedSharePriceReportText",
                company.getId(),
                Bank.format(this, space.getPrice())));
        String msgContinue = "";
        if (gameEndWhen == GameEnd.AFTER_SET_OF_ORS)
            msgContinue = LocalText.getText("gameOverPlaySetOfORs");
        else if (gameEndWhen == GameEnd.AFTER_THIS_OR)
            msgContinue = LocalText.getText("gameOverPlayOnlyOR");
        String msg = LocalText.getText("MaxedSharePriceDisplayText",
                company.getId(),
                Bank.format(this, space.getPrice()),
                msgContinue);
        DisplayBuffer.add(this, msg);
        addToNextPlayerMessages(msg, true);
    }

    protected void finishGame() {
        gameOver.set(true);

        // Capture final worth (labeled "End")
        capturePlayerWorthSnapshot("End");
        captureCompanyPayoutSnapshot("End");

        saveFinishedGame();

        String message = LocalText.getText("GameOver");
        ReportBuffer.add(this, message);

        ReportBuffer.add(this, "");

        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                // Gather players and final net worths for potential upload
                java.util.List<Player> players = new java.util.ArrayList<>();
                java.util.List<Integer> scores = new java.util.ArrayList<>();
                for (Player p : getRoot().getPlayerManager().getPlayers()) {
                    players.add(p);
                    scores.add(p.getWorth());
                }

                // Prompt the user
                // upload will go to
                // https://docs.google.com/spreadsheets/d/1lXyCBjjLLzfeajpdS_PEqI3z4IGDIqj5gRAtiPpyqck/edit?pli=1&gid=0#gid=0

                int dialogResult = javax.swing.JOptionPane.showConfirmDialog(null,
                        "Do you want to upload the final results to the server?",
                        "Upload Results",
                        javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.QUESTION_MESSAGE);

                if (dialogResult == javax.swing.JOptionPane.YES_OPTION) {
                    try {
                        ResultUploader.uploadGameResult(getGameName(), players, scores);
                        log.info("results upload triggered.");
                    } catch (Exception e) {
                        log.error("Failed to transmit results: " + e.getMessage());
                    }
                }

                displayWorthChart(null);
            });
        }

        // Create the round and get a reference to it
        RoundFacade endRound = createRound(EndOfGameRound.class, "EndOfGameRound");

        // Immediately call setPossibleActions() on the new round to trigger
        // the dialog box. We must also populate correction/undo actions
        // to prevent the UI from getting stuck if the dialog is closed.
        possibleActions.clear();
        // endRound.setPossibleActions();
        setCorrectionActions(); // Add corrections (e.g., Undo/Redo)

        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
        // if (changeStack.isUndoPossible(getCurrentPlayer())) {
        // possibleActions.add(new GameAction(getRoot(), GameAction.Mode.UNDO));
        // }
        if (changeStack.isUndoPossible()) {
            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.FORCED_UNDO));
        }
        if (changeStack.isRedoPossible()) {
            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.REDO));
        }

    }

    protected void saveFinishedGame() {
        File oldGamesDir = new File("oldgames", getGameName());
        if (!oldGamesDir.exists()) {
            oldGamesDir.mkdirs();
        }
        if (this.sessionStartTimestamp == null) {
            this.sessionStartTimestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        }
        String filename = String.format("%s_%s_Final_%05d.rails", getGameName(), this.sessionStartTimestamp,
                actionCount.value());
        File saveFile = new File(oldGamesDir, filename);
        GameSaver gameSaver = new GameSaver(getRoot().getGameData(), executedActions.view());
        try {
            gameSaver.saveGame(saveFile);
            log.info("Saved finished game to {}", saveFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save finished game", e);
        }
    }

    protected void capturePlayerWorthSnapshot(String roundId) {
        // Ensure the map is initialized and CLONED to trigger StateManager updates
        LinkedHashMap<String, Map<String, Double>> history = playerWorthHistory.value();
        LinkedHashMap<String, Map<String, Double>> newHistory;
        if (history == null) {
            newHistory = new LinkedHashMap<>();
        } else {
            newHistory = new LinkedHashMap<>(history);
        }

        Map<String, Double> snapshot = new HashMap<>();
        List<Player> players = getRoot().getPlayerManager().getPlayers();

        // Calculate worth for all players
        for (Player player : players) {
            snapshot.put(player.getId(), (double) player.getWorth());
        }

        // Only log if we have not already logged this ID (e.g. from an interruption)
        if (!newHistory.containsKey(roundId)) {
            newHistory.put(roundId, snapshot);
            playerWorthHistory.set(newHistory); // Update the state object
        }

        LinkedHashMap<String, Map<String, PlayerAssetSnapshot>> assetHistory = playerAssetHistory.value();
        LinkedHashMap<String, Map<String, PlayerAssetSnapshot>> newAssetHistory;
        if (assetHistory == null) {
            newAssetHistory = new LinkedHashMap<>();
        } else {
            newAssetHistory = new LinkedHashMap<>(assetHistory);
        }

        Map<String, PlayerAssetSnapshot> roundAssets = new HashMap<>();

        for (Player p : players) {
            PlayerAssetSnapshot asset = new PlayerAssetSnapshot();
            asset.cash = p.getCash();

            // Record shares for all public companies
            for (PublicCompany comp : getAllPublicCompanies()) {
                int share = p.getPortfolioModel().getShare(comp);
                if (share > 0) {
                    asset.sharePercents.put(comp.getId(), share);
                    int price = (comp.getCurrentSpace() != null) ? comp.getCurrentSpace().getPrice() : 0;
                    int value = (share * price) / 10;
                    asset.holdingValues.put(comp.getId(), value);
                }
            }
            roundAssets.put(p.getName(), asset);
        }
        newAssetHistory.put(roundId, roundAssets);
        playerAssetHistory.set(newAssetHistory);

        LinkedHashMap<String, Map<String, Integer>> tHistory = playerTimeHistory.value();
        LinkedHashMap<String, Map<String, Integer>> newTHistory;
        if (tHistory == null) {
            newTHistory = new LinkedHashMap<>();
        } else {
            newTHistory = new LinkedHashMap<>(tHistory);
        }

        Map<String, Integer> roundTime = new HashMap<>();
        for (Player p : players) {
            roundTime.put(p.getName(), p.getTimeBankModel().value());
        }
        newTHistory.put(roundId, roundTime);
        playerTimeHistory.set(newTHistory);

    }

    public LinkedHashMap<String, Map<String, PlayerAssetSnapshot>> getPlayerAssetHistory() {
        return playerAssetHistory.value();
    }

    protected void captureCompanyPayoutSnapshot(String roundId) {
        // 1. Capture Cumulative History (Existing Logic)
        LinkedHashMap<String, Map<String, Integer>> history = companyPayoutHistory.value();
        LinkedHashMap<String, Map<String, Integer>> newHistory;
        if (history == null)
            newHistory = new LinkedHashMap<>();
        else
            newHistory = new LinkedHashMap<>(history);

        if (!newHistory.containsKey(roundId)) {
            Map<String, Integer> currentSnapshot = cumulativeCompanyPayouts.value();
            if (currentSnapshot == null)
                currentSnapshot = new HashMap<>();
            newHistory.put(roundId, new HashMap<>(currentSnapshot));
            companyPayoutHistory.set(newHistory);
        }

        LinkedHashMap<String, Map<String, Integer>> instHistory = instantaneousPayoutHistory.value();
        LinkedHashMap<String, Map<String, Integer>> newInstHistory;
        if (instHistory == null)
            newInstHistory = new LinkedHashMap<>();
        else
            newInstHistory = new LinkedHashMap<>(instHistory);

        if (!newInstHistory.containsKey(roundId)) {
            Map<String, Integer> roundSnapshot = currentRoundPayouts.value();
            if (roundSnapshot == null)
                roundSnapshot = new HashMap<>();

            // Save the snapshot
            newInstHistory.put(roundId, new HashMap<>(roundSnapshot));
            instantaneousPayoutHistory.set(newInstHistory);

            // CRITICAL: Reset the accumulator for the next round
            // We set it to a new empty map (state-safe)
            currentRoundPayouts.set(new HashMap<>());

        }
    }

    protected void updatePayoutTracker(PossibleAction action) {
        if (action == null)
            return;

        if (action instanceof SetDividend) {
            SetDividend sd = (SetDividend) action;
            PublicCompany company = sd.getCompany();
            if (company == null)
                return;

            int amount = 0;
            // Determine amount based on allocation type
            if (sd.getRevenueAllocation() == SetDividend.PAYOUT) {
                amount = sd.getActualRevenue();
            } else if (sd.getRevenueAllocation() == SetDividend.SPLIT) {
                amount = sd.getActualRevenue() / 2;
            }

            if (amount > 0) {
                String compId = company.getId();

                // 1. Update Cumulative
                Map<String, Integer> cumulative = cumulativeCompanyPayouts.value();
                if (cumulative == null)
                    cumulative = new HashMap<>();
                else
                    cumulative = new HashMap<>(cumulative); // Copy-on-write

                int newCumulativeTotal = cumulative.getOrDefault(compId, 0) + amount;
                cumulative.put(compId, newCumulativeTotal);
                cumulativeCompanyPayouts.set(cumulative);

                // 2. Update Instantaneous
                Map<String, Integer> current = currentRoundPayouts.value();
                if (current == null)
                    current = new HashMap<>();
                else
                    current = new HashMap<>(current);

                int newCurrentTotal = current.getOrDefault(compId, 0) + amount;
                current.put(compId, newCurrentTotal);
                currentRoundPayouts.set(current);

            }
        }
    }

    public LinkedHashMap<String, Map<String, Integer>> getCompanyPayoutHistory() {
        return companyPayoutHistory.value();
    }

    /**
     * Public accessor for the worth history, intended for UI/Reporting.
     */
    public LinkedHashMap<String, Map<String, Double>> getPlayerWorthHistory() {
        return playerWorthHistory.value();
    }

    /**
     * Called from the UI to launch the Company Payout Chart.
     * * @param parentFrame The main UI frame required as the dialog parent.
     */
    public void displayCompanyPayoutChart(Object parentFrame) {
        try {
            Class<?> chartClass = Class.forName("net.sf.rails.ui.swing.charts.CompanyPayoutChartWindow");
            java.lang.reflect.Method showMethod = chartClass.getMethod("showChart", JFrame.class, GameManager.class);

            JFrame frame = (JFrame) parentFrame;
            showMethod.invoke(null, frame, this);

        } catch (Exception e) {
        }
    }

    /**
     * Public accessor to trigger the UI to display ONLY the final ranking list.
     * This is intended for the "Game End Report" menu item.
     * * @param parentFrame The main UI frame required as the dialog parent.
     */
    public void displayFinalRankingOnly(Object parentFrame) {
        try {
            // We assume the worth chart class has a static method specifically for the
            // ranking report
            // The method name should be clear, e.g., showRankingReport
            Class<?> chartClass = Class.forName("net.sf.rails.ui.swing.charts.WorthChartWindow");

            // The method should accept the JFrame parent and GameManager.
            java.lang.reflect.Method showMethod = chartClass.getMethod("showRankingReport", JFrame.class,
                    GameManager.class);

            // Cast the parentFrame object to a generic JFrame
            JFrame frame = (JFrame) parentFrame;

            // Call the static showRankingReport method
            showMethod.invoke(null, frame, this);

        } catch (Exception e) {

        }
    }

    public boolean isGameOver() {
        return gameOver.value();
    }

    public BooleanState getGameOverPendingModel() {
        return gameOverPending;
    }

    public void setGameOverReportedUI(boolean b) {
        gameOverReportedUI = b;
    }

    public boolean getGameOverReportedUI() {
        return (gameOverReportedUI);
    }

    public RoundFacade getCurrentRound() {
        return currentRound.value();
    }

    public GenericState<RoundFacade> getCurrentRoundModel() {
        return currentRound;
    }

    public List<PublicCompany> getAllPublicCompanies() {
        return getRoot().getCompanyManager().getAllPublicCompanies();
    }

    public List<PrivateCompany> getAllPrivateCompanies() {
        return getRoot().getCompanyManager().getAllPrivateCompanies();
    }

    public StartPacket getStartPacket() {
        return startPacket.value();
    }

    public Phase getCurrentPhase() {
        return getRoot().getPhaseManager().getCurrentPhase();
    }

    public boolean canAnyCompanyHoldShares() {
        return (Boolean) getGuiParameter(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES);
    }

    public String getClassName(GuiDef.ClassName key) {
        switch (key) {

            case GAME_UI_MANAGER:
                return gameUIManagerClassName;

            case OR_UI_MANAGER:
                return orUIManagerClassName;

            case STATUS_WINDOW:
                return statusWindowClassName;

            case START_ROUND_WINDOW:
                return startRoundWindowClassName;

            case GAME_STATUS:
                return gameStatusClassName;

            default:
                return "";
        }
    }

    public Object getGuiParameter(GuiDef.Parm key) {

        return guiParameters.getOrDefault(key, false);
    }

    public void setGuiParameter(GuiDef.Parm key, boolean value) {

        guiParameters.put(key, value);
    }

    public void setGameParameter(GameDef.Parm key, Object value) {
        gameParameters.put(key, value);
    }

    /* Returned value must be cast to the appropriate type */
    public Object getGameParameter(GameDef.Parm key) {
        return gameParameters.getOrDefault(key, false);
    }

    public int getParmAsInt(GameDef.Parm key) {
        Object parm = getGameParameter(key);
        if (parm instanceof Integer) {
            return (Integer) parm;
        } else {
            return -1;
        }
    }

    public boolean getParmAsBoolean(GameDef.Parm key) {
        Object parm = getGameParameter(key);
        if (parm instanceof Boolean) {
            return (Boolean) parm;
        } else {
            return false;
        }
    }

    /**
     * Move the special property to the party that will later benefit from it:
     * the company or the player (in the latter case it is stored in the GameManager
     * as a "common" property: buyable by all companies.
     * 
     * @param owner The current buyer (and future seller) of the property.
     * @param sps   The (set of) property(ies) to be allocated.
     */
    public void allocateSpecialProperties(MoneyOwner owner, Set<SpecialProperty> sps) {
        // Move any special abilities to the portfolio, if configured so.

        if (sps != null) {
            // Need intermediate List to avoid ConcurrentModificationException
            List<SpecialProperty> spsToMoveToPC = new ArrayList<>(2);
            List<SpecialProperty> spsToMoveToGM = new ArrayList<>(2);
            List<SpecialProperty> spsMoveToPlayer = new ArrayList<>(2);
            for (SpecialProperty sp : sps) {
                if ((owner instanceof PublicCompany && sp.isUsableIfOwnedByCompany())
                        || (owner instanceof Player && sp.isUsableIfOwnedByPlayer())
                                && "toGameManager".equalsIgnoreCase(sp.getTransferText())) {
                    // This must be SellBonusToken - remember the owner!
                    if (sp instanceof SellBonusToken) {
                        // TODO: Check if this works correctly
                        ((SellBonusToken) sp).setSeller(owner);
                        spsToMoveToGM.add(sp);
                    }
                    if (sp instanceof LocatedBonus) {
                        spsToMoveToPC.add(sp);
                    }

                } else if (owner instanceof PublicCompany && sp.isUsableIfOwnedByCompany()
                        && "toCompany".equalsIgnoreCase(sp.getTransferText())) {
                    spsToMoveToPC.add(sp);

                } else if (owner instanceof Player && sp.isUsableIfOwnedByPlayer()
                        && "toPlayer".equalsIgnoreCase(sp.getTransferText())) {
                    spsMoveToPlayer.add(sp);
                }
            }
            for (SpecialProperty sp : spsToMoveToPC) {
                sp.moveTo(owner);
            }
            for (SpecialProperty sp : spsToMoveToGM) {
                this.addSpecialProperty(sp);
            }
            for (SpecialProperty sp : spsMoveToPlayer) {
                sp.moveTo(owner);
            }
        }
    }

    // TODO: Was the int position argument required?
    public boolean addSpecialProperty(SpecialProperty property) {
        if (commonSpecialProperties == null) {
            commonSpecialProperties = PortfolioSet.create(this,
                    "CommonSpecialProperties", SpecialProperty.class);
        }
        return commonSpecialProperties.add(property);
    }

    // TODO: Write new SpecialPropertiesModel

    public Map<String, Integer> getCumulativeCompanyPayouts() {
        return cumulativeCompanyPayouts.value();
    }

    /**
     * Launches the Investment Multiplier Chart.
     */
    public void displayMultiplierChart(Object parentFrame) {
        try {
            Class<?> chartClass = Class.forName("net.sf.rails.ui.swing.charts.MultiplierChartWindow");
            java.lang.reflect.Method showMethod = chartClass.getMethod("showChart", JFrame.class, GameManager.class);
            showMethod.invoke(null, (JFrame) parentFrame, this);
        } catch (Exception e) {
        }
    }

    public List<SpecialProperty> getCommonSpecialProperties() {
        return getSpecialProperties(null, false);
    }

    public Portfolio<SpecialProperty> getCommonSpecialPropertiesPortfolio() {
        return commonSpecialProperties;
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecialProperty> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {

        List<T> result = new ArrayList<>();

        if (commonSpecialProperties != null) {
            for (SpecialProperty sp : commonSpecialProperties) {
                if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                        && sp.isExecutionable()
                        && (!sp.isExercised() || includeExercised)) {
                    result.add((T) sp);
                }
            }
        }

        return result;
    }

    public GuiHints getUIHints() {
        return guiHints;
    }

    public CorrectionManager getCorrectionManager(CorrectionType ct) {
        CorrectionManager cm = correctionManagers.get(ct);
        if (cm == null) {
            cm = ct.newCorrectionManager(this);
            correctionManagers.put(ct, cm);
        }
        return cm;
    }

    public List<PublicCompany> getCompaniesInRunningOrder() {

        Map<Integer, PublicCompany> operatingCompanies = new TreeMap<>();
        StockSpace space;
        int key;
        int minorNo = 0;
        for (PublicCompany company : getRoot().getCompanyManager().getAllPublicCompanies()) {

            if (company.isClosed()) {
                continue;
            }

            // Key must put companies in reverse operating order, because sort
            // is ascending.
            if (company.hasStockPrice() && company.hasStarted()) {
                space = company.getCurrentSpace();
                key = 1000000 * (999 - space.getPrice()) + 10000
                        * (99 - space.getColumn()) + 100
                                * space.getRow()
                        + space.getStackPosition(company);
            } else {
              key = 2000000000 + (++minorNo);
            }
            operatingCompanies.put(key, company);
        }

        return new ArrayList<>(operatingCompanies.values());
    }

    /**
     * Defines the UI display order for companies.
     * Defaults to the engine's strict running order (which resolves price ties
     * using market columns/stack position).
     * Can be overridden by game-specific managers (e.g., 1835 for 'PR').
     */
    public List<PublicCompany> getCompaniesInDisplayOrder(List<PublicCompany> companies) {
        List<PublicCompany> runningOrder = getCompaniesInRunningOrder();
        List<PublicCompany> displayOrder = new ArrayList<>();

        // Retain only the companies requested for display, keeping the running order.
        for (PublicCompany c : runningOrder) {
            if (companies.contains(c)) {
                displayOrder.add(c);
            }
        }

        // Add any companies that were missing from running order (fail-safe)
        for (PublicCompany c : companies) {
            if (!displayOrder.contains(c)) {
                displayOrder.add(c);
            }
        }

        return displayOrder;
    }

    public int getCompanyReleaseStep() {
        return companyReleaseStep.value();
    }

    public void setCompanyReleaseStep(int value) {
        companyReleaseStep.set(value);
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public void setSkipDone(GameDef.OrStep step) {
        skipNextDone = true;
        skippedStep = step;
    }

    public void resetStorage() {
        objectStorage = new HashMap<>();
        storageIds = new HashMap<>();
    }

    public int getStorageId(String typeName) {
        Integer id = storageIds.get(typeName);
        if (id == null)
            id = 0;
        return id;
    }

    public int storeObject(String typeName, Object object) {
        Integer id = storageIds.get(typeName);
        if (id == null)
            id = 0;
        objectStorage.put(typeName + id, object);
        storageIds.put(typeName, id + 1); // store next id
        return id;
    }

    public Object retrieveObject(String typeName, int id) {
        return objectStorage.get(typeName + id);
    }

    // TODO (Rails2.0): rewrite this, use PhaseAction interface stored at
    // PhaseManager
    public void processPhaseAction(String name, String value) {
        getCurrentRound().processPhaseAction(name, value);
    }

    // FIXME (Rails2.0): does nothing now, replace this with a rewrite
    public void addToNextPlayerMessages(String s, boolean undoable) {

    }

    // shortcut to PlayerManager
    public int getPlayerCertificateLimit(Player player) {
        return getRoot().getPlayerManager().getPlayerCertificateLimit(player);
    }

    // shortcut to PlayerManager
    public Player getCurrentPlayer() {
        RoundFacade round = getCurrentRound();
        if (round != null && round.getCurrentPlayer() != null) {
            return round.getCurrentPlayer();
        }
        return getRoot().getPlayerManager().getCurrentPlayer();
    }

    /*
     * public void setNationalToFound(String national) {
     * 
     * for (PublicCompany company : this.getAllPublicCompanies()) {
     * if ( "national".equals(company.getId())) {
     * this.nationalToFound = company;
     * }
     * }
     * }
     * 
     * public PublicCompany getNationalToFound() {
     * // TODO Auto-generated method stub
     * return nationalToFound;
     * }
     * 
     * public void setNationalFormationStartingPlayer(PublicCompany
     * nationalToFound2, Player currentPlayer) {
     * this.NationalFormStartingPlayer.put(nationalToFound2, currentPlayer);
     * 
     * }
     * 
     * public Player getNationalFormationStartingPlayer(PublicCompany comp) {
     * return this.NationalFormStartingPlayer.get(comp);
     * }
     */

    /**
     * Registry of exchanged certificates to be denied income
     * because their precursors produced revenue in the same OR.
     */
    private final HashSetState<Certificate> blockedCertificates = HashSetState.create(this, "BlockedCertificates");

    /**
     * Registry of exchanged trains that may not run for a major company
     * because they have already run for the associated minor in the same OR.
     * NOTE: here and in many other places the term "minors" includes coal
     * companies.
     */
    private final HashSetState<Train> blockedTrains = HashSetState.create(this, "BlockedTrains");

    public void clearBlockedCertificates() {
        blockedCertificates.clear();
    }

    public void blockCertificate(Certificate certificate) {
        blockedCertificates.add(certificate);
    }

    public boolean isCertificateBlocked(Certificate certificate) {
        return blockedCertificates.contains(certificate);
    }

    public void clearBlockedTrains() {
        blockedTrains.clear();
    }

    public void blockTrain(Train train) {
        blockedTrains.add(train);
    }

    public boolean isTrainBlocked(Train train) {
        return blockedTrains.contains(train);
    }

    // ------------------------------------
    // Random generator
    // Used by SOH
    // ------------------------------------
    private Random randomGenerator;

    public Random getRandomGenerator() {
        if (randomGenerator == null) {
            long seed = Long.parseLong(getRoot().getGameOptions().get(GameOption.RANDOM_SEED));
            randomGenerator = new Random(seed);
        }
        return randomGenerator;
    }

    /**
     * Public accessor to the list of players for UI binding.
     * 
     * @return The list of Player objects.
     */
    public List<Player> getPlayers() {
        return getRoot().getPlayerManager().getPlayers();
    }

    // ++ TIME MANAGEMENT CONSEQUENCE ++
    // Default to NONE
    private TimeConsequence timeConsequence = TimeConsequence.NONE;

    public void setTimeConsequence(TimeConsequence consequence) {
        this.timeConsequence = (consequence != null) ? consequence : TimeConsequence.NONE;
    }

    public TimeConsequence getTimeConsequence() {
        return this.timeConsequence;
    }

    // ++ START AI STATE RESTORATION SETTERS ++
    // These methods are for 're-hydrating' a saved state and bypass normal logic.

    public void setAbsoluteActionCounter_AI(int count) {
        // This is a plain int in GameManager, not an IntegerState
        this.absoluteActionCounter = count;
    }

    public void setStartRoundNumber_AI(int count) {
        this.startRoundNumber.set(count);
    }

    public void setStockRoundNumber_AI(int count) {
        this.stockRoundNumber.set(count);
    }

    public void setAbsoluteORNumber_AI(int count) {
        this.absoluteORNumber.set(count);
    }

    public void setRelativeORNumber_AI(int count) {
        this.relativeORNumber.set(count);
    }

    public void setCurrentRound_AI(Round round) {
        this.currentRound.set(round);
        // Also sync the macro-round tracker so the engine knows we are inside an active
        // OR/SR
        if (round instanceof OperatingRound || round instanceof StockRound) {
            this.currentSRorOR.set(round);
        }
    }

    /**
     * Used by OperatingRound during a reload to "look ahead" at the next action
     * in the save file. This is required to fix the "Baden" relay pause.
     * 
     * @return The *next* PossibleAction in the log, or null if not reloading
     *         or if at the end of the list.
     */
    public PossibleAction getNextActionFromLog() {
        // --- START FIX ---
        // INSTRUMENTATION: Strict logging to diagnose Fatal Reload Error
        boolean reloadingState = isReloading();
        boolean listExists = (this.actionsBeingReloaded != null);
        int listSize = listExists ? this.actionsBeingReloaded.size() : -1;
        int currentIndex = this.reloadActionIndex;
        int targetIndex = currentIndex + 1;

        log.info("DEBUG_INSTRUMENTATION: getNextActionFromLog() called.");
        log.info(String.format(
                "DEBUG_INSTRUMENTATION: State -> isReloading=%b, listExists=%b, size=%d, currentIndex=%d, targetIndex=%d",
                reloadingState, listExists, listSize, currentIndex, targetIndex));

        if (!reloadingState) {
            log.info("DEBUG_INSTRUMENTATION: Returning NULL because !isReloading()");
            return null;
        }

        if (!listExists) {
            log.info("DEBUG_INSTRUMENTATION: Returning NULL because actionsBeingReloaded is null");
            return null;
        }

        if (targetIndex >= listSize) {
            log.info("DEBUG_INSTRUMENTATION: Returning NULL because targetIndex >= listSize (End of Log)");
            return null;
        }

        PossibleAction action = this.actionsBeingReloaded.get(targetIndex);
        String actionName = (action != null) ? action.toString() : "null_object";
        log.info("DEBUG_INSTRUMENTATION: Returning Action -> " + actionName);

        return action;
        // --- END FIX ---
    }

    public boolean processOnReload(PossibleAction action) {
        getRoot().getReportManager().getDisplayBuffer().clear();
        RoundFacade roundBefore = getCurrentRound();
        Object actorBefore = getCurrentPlayer();
        if (roundBefore instanceof OperatingRound) {
            actorBefore = ((OperatingRound) roundBefore).operatingCompany.value();
        }

        if (this.logOutputDirectory != null) {
            try {
                String filename = String.format("state_%05d.json", getActionCountModel().value());
                File outputFile = new File(this.logOutputDirectory, filename);
                net.sf.rails.game.ai.snapshot.JsonStateSerializer.serialize(this, outputFile.getAbsolutePath());
            } catch (Exception e) {
            }
        }

        logActionTaken(action);
        logAction(action);

        if (action instanceof NullAction && !possibleActions.contains(NullAction.class)) {
            return true;
        }

        if (!possibleActions.validate(action)) {
            boolean classMatch = false;
            for (PossibleAction pa : possibleActions.getList()) {
                if (pa.getClass().equals(action.getClass())) {
                    classMatch = true;
                    break;
                }
            }

            if (classMatch) {
                log.warn("RELOAD WARNING: Player mismatch on action " + action
                        + ". Bypassing strict validation to allow Round to sync state.");
            } else {
                DisplayBuffer.add(this, LocalText.getText("ActionNotAllowed", action.toString()));

                StringBuilder sb = new StringBuilder();
                sb.append("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
                sb.append("FATAL RELOAD ERROR: Action not in PossibleActions list.\n");
                sb.append("Failed Action: ").append(action).append("\n");
                sb.append("Current Round: ").append(getCurrentRound() != null ? getCurrentRound().toString() : "null")
                        .append("\n");
                sb.append("Bank Cash:     ").append(getRoot().getBank().getPurse().value()).append("\n");

                int actionCount = (possibleActions.getList() != null) ? possibleActions.getList().size() : 0;
                sb.append("--- Available Possible Actions (" + actionCount + ") ---\n");

                if (actionCount == 0) {
                    sb.append("  (NONE)\n");
                } else {
                    for (PossibleAction pa : possibleActions.getList()) {
                        if (pa.toString().contains("CorrectionModeAction")) {
                            continue;
                        }
                        sb.append("  > ").append(pa).append("\n");
                    }
                }
                sb.append("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");

                String error = sb.toString();
                log.error(error);

                // CRASH IMMEDIATELY
                throw new RuntimeException(error);
            }
        }

        boolean doProcess = true;
        if (skipNextDone) {
            if (action instanceof NullAction
                    && ((NullAction) action).getMode() == NullAction.Mode.DONE) {
                if (currentRound.value() instanceof OperatingRound
                        && ((OperatingRound) currentRound.value()).getStep() == skippedStep) {
                    doProcess = false;
                }
            }
        }
        skipNextDone = false;
        skippedStep = null;

        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();

        // --- 2. STRICT EXECUTION CHECK ---
        // If process() returns false, we crash instead of logging/ignoring.
        if (doProcess && !processCorrectionActions(action) && !getCurrentRound().process(action)) {

            String msg = "FATAL ERROR: Engine returned false for action \""
                    + action + "\" in round " + getCurrentRound().getRoundName();

            log.error(msg);
            DisplayBuffer.add(this, msg);

            // CRASH IMMEDIATELY
            throw new RuntimeException(msg);
        }

        executedActions.add(action);
        updatePayoutTracker(action);

        if (isTimeManagementEnabled() && action.getPlayer() != null) {
            int timeSpent = action.getExecutionTimeSeconds();
            if (timeSpent > 0) {
                action.getPlayer().getTimeBankModel().add(-timeSpent);
                log.info("RELOAD [Move " + getActionCountModel().value() + "] Player: " + action.getPlayer().getName()
                        + " | Deducted: " + timeSpent + "s | Remaining: "
                        + action.getPlayer().getTimeBankModel().value() + "s");
            }
        }

        // Capture snapshots per move so the timeline slider works when loaded from a
        // save
        String moveId = String.valueOf(getActionCountModel().value());
        capturePlayerWorthSnapshot(moveId);
        captureCompanyPayoutSnapshot(moveId);

        possibleActions.clear();
        getCurrentRound().setPossibleActions();
        changeStack.close(action);

        RoundFacade roundAfter = getCurrentRound();
        Object actorAfter = getCurrentPlayer();
        if (roundAfter instanceof OperatingRound) {
            actorAfter = ((OperatingRound) roundAfter).operatingCompany.value();
        }

        boolean roundChanged = (roundBefore != roundAfter);
        if (roundChanged) {
            markRoundBoundary();
        }

        boolean actorChanged = (actorBefore == null && actorAfter != null) ||
                (actorBefore != null && !actorBefore.equals(actorAfter));

        if (roundChanged || actorChanged) {
            String rId = (getCurrentRound() != null) ? getCurrentRound().getId() : "Start";
            String snapId = "Move_" + getActionCountModel().value() + ":" + rId;
            capturePlayerWorthSnapshot(snapId);
            captureCompanyPayoutSnapshot(snapId);
        }

        if (!isGameOver())
            setCorrectionActions();

        return true;
    }

    public LinkedHashMap<String, Map<String, Integer>> getInstantaneousPayoutHistory() {
        return instantaneousPayoutHistory.value();
    }

    /**
     * Public getter for the configured OperatingRound class.
     * Required by GameStateRestorer.
     */
    public Class<? extends OperatingRound> getOperatingRoundClass() {
        return this.operatingRoundClass;
    }

    /**
     * Public getter for the configured StockRound class.
     * Required by GameStateRestorer.
     */
    public Class<? extends StockRound> getStockRoundClass() {
        return this.stockRoundClass;
    }

    /**
     * Public getter for the actionCount IntegerState.
     * Required by JsonStateSerializer.
     */
    public IntegerState getActionCountModel() {
        return this.actionCount;
    }

    /**
     * AI Accessor: Allows the AI to undo the last simulation step.
     * Wraps the standard GameAction.Mode.UNDO logic.
     */
    public void undo() {
        process(new GameAction(getRoot(), GameAction.Mode.UNDO));
    }

    public List<String> getGameReport() {

        List<String> b = new ArrayList<>();

        List<Player> rankedPlayersRaw = new ArrayList<>(getRoot().getPlayerManager().getPlayers());

        // Sort Descending: Winner (Highest Worth) First
        Collections.sort(rankedPlayersRaw, (p1, p2) -> {
            int result = Integer.compare(p2.getWorth(), p1.getWorth());
            if (result == 0) {
                return p1.getId().compareTo(p2.getId());
            }
            return result;
        });

        Player winnerRaw = rankedPlayersRaw.get(0);
        double winnerWorthRaw = winnerRaw.getWorth();

        b.add(LocalText.getText("EoGWinner") + winnerRaw.getId() + "!");
        b.add(LocalText.getText("EoGFinalRanking") + " (Raw Worth):");

        int rank = 1;
        // Iterate normally (Winner first) for the report
        for (Player p : rankedPlayersRaw) {
            double percent = (winnerWorthRaw > 0) ? (p.getWorth() / winnerWorthRaw) * 100.0 : 0.0;
            String line = String.format("%d. %s (%s) - %.1f%%",
                    rank,
                    p.getId(),
                    Bank.format(this, p.getWorth()),
                    percent);

            b.add(line);
            rank++;
        }
        List<Player> rankedPlayersTimeAdj = new ArrayList<>(getRoot().getPlayerManager().getPlayers());

        // Helper to calculate adjusted worth: Net Worth + (Negative TimeBank)
        // This ensures strictly negative times are subtracted, while positive times are
        // ignored.
        java.util.function.ToIntFunction<Player> getAdjustedWorth = p -> {
            int val = p.getWorth();
            int time = p.getTimeBankModel().value();
            return val + Math.min(0, time);
        };

        Collections.sort(rankedPlayersTimeAdj, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
                // Sort by time-adjusted worth, descending
                int val1 = getAdjustedWorth.applyAsInt(p1);
                int val2 = getAdjustedWorth.applyAsInt(p2);
                int result = -Integer.compare(val1, val2);

                // Then by name
                if (result == 0) {
                    result = p1.getId().compareTo(p2.getId());
                }
                return result;
            }
        });

        // 2. Report winner based on time-adjusted worth
        Player winnerTimeAdj = rankedPlayersTimeAdj.get(0);
        double winnerWorthTimeAdj = getAdjustedWorth.applyAsInt(winnerTimeAdj);

        // Separate the reports with a blank line for clarity
        b.add("");
        b.add(LocalText.getText("EoGFinalRanking") + " (Time-Adjusted Worth):");

        rank = 1;
        for (Player p : rankedPlayersTimeAdj) {
            double worthTimeAdj = getAdjustedWorth.applyAsInt(p);
            double percent = (winnerWorthTimeAdj > 0) ? (worthTimeAdj / winnerWorthTimeAdj) * 100.0 : 0.0;
            String line = String.format("%d. %s (%s) - %.1f%%",
                    rank,
                    p.getId(),
                    Bank.format(this, (int) worthTimeAdj), // Format the adjusted worth
                    percent);

            b.add(line);
            rank++;
        }

        return b;
    }

    // --- REPLACEMENT FOR LOGGING SECTION ---

    // 1. STATE VARIABLES
    protected final GenericState<String> statusMessageState = new GenericState<>(this, "StatusMessageState");
    protected final GenericState<String> lastLogActor = new GenericState<>(this, "LastLogActor");
    protected final GenericState<String> lastLogCompany = new GenericState<>(this, "LastLogCompany");
    protected final ArrayListState<String> passedPlayers = new ArrayListState<>(this, "PassedPlayersList");

    // 2. TRANSIENT FIELD (Standard Java List)
    // We initialize it here to avoid NPEs on new games

    private Class<? extends RoundFacade> lastRoundClass = null;
    private String currentLogPrefix = "";

    public String getLastActionSummary() {
        if (statusMessageState == null)
            return "";
        String hist = statusMessageState.value();
        if (Util.hasValue(hist)) {
            if (Util.hasValue(currentLogPrefix)) {
                return currentLogPrefix + " " + hist;
            }
            return hist;
        }
        return "";
    }

    // --- ROBUST GETTER (Prevents UI Crashes) ---
    public String getPassedPlayersLog() {
        if (passedPlayers.isEmpty())
            return "";
        return String.join(", ", passedPlayers);
    }

    protected void clearStatusMessage() {
        if (statusMessageState != null)
            statusMessageState.set("");
        currentLogPrefix = "";
        lastRoundClass = null;
        if (passedPlayers != null)
            passedPlayers.clear();
    }

    /**
     * Calculate the worth of a public company certificate for player
     * net-worth/bankruptcy,
     * used to override the market price (e.g., for 1835 Minors before flotation).
     * Default behavior is to return -1, indicating no override; the market price
     * should be used.
     */
    public int getPublicCompanyWorth(PublicCompany company) {
        return -1; // Default: No override, use market price
    }

    /**
     * Called from the UI to launch the Worth History Chart.
     * 
     * @param parentFrame The main UI frame (e.g., StatusWindow) required as the
     *                    dialog parent.
     */
    public void displayWorthChart(Object parentFrame) {
        // We use reflection/dynamic class loading here to avoid making GameManager
        // dependent on specific UI classes (like WorthChartWindow) at compile time.
        try {
            Class<?> chartClass = Class.forName("net.sf.rails.ui.swing.charts.WorthChartWindow");
            // Use JFrame.class for reflection method lookup to handle the generic parent
            // frame
            java.lang.reflect.Method showMethod = chartClass.getMethod("showChart", JFrame.class, GameManager.class);

            // Cast the parentFrame object to a generic JFrame
            JFrame frame = (JFrame) parentFrame;

            // Call the static showChart method
            showMethod.invoke(null, frame, this);

        } catch (Exception e) {
        }
    }

    private void loadJsonState(File saveFile) {
        if (!isTimeManagementEnabled())
            return;

        File metaFile = new File(saveFile.getAbsolutePath() + ".state.json");
        if (!metaFile.exists()) {
            return;
        }

        // Parse the JSON manually to extract just the time banks
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(metaFile))) {
            String line;
            String currentPlayer = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"id\"")) {
                    currentPlayer = line.split(":")[1].replace("\"", "").replace(",", "").trim();
                } else if (line.contains("\"timeBankSeconds\"") && currentPlayer != null) {
                    int seconds = Integer.parseInt(line.split(":")[1].replace(",", "").trim());
                    for (Player p : getRoot().getPlayerManager().getPlayers()) {
                        if (p.getName().equals(currentPlayer)) {
                            p.getTimeBankModel().set(seconds);
                            break;
                        }
                    }
                    currentPlayer = null; // reset for next player
                }
            }
        } catch (Exception e) {
            log.error("Failed to read time data from state.json", e);
        }
    }

    private void clearAutosaves() {
        String path = Config.get("save.recovery.filepath");
        File dir;
        if (Util.hasValue(path)) {
            dir = new File(path).getParentFile();
        } else {
            dir = new File("autosave");
        }
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".rails") || name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }

    private void clearStateLogs() {
        java.io.File dir = new java.io.File("logs/state");
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File f : dir.listFiles()) {
                if (f.getName().endsWith(".json")) {
                    f.delete();
                }
            }
        } else {
            dir.mkdirs();
        }
    }

    private void truncateStateLogs(int currentMove) {
        java.io.File dir = new java.io.File("logs/state");
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File f : dir.listFiles()) {
                if (f.getName().startsWith("state_") && f.getName().endsWith(".json")) {
                    try {
                        int move = Integer.parseInt(f.getName().substring(6, 11));
                        if (move > currentMove)
                            f.delete();
                    } catch (NumberFormatException e) {
                        // Ignore files that do not match the exact pattern
                    }
                }
            }
        }
    }

    /**
     * Called by process() to record human-readable history.
     * Includes robust error handling to prevent game freezes.
     */

    private transient PossibleAction lastLoggedAction = null;
    private transient int lastLoggedMoveNumber = -1;

    public void logAction(PossibleAction action) {
        logAction(action, actionCount.value());
    }

    public void logAction(PossibleAction action, int moveNumber) {
        if (action == null)
            return;

        // Strict De-duplication:
        // If we are asked to log the EXACT same action object for the EXACT same move
        // number, we ignore it.
        if (action == lastLoggedAction && moveNumber == lastLoggedMoveNumber) {
            // INSTRUMENTATION: Explicitly log that a duplicate was suppressed
            return;
        }
        lastLoggedAction = action;
        lastLoggedMoveNumber = moveNumber;

        try {
            // --- 0. SETUP VARIABLES ---
            String actorName = action.getPlayerName();
            String actionString = action.toString();

            // --- 1. RESET LOGIC (Turn Boundary) ---
            String oldActor = lastLogActor.value();

            String newCompany = "";
            Company actingCompany = null;
            if (action instanceof PossibleORAction) {
                actingCompany = ((PossibleORAction) action).getCompany();
            } else if (action instanceof BuyTrain) {
                actingCompany = ((BuyTrain) action).getCompany();
            } else if (action instanceof SetDividend) {
                actingCompany = ((SetDividend) action).getCompany();
            }
            if (actingCompany != null)
                newCompany = actingCompany.getId();

            String oldCompany = lastLogCompany.value();
            if (oldCompany == null)
                oldCompany = "";

            boolean actorChanged = (oldActor == null) || !actorName.equals(oldActor);
            boolean companyChanged = Util.hasValue(newCompany) && !newCompany.equals(oldCompany);

            RoundFacade currentRound = getCurrentRound();
            boolean roundChanged = (lastRoundClass == null || !lastRoundClass.equals(currentRound.getClass()));

            if (actorChanged || companyChanged || roundChanged) {
                statusMessageState.set("");
                currentLogPrefix = "";
                lastRoundClass = currentRound.getClass();
                lastLogActor.set(actorName);
                lastLogCompany.set(newCompany);

                if (roundChanged)
                    passedPlayers.clear();
            }

            // --- 2. UPDATE PREFIX ---
            if (actingCompany != null) {
                currentLogPrefix = actingCompany.getId() + " (" + actorName + ")";
            } else if (!Util.hasValue(currentLogPrefix)) {
                currentLogPrefix = actorName;
            }

            String entry = "";

            // --- 3. FORMATTING & PASS LOGIC ---
            if (action instanceof BuyCertificate) {
                BuyCertificate bc = (BuyCertificate) action;
                // calculate correct percentage using Base Share Unit * Multiplier
                // If Share Unit is 10%, and we buy 2 units (20% cert), result is 20.
                int baseUnit = bc.getCompany().getShareUnit();
                int sharePercent = bc.getNumberBought() * bc.getSharePerCertificate();

                // Fallback if sharePerCertificate is just a multiplier (e.g. 1 or 2)
                if (sharePercent < baseUnit)
                    sharePercent = sharePercent * baseUnit;

                String compId = bc.getCompany().getId();

                boolean isNationalization = false;
                if (bc.getFromPortfolio() != null && bc.getFromPortfolio().getParent() instanceof Player) {
                    Player seller = (Player) bc.getFromPortfolio().getParent();
                    if (!seller.getName().equals(actorName)) {
                        isNationalization = true;
                        entry = "nationalizes " + sharePercent + "% from " + seller.getName();
                    }
                }

                if (!isNationalization) {
                    entry = "bought " + sharePercent + "% " + compId;
                }
                passedPlayers.clear();
            } else if (action instanceof SellShares) {
                SellShares ss = (SellShares) action;
                // Calculate actual percentage
                int sharePercent = ss.getShareUnits() * ss.getCompany().getShareUnit() * ss.getNumber();
                entry = "sold " + sharePercent + "% " + ss.getCompany().getId();

                // Conditional Pass Logic ---
                // Rule: Selling counts as a "Pass" (Green Text) ONLY if the player
                // has NOT bought anything this turn.
                if (getCurrentRound() instanceof StockRound) {
                    StockRound sr = (StockRound) getCurrentRound();
                    if (!sr.hasPlayerBoughtThisTurn()) {
                        // If they haven't bought, this sell counts as a pass (so far)
                        if (!passedPlayers.contains(actorName)) {
                            passedPlayers.add(actorName);
                        }
                    }
                }
                // Note: If they Buy *after* selling, the Buy block above will
                // trigger on the next action and clear this entry. Correct.

            } else if (action instanceof BuyStartItem) {
                String itemName = "start item";
                if (actionString.contains("startItem=")) {
                    try {
                        itemName = actionString.substring(actionString.indexOf("startItem=") + 10).split(",")[0];
                    } catch (Exception e) {
                    }
                }
                entry = "bought " + itemName;
                passedPlayers.clear();
            } else if (action instanceof LayTile) {
                LayTile lt = (LayTile) action;
                String location = lt.getChosenHex().getId();
                String cityName = lt.getChosenHex().getStopName();
                if (Util.hasValue(cityName))
                    location += " (" + cityName + ")";
                entry = "laid tile " + lt.getLaidTile().getId() + " on " + location;
            } else if (action instanceof LayToken) {
                LayToken lt = (LayToken) action;
                String location = lt.getChosenHex().getId();
                String cityName = lt.getChosenHex().getStopName();
                if (Util.hasValue(cityName))
                    location += " (" + cityName + ")";
                entry = "placed token on " + location;
            } else if (action instanceof BuyTrain) {
                BuyTrain bt = (BuyTrain) action;
                String trainName = (bt.getTrain() != null) ? bt.getTrain().getName().split("_")[0] : "?";
                String sourceName = "IPO";

                // Extract Source
                if (actionString.contains("from=") || actionString.contains("portfolio=")) {
                    if (actionString.contains("Bank") || actionString.contains("Pool")
                            || actionString.contains("IPO")) {
                        sourceName = "IPO";
                    } else {
                        // Extract specific source name
                        sourceName = "Market";
                        try {
                            if (actionString.contains("from="))
                                sourceName = actionString.substring(actionString.indexOf("from=") + 5)
                                        .split("[,\\}]")[0];
                            else if (actionString.contains("portfolio="))
                                sourceName = actionString.substring(actionString.indexOf("portfolio=") + 10)
                                        .split("[,\\}]")[0];
                        } catch (Exception e) {
                        }
                    }
                }

                // Extract Price
                int price = bt.getPricePaid();

                if ("IPO".equals(sourceName)) {
                    entry = "buys fresh " + trainName;
                } else {
                    entry = "buys " + trainName + " from " + sourceName + " for " + price;
                }
            } else if (action instanceof SetDividend) {
                SetDividend da = (SetDividend) action;
                String alloc = "paid out";
                if (da.getRevenueAllocation() == SetDividend.WITHHOLD)
                    alloc = "withheld";
                else if (da.getRevenueAllocation() == SetDividend.SPLIT)
                    alloc = "split";
                entry = "revenue " + da.getActualRevenue() + " (" + alloc + ")";
            } else if (action.getClass().getSimpleName().contains("ExchangeForPrussian")) {
                String comp = "Share";
                if (actionString.contains("companyToExchange=")) {
                    try {
                        int start = actionString.indexOf("companyToExchange=") + 18;
                        int end = actionString.indexOf(",", start);
                        if (end == -1)
                            end = actionString.indexOf("]", start);
                        if (end > start)
                            comp = actionString.substring(start, end);
                    } catch (Exception e) {
                    }
                }
                entry = "exchanges " + comp;
            } else if (action instanceof DiscardTrain) {
                DiscardTrain dt = (DiscardTrain) action;
                String tName = (dt.getDiscardedTrain() != null) ? dt.getDiscardedTrain().getName() : "train";
                entry = "discards " + tName;
            } else if (action instanceof NullAction) {
                NullAction na = (NullAction) action;
                if (na.getMode() == NullAction.Mode.PASS) {
                    entry = "passed";
                    if (!passedPlayers.contains(actorName)) {
                        passedPlayers.add(actorName);
                    }
                } else if (na.getMode() == NullAction.Mode.DONE) {
                    entry = "finished turn";
                } else {
                    return;
                }
            } else {

                entry = actionString;
            }
            // Unified Console Output: Single source for Move #, Actor, and Action details
            if (Util.hasValue(entry)) {
                StringBuilder sb = new StringBuilder();
                // Use the provided moveNumber to allow replay overrides
                sb.append("Move #").append(moveNumber).append(" ");
                sb.append("[").append(currentLogPrefix).append("] : ");
                sb.append(entry);
                sb.append(action.isAIAction() ? " [AI]" : " [Human]");
                // Expose the temporal payload directly in the log
                sb.append(" [").append(action.getExecutionTimeSeconds()).append("s]");
                if (isReloading()) {
                    sb.append(" [loaded]");
                }
                log.info(sb.toString());
            }

            // --- 4. SMART APPEND LOGIC ---
            String currentHistory = statusMessageState.value();
            if (currentHistory == null)
                currentHistory = "";
            // If the history already ends with exactly this entry, do not append it again.
            // This fixes the "Double Blue Text" issue (e.g. "Laid Tile X, Laid Tile X").
            if (currentHistory.endsWith(entry)) {
                return;
            }

            if (entry.startsWith("sold ") && currentHistory.contains("sold ")) {
                try {
                    String lastEntry = currentHistory.substring(currentHistory.lastIndexOf("sold "));
                    String compName = entry.substring(entry.lastIndexOf(" ") + 1);

                    if (lastEntry.endsWith(compName)) {
                        int oldPct = Integer.parseInt(lastEntry.split(" ")[1].replace("%", ""));
                        int newPct = Integer.parseInt(entry.split(" ")[1].replace("%", ""));
                        int total = oldPct + newPct;
                        String baseHistory = currentHistory.substring(0, currentHistory.lastIndexOf("sold "));
                        statusMessageState.set(baseHistory + "sold " + total + "% " + compName);
                        return;
                    }
                } catch (Exception e) {
                }
            }

            if (currentHistory.length() > 0) {
                statusMessageState.set(currentHistory + ", " + entry);
            } else {
                statusMessageState.set(entry);
            }

        } catch (Exception e) {
            // SILENT FAIL: Log error but DO NOT crash the game
            log.error("Error generating history log", e);
        }
    }

    private void debugLogPossibleActions() {
        // if (possibleActions == null || possibleActions.getList() == null ||
        // possibleActions.getList().isEmpty()) {
        // return;
        // }

        // StringBuilder sb = new StringBuilder();
        // sb.append("\n=== GM STATE: Current PossibleActions List ===\n");
        // int count = 0;
        // for (PossibleAction pa : possibleActions.getList()) {
        // // Filter out CorrectionModeAction entries from the log output
        // if (pa.toString().contains("CorrectionModeAction")) {
        // continue;
        // }

        // String hash = Integer.toHexString(System.identityHashCode(pa));
        // sb.append(String.format(" [%d] Class: %-20s | Hash: %s | Str: %s\n",
        // count++,
        // pa.getClass().getSimpleName(),
        // hash,
        // pa.toString()));
        // }
        // sb.append("==============================================\n");
        // log.info(sb.toString());
    }

}

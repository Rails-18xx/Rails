package net.sf.rails.ui.swing;

import java.util.*;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import net.sf.rails.algorithms.NetworkAdapter;
import net.sf.rails.algorithms.NetworkGraph; // FIXED: Added missing import
import net.sf.rails.algorithms.NetworkVertex; // FIXED: Added missing import
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialSingleTileLay;
import net.sf.rails.game.special.SpecialTileLay;
import net.sf.rails.game.state.Owner;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;
import net.sf.rails.ui.swing.hexmap.TokenHexUpgrade;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;
import rails.game.correct.ClosePrivate;
import rails.game.correct.OperatingCost;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import net.sf.rails.game.ai.TokenLayOption;
import net.sf.rails.game.ai.TileLayOption;
import net.sf.rails.game.ai.AIPlayer;

import static net.sf.rails.ui.swing.GameUIManager.EXCHANGE_TOKENS_DIALOG;

public class ORUIManager implements DialogOwner {

    private static final Logger log = LoggerFactory.getLogger(ORUIManager.class);

    protected transient GameUIManager gameUIManager;
    protected transient NetworkAdapter networkAdapter;

    protected transient ORWindow orWindow;
    protected transient ORPanel orPanel;
    private transient UpgradesPanel upgradePanel;
    private transient MapPanel mapPanel;
    private transient HexMap map;
    protected transient MessagePanel messagePanel;
    private transient RemainingTilesWindow remainingTiles;

    protected transient FloatingUpgradesDialog floatingUpgradesDialog;
  

    protected OperatingRound oRound;
    private transient List<PublicCompany> companies;

    protected PublicCompany orComp;
    protected int orCompIndex;

    private LocalSteps localStep;

    private List<TileLayOption> currentValidTileLays = new ArrayList<>();
    private List<TokenLayOption> currentValidTokenLays = new ArrayList<>();
    private Integer[] separatorLines = null; // For 1826 Token Exchange

    protected final GUIHexUpgrades hexUpgrades = GUIHexUpgrades.create();

    /* Local substeps */
    public enum LocalSteps {
        INACTIVE, SELECT_HEX, SELECT_UPGRADE, SET_REVENUE, SELECT_PAYOUT
    }

    private boolean showHomeIdentifiers = true;
    private boolean showRevenueRoutes = true;
    private boolean showFancyCityValues = true; // Default to off

    public boolean isShowFancyCityValues() {
        return showFancyCityValues;
    }

    private boolean showFloatingTiles = true;

    public boolean isShowFloatingTiles() {
        return showFloatingTiles;
    }

    public void toggleFloatingTiles() {
        this.showFloatingTiles = !this.showFloatingTiles;
        log.info("DEBUG: Toggled Floating Tiles: " + showFloatingTiles);
        // Map repaint or UI refresh trigger will go here
    }

    public void toggleFancyCityValues() {
        this.showFancyCityValues = !this.showFancyCityValues;
        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }
    }

    public boolean isShowHomeIdentifiers() {
        return showHomeIdentifiers;
    }

    public void toggleHomeIdentifiers() {
        this.showHomeIdentifiers = !this.showHomeIdentifiers;
        if (map != null)
            map.repaintAll(new Rectangle(map.getSize()));
    }

    public boolean isShowRevenueRoutes() {
        return showRevenueRoutes;
    }

    public void toggleRevenueRoutes() {
        this.showRevenueRoutes = !this.showRevenueRoutes;
        // Trigger the ORPanel to immediately redraw or clear the paths
        if (orPanel != null)
            orPanel.redrawRoutes();
    }

    private boolean showFriendlyHexes = true;

    private boolean showDestinationMarkers = true;

    public boolean isShowDestinationMarkers() {
        return showDestinationMarkers;
    }

    public void toggleDestinationMarkers() {
        this.showDestinationMarkers = !this.showDestinationMarkers;

        // Refresh the highlights to catch any 1870 destination markers
        updateCompanyHighlights();

        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }
    }

    public boolean isShowFriendlyHexes() {
        return showFriendlyHexes;
    }

    public void toggleFriendlyHexes() {
        this.showFriendlyHexes = !this.showFriendlyHexes;

        // Force an immediate update of the highlight states
        updateCompanyHighlights();

        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }

        log.info("DEBUG: Toggled Friendly Hexes: " + showFriendlyHexes);
    }

    /* Keys of dialogs owned by this class */
    public static final String SELECT_DESTINATION_COMPANIES_DIALOG = "SelectDestinationCompanies";
    public static final String REPAY_LOANS_DIALOG = "RepayLoans";
    public static final String GOT_PERMISSION_DIALOG = "AskedPermissionDialog";
    public static final String TOKEN_EXCHANGE_DIALOG = "SelectTokensToExchange";

    public ORUIManager() {
    }

    private boolean showCompanyHighlights = true; // Default to ON, or set to false

    public void toggleCompanyHighlights() {
        this.showCompanyHighlights = !this.showCompanyHighlights;

        // Force an immediate update
        updateCompanyHighlights();
        map.repaintAll(new Rectangle(map.getSize()));

        log.info("DEBUG: Toggled Company Highlights: " + showCompanyHighlights);
    }

    // FIXED: Must be public for subclasses in other packages
    public void setGameUIManager(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        this.networkAdapter = NetworkAdapter.create(gameUIManager.getRoot());
    }

    // FIXED: Must be public for subclasses in other packages (Fixes @Override error
    // in _18Scan/_1837)
    public void init(ORWindow orWindow) {
        this.orWindow = orWindow;
        orPanel = orWindow.getORPanel();
        mapPanel = orWindow.getMapPanel();
        upgradePanel = orWindow.getUpgradePanel();
        upgradePanel.setHexUpgrades(hexUpgrades);
        map = mapPanel.getMap();
        messagePanel = orWindow.getMessagePanel();
        floatingUpgradesDialog = new FloatingUpgradesDialog(orWindow, this);
    }

    protected void initOR(OperatingRound or) {
        oRound = or;
        companies = or.getOperatingCompanies();
        orWindow.activate(oRound);
        this.orCompIndex = -1;
        this.orComp = null;
    }

    public void finish() {
        if (orWindow != null)
            orWindow.finish();
        if (upgradePanel != null)
            upgradePanel.setInactive();

        if (floatingUpgradesDialog != null) {
            floatingUpgradesDialog.setVisible(false);
        }

        setLocalStep(LocalSteps.INACTIVE);

        if (hexUpgrades != null && map != null) {
            for (GUIHex guiHex : hexUpgrades.getHexes()) {
                guiHex.setState(GUIHex.State.NORMAL);
            }
            hexUpgrades.clear();
            map.selectHex(null);
        }

        if (orPanel != null) {
            orPanel.finish();
        }

        if (!(gameUIManager.getCurrentRound() instanceof ShareSellingRound)) {
            orComp = null;
        }
    }

    public void updateScale() {
        if (orPanel != null) {
            orPanel.updateScale();
        }
    }

    protected void setLocalStep(LocalSteps localStep) {
        if (this.localStep == localStep)
            return;
        SoundManager.notifyOfORLocalStep(localStep);
        this.localStep = localStep;
        updateMessage();
        if (upgradePanel != null) {
            switch (localStep) {
                case INACTIVE:
                    upgradePanel.setInactive();
                    if (floatingUpgradesDialog != null)
                        floatingUpgradesDialog.setVisible(false); // <--- ADD
                    break;
                case SELECT_HEX:
                    upgradePanel.setActive();
                    if (floatingUpgradesDialog != null)
                        floatingUpgradesDialog.setVisible(false); // <--- ADD
                    break;
                case SELECT_UPGRADE:
                    upgradePanel.setSelect(map.getSelectedHex());
                    break;
                default:
                    upgradePanel.setInactive();
                    if (floatingUpgradesDialog != null)
                        floatingUpgradesDialog.setVisible(false); // <--- ADD
            }
        }
    }

    public void updateStatus(boolean myTurn) {
        updateStatus(null, myTurn);
    }

    public void updateStatus(PossibleAction actionToComplete, boolean myTurn) {

        // // Inject the highlight update here
        // updateCompanyHighlights();

        if (map != null && getRoot() != null && getRoot().getMapManager() != null) {
            String[] testHexIds = { "L12", "M13" };

            for (String id : testHexIds) {
                // Get the "Engine Truth" directly from the MapManager
                net.sf.rails.game.MapHex engineHex = getRoot().getMapManager().getHex(id);
                // Get the "UI Wrapper"
                net.sf.rails.ui.swing.hexmap.GUIHex uiHex = map.getHex(engineHex);

                if (uiHex != null && engineHex != null) {
                    // The specific MapHex instance the UI is currently holding
                    net.sf.rails.game.MapHex uiHeldHex = uiHex.getHex();

                    int uiHash = System.identityHashCode(uiHeldHex);
                    int engineHash = System.identityHashCode(engineHex);

                    String uiTile = (uiHeldHex.getCurrentTile() != null) ? uiHeldHex.getCurrentTile().getId() : "null";
                    String engineTile = (engineHex.getCurrentTile() != null) ? engineHex.getCurrentTile().getId()
                            : "null";

                }
            }
        }

        RoundFacade currentRound = gameUIManager.getCurrentRound();
        PossibleActions possibleActions = getPossibleActions();

        if (gameUIManager != null && gameUIManager.getStatusWindow() != null
                && gameUIManager.getStatusWindow().getGameStatus() != null) {
            gameUIManager.getStatusWindow().getGameStatus().refreshDashboard();
        }

        // CRITICAL: Rebind UI components to the current Model.
        // After an Undo/Load, the MapHex objects in GUIHex are "stale" (dead objects).
        // We must point them to the live MapHex objects in the current GameState.
        rebindVisualHexes();

        // Force repaint to clear any "Ghost" pixels from the previous state
        if (mapPanel != null)
            mapPanel.repaint();

        // Extract Undo/Redo
        GameAction undoAction = null;
        GameAction redoAction = null;
        if (myTurn && possibleActions != null) {
            for (GameAction action : possibleActions.getType(GameAction.class)) {
                if (action.getMode() == GameAction.Mode.UNDO)
                    undoAction = action;
                if (action.getMode() == GameAction.Mode.REDO)
                    redoAction = action;
            }
        }

        if (possibleActions == null) {
            if (orPanel != null)
                orPanel.disableButtons();
            return;
        }

        // --- SPECIAL MODE DETECTION ---
        boolean hasSpecialActions = false;
        if (possibleActions != null && !possibleActions.isEmpty()) {
            PossibleAction first = possibleActions.getList().get(0);

            if (first instanceof GuiTargetedAction) {
                hasSpecialActions = true;
            } else if (!possibleActions.getType(LayTileAndHomeTokenAction.class).isEmpty()) {
                hasSpecialActions = true;
            }
        }

        if (hasSpecialActions) {
            setLocalStep(LocalSteps.INACTIVE);
            if (orPanel != null) {
                orPanel.setSpecialMode(true);
                orPanel.updateDynamicActions(possibleActions.getList());

                orPanel.enableUndo(undoAction);
                orPanel.enableRedo(redoAction);
                orPanel.revalidate();
                orPanel.repaint();
                orPanel.redisplay();
            }
            return;
        }

        // --- STANDARD OPERATING ROUND ---
        if (!(currentRound instanceof OperatingRound)) {
            if (orPanel != null) {
                orPanel.disableButtons();
                // RIGOROUS CLEANUP: If the engine transitions to a non-Operating Round
                // the ORUIManager aborts further updates. We must force the ORPanel to
                // wipe its display so special actions from the previous turn do not linger.
                orPanel.finish();
            }
            return;
        }

        this.oRound = (OperatingRound) currentRound;
        if (orPanel != null)
            orPanel.setSpecialMode(false);

        PublicCompany currentEngineCompany = oRound.getOperatingCompany();
        int currentEngineIndex = oRound.getOperatingCompanyIndex();
        GameDef.OrStep orStep = oRound.getStep();

        if (currentEngineCompany == null || currentEngineIndex < 0) {
            if (this.orCompIndex >= 0)
                orPanel.finishORCompanyTurn(this.orCompIndex);
            setLocalStep(LocalSteps.INACTIVE);
            orPanel.disableButtons();
            return;
        }

        boolean isCompanyChangeOrInitialization = (this.orComp == null || this.orComp != currentEngineCompany);
        if (isCompanyChangeOrInitialization) {
            if (this.orCompIndex >= 0)
                orPanel.finishORCompanyTurn(this.orCompIndex);
            setLocalStep(LocalSteps.INACTIVE);
            this.orCompIndex = currentEngineIndex;
            this.orComp = currentEngineCompany;
            orPanel.initORCompanyTurn(this.orComp, this.orCompIndex);
            List<PublicCompany> currentEngineCompanies = oRound.getOperatingCompanies();
            if (this.companies == null || !Iterables.elementsEqual(this.companies, currentEngineCompanies)) {
                this.companies = currentEngineCompanies;
            }
        }

        // Status Text
        String historyText = gameUIManager.getGameManager().getLastActionSummary();
        Player currentPlayer = getRoot().getPlayerManager().getCurrentPlayer();
        String playerName = (currentPlayer != null) ? currentPlayer.getId().toUpperCase() : "PLAYER?";
        String companyName = (orComp != null) ? " (" + orComp.getId() + ")" : "";
        String stepName = (orStep != null) ? orStep.toString().replace('_', ' ') : "Operating";

        String combinedText = "<html><font color='blue' size='4'>" + historyText + "</font><br>" +
                "<font color='red' size='6'>Thinking: <b>" + playerName + "</b>" + companyName + " - " + stepName
                + "</font></html>";

        messagePanel.setMessage(combinedText);
        if (gameUIManager.statusWindow != null) {
            gameUIManager.statusWindow.updateActivityPanel(combinedText);
        }

        // AI Logic
        if (gameUIManager.isCurrentPlayerAI() && orStep == GameDef.OrStep.DISCARD_TRAINS) {
            PossibleActions freshActions = getPossibleActions();
            List<DiscardTrain> discardActions = freshActions.getType(DiscardTrain.class);
            if (!discardActions.isEmpty()) {
                DiscardTrain bestAction = null;
                int minTrainValue = Integer.MAX_VALUE;
                for (DiscardTrain action : discardActions) {
                    if (action.getDiscardedTrain() == null)
                        continue;
                    int trainValue = action.getDiscardedTrain().getCost();
                    if (trainValue < minTrainValue) {
                        minTrainValue = trainValue;
                        bestAction = action;
                    }
                }
                if (bestAction != null) {
                    bestAction.setAIAction(true);
                    orWindow.process(bestAction);
                    return;
                }
            }
        }

        // 1. Force Map Repaint: The Model (Game State) may have changed (e.g., Undo),
        // so we must redraw the board pixels even if we are not in a 'Map Phase'.
        // Fix: Force HexMap layers to mark their buffers as dirty.
        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }

        // 2. Map Interaction Logic
        if (orStep == GameDef.OrStep.LAY_TRACK || orStep == GameDef.OrStep.LAY_TOKEN) {
            setMapRelatedActions(possibleActions);
        } else {
            // Cleanup: If we transitioned (or Undid) into a non-map phase (like Buy Train),
            // we must clear any stale highlights or selected hexes from the previous state.
            if (hexUpgrades != null && hexUpgrades.hasElements()) {
                hexUpgrades.clear();
                if (map != null)
                    map.selectHex(null);
            }
        }

        // Automation Checks
        boolean isUndoOrRedo = false;
        PossibleAction lastAction = gameUIManager.getLastAction();
        if (lastAction instanceof GameAction) {
            GameAction.Mode mode = ((GameAction) lastAction).getMode();
            if (mode == GameAction.Mode.UNDO || mode == GameAction.Mode.FORCED_UNDO || mode == GameAction.Mode.REDO) {
                isUndoOrRedo = true;
            }
        }

        boolean hasSpecialOR = false;
        if (possibleActions != null) {
            for (PossibleAction pa : possibleActions.getList()) {
                if (pa instanceof rails.game.action.SpecialORAction ||
                        pa instanceof net.sf.rails.game.specific._1817.action.PayLoanInterest_1817 ||
                        pa instanceof net.sf.rails.game.specific._1817.action.RepayLoans_1817) {
                    hasSpecialOR = true;
                    break;
                }
            }
        }

        if (!isUndoOrRedo) {
            if (orStep == GameDef.OrStep.LAY_TRACK) {
                boolean canLay = !possibleActions.getType(LayTile.class).isEmpty();
                boolean canSpecial = !possibleActions.getType(UseSpecialProperty.class).isEmpty();
                if (!canLay && !canSpecial && !hasSpecialOR) {
                    // Only return if we actually processed the SKIP.
                    // If no SKIP exists (e.g., only DONE exists), fall through to update UI.
                    if (processNullAction(possibleActions, NullAction.Mode.SKIP))
                        return;
                }
            } else if (orStep == GameDef.OrStep.LAY_TOKEN) {
                if (possibleActions.getType(LayToken.class).isEmpty() && !hasSpecialOR) {
                    if (processNullAction(possibleActions, NullAction.Mode.SKIP))
                        return;
                }
            } else if (orStep == GameDef.OrStep.BUY_TRAIN) {
                if (possibleActions.getType(BuyTrain.class).isEmpty() && !hasSpecialOR) {
                    // This was the specific bug: It tried to SKIP, failed (because only DONE
                    // existed),
                    // but returned anyway. Now it will fall through.
                    if (processNullAction(possibleActions, NullAction.Mode.SKIP))
                        return;
                }
            }
        }

        // Delegate UI Setup
        if (orStep == GameDef.OrStep.LAY_TRACK) {
            orPanel.initTileLayingStep();
            orPanel.setupConfirm();
            orPanel.updateDynamicActions(possibleActions.getList());
            updateHexBuildNumbers(true);
        } else if (orStep == GameDef.OrStep.LAY_TOKEN) {
            boolean hasButtonActions = false;
            List<LayToken> tokenActions = possibleActions.getType(LayToken.class);
            if (!tokenActions.isEmpty()) {
                hasButtonActions = true;
                for (LayToken action : tokenActions) {
                    if (action instanceof LayBaseToken && ((LayBaseToken) action).getType() != LayBaseToken.HOME_CITY) {
                        hasButtonActions = false;
                        break;
                    } else if (!(action instanceof LayBaseToken)) {
                        hasButtonActions = false;
                        break;
                    }
                }
            }

            if (hasButtonActions) {
                if (orPanel != null)
                    orPanel.resetHexCycle();
                orPanel.initTokenLayingStep();
                orPanel.updateDynamicActions(possibleActions.getList());
            } else {
                orPanel.initTokenLayingStep();
                orPanel.setupConfirm();
                orPanel.updateDynamicActions(possibleActions.getList());
                if (localStep == LocalSteps.SELECT_UPGRADE)
                    orPanel.enableConfirm(true);

                updateHexBuildNumbers(true);
            }
        } else if (orStep == GameDef.OrStep.CALC_REVENUE) {
            if (orPanel != null)
                orPanel.resetHexCycle();
            setLocalStep(LocalSteps.SELECT_PAYOUT);
            orPanel.initTrainBuying(new ArrayList<>());
            orPanel.updateDynamicActions(possibleActions.getList());

            if (possibleActions.contains(SetDividend.class)) {
                SetDividend action = possibleActions.getType(SetDividend.class).get(0);
                SetDividend completedAction = (actionToComplete instanceof SetDividend) ? (SetDividend) actionToComplete
                        : action;
                orPanel.initPayoutStep(orCompIndex, completedAction,
                        completedAction.isAllocationAllowed(SetDividend.WITHHOLD),
                        completedAction.isAllocationAllowed(SetDividend.SPLIT),
                        completedAction.isAllocationAllowed(SetDividend.PAYOUT));
            }
        } else if (orStep == GameDef.OrStep.BUY_TRAIN) {
            if (orPanel != null)
                orPanel.resetHexCycle();
            setLocalStep(LocalSteps.INACTIVE);
            orPanel.initTrainBuying(possibleActions.getType(BuyTrain.class));
            PossibleActions freshActions = getPossibleActions();
            orPanel.updateDynamicActions(freshActions != null ? freshActions.getList() : possibleActions.getList());
        } else if (orStep == GameDef.OrStep.DISCARD_TRAINS) {
            if (orPanel != null)
                orPanel.resetHexCycle();
            setLocalStep(LocalSteps.INACTIVE);
            orPanel.updateDynamicActions(possibleActions.getList());
        } else {
            orPanel.initTrainBuying(new ArrayList<>());
        }

        orPanel.enableUndo(undoAction);
        orPanel.enableRedo(redoAction);

        // Inject the highlight update here
        updateCompanyHighlights();

        orPanel.redisplay();
    }

    private boolean processNullAction(PossibleActions actions, NullAction.Mode mode) {
        for (PossibleAction pa : actions.getList()) {
            if (pa instanceof NullAction && ((NullAction) pa).getMode() == mode) {
                orWindow.process(pa);
                return true;
            }
        }
        return false; // Action not found
    }

    private boolean showMapMarkings = true;
    private boolean showHexNames = true;
    private boolean showTerrainCosts = true;

    public boolean isShowTerrainCosts() {
        return showTerrainCosts;
    }

    public void toggleTerrainCosts() {
        this.showTerrainCosts = !this.showTerrainCosts;
        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }
    }

    public boolean isShowHexNames() {
        return showHexNames;
    }

    public void toggleHexNames() {
        this.showHexNames = !this.showHexNames;
        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }
    }

    /**
     * Resets all map overlays to hidden.
     */
    public void hideAllOverlays() {
        this.showHexNames = false;
        this.showTerrainCosts = false;
        this.showFriendlyHexes = false;
        this.showDestinationMarkers = false;
        this.showHomeIdentifiers = false;
        this.showRevenueRoutes = false;
        this.showFancyCityValues = false;

        // Synchronize internal highlight logic and train paths[cite: 1, 3]
        updateCompanyHighlights();
        if (orPanel != null) {
            orPanel.redrawRoutes();
        }

        // Repaint the entire map to reflect all changes[cite: 1]
        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }
        log.info("DEBUG: All 6 map overlays hidden via Master Reset.");
    }

    public boolean isShowMapMarkings() {
        return showMapMarkings;
    }

    public void toggleMapMarkings() {
        // --- START FIX ---
        this.showMapMarkings = !this.showMapMarkings;

        this.showHexNames = this.showMapMarkings;
        this.showFriendlyHexes = this.showMapMarkings;
        this.showDestinationMarkers = this.showMapMarkings;
        this.showHomeIdentifiers = this.showMapMarkings;
        this.showRevenueRoutes = this.showMapMarkings;

        updateCompanyHighlights();

        if (orPanel != null) {
            orPanel.redrawRoutes();
        }

        if (map != null) {
            map.repaintAll(new Rectangle(map.getSize()));
        }
        // --- END FIX ---
    }

    public void processAction(String command, List<PossibleAction> actions, Component source) {

        if (command.equals(ORPanel.REM_TILES_CMD)) {
            displayRemainingTiles();
            return;
        }

        RoundFacade currentRound = gameUIManager.getCurrentRound();
        boolean isOR = (currentRound instanceof OperatingRound);
        // Allow if OR, OR if we have special actions (GuiTargetedAction)
        // Allow NullAction (Pass/Done) to pass through even in non-OR rounds
        boolean hasSpecialActions = (actions != null && !actions.isEmpty()
                && (actions.get(0) instanceof GuiTargetedAction || actions.get(0) instanceof NullAction));

        if (!isOR && !hasSpecialActions) {
            return;
        }

        try {
            PossibleAction actionToProcess = (actions != null && !actions.isEmpty()) ? actions.get(0) : null;

            if (actionToProcess instanceof BuyTrain) {
                processBuyTrain((BuyTrain) actionToProcess);
                return;
            }

            if (actionToProcess != null && !processGameSpecificActions(actions)) {
                if (actionToProcess instanceof SetDividend) {
                    setDividend(command, (SetDividend) actionToProcess);
                } else if (actionToProcess instanceof BuyBonusToken) {
                    buyBonusToken((BuyBonusToken) actionToProcess);
                } else if (actionToProcess instanceof NullAction || actionToProcess instanceof GameAction) {
                    orWindow.process(actionToProcess);
                } else if (actionToProcess instanceof ReachDestinations) {
                    reachDestinations((ReachDestinations) actionToProcess);
                } else if (actionToProcess instanceof TakeLoans) {
                    takeLoans((TakeLoans) actionToProcess);
                } else if (actionToProcess instanceof RepayLoans) {
                    repayLoans((RepayLoans) actionToProcess);
                } else if (actionToProcess instanceof UseSpecialProperty) {
                    useSpecialProperty((UseSpecialProperty) actionToProcess);
                } else if (actionToProcess instanceof ClosePrivate) {
                    gameUIManager.processAction(actionToProcess);
                } else if (actionToProcess instanceof BuyPrivate) {
                    processBuyPrivate((BuyPrivate) actionToProcess);
                } else {
                    orWindow.process(actionToProcess);
                }
            } else {
                if (command.equals(ORPanel.OPERATING_COST_CMD)) {
                    operatingCosts();
                }
            }
        } catch (Exception e) {
            log.error("Error processing action command: " + command, e);
        }

    }

    // --- RESTORED PLUMBING METHODS ---

    public GameUIManager getGameUIManager() {
        return gameUIManager;
    }

    public HexMap getMap() {
        return map;
    }

    public ORWindow getORWindow() {
        return orWindow;
    }

    public ORPanel getORPanel() {
        return orPanel;
    }

    public UpgradesPanel getUpgradePanel() {
        return upgradePanel;
    }

    public Integer[] getSeparatorLines() {
        return separatorLines;
    }

    public void clearSeparatorLines() {
        separatorLines = null;
    }

    public net.sf.rails.ui.swing.hexmap.HexMap getHexMap() {
        return this.map;
    }

    public boolean hexClicked(GUIHex clickedHex, GUIHex selectedHex, boolean rightClick) {
        if (localStep == null)
            return false;

        if (selectedHex == clickedHex) {
            if (localStep == LocalSteps.SELECT_UPGRADE) {
                if (rightClick)
                    upgradePanel.nextUpgrade();
                else
                    upgradePanel.nextSelection();

                return true;
            }
            return false;
        }

        if (clickedHex == null) {
            if (localStep == LocalSteps.SELECT_UPGRADE) {
                if (selectedHex != null)
                    map.selectHex(null);
                setLocalStep(LocalSteps.SELECT_HEX);

                return true;
            }
            return false;
        }

        if (hexUpgrades.containsVisible(clickedHex)) {
            switch (localStep) {
                case SELECT_HEX:
                    if (!gotPermission(clickedHex))
                        return false;
                case SELECT_UPGRADE:
                    map.selectHex(clickedHex);
                    setLocalStep(LocalSteps.SELECT_UPGRADE);
                    if (upgradePanel != null)
                        upgradePanel.setSelect(clickedHex);
                    if (orPanel != null)
                        orPanel.enableConfirm(true);

                    // --- SHOW FLOATING DIALOG NEAR HEX ---
                    if (showFloatingTiles && floatingUpgradesDialog != null) {
                        try {
                            java.awt.Point screenPos = mapPanel.getLocationOnScreen();
                            java.awt.Rectangle hexBounds = clickedHex.getBounds();
                            // Position to the right and slightly down from the clicked hex
                            floatingUpgradesDialog.showUpgrades(clickedHex, hexUpgrades,
                                    new java.awt.Point(screenPos.x + hexBounds.x + hexBounds.width + 10,
                                            screenPos.y + hexBounds.y));
                        } catch (Exception e) {
                            // Fallback if component hasn't rendered yet
                            floatingUpgradesDialog.showUpgrades(clickedHex, hexUpgrades, null);
                        }
                    }

                    return true;
                default:
                    return false;
            }
        }

        switch (localStep) {
            case SELECT_UPGRADE:
                map.selectHex(null);
                setLocalStep(LocalSteps.SELECT_HEX);

                return false;
            default:
                return false;
        }
    }

    public void updateHexBuildNumbers(boolean show) {
        if (map == null || orPanel == null)
            return;

        Rectangle mapBounds = new Rectangle(map.getSize());

        if (!show) {
            for (GUIHex guiHex : map.getHexes()) {
                if (guiHex.getCustomOverlayText() != null) {
                    guiHex.setCustomOverlayText(null);
                }
            }
            map.repaintAll(mapBounds);
            return;
        }

        Collection<GUIHex> hexes = hexUpgrades.getHexes();

        // Was: if (hexes == null || hexes.isEmpty()) return;
        // Change: Removed early exit. We must proceed to repaintAll(mapBounds)
        // at the end of the method to clear "Ghost Tiles" after an Undo,
        // even if there are no specific hex numbers to update.

        if (hexes != null && !hexes.isEmpty()) {
            for (GUIHex guiHex : hexes) {
                GUIHex.State state = guiHex.getState();
                // Show text for valid (Green/Red) AND invalid (Pink) hexes
                if (state == GUIHex.State.SELECTABLE || state == GUIHex.State.TOKEN_SELECTABLE
                        || state == GUIHex.State.INVALIDS) {
                    String hexId = guiHex.getHex().getId();
                    StringBuilder overlayText = new StringBuilder(hexId);

                    for (HexUpgrade upgrade : hexUpgrades.getUpgrades(guiHex)) {
                        if (upgrade instanceof TokenHexUpgrade) {
                            // The upgrade object already calculated the cost during 'validates()'.
                            // We just retrieve it.
                            int cost = ((TokenHexUpgrade) upgrade).getCost();

                            if (cost > 0) {
                                overlayText.append("<br>").append(gameUIManager.format(cost));
                            }
                            break;
                        }
                    }

                    guiHex.setCustomOverlayText("<html>" + overlayText.toString() + "</html>");
                } else {
                    guiHex.setCustomOverlayText(null);
                }
            }
        }

        // This line forces the Layers to mark their buffers as dirty and redraw from
        // the Model.
        map.repaintAll(mapBounds);
    }
    // --- OTHER HELPERS ---

    private void displayRemainingTiles() {
        if (remainingTiles == null)
            remainingTiles = new RemainingTilesWindow(orWindow);
        else
            remainingTiles.activate();
    }

    protected boolean processGameSpecificActions(List<PossibleAction> actions) {
        return false;
    }

    protected void setDividend(String command, SetDividend action) {
        if (!command.equals(ORPanel.SET_REVENUE_CMD))
            orWindow.process(action);
    }

    private void buyBonusToken(BuyBonusToken action) {
        orWindow.process(action);
    }

    protected void reachDestinations(ReachDestinations action) {
        List<String> options = new ArrayList<>();
        for (PublicCompany company : action.getPossibleCompanies())
            options.add(company.getId());
        if (options.size() > 0) {
            orWindow.setVisible(true);
            orWindow.toFront();
            CheckBoxDialog dialog = new CheckBoxDialog(SELECT_DESTINATION_COMPANIES_DIALOG, this, orWindow,
                    LocalText.getText("DestinationsReached"), LocalText.getText("DestinationsReachedPrompt"),
                    options.toArray(new String[0]));
            setCurrentDialog(dialog, action);
        }
    }

    protected boolean gotPermission(GUIHex guiHex) {
        MapHex hex = guiHex.getHex();
        if (!hex.isReservedForCompany() || !hex.isPreprintedTileCurrent())
            return true;
        HexUpgrade hexUpgrade = (HexUpgrade) hexUpgrades.getUpgrades(guiHex).toArray()[0];
        if (!(hexUpgrade instanceof TileHexUpgrade))
            return true;
        TileHexUpgrade upgrade = (TileHexUpgrade) hexUpgrade;
        LayTile action = upgrade.getAction();
        if (action.getPlayer().equals(guiHex.getHex().getReservedForCompany().getPresident()))
            return true;

        ConfirmationDialog dialog = new ConfirmationDialog(GOT_PERMISSION_DIALOG, this, orWindow,
                LocalText.getText("GotPermission"),
                LocalText.getText("GotPermissionDialog", guiHex.getHex().getReservedForCompany().getPresident(),
                        upgrade.getHex().getHex()),
                LocalText.getText("Yes"), LocalText.getText("No"));
        setCurrentDialog(dialog, action);
        return true;
    }

    public void confirmUpgrade() {
        HexUpgrade upgrade = hexUpgrades.getActiveUpgrade();
        if (upgrade instanceof TileHexUpgrade)
            layTile((TileHexUpgrade) upgrade);
        if (upgrade instanceof TokenHexUpgrade)
            layToken((TokenHexUpgrade) upgrade);
    }

    public void skipUpgrade() {
        if (getPossibleActions().containsCorrections()) {
            map.selectHex(null);
            setLocalStep(LocalSteps.SELECT_HEX);
        } else {
            orWindow.process(new NullAction(gameUIManager.getRoot(), NullAction.Mode.SKIP));
        }
    }

    protected void layTile(TileHexUpgrade upgrade) {
        LayTile allowance = upgrade.getAction();
        allowance.setChosenHex(upgrade.getHex().getHex());
        allowance.setOrientation(upgrade.getCurrentRotation().getTrackPointNumber());
        allowance.setLaidTile(upgrade.getUpgrade().getTargetTile());
        allowance.setRelayBaseTokens(upgrade.isRelayBaseTokens());
        if (!orWindow.process(allowance))
            setLocalStep(LocalSteps.SELECT_HEX);
    }

    private void layToken(TokenHexUpgrade upgrade) {
        LayToken action = upgrade.getAction();
        if (action instanceof LayBaseToken)
            layBaseToken(upgrade);
        else if (action instanceof LayBonusToken)
            layBonusToken(upgrade);
    }

    private void layBaseToken(TokenHexUpgrade upgrade) {
        LayBaseToken action = (LayBaseToken) upgrade.getAction();
        action.setChosenHex(upgrade.getHex().getHex());
        if (upgrade.getSelectedStop() != null)
            action.setChosenStation(upgrade.getSelectedStop().getRelatedStationNumber());
        if (!orWindow.process(action))
            setLocalStep(LocalSteps.SELECT_HEX);
    }

    public void layBonusToken(TokenHexUpgrade upgrade) {
        LayToken action = upgrade.getAction();
        GUIHex selectedHex = mapPanel.getMap().getSelectedHex();
        if (selectedHex != null) {
            action.setChosenHex(selectedHex.getHex());
            if (orWindow.process(action)) {
                upgradePanel.setActive();
                map.selectHex(null);
                map.repaintTokens(selectedHex.getBounds());
            }
        }
    }

    public void operatingCosts() {
        List<String> textOC = new ArrayList<>();
        List<OperatingCost> actionOC = getPossibleActions().getType(OperatingCost.class);
        for (OperatingCost ac : actionOC) {
            String cost = ac.isFreeEntryAllowed() ? LocalText.getText("OCAmountEntry")
                    : gameUIManager.format(ac.getAmount());
            if (ac.getOCType() == OperatingCost.OCType.LAY_TILE)
                textOC.add(LocalText.getText("OCLayTile", cost));
            if (ac.getOCType() == OperatingCost.OCType.LAY_BASE_TOKEN)
                textOC.add(LocalText.getText("OCLayBaseToken", cost));
        }
        if (!textOC.isEmpty()) {
            String chosenOption = (String) JOptionPane.showInputDialog(orWindow, LocalText.getText("OCSelectMessage"),
                    LocalText.getText("OCSelectTitle"), JOptionPane.QUESTION_MESSAGE, null, textOC.toArray(),
                    textOC.get(0));
            if (chosenOption != null) {
                OperatingCost chosenAction = actionOC.get(textOC.indexOf(chosenOption));
                if (chosenAction.isFreeEntryAllowed()) {
                    String input = JOptionPane.showInputDialog(orWindow,
                            LocalText.getText("OCDialogMessage", chosenOption));
                    try {
                        chosenAction.setAmount(Integer.parseInt(input));
                    } catch (Exception e) {
                        chosenAction.setAmount(0);
                    }
                }
                if (orWindow.process(chosenAction))
                    updateMessage();
            }
        }
    }

    public void buyPrivate() {
        List<String> options = new ArrayList<>();
        List<BuyPrivate> actions = getPossibleActions().getType(BuyPrivate.class);
        for (BuyPrivate bp : actions) {
            String price = bp.getMinimumPrice() < bp.getMaximumPrice()
                    ? gameUIManager.format(bp.getMinimumPrice()) + "..." + gameUIManager.format(bp.getMaximumPrice())
                    : gameUIManager.format(bp.getMaximumPrice());
            options.add(LocalText.getText("BuyPrivatePrompt", bp.getPrivateCompany().getId(),
                    bp.getPrivateCompany().getOwner().getId(), price));
        }
        if (!actions.isEmpty()) {
            String chosen = (String) JOptionPane.showInputDialog(orWindow, LocalText.getText("BUY_WHICH_PRIVATE"),
                    LocalText.getText("WHICH_PRIVATE"), JOptionPane.QUESTION_MESSAGE, null, options.toArray(),
                    options.get(0));
            if (chosen != null)
                processBuyPrivate(actions.get(options.indexOf(chosen)));
        }
    }

    public void processBuyPrivate(BuyPrivate action) {
        if (action == null)
            return;

        int minPrice = action.getMinimumPrice();
        int maxPrice = action.getMaximumPrice();
        int price = minPrice;

        // Gather Buyer Information
        PublicCompany buyer = this.orComp;
        String buyerId = (buyer != null) ? buyer.getId() : "Unknown";
        String directorName = (buyer != null && buyer.getPresident() != null) ? buyer.getPresident().getId()
                : "Unknown";

        // Gather Private Company Information
        PrivateCompany pc = action.getPrivateCompany();
        String pcName = (pc.getName() != null) ? pc.getName() : "Unknown";
        String pcId = (pc.getId() != null) ? pc.getId() : "Unknown";
        String ownerName = (pc.getOwner() != null) ? pc.getOwner().getId() : "Unknown";

        // Extract Hover/Info text and format for HTML display
        String infoText = pc.getInfoText();
        if (infoText != null && !infoText.isEmpty()) {
            infoText = infoText.replaceAll("\\n", "<br>");
        } else {
            infoText = "No additional information available.";
        }

        String formattedMin = gameUIManager.format(minPrice);
        String formattedMax = gameUIManager.format(maxPrice);

        // Build the rich HTML dialog content
        StringBuilder msg = new StringBuilder();
        msg.append("<html><div style='width: 350px; font-family: sans-serif;'>");
        msg.append("<h3 style='margin-top: 0;'>Buy Private Company</h3>");

        msg.append("<table width='100%'>");
        msg.append("<tr><td><b>Buyer:</b></td><td>").append(buyerId).append(" (Director: ").append(directorName)
                .append(")</td></tr>");
        msg.append("<tr><td><b>Private:</b></td><td>").append(pcName).append(" [").append(pcId).append("]</td></tr>");
        msg.append("<tr><td><b>Owner:</b></td><td>").append(ownerName).append("</td></tr>");
        msg.append("<tr><td><b>Price Range:</b></td><td>").append(formattedMin).append(" - ").append(formattedMax)
                .append("</td></tr>");
        msg.append("</table><br>");

        msg.append("<b>Details:</b><br>");
        msg.append("<div style='padding: 5px; border: 1px solid gray; background-color: #f9f9f9; font-size: 10px;'>");
        msg.append(infoText).append("</div><br>");

        // Handle Variable vs Fixed Price Dialogs
        if (minPrice != maxPrice) {
            msg.append("<b>Enter purchase price:</b></div></html>");

            // Note: showInputDialog returns an Object. We cast it to String if it's not
            // null.
            Object inputObj = JOptionPane.showInputDialog(
                    orWindow,
                    msg.toString(),
                    "Negotiate Price",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    String.valueOf(price));

            if (inputObj == null) {
                return; // User canceled
            }

            try {
                String inputStr = inputObj.toString().replaceAll("[^0-9]", ""); // Strip non-numeric characters just in
                                                                                // case
                price = Integer.parseInt(inputStr);
            } catch (Exception e) {
                return; // Invalid input or empty
            }

            // Validate bounds before processing
            if (price < minPrice || price > maxPrice) {
                JOptionPane.showMessageDialog(orWindow, "Price out of bounds (" + minPrice + " - " + maxPrice + ")",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

        } else {
            msg.append("<b>Confirm purchase for ").append(formattedMin).append("?</b></div></html>");
            if (JOptionPane.showConfirmDialog(orWindow, msg.toString(), "Confirm Purchase", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                return; // User canceled
            }
        }

        action.setPrice(price);
        if (orWindow.process(action)) {
            updateMessage();
        }
    }

    protected void takeLoans(TakeLoans action) {
        if (action.getMaxNumber() == 1) {
            if (JOptionPane.showConfirmDialog(orWindow,
                    LocalText.getText("TakeLoanPrompt", action.getCompanyName(),
                            gameUIManager.format(action.getPrice())),
                    LocalText.getText("PleaseConfirm"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                action.setNumberTaken(1);
                orWindow.process(action);
            }
        }
    }

    protected void repayLoans(RepayLoans action) {
        if (action.getMinNumber() == action.getMaxNumber()) {
            JOptionPane.showMessageDialog(orWindow,
                    LocalText.getText("RepayLoan", action.getMinNumber(), gameUIManager.format(action.getPrice()),
                            gameUIManager.format(action.getMinNumber() * action.getPrice())));
            action.setNumberTaken(action.getMinNumber());
            orWindow.process(action);
        } else {
            String[] options = new String[action.getMaxNumber() - action.getMinNumber() + 1];
            for (int i = 0; i < options.length; i++) {
                int count = action.getMinNumber() + i;
                options[i] = (count == 0) ? LocalText.getText("None")
                        : LocalText.getText("RepayLoan", count, gameUIManager.format(action.getPrice()),
                                gameUIManager.format(count * action.getPrice()));
            }
            RadioButtonDialog dialog = new RadioButtonDialog(REPAY_LOANS_DIALOG, gameUIManager, orWindow,
                    LocalText.getText("Select"), LocalText.getText("SelectLoansToRepay", action.getCompanyName()),
                    options, 0);
            setCurrentDialog(dialog, action);
        }
    }

    protected void useSpecialProperty(UseSpecialProperty action) {
        gameUIManager.processAction(action);
    }

    protected void checkHexVisibilityOnUI(PossibleActions actions) {
        for (GUIHex hex : hexUpgrades.getHexes()) {
            boolean validFound = false;
            boolean invalidFound = false;
            for (HexUpgrade upgrade : hexUpgrades.getUpgrades(hex)) {
                if (upgrade.isValid()) {
                    hex.setState(upgrade instanceof TokenHexUpgrade ? GUIHex.State.TOKEN_SELECTABLE
                            : GUIHex.State.SELECTABLE);
                    validFound = true;
                    break;
                } else if (upgrade.isVisible())
                    invalidFound = true;
            }
            if (!validFound && invalidFound)
                hex.setState(GUIHex.State.INVALIDS);
        }
    }

    private void defineTileUpgrades(List<LayTile> actions) {
        for (LayTile layTile : actions) {
            if (layTile.getType() == LayTile.GENERIC || layTile.getType() == LayTile.GENERIC_EXCL_LOCATIONS) {
                addConnectedTileLays(layTile);
            } else if (layTile.getType() == LayTile.SPECIAL_PROPERTY) {
                if (layTile.getSpecialProperty().requiresConnection())
                    addConnectedTileLays(layTile);
                else
                    addLocatedTileLays(layTile);
            } else if (layTile.getType() == LayTile.LOCATION_SPECIFIC) {
                addLocatedTileLays(layTile);
            } else if (layTile.getType() == LayTile.CORRECTION) {
                addCorrectionTileLays(layTile);
            }
        }
    }

    private void addConnectedTileLays(LayTile layTile) {
        NetworkGraph graph = networkAdapter.getRouteGraph(layTile.getCompany(), true, false);
        Set<MapHex> validHexes = Sets.union(graph.getReachableSides().keySet(), graph.getPassableStations().keySet());
        Phase currentPhase = gameUIManager.getCurrentPhase();
        String algo = GameOption.getValue(gameUIManager.getRoot(), "RouteAlgorithm");

        for (MapHex hex : validHexes) {
            if (layTile.getType() == LayTile.GENERIC_EXCL_LOCATIONS && layTile.getLocations().contains(hex))
                continue;

            // Enforce location constraints for connected tile lays
            // Prevents specific hex actions (like 1837 AB/BrB) from spawning duplicate
            // upgrades globally
            if (layTile.getType() != LayTile.GENERIC_EXCL_LOCATIONS
                    && layTile.getLocations() != null
                    && !layTile.getLocations().isEmpty()
                    && !layTile.getLocations().contains(hex)) {
                continue;
            }

            GUIHex guiHex = map.getHex(hex);
            Set<TileHexUpgrade> upgrades = TileHexUpgrade.create(guiHex, graph.getReachableSides().get(hex),
                    graph.getPassableStations().get(hex), layTile, algo);
            EnumSet<TileHexUpgrade.Invalids> allowances = hex.isReservedForCompany()
                    ? EnumSet.of(TileHexUpgrade.Invalids.HEX_RESERVED)
                    : EnumSet.noneOf(TileHexUpgrade.Invalids.class);
            TileHexUpgrade.validates(upgrades, currentPhase, allowances);
            hexUpgrades.putAll(guiHex, upgrades);
        }
    }

    private void addLocatedTileLays(LayTile layTile) {
        if (layTile.getLocations() != null) {
            for (MapHex hex : layTile.getLocations()) {
                GUIHex guiHex = map.getHex(hex);
                Set<TileHexUpgrade> upgrades = TileHexUpgrade.createLocated(guiHex, layTile);
                TileHexUpgrade.validates(upgrades, gameUIManager.getCurrentPhase());
                hexUpgrades.putAll(guiHex, upgrades);
            }
        }
    }

    private void defineTokenUpgrades(List<LayToken> actions) {
        for (LayToken action : actions) {
            if (action instanceof LayBaseToken) {
                LayBaseToken lbt = (LayBaseToken) action;
                if (lbt.getType() == LayBaseToken.GENERIC)
                    addGenericTokenLays(lbt);
                else if (lbt.getLocations() != null)
                    addLocatedTokenLays(lbt);
                else
                    addGenericTokenLays(lbt);
            } else if (action instanceof LayBonusToken) {
                addLocatedTokenLays(action);
            }
        }
    }

    private void addGenericTokenLays(LayBaseToken action) {
        PublicCompany company = action.getCompany();
        if (company.getBaseTokenLayCostMethod() != PublicCompany.BaseCostMethod.ROUTE_DISTANCE) {
            NetworkGraph graph = networkAdapter.getRouteGraph(company, true, false);
            Multimap<MapHex, Stop> hexStops = graph.getTokenableStops(company);
            for (MapHex hex : hexStops.keySet()) {
                GUIHex guiHex = map.getHex(hex);
                TokenHexUpgrade upgrade = TokenHexUpgrade.create(this, guiHex, hexStops.get(hex), action);
                TokenHexUpgrade.validates(upgrade);
                hexUpgrades.put(guiHex, upgrade);
            }
        }
    }

    protected void addLocatedTokenLays(LayToken action) {
        for (MapHex hex : action.getLocations()) {
            GUIHex guiHex = map.getHex(hex);
            TokenHexUpgrade upgrade = TokenHexUpgrade.create(this, guiHex, hex.getTokenableStops(action.getCompany()),
                    action);
            TokenHexUpgrade.validates(upgrade);
            hexUpgrades.put(guiHex, upgrade);
        }
    }

    private void addCorrectionTileLays(LayTile layTile) {
        EnumSet<TileHexUpgrade.Invalids> allowances = EnumSet.of(TileHexUpgrade.Invalids.HEX_RESERVED);
        for (GUIHex hex : map.getHexes()) {
            Set<TileHexUpgrade> upgrades = TileHexUpgrade.createCorrection(hex, layTile);
            TileHexUpgrade.validates(upgrades, gameUIManager.getCurrentPhase(), allowances);
            hexUpgrades.putAll(hex, upgrades);
        }
    }

    public void updateMessage() {
    } // Unused

    public LocalSteps getLocalStep() {
        return this.localStep;
    }

    public void setMapRelatedActions(PossibleActions actions) {
        this.networkAdapter = NetworkAdapter.create(gameUIManager.getRoot());
        currentValidTileLays.clear();
        currentValidTokenLays.clear();

        // AGGRESSIVE CLEANUP: Iterate ALL hexes to clear "Ghost" previews.
        // During Undo, map.getSelectedHex() is often null, so the specific hex
        // that holds the visual artifact is missed by the standard cleanup.
        if (map != null) {
            for (GUIHex hex : map.getHexes()) {
                // Clear any pending visual upgrade (the ghost tile)
                if (hex.getUpgrade() != null) {
                    hex.setUpgrade(null);
                }
                // Reset state to ensure no "Green/Red" highlights linger from a future turn
                if (hex.getState() != GUIHex.State.NORMAL) {
                    hex.setState(GUIHex.State.NORMAL);
                }
            }
            // Clear global selection
            map.setSelectedHex(null);
        }

        // Ensure the collection is cleared so we can rebuild it fresh below
        if (hexUpgrades != null) {
            hexUpgrades.clear();
        }

        List<LayTile> tiles = actions.getType(LayTile.class);
        List<LayToken> tokens = actions.getType(LayToken.class);

        if (!tiles.isEmpty())
            defineTileUpgrades(tiles);
        if (!tokens.isEmpty())
            defineTokenUpgrades(tokens);
        hexUpgrades.build();
        checkHexVisibilityOnUI(actions);
        if (orPanel != null)
            orPanel.updateCycleableHexes(hexUpgrades.getHexes());

        populateTileLayOptionsFromHexUpgrades();
        populateTokenLayOptionsFromHexUpgrades();

        if (tiles.isEmpty() && !tokens.isEmpty()) {
            boolean buttonOnly = true;
            for (LayToken t : tokens) {
                if (t instanceof LayBaseToken && ((LayBaseToken) t).getType() != LayBaseToken.HOME_CITY) {
                    buttonOnly = false;
                    break;
                }
            }
            setLocalStep(buttonOnly ? LocalSteps.INACTIVE : LocalSteps.SELECT_HEX);
        } else if (tiles.isEmpty() && tokens.isEmpty()) {
            setLocalStep(LocalSteps.INACTIVE);
        } else {
            setLocalStep(LocalSteps.SELECT_HEX);
        }
    }

    private void populateTileLayOptionsFromHexUpgrades() {
        if (hexUpgrades == null)
            return;
        for (GUIHex guiHex : hexUpgrades.getHexes()) {
            for (HexUpgrade upgrade : hexUpgrades.getUpgrades(guiHex)) {
                if (upgrade instanceof TileHexUpgrade && upgrade.isValid()) {
                    TileHexUpgrade th = (TileHexUpgrade) upgrade;
                    for (net.sf.rails.game.HexSide side : th.getRotations()) {
                        currentValidTileLays.add(new TileLayOption(guiHex.getHex(), th.getUpgrade().getTargetTile(),
                                side.getTrackPointNumber(), th.getAction()));
                    }
                }
            }
        }
    }

    private void populateTokenLayOptionsFromHexUpgrades() {
        if (hexUpgrades == null)
            return;
        for (GUIHex guiHex : hexUpgrades.getHexes()) {
            for (HexUpgrade upgrade : hexUpgrades.getUpgrades(guiHex)) {
                if (upgrade instanceof TokenHexUpgrade && upgrade.isValid()) {
                    TokenHexUpgrade th = (TokenHexUpgrade) upgrade;
                    if (th.getAction() instanceof LayBaseToken)
                        currentValidTokenLays.add(new TokenLayOption(guiHex.getHex(), th.getSelectedStop(),
                                (LayBaseToken) th.getAction()));
                }
            }
        }
    }

    public void processAIMove() {
        try {
            PossibleActions currentActions = getPossibleActions();
            if (currentActions == null)
                currentActions = PossibleActions.create();
            if (currentActions.isEmpty())
                return;

            setMapRelatedActions(currentActions);

            PublicCompany comp = this.orComp != null ? this.orComp
                    : (oRound != null ? oRound.getOperatingCompany() : null);
            if (comp == null)
                return;

            AIPlayer ai = new AIPlayer("AI_OR", this.gameUIManager.getGameManager());
            PossibleAction chosen = ai.chooseMove(comp, currentActions, currentValidTileLays, currentValidTokenLays);

            if (chosen != null) {
                orWindow.process(chosen);
            } else {
                for (PossibleAction pa : currentActions.getList()) {
                    if (pa instanceof NullAction) {
                        orWindow.process(pa);
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void dialogActionPerformed() {
        JDialog d = getCurrentDialog();
        PossibleAction a = getCurrentDialogAction();
        if (d instanceof CheckBoxDialog && a instanceof ReachDestinations) {
            boolean[] sel = ((CheckBoxDialog) d).getSelectedOptions();
            for (int i = 0; i < sel.length; i++)
                if (sel[i])
                    ((ReachDestinations) a).addReachedCompany(((ReachDestinations) a).getPossibleCompanies().get(i));
        } else if (d instanceof ConfirmationDialog && a instanceof LayTile) {
            if (!((ConfirmationDialog) d).getAnswer()) {
                if (map != null)
                    map.selectHex(null);
                setLocalStep(LocalSteps.SELECT_HEX);
            }
            a = null; // Do not process the incomplete LayTile action directly
        }
        gameUIManager.processAction(a);
    }

    @Override
    public JDialog getCurrentDialog() {
        return gameUIManager.getCurrentDialog();
    }

    @Override
    public PossibleAction getCurrentDialogAction() {
        return gameUIManager.getCurrentDialogAction();
    }

    @Override
    public void setCurrentDialog(JDialog dialog, PossibleAction action) {
        gameUIManager.setCurrentDialog(dialog, action);
        if (!(dialog instanceof MessageDialog))
            orPanel.disableButtons();
    }

    public RailsRoot getRoot() {
        return gameUIManager.getRoot();
    }

    public PossibleActions getPossibleActions() {
        return gameUIManager.getGameManager().getPossibleActions();
    }

    // ... (lines of unchanged context code) ...
    /*
     * Updates all GUIHex objects to point to the live MapHex objects from the
     * current Game Manager.
     * Uses reflection to ensure access to the private 'hex' field.
     */
    private void rebindVisualHexes() {
        return;
    }

    private void updateCompanyHighlights() {
        if (map == null || oRound == null)
            return;

        // If toggled OFF, clear map and return immediately
        if (!showCompanyHighlights) {
            map.setOwnerHighlight(null, null);
            return;
        }

        // If toggled OFF, clear all highlights from the map and exit
        if (!showFriendlyHexes) {
            map.setOwnerHighlight(null, null);
            return;
        }

        PublicCompany currentComp = oRound.getOperatingCompany();
        if (currentComp == null) {
            map.setOwnerHighlight(null, null);
            return;
        }

        Player currentOwner = currentComp.getPresident();
        // Fix: In steps like MERGE, a company might momentarily lack a president.
        if (currentOwner == null) {
            map.setOwnerHighlight(null, null);
            return;
        }

        List<GUIHex> hexesToHighlight = new ArrayList<>();
        // Store specific labels for each hex
        Map<GUIHex, String> specificLabels = new HashMap<>();
        // Store boolean if hex contains the active operating company
        Map<GUIHex, Boolean> isActiveOperatingMap = new HashMap<>();

        net.sf.rails.game.MapManager mapManager = getRoot().getMapManager();
        net.sf.rails.game.CompanyManager companyManager = getRoot().getCompanyManager();
        Map<MapHex, GUIHex> guiHexesMap = map.getGuiHexes();

        if (guiHexesMap != null && mapManager != null && companyManager != null) {

            // 1. Get all companies for this player
            List<PublicCompany> playerCompanies = new ArrayList<>();
            for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
                if (comp.getPresident() != null &&
                        comp.getPresident().getName().equals(currentOwner.getName()) &&
                        !comp.isClosed()) {
                    playerCompanies.add(comp);
                }
            }

            // 2. Scan Hexes
            for (GUIHex guiHex : guiHexesMap.values()) {
                if (guiHex == null || guiHex.getHex() == null)
                    continue;

                String hexId = guiHex.getHex().getId();
                MapHex liveHex = mapManager.getHex(hexId);

                if (liveHex != null && liveHex.getStopsMap() != null) {
                    boolean foundToken = false;
                    boolean hasOperatingCompany = false;
                    String hexLabel = null;

                    for (Stop stop : liveHex.getStopsMap().values()) {

                        // Check for ANY player company on this stop
                        for (PublicCompany comp : playerCompanies) {
                            if (stop.hasTokenOf(comp)) {
                                foundToken = true;
                                if (comp.equals(currentComp)) {
                                    hasOperatingCompany = true;
                                    hexLabel = comp.getId(); // Prioritize operating company label
                                } else if (hexLabel == null) {
                                    hexLabel = comp.getId(); // Set portfolio label if operating not found yet
                                }
                            }
                        }
                    }

                    if (foundToken) {
                        hexesToHighlight.add(guiHex);
                        specificLabels.put(guiHex, hexLabel);
                        isActiveOperatingMap.put(guiHex, hasOperatingCompany);
                    }
                }
            }
        }

        // 3. Activate Highlights (Pass NULL as label to avoid overwriting everything
        // with one name)
        map.setOwnerHighlight(hexesToHighlight, null);

        // 4. Apply Specific Labels and Active/Portfolio styles individually
        for (Map.Entry<GUIHex, String> entry : specificLabels.entrySet()) {
            GUIHex guiHex = entry.getKey();
            boolean isOperating = isActiveOperatingMap.get(guiHex);
            guiHex.setActiveOwnerHighlight(true, entry.getValue(), isOperating);
        }

        // 5. Force Repaint to show changes
        map.repaintAll(new Rectangle(map.getSize()));

        // 6. 1870 Connection Run: Permanent Planning Highlight
        if (currentComp instanceof net.sf.rails.game.specific._1870.PublicCompany_1870) {
            net.sf.rails.game.specific._1870.PublicCompany_1870 comp1870 = (net.sf.rails.game.specific._1870.PublicCompany_1870) currentComp;

            if (comp1870.getDestinationHex() != null && !comp1870.hasConnected()) {
                GUIHex destGuiHex = map.getHex(comp1870.getDestinationHex());
                // We pass 'false' for the hover-specific aggressive logic,
                // keeping this as a persistent planning border.
                if (destGuiHex != null) {
                    destGuiHex.setDestinationHighlight(true);
                }
            }
        }

    }

    public void processBuyTrain(BuyTrain action) {
        try {
            Owner buyingOwner = null;
            Owner sellingOwner = action.getFromOwner();

            if (oRound != null) {
                buyingOwner = oRound.getOperatingCompany();
            }
            if (buyingOwner == null) {
                buyingOwner = action.getOwner();
            }

            boolean isBankSale = (sellingOwner instanceof net.sf.rails.game.financial.Bank) ||
                    (sellingOwner != null && sellingOwner.getParent() instanceof net.sf.rails.game.financial.Bank) ||
                    "IPO".equals(sellingOwner != null ? sellingOwner.getId() : "") ||
                    "Pool".equals(sellingOwner != null ? sellingOwner.getId() : "");

            if (isBankSale) {
                if (action.getPricePaid() == 0 && action.getFixedCost() > 0) {
                    action.setPricePaid(action.getFixedCost());
                }
                if (orWindow.process(action))
                    updateMessage();
                return;
            }

            int defaultPrice = 1;
            int maxCash = 0;
            String buyerName = "Unknown";

            if (buyingOwner instanceof PublicCompany) {
                PublicCompany buyingCompany = (PublicCompany) buyingOwner;
                buyerName = buyingCompany.getId();
                maxCash = buyingCompany.getPurseMoneyModel().value();
                defaultPrice = (maxCash > 0) ? maxCash : 1;
            }

            String trainName = "?";
            if (action.getTrain() != null && action.getTrain().getName() != null) {
                // Strip out the instance ID suffix (e.g., "4_2" becomes "4")
                trainName = action.getTrain().getName().replaceAll("_\\d+$", "");
            }
            String sellerName = (sellingOwner != null) ? sellingOwner.getId() : "Unknown";

            String message = String.format("%s buys a %s train from %s", buyerName, trainName, sellerName);
            String detail = String.format("(%s Treasury: %s)", buyerName, gameUIManager.format(maxCash));
            String fullMessage = "<html><h3>" + message + "</h3>" + detail + "<br>Enter Purchase Price:</html>";

            String amountString = (String) JOptionPane.showInputDialog(
                    orWindow, fullMessage, "Negotiate Price", JOptionPane.QUESTION_MESSAGE,
                    null, null, String.valueOf(defaultPrice));

            if (amountString == null)
                return;

            int price = 0;
            try {
                price = Integer.parseInt(amountString.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                price = 0;
            }

            if (price <= 0) {
                JOptionPane.showMessageDialog(orWindow, "Price must be > 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (action.getPriceMode() == PriceMode.VARIABLE) {
                if (price < action.getMinPrice() || price > action.getMaxPrice()) {
                    JOptionPane.showMessageDialog(orWindow,
                            "Price out of bounds (" + action.getMinPrice() + "-" + action.getMaxPrice() + ")", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (buyingOwner instanceof PublicCompany) {
                    PublicCompany comp = (PublicCompany) buyingOwner;
                    if (price > comp.getCash()) {
                        action.setAddedCash(price - comp.getCash());
                    }
                }
            }

            action.setPricePaid(price);
            if (orWindow.process(action))
                updateMessage();

        } catch (Exception e) {
            log.error("TRAIN_BUY_CRASH", e);
            orWindow.process(action);
        }
    }

}

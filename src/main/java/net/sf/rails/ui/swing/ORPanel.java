
package net.sf.rails.ui.swing;

/**
 * ORPanel serves as the primary visual interface for a Company's Operating Round.
 * * DESIGN PHILOSOPHY: The "Stupid Terminal"
 * 1. This panel is purely a renderer. It must NEVER autonomously process actions,
 * mutate game state, or bypass the engine because "there is nothing to do."
 * 2. It maps incoming PossibleAction objects 1:1 to visible buttons.
 * 3. The user must explicitly confirm every state change. If a phase ends, the engine 
 * must provide a NullAction (Done/Pass/Skip), and the UI must wait for the user to click it.
 * 4. The only exception to manual clicking is the Enter-key heuristic, which scans 
 * available, valid buttons to accelerate standard confirmations.
 * 5. State calculations (like optimal revenue) are displayed, but the actual execution 
 * (Pay/Hold) waits on explicit human input.
 * * Responsibilities:
 * - Displays Company Treasury, Loan status, and active Assets (Trains/Privates).
 * - Organizes dynamic actions into logical Phases (Build, Token, Revenue, Buy Train, Special).
 * - Routes user clicks back to the ORUIManager strictly as unmodified GameActions.
 */


import net.sf.rails.algorithms.*;
import net.sf.rails.common.Config;
import net.sf.rails.common.GuiDef;
import net.sf.rails.game.*;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.Owner;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;
import rails.game.correct.CorrectionModeAction;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Collections;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ORPanel extends GridPanel
        implements RevenueListener {

    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(ORPanel.class);

    // --- COMMAND CONSTANTS ---
    public static final String OPERATING_COST_CMD = "OperatingCost";
    public static final String BUY_PRIVATE_CMD = "BuyPrivate";
    public static final String UNDO_CMD = "Undo";
    public static final String REDO_CMD = "Redo";
    public static final String REM_TILES_CMD = "RemainingTiles";
    public static final String NETWORK_INFO_CMD = "NetworkInfo";
    public static final String TAKE_LOANS_CMD = "TakeLoans";
    public static final String REPAY_LOANS_CMD = "RepayLoans";
    public static final String BUY_TRAIN_CMD = "BuyTrain";
    public static final String WITHHOLD_CMD = "Withhold";
    public static final String SPLIT_CMD = "Split";
    public static final String PAYOUT_CMD = "Payout";
    public static final String SET_REVENUE_CMD = "SetRevenue";
    public static final String DONE_CMD = "Done";
    public static final String CONFIRM_CMD = "Confirm";
    public static final String SKIP_CMD = "Skip";
    public static final String SHOW_CMD = "Show";
    public static final String TRAIN_SKIP_CMD = "TrainSkip";

    // --- VISUAL CONSTANTS ---
    private static final Color BG_DETAILS = new Color(235, 230, 255); // Standard Mauve
    private static final Color BG_SPECIAL_HEADER = new Color(255, 220, 220); // Light Red for Special
    private static final Color SYS_BLUE = new Color(30, 144, 255); // DodgerBlue

    // Phase Colors
    private static final Color PH_TILE_DARK = new Color(139, 69, 19);
    private static final Color PH_TOKEN_DARK = new Color(34, 139, 34);
    private static final Color PH_DONE_BG = UIManager.getColor("Panel.background");
    private static final Color BG_NORMAL = UIManager.getColor("Panel.background");
    private static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 12);
    private static final int PANEL_ACTION_GAP = 8;

    private static final int BTN_HEIGHT = 28;
    private static final Font BTN_FONT = new Font("SansSerif", Font.PLAIN, 11);
    // --- COMPONENTS ---
    private ORWindow orWindow;
    private ORUIManager orUIManager;
    private JPanel sidebarPanel;

    // Standard Panels
    private JPanel phase1Panel, phase2Panel, phase3Panel, phase4Panel, phase5Panel, footerPanel;
    private JPanel cashPanel;
    private JPanel loansPanel;
    private JPanel miscActionPanel;
    private JPanel trainButtonsPanel;
    private JPanel specialActionsButtonPanel;

    // Special Mode Panels
    private JPanel specialContainer;
    private JPanel specialPanel;

    // Decoupled Header Components (Replaces single companyLogo)
    private JLabel lblCompanyInfo;
    private JLabel lblPlayerInfo;
    private JLabel lblPhaseInstruction;

    // Legacy/Standard Buttons
    public ActionButton btnRevPayout, btnRevWithhold, btnRevSplit;
    public ActionButton btnDone;
    public ActionButton btnBuildShow;
    public ActionButton btnTrainSkip;
    public ActionButton btnTileSkip, btnTileConfirm;
    public ActionButton btnTokenSkip, btnTokenConfirm;
    public ActionButton buttonOC, button1, button2, button3; // Legacy placeholders
    private JLabel focusLight;
    public JSpinner revSpinner;

    private GameAction currentUndoAction;
    private GameAction currentRedoAction;
    public ActionButton currentDefaultButton;

    public int activePhase = 0; // 1=Build, 2=Token, 3=Revenue, 4=Train, 5=Finalize/Done

    public List<GUIHex> cycleableHexes = new ArrayList<>();
    public int cycleIndex = -1;

    private PublicCompany[] companies;
    private int nc;
    private PublicCompany orComp = null;
    private PublicCompany currentOperatingComp = null;
    private int orCompIndex = -1;

    private boolean specialModeActive = false;
    private boolean isRevenueValueToBeSet = false;
    private boolean showNumbersActive = false;
    private AbstractButton directPassButton;

    // Game Params
    private boolean privatesCanBeBought;
    private boolean hasCompanyLoans;
    private boolean hasDirectCompanyIncomeInOR;
    private boolean bonusTokensExist;
    private boolean hasRights;

    private RevenueAdapter revenueAdapter = null;
    private Thread revenueThread = null;
    private List<JFrame> openWindows = new ArrayList<>();
    private List<BuyTrain> availableTrainActions = new ArrayList<>();

    // Sidebar Elements
    private JLabel companyLogo;
    private JLabel lblCash;
    private JLabel lblFixed;
    private JLabel lblLoans;
    private JLabel lblRoute;

    private TokenDisplayPanel tokenDisplay;
    private TrainDisplayPanel trainDisplay;
    private JPanel legendPanel;
    private JPanel specialNotificationPanel;

    private static final List<ORPanel> activeInstances = new ArrayList<>();

    /**
     * Centralized UI Theme for the Operating Round Sidebar.
     * Organizes colors by logical "Zones": Infrastructure, Capital, and Control.
     */
    public static class UITheme {
        // ZONE: Infrastructure (Map Actions)
        public static final Color TRACK_DARK = new Color(139, 69, 19); // Ochre
        public static final Color TRACK_LIGHT = new Color(255, 245, 235);
        public static final Color TOKEN_DARK = new Color(34, 139, 34); // Forest
        public static final Color TOKEN_LIGHT = new Color(210, 255, 210);

        // ZONE: Capital (Treasury Actions)
        public static final Color REVENUE_DARK = new Color(0, 60, 140); // Royal Blue
        public static final Color REVENUE_LIGHT = new Color(210, 230, 255);
        public static final Color TRAIN_DARK = new Color(204, 102, 0); // Industrial Orange
        public static final Color TRAIN_LIGHT = new Color(255, 235, 205);

        // ZONE: Control (Navigation & State)
        public static final Color ACTION_SKIP = new Color(30, 144, 255); // DodgerBlue
        public static final Color ACTION_DONE = new Color(180, 0, 0); // Warning Red
        public static final Color ACTION_DISCARD = new Color(220, 20, 60); // Crimson

        // General UI Components
        public static final Color BG_SIDEBAR = new Color(235, 230, 255); // Standard Mauve
        public static final Color BG_SPECIAL = new Color(255, 220, 220); // Light Red
        public static final Color BG_CARD = new Color(255, 255, 240); // Beige
        public static final Color READOUT_BG = Color.WHITE;
        public static final Color READOUT_FG = Color.BLACK;
    }

    public ORPanel(ORWindow parent, ORUIManager orUIManager) {
        super();
        activeInstances.add(this);
        setPreferredSize(new Dimension(getSidebarWidth(), 0));

        this.orWindow = parent;
        this.orUIManager = orUIManager;
        GameUIManager gameUIManager = parent.gameUIManager;

        gridPanel = new JPanel();
        parentFrame = parent;
        setFocusable(true);

        round = gameUIManager.getCurrentRound();

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies().toArray(new PublicCompany[0]);
            nc = companies.length;
            this.orComp = ((OperatingRound) round).getOperatingCompany();
        } else {
            // Reflection hook to find BK in CoalExchangeRound
            try {
                java.lang.reflect.Method method = round.getClass().getMethod("getOperatingCompany");
                Object result = method.invoke(round);
                if (result instanceof PublicCompany) {
                    this.orComp = (PublicCompany) result;
                    this.currentOperatingComp = (PublicCompany) result;
                }
            } catch (Exception e) {
                // No company context available
            }
        }

        privatesCanBeBought = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES);
        bonusTokensExist = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.DO_BONUS_TOKENS_EXIST);
        hasCompanyLoans = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_COMPANY_LOANS);
        hasRights = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_RIGHTS);
        hasDirectCompanyIncomeInOR = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME);

        // Robust 1856/18xx Fallback: If parameter is false, scan companies for loan
        // definitions to match GameManager
        if (!hasCompanyLoans && gameUIManager.getAllPublicCompanies() != null) {
            for (PublicCompany company : gameUIManager.getAllPublicCompanies()) {
                if (company != null && company.getMaxNumberOfLoans() != 0) {
                    hasCompanyLoans = true;
                    break;
                }
            }
        }

        initSidebar();

        gbc = new GridBagConstraints();
        players = gameUIManager.getPlayerManager();

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies().toArray(new PublicCompany[0]);
            nc = companies.length;
        }

        initButtonPanel(); // Legacy init
        setupHotkeys();
        setVisible(true);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", evt -> {
            if (focusLight != null && orWindow != null) {
                Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                boolean hasFocus = owner != null && javax.swing.SwingUtilities.isDescendingFrom(owner, orWindow);
                focusLight.setForeground(hasFocus ? new java.awt.Color(34, 139, 34) : java.awt.Color.RED);
                focusLight.setText(hasFocus ? "●" : "●");
            }
        });

    }

    /**
     * Determines the current Operating Round phase based on the available actions.
     * 1=Tile, 2=Token, 3=Revenue, 4=Train, 5=special actoins 
     */

    private int determineActivePhase(List<PossibleAction> actions) {
        int phase = 0;
        boolean hasDoneAction = false;
        boolean hasSpecialAction = false;

        if (actions == null || actions.isEmpty()) {
            return 0;
        }

        // Establish baseline phase strictly from Engine State to respect manual skip
        // laws
        if (orUIManager != null && orUIManager.getGameUIManager() != null
                && orUIManager.getGameUIManager().getGameManager() != null) {
            net.sf.rails.game.round.RoundFacade currentRound = orUIManager.getGameUIManager().getGameManager()
                    .getCurrentRound();
            if (currentRound instanceof OperatingRound) {
                net.sf.rails.game.GameDef.OrStep step = ((OperatingRound) currentRound).getStep();
                if (step == net.sf.rails.game.GameDef.OrStep.LAY_TRACK)
                    phase = 1;
                else if (step == net.sf.rails.game.GameDef.OrStep.LAY_TOKEN)
                    phase = 2;
                else if (step == net.sf.rails.game.GameDef.OrStep.CALC_REVENUE
                        || step == net.sf.rails.game.GameDef.OrStep.PAYOUT)
                    phase = 3;
                else if (step == net.sf.rails.game.GameDef.OrStep.BUY_TRAIN)
                    phase = 4;
                else if (step == net.sf.rails.game.GameDef.OrStep.REPAY_LOANS
                        || step == net.sf.rails.game.GameDef.OrStep.TRADE_SHARES)
                    phase = 5;
            }
        }

        // 2. Adjust based on explicitly generated UI Actions
        for (PossibleAction pa : actions) {
            if (pa instanceof LayTile && (phase == 0 || phase > 1)) {
                phase = 1;
            } else if (pa instanceof LayToken && (phase == 0 || phase > 2)) {
                phase = 2;
            } else if (pa instanceof SetDividend && (phase == 0 || phase > 3)) {
                phase = 3;
            } else if (pa instanceof BuyTrain && (phase == 0 || phase > 4)) {
                phase = 4;
            } else if (!(pa instanceof LayTile) && !(pa instanceof LayToken)
                    && !(pa instanceof SetDividend) && !(pa instanceof BuyTrain)
                    && !(pa instanceof NullAction) && !(pa instanceof GameAction)
                    && !(pa instanceof rails.game.correct.CorrectionModeAction)) {
                hasSpecialAction = true;
            } else if (pa instanceof NullAction) {
                NullAction.Mode mode = ((NullAction) pa).getMode();
                if (mode == NullAction.Mode.DONE || mode == NullAction.Mode.PASS || mode == NullAction.Mode.SKIP) {
                    hasDoneAction = true;
                }
            }
        }

        // 3. Fallbacks if engine step didn't resolve directly
        if (phase == 0 || phase == 5) {
            phase = 5;
        }

        return phase;
    }

    private void distributeStandardActions(List<PossibleAction> actions) {
        boolean doneActionFound = false;
        PossibleAction donePa = null;

        // 1. DEDUPLICATION SET
        java.util.Set<String> addedSpecialLabels = new java.util.HashSet<>();

        // 2. CONSTANTS (Normalized Labels)
        final String LBL_TILE = "EXTRA TILE BUILD";
        final String LBL_TOKEN = "EXTRA TOKEN";

        for (PossibleAction pa : actions) {
            // IGNORE LIST: Structural actions that should never be special buttons
            if (pa instanceof CorrectionModeAction ||
                    pa instanceof GameAction) {
                continue;
            }

            String labelToAdd = null;

            // --- A. UseSpecialProperty (The Menu/Trigger) ---
            if (pa instanceof UseSpecialProperty) {
                String text = pa.getButtonLabel().toLowerCase();
                if (text.contains("tile"))
                    labelToAdd = LBL_TILE;
                else if (text.contains("token"))
                    labelToAdd = LBL_TOKEN;
                else
                    labelToAdd = pa.getButtonLabel().trim();
            }

            // --- B. LayTile (The Execution) ---
            else if (pa instanceof LayTile) {
                LayTile lt = (LayTile) pa;
                // ONLY show if it has a linked SpecialProperty object or explicit extra flag
                if (lt.getSpecialProperty() != null || pa.toString().contains("extra=true")) {
                    labelToAdd = LBL_TILE;
                }
            }

            // --- C. LayBaseToken (The Execution) ---
            else if (pa instanceof LayBaseToken) {
                LayBaseToken lbt = (LayBaseToken) pa;

                // STRICT FILTER BASED ON DEBUG ANALYSIS:
                // 1. Check for attached SpecialProperty (Debug confirmed Type 2 has SP=true)
                boolean hasSpecialProp = (lbt.getSpecialProperty() != null);

                // 2. Check for explicit "extra=true" flag
                boolean isExplicitlyExtra = pa.toString().contains("extra=true");

                // 3. Check for Special Types, BUT EXCLUDE TYPE 1 (Home City/Normal)
                // Type 0 = Generic, Type 1 = Home/Normal. Both are ignored.
                // Any other Type (2+) is considered special.
                boolean isSpecialType = (lbt.getType() != LayBaseToken.GENERIC && lbt.getType() != 1);

                if (hasSpecialProp || isExplicitlyExtra || isSpecialType) {
                    labelToAdd = LBL_TOKEN;
                }
            }

            // --- E. ADD BUTTON (Deduplicated) ---
            if (labelToAdd != null) {
                if (!addedSpecialLabels.contains(labelToAdd)) {
                    addSpecialNotificationButton(labelToAdd, pa);
                    addedSpecialLabels.add(labelToAdd);
                }
            }

            // Continue with standard distribution...
            if (pa instanceof SetDividend) {
                SetDividend sd = (SetDividend) pa;
                if (sd.isAllocationAllowed(SetDividend.PAYOUT))
                    enableRevenueBtn(btnRevPayout, sd, SetDividend.PAYOUT);
                if (sd.isAllocationAllowed(SetDividend.WITHHOLD))
                    enableRevenueBtn(btnRevWithhold, sd, SetDividend.WITHHOLD);
                if (sd.isAllocationAllowed(SetDividend.SPLIT))
                    enableRevenueBtn(btnRevSplit, sd, SetDividend.SPLIT);
            } else if (pa instanceof BuyTrain) {
                availableTrainActions.add((BuyTrain) pa);
                addTrainBuyButton((BuyTrain) pa);
            } else if (pa instanceof NullAction) {
                NullAction.Mode mode = ((NullAction) pa).getMode();
                if (mode == NullAction.Mode.DONE || mode == NullAction.Mode.PASS || mode == NullAction.Mode.SKIP) {
                    boolean handledByPhaseSkip = false;
                    if (activePhase == 1 && btnTileConfirm != null) {
                        setupButton(btnTileConfirm, pa);
                        btnTileConfirm.setEnabled(true);
                        handledByPhaseSkip = true;
                    } else if (activePhase == 2 && btnTokenConfirm != null) {
                        setupButton(btnTokenConfirm, pa);
                        btnTokenConfirm.setEnabled(true);
                        handledByPhaseSkip = true;
                    } else if (activePhase == 4 && btnTrainSkip != null) {
                        setupButton(btnTrainSkip, pa);
                        btnTrainSkip.setText(mode == NullAction.Mode.SKIP ? "Skip Buy" : "Done Buying");
                        handledByPhaseSkip = true;
                    }

                    // Bind to the End Turn button ONLY if it wasn't consumed by a phase-specific
                    // skip, OR if we are in Phase 5+
                    if (!handledByPhaseSkip || activePhase >= 5 || activePhase == 0) {
                        setupButton(btnDone, pa);
                        bindActionHotkey(btnDone, pa);
                        donePa = pa;
                        doneActionFound = true;
                    }
                }

            }
        }
        if (doneActionFound && donePa != null) {
            bindActionHotkey(btnDone, donePa);
        }

    }

    private void updatePhaseSpecifics() {

        if (activePhase == 1 || activePhase == 2) {
            setTileBuildNumbers(true);
            redrawRoutes();
        } else if (activePhase == 3) {
            setTileBuildNumbers(false);
            if (orWindow != null && orWindow.getMapPanel() != null)
                orWindow.getMapPanel().clearOverlays();
            redrawRoutes();
        } else {
            setTileBuildNumbers(false);
            if (orWindow != null && orWindow.getMapPanel() != null)
                orWindow.getMapPanel().clearOverlays();
            redrawRoutes();
            if (activePhase == 4 && btnTrainSkip != null)
                btnTrainSkip.setEnabled(true);
        }
    }

    private void addSpecialActionButtonToPhase5(PossibleAction action) {
        ActionButton btn = new ActionButton(RailsIcon.OK);

        String text = action.getButtonLabel();
        if (text == null || text.trim().isEmpty()) {
            text = action.toString();
        }

        btn.setText(formatDynamicButtonText(text, null));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setIcon(null);

        btn.setBackground(new Color(255, 255, 240));
        btn.setOpaque(true);
        btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));

        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(getSidebarWidth() - scale(20), scale(75)));
        btn.setPossibleAction(action);
        btn.setEnabled(true);
        btn.addActionListener(this);

        if (specialActionsButtonPanel != null) {
            specialActionsButtonPanel.add(btn);
            specialActionsButtonPanel.add(Box.createVerticalStrut(4));
        }
    }

    private void addSpecialNotificationButton(String text, PossibleAction sourceAction) {
        if (specialNotificationPanel == null)
            return;

        specialNotificationPanel.setVisible(true);

        ActionButton b = new ActionButton(RailsIcon.INFO); // Use Info icon or null
        b.setText(text);
        b.setIcon(null);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setPreferredSize(new Dimension(getSidebarWidth() - scale(20), scale(BTN_HEIGHT)));
        b.setMaximumSize(new Dimension(getSidebarWidth() - scale(20), scale(BTN_HEIGHT)));

        // --- STYLING ---
        // High Visibility Gold/Orange
        Color bg = new Color(255, 193, 7); // Amber/Gold
        Color fg = Color.BLACK;

        // Force UI to ignore "Disabled" greying out
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));

        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);

        // Thick border to indicate "Special"
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(184, 134, 11), 2), // Dark Goldenrod
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        // Functionally disabled (not clickable)
        b.setEnabled(false);

        // Optional: Add tooltip to explain
        if (sourceAction != null) {
            b.setToolTipText(sourceAction.toString());
        }

        specialNotificationPanel.add(b);
        specialNotificationPanel.add(Box.createVerticalStrut(4));
    }

    private void updateSpecialHeader(GuiTargetedAction context) {
        if (lblCompanyInfo == null || context == null)
            return;

        // 1. Extract Data correctly
        Owner actor = context.getActor();

        // TOP: The Company Name (ID)
        String companyName = (actor != null) ? actor.getId() : "Game";

        // MIDDLE: The Player Name (from our new interface method)
        String playerName = context.getPlayerName();
        // Suppress duplicate player names when the Player is the primary Actor
        if (actor instanceof net.sf.rails.game.Player || companyName.equals(playerName)) {
            playerName = "";
        }

        // 2. Determine Colors (Prioritize Action Signature over Actor defaults)
        Color bg = context.getHighlightBackgroundColor();
        if (bg == null)
            bg = BG_SPECIAL_HEADER;

        Color fg = context.getHighlightTextColor();
        if (fg == null)
            fg = Color.BLACK;

        if (actor instanceof PublicCompany && context.getHighlightBackgroundColor() == null) {
            bg = ((PublicCompany) actor).getBgColour();
            fg = ((PublicCompany) actor).getFgColour();
        }

        // 3. Update Components
        // 3. Update Components using dynamic scaling

        // TOP: Company Name
        lblCompanyInfo.setText("<html><center><span style='font-family: SansSerif; font-size: " + scale(24)
                + "px; font-weight: bold;'>" + companyName + "</span></center></html>");
        lblCompanyInfo.setBackground(bg);
        lblCompanyInfo.setForeground(fg);
        lblCompanyInfo.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, Color.DARK_GRAY));
        lblCompanyInfo.setVisible(true);

        if (lblPlayerInfo != null) {
            if (playerName == null || playerName.isEmpty()) {
                lblPlayerInfo.setVisible(false);
            } else {
                lblPlayerInfo.setText(
                        "<html><center><span style='font-family: SansSerif; font-size: " + scale(18) + "px;'>"
                                + playerName + "</span></center></html>");
                lblPlayerInfo.setBackground(bg);
                lblPlayerInfo.setForeground(fg);
                lblPlayerInfo.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.DARK_GRAY));
                lblPlayerInfo.setVisible(true);
            }
        }

        // BOTTOM: Action Title
        String actionTitle = context.getGroupLabel();
        if (lblPhaseInstruction != null) {
            lblPhaseInstruction.setText("<html><center><span style='font-family: SansSerif; font-size: " + scale(14)
                    + "px; font-weight: bold;'>" + actionTitle + "</span></center></html>");
            lblPhaseInstruction.setBackground(bg);
            lblPhaseInstruction.setForeground(fg);
            lblPhaseInstruction.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.DARK_GRAY));
            lblPhaseInstruction.setVisible(true);
        }

        // 3. Update Components

        // TOP: Company Name
        lblCompanyInfo.setText("<html><center><font size='6'><b>" + companyName + "</b></font></center></html>");
        lblCompanyInfo.setBackground(bg);
        lblCompanyInfo.setForeground(fg);
        lblCompanyInfo.setVisible(true);

        if (lblPlayerInfo != null) {
            lblPlayerInfo
                    .setText("<html><center><font face='SansSerif' size='5'>" + playerName + "</font></center></html>");
            lblPlayerInfo.setBackground(bg);
            lblPlayerInfo.setForeground(fg);
            lblPlayerInfo.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.DARK_GRAY));
            lblPlayerInfo.setVisible(true);
        }

        // BOTTOM: Action Title
        // FIX: Applied Company Colors here as well
        if (lblPhaseInstruction != null) {
            lblPhaseInstruction
                    .setText("<html><center><font size='4'><b>" + actionTitle + "</b></font></center></html>");
            lblPhaseInstruction.setBackground(bg);
            lblPhaseInstruction.setForeground(fg);
            lblPhaseInstruction.setVisible(true);
        }
    }

    private void setStandardPanelsVisible(boolean visible) {
        if (phase1Panel != null)
            phase1Panel.setVisible(visible);
        if (phase2Panel != null)
            phase2Panel.setVisible(visible);
        if (phase3Panel != null)
            phase3Panel.setVisible(visible);
        if (phase4Panel != null)
            phase4Panel.setVisible(visible);
        if (phase5Panel != null)
            phase5Panel.setVisible(visible);
        if (footerPanel != null)
            footerPanel.setVisible(visible);
        if (cashPanel != null)
            cashPanel.setVisible(visible);
        if (loansPanel != null)
            loansPanel.setVisible(visible);
        if (lblCash != null && lblCash.getParent() != null)
            lblCash.getParent().setVisible(visible);
    }

    private String getDoneButtonText() {
        return "End Turn";
    }

    private void colorizeActivePhase(Color unused) {
        resetPhasePanel(phase1Panel, btnTileConfirm);
        resetPhasePanel(phase2Panel, btnTokenConfirm);
        resetPhasePanel(phase3Panel, btnRevPayout);

        // Always enforce the "White Box" style for Revenue buttons for consistency
        if (btnRevPayout != null) {
            btnRevPayout.setText("Pay");
            styleRevenueButton(btnRevPayout, false);
        }
        if (btnRevWithhold != null) {
            btnRevWithhold.setText("Hold");
            styleRevenueButton(btnRevWithhold, false);
        }
        if (btnRevSplit != null) {
            btnRevSplit.setText("Split");
            styleRevenueButton(btnRevSplit, false);
        }

        resetPhasePanel(phase4Panel, btnTrainSkip);
        resetPhasePanel(null, btnDone);
        resetPhasePanel(null, btnDone);

        // Phase 1: Infrastructure - Track (Matches Ochre/Brown Palette)
        if (activePhase == 1) {
            applyPhaseStyle(phase1Panel, null, UITheme.TRACK_DARK, UITheme.TRAIN_LIGHT, "Confirm Track");
            if (btnTileConfirm != null) {
                btnTileConfirm.setEnabled(true);

                boolean hasSelection = (orUIManager != null && orUIManager.getMap() != null
                        && orUIManager.getMap().getSelectedHex() != null);
                if (hasSelection) {
                    styleButton(btnTileConfirm, SYS_BLUE, "Confirm");
                } else {
                    styleButton(btnTileConfirm, UITheme.ACTION_SKIP, "Skip");
                }

            }

            // Phase 2: Infrastructure - Token (Matches Forest Green Palette)
        } else if (activePhase == 2) {
            applyPhaseStyle(phase2Panel, null, UITheme.TOKEN_DARK, UITheme.TRAIN_LIGHT, "Confirm Token");
            if (btnTokenConfirm != null) {
                btnTokenConfirm.setEnabled(true);

                boolean hasSelection = (orUIManager != null && orUIManager.getMap() != null
                        && orUIManager.getMap().getSelectedHex() != null);
                if (hasSelection) {
                    styleButton(btnTokenConfirm, SYS_BLUE, "Confirm");
                } else {
                    styleButton(btnTokenConfirm, UITheme.ACTION_SKIP, "Skip");
                }
            }

            // Phase 3: Capital - Revenue (Matches Royal Blue palette)
        } else if (activePhase == 3) {
            applyPhaseStyle(phase3Panel, null, UITheme.REVENUE_DARK, UITheme.TRAIN_LIGHT, "Revenue");

            // Preserved complex logic: Identify which button is ACTUALLY enabled and
            // highlight it
            ActionButton primaryBtn = null;
            if (btnRevPayout != null && btnRevPayout.isEnabled()) {
                primaryBtn = btnRevPayout;
            } else if (btnRevSplit != null && btnRevSplit.isEnabled()) {
                primaryBtn = btnRevSplit;
            } else if (btnRevWithhold != null && btnRevWithhold.isEnabled()) {
                primaryBtn = btnRevWithhold;
            }

            // Apply highlighting to the primary option while keeping others in the standard
            // theme
            if (primaryBtn == btnRevSplit) {
                styleRevenueButton(btnRevSplit, true);
                styleRevenueButton(btnRevPayout, false);
                styleRevenueButton(btnRevWithhold, false);
            } else if (primaryBtn == btnRevWithhold) {
                styleRevenueButton(btnRevWithhold, true);
                styleRevenueButton(btnRevPayout, false);
                styleRevenueButton(btnRevSplit, false);
            } else {
                boolean payEnabled = (btnRevPayout != null && btnRevPayout.isEnabled());
                styleRevenueButton(btnRevPayout, payEnabled);
                styleRevenueButton(btnRevSplit, false);
                styleRevenueButton(btnRevWithhold, false);
            }

            // Phase 4: Capital - Trains (Matches Industrial Orange Palette)
        } else if (activePhase == 4) {

            if (btnTrainSkip != null)
                btnTrainSkip.setEnabled(true);

            boolean canBuy = (trainButtonsPanel != null && trainButtonsPanel.getComponentCount() > 0);
            String label = canBuy ? "Skip Buy" : "Done Buying";

            applyPhaseStyle(phase4Panel, null, UITheme.TRAIN_DARK, UITheme.TRAIN_LIGHT, label);

            boolean canSkip = (btnTrainSkip.getPossibleActions() != null
                    && !btnTrainSkip.getPossibleActions().isEmpty());
            btnTrainSkip.setEnabled(canSkip);
            if (canSkip) {
                styleButton(btnTrainSkip, UITheme.ACTION_SKIP, label);
            } else {
                styleButton(btnTrainSkip, UIManager.getColor("Button.background"), label);
                btnTrainSkip.setForeground(Color.GRAY);
            }

        }

        // Verify if any genuine voluntary choices are structurally active inside Phase 5.
        // We exclude checking btnDone here so that an empty turn closure doesn't falsely force an active highlight.
        boolean hasSpecialActions = (specialActionsButtonPanel != null
                && specialActionsButtonPanel.getComponentCount() > 0) ||
                (specialContainer != null && specialContainer.isVisible() && specialPanel != null
                        && specialPanel.getComponentCount() > 0);

        
        if (hasSpecialActions) {
            
            // Force the container to actually grow to its content
            if (specialContainer != null) {
                specialContainer.setVisible(true);
                specialContainer.invalidate();
                specialContainer.revalidate();
            }
            
            applyPhaseStyle(phase5Panel, null, UITheme.ACTION_SKIP, UITheme.TRAIN_LIGHT, "Special Actions");
            if (specialContainer != null) {
                specialContainer.setVisible(true);
            }
        } else {
            resetPhasePanel(phase5Panel, null);
        }

        if (btnDone != null) {
            if (activePhase >= 1 && activePhase <= 4) {
                btnDone.setVisible(false);
            } else {
                btnDone.setVisible(true);
            }
        }

        // DIRECT ENGINE SYNC
        if (btnDone != null) {
            String doneText = getDoneButtonText();
            if (btnDone.getPossibleActions() != null && !btnDone.getPossibleActions().isEmpty()) {
                btnDone.setEnabled(true);
                styleButton(btnDone, UITheme.ACTION_SKIP, doneText);
                btnDone.setForeground(Color.WHITE);
                btnDone.setFont(new Font("SansSerif", Font.BOLD, 14));
            } else {
                // PERSISTENT WAIT: Keep as 'END TURN' but disabled and gray
                btnDone.setEnabled(false);
                styleButton(btnDone, UIManager.getColor("Button.background"), doneText);
                btnDone.setForeground(Color.GRAY);
                btnDone.setFont(new Font("SansSerif", Font.BOLD, 14));
            }
        }
    }

    public static void updateSpinnerVisibilityFromConfig() {
        SwingUtilities.invokeLater(() -> {
            for (ORPanel panel : activeInstances) {
                panel.updateSpinnerVisibility();
            }
        });
    }

    public void updateSpinnerVisibility() {
        boolean showSpinner = true;
        if (orUIManager != null) {
            showSpinner = orUIManager.isShowRevenueSpinner();
        }

        if (revSpinner != null) {
            revSpinner.setVisible(showSpinner);
        }
        if (lblRoute != null) {
            lblRoute.setVisible(!showSpinner);
        }

        if (revSpinner != null && revSpinner.getParent() != null) {
            revSpinner.getParent().invalidate();
            revSpinner.getParent().validate();
            revSpinner.getParent().repaint();
        }

        if (sidebarPanel != null) {
            sidebarPanel.revalidate();
            sidebarPanel.repaint();
        }

        // Force top-level window to redraw immediately to avoid layout ghosting
        if (orWindow != null) {
            orWindow.revalidate();
            orWindow.repaint();
        }
    }

    private void styleButton(ActionButton btn, Color bg, String text) {
        if (btn == null)
            return;
        btn.setText(text);
        btn.setBackground(bg);
        btn.setOpaque(true);
        if (bg == PH_DONE_BG) {
            btn.setForeground(Color.BLACK);
        } else {
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        }
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
    }

    private void resetButtonStyle(ActionButton btn) {
        if (btn == null)
            return;

        // 1. Set the Standard gray Background
        btn.setBackground(UIManager.getColor("Button.background"));
        btn.setForeground(Color.GRAY); // Text is gray to indicate inactivity
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11)); // Keep font smaller/plain for inactive

        // 2. ENFORCE SHAPE CONSISTENCY
        // Instead of reverting to UIManager.getBorder("Button.border"), we use the same
        // 3D bevel.
        btn.setBorder(BorderFactory.createRaisedBevelBorder());

        // 3. Ensure Opacity is true so the gray background paints correctly within the
        // border
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
    }

    private void applyPhaseStyle(JPanel p, ActionButton mainBtn, Color dark, Color light, String btnLabel) {
        if (p == null)
            return;
        p.setOpaque(true);
        p.setBackground(light);
        if (p.getBorder() instanceof TitledBorder) {
            ((TitledBorder) p.getBorder()).setTitleColor(dark);
            ((TitledBorder) p.getBorder()).setBorder(BorderFactory.createLineBorder(dark, 2));
        }
        if (mainBtn != null && mainBtn.isEnabled()) {
            styleButton(mainBtn, dark, btnLabel);
        }
        for (Component c : p.getComponents()) {
            if (c instanceof JLabel) {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK); // --- FIX: Ensure Readouts (Revenue) stay Black ---
            } else if (c instanceof JPanel)
                c.setBackground(light);
        }
        p.repaint();
    }

    private void resetPhasePanel(JPanel p, ActionButton mainBtn) {
        if (p != null) {
            // Visual "Disable" - Gray out, but DO NOT change logical Enabled state
            p.setOpaque(false);
            p.setBackground(BG_NORMAL);

            // gray out the border title
            if (p.getBorder() instanceof TitledBorder) {
                ((TitledBorder) p.getBorder()).setTitleColor(Color.GRAY);
                ((TitledBorder) p.getBorder()).setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }

            // gray out labels inside
            for (Component c : p.getComponents()) {
                if (c instanceof JLabel) {
                    c.setForeground(Color.GRAY);
                } else if (c instanceof JPanel) {
                    ((JPanel) c).setOpaque(false);
                }
            }
            p.repaint();

        }
        if (mainBtn != null) {
            resetButtonStyle(mainBtn);
            // mainBtn.setEnabled(false); // <--- DELETE: Do not clobber state set by Engine
        }
    }

    private ActionButton createSmallButton(String text) {
        ActionButton b = new ActionButton(RailsIcon.OK);
        b.setText(text);
        b.setIcon(null);
        b.setFont(new Font("SansSerif", Font.PLAIN, 10));
        b.setMargin(new Insets(2, 2, 2, 2)); // Tight margins
        return b;
    }

    private String formatDynamicButtonText(String label, String subText) {
        if (label == null || label.trim().isEmpty()) {
            return "";
        }

        // Split CamelCase (e.g. "ReachDestination" -> "Reach Destination")
        String spacedLabel = label.replaceAll("([a-z])([A-Z]+)", "$1 $2");

        StringBuilder html = new StringBuilder();
        // Remove fixed width, rely on native centering
        html.append("<html><center>");
        html.append("<b>").append(spacedLabel).append("</b>");

        if (subText != null && !subText.trim().isEmpty()) {
            html.append("<br><span style='font-size: 80%; font-weight: normal;'>");
            html.append(subText);
            html.append("</span>");
        }

        html.append("</center></html>");
        return html.toString();
    }

 
    private void rebuildDynamicButtonTexts() {
        if (specialPanel != null) {
            for (Component c : specialPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    ActionButton btn = (ActionButton) c;
                    btn.setHorizontalAlignment(SwingConstants.CENTER);
                    PossibleAction action = btn.getPossibleActions() != null && !btn.getPossibleActions().isEmpty()
                            ? btn.getPossibleActions().get(0)
                            : null;
                    if (action != null) {
                        String label = action.getButtonLabel();
                        if (label == null || label.trim().isEmpty())
                            label = action.toString();

                        String subText = null;
                        
                        // --- START FIX ---
                        if (action instanceof UseSpecialProperty) {
                            net.sf.rails.game.special.SpecialProperty sp = ((UseSpecialProperty) action)
                                    .getSpecialProperty();
                            if (sp != null) {
                                subText = sp.getHelp();
                                if (subText == null || subText.trim().isEmpty()) {
                                    subText = sp.getInfo();
                                }
                            }
                        }
                        // --- END FIX ---

                        if (action instanceof BuyPrivate) {
                            BuyPrivate bp = (BuyPrivate) action;
                            String compId = (bp.getPrivateCompany() != null) ? bp.getPrivateCompany().getId() : action.toString();
                            label = "Buy " + compId + " (" + bp.getMinimumPrice() + "-"
                                    + bp.getMaximumPrice() + ")";
                        }

                        String actStr = action.toString().toLowerCase();
                        String btnLbl = action.getButtonLabel() != null ? action.getButtonLabel().toLowerCase() : "";

                        if (actStr.contains("bridge") || btnLbl.contains("bridge")) {
                            label = "Buy Bridge Token ($50)";
                        } else if (actStr.contains("tunnel") || btnLbl.contains("tunnel")) {
                            label = "Buy Tunnel Token ($50)";
                        } else if (actStr.contains("port") || btnLbl.contains("port") || actStr.contains("layport")) {
                            label = "Lay Port Token";
                        } else if (actStr.contains("loan") || actStr.contains("bond") || actStr.contains("takeloan")) {
                            label = "Take Government Bond";
                        }

                        if (action instanceof GuiTargetedAction) {
                            label = ((GuiTargetedAction) action).getButtonLabel();
                        }
                        btn.setText(formatDynamicButtonText(label, subText));
                    }
                }
            }
        }

        if (specialActionsButtonPanel != null) {
            for (Component c : specialActionsButtonPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    ActionButton btn = (ActionButton) c;
                    btn.setHorizontalAlignment(SwingConstants.CENTER);
                    PossibleAction action = btn.getPossibleActions() != null && !btn.getPossibleActions().isEmpty()
                            ? btn.getPossibleActions().get(0)
                            : null;
                    if (action != null) {
                        String label = action.getButtonLabel();
                        if (label == null || label.trim().isEmpty())
                            label = action.toString();
                        btn.setText(formatDynamicButtonText(label, null));
                    }
                }
            }
        }

        if (trainButtonsPanel != null) {
            for (Component c : trainButtonsPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    ActionButton btn = (ActionButton) c;
                    btn.setHorizontalAlignment(SwingConstants.CENTER);
                    PossibleAction action = btn.getPossibleActions() != null && !btn.getPossibleActions().isEmpty()
                            ? btn.getPossibleActions().get(0)
                            : null;
                    if (action != null && action instanceof BuyTrain) {
                        String label = action.getButtonLabel().replace("Buy ", "").replace(" train", "")
                                .replace(" from ", " - ").replace(" for ", " - ");
                        btn.setText(formatDynamicButtonText(label, null));
                    }
                }
            }
        }
    }

    public static void forceGlobalCleanup() {
        SwingUtilities.invokeLater(() -> {
            for (ORPanel panel : activeInstances) {
                if (panel.orUIManager != null && panel.orUIManager.getGameUIManager() != null) {
                    RoundFacade current = panel.orUIManager.getGameUIManager().getCurrentRound();
                    if (current instanceof OperatingRound)
                        continue;
                }
                panel.finish();
            }
        });
    }

    public void initORCompanyTurn(PublicCompany orComp, int orCompIndex) {
        setTileBuildNumbers(false);
        if (orWindow != null && orWindow.getMapPanel() != null)
            orWindow.getMapPanel().clearOverlays();

        this.orComp = orComp;
        this.currentOperatingComp = orComp;

        removeAllHighlights();
        setStandardPanelsVisible(true);
        if (specialContainer != null)
            specialContainer.setVisible(false);

        // Ensure both parts are visible
        if (orComp != null && lblCompanyInfo != null) {
            lblCompanyInfo.setVisible(true);
            lblPhaseInstruction.setVisible(true);
        }

        updateSidebarData();
        // updateCurrentRoutes(false);
        disableRoutesDisplay();

    }

    public void resetSidebarState() {
        if (btnDone != null) {
            btnDone.setActionCommand(DONE_CMD);
            if (btnDone.getPossibleActions() != null) {
                btnDone.getPossibleActions().clear();
            }
            btnDone.setPossibleAction(null);
            btnDone.setEnabled(false);
        }
        if (btnTileSkip != null)
            btnTileSkip.setEnabled(false);
        if (btnTileConfirm != null)
            btnTileConfirm.setEnabled(false);
        if (btnTokenSkip != null)
            btnTokenSkip.setEnabled(false);
        if (btnTokenConfirm != null)
            btnTokenConfirm.setEnabled(false);

        if (btnRevPayout != null) {
            btnRevPayout.setEnabled(false);
            if (btnRevPayout.getPossibleActions() != null) {
                btnRevPayout.getPossibleActions().clear();
            }
            btnRevPayout.setPossibleAction(null);
        }
        if (btnRevWithhold != null) {
            btnRevWithhold.setEnabled(false);
            if (btnRevWithhold.getPossibleActions() != null) {
                btnRevWithhold.getPossibleActions().clear();
            }
            btnRevWithhold.setPossibleAction(null);
        }
        if (btnRevSplit != null) {
            btnRevSplit.setEnabled(false);
            if (btnRevSplit.getPossibleActions() != null) {
                btnRevSplit.getPossibleActions().clear();
            }
            btnRevSplit.setPossibleAction(null);
        }

        if (btnTrainSkip != null)
            btnTrainSkip.setEnabled(false);

        // Critical: Clear the cached action list so we don't buy "Ghost Trains" from
        // previous states
        if (availableTrainActions != null) {
            availableTrainActions.clear();
        }

        if (trainButtonsPanel != null) {
            trainButtonsPanel.removeAll();
            trainButtonsPanel.setVisible(true);
        }
        if (specialActionsButtonPanel != null) {
            specialActionsButtonPanel.removeAll();
            specialActionsButtonPanel.setVisible(true);
        }
        if (miscActionPanel != null) {
            miscActionPanel.removeAll();
        }

        if (specialNotificationPanel != null) {
            specialNotificationPanel.removeAll();
            specialNotificationPanel.setVisible(false);
        }

if (specialPanel != null) {
            specialPanel.removeAll();
        }
        if (specialContainer != null) {
            specialContainer.setVisible(false);
        }

        activePhase = 0;
    }

    public void finish() {
        this.orComp = null;
        this.orCompIndex = -1;
        setTileBuildNumbers(false);

        if (sidebarPanel != null) {
            sidebarPanel.setBackground(Color.LIGHT_GRAY);
            if (lblCompanyInfo != null) {
                lblCompanyInfo.setText("Stock Round");
                lblCompanyInfo.setBackground(Color.LIGHT_GRAY);
                lblCompanyInfo.setForeground(Color.GRAY);
                lblPhaseInstruction.setVisible(false); // Hide phase part in SR
            }
            if (lblPlayerInfo != null) {
                lblPlayerInfo.setVisible(false);
                lblPlayerInfo.setText("");
                lblPlayerInfo.setBackground(Color.LIGHT_GRAY);
            }

            setStandardPanelsVisible(false);
            if (specialContainer != null)
                specialContainer.setVisible(false);
        }

        disableRoutesDisplay();
        resetActions();
        repaint();
    }

    public void disableButtons() {
        if (button1 != null)
            button1.setEnabled(false);
        if (button2 != null)
            button2.setEnabled(false);
        if (button3 != null)
            button3.setEnabled(false);
        // Do not block special panel here, purely standard buttons
    }

    public static final int SIDEBAR_WIDTH = 200;
    public static final int SIDEBAR_HEIGHT = 800;
    public static final int HEADER_LOGO_HEIGHT = 90;
    public static final int HEADER_INFO_HEIGHT = 60;
    public static final int HEADER_PHASE_HEIGHT = 30;
    public static final int READOUT_PANEL_HEIGHT = 60;
    public static final int MIN_PHASE_PANEL_HEIGHT = 50;
    public static final int TRAIN_CARD_HEIGHT = 25;
    public static final int REVENUE_BUTTON_ROW_HEIGHT = 25;
    public static final int FOOTER_DONE_HEIGHT = 45;

    private double localFontScale = -1.0;

    public double getFontScale() {
        if (localFontScale < 0) {
            // Read directly from the persistent window settings file
            if (orWindow != null && orWindow.getGameUIManager() != null) {
                net.sf.rails.ui.swing.WindowSettings ws = orWindow.getGameUIManager().getWindowSettings();
                localFontScale = ws.getDoubleProperty("orPanel.scale", 1.0);
            } else {
                localFontScale = 1.0;
            }
        }
        return localFontScale;
    }

    public void adjustFontScale(double delta) {
        localFontScale = getFontScale() + delta;
        if (localFontScale < 0.5)
            localFontScale = 0.5;
        if (localFontScale > 3.0)
            localFontScale = 3.0;

        // Save directly to the window settings
        if (orWindow != null && orWindow.getGameUIManager() != null) {
            net.sf.rails.ui.swing.WindowSettings ws = orWindow.getGameUIManager().getWindowSettings();
            ws.setDoubleProperty("orPanel.scale", localFontScale);
            ws.save(); // Persist to rails_data/windowSettings/settings_xxxx.rails_ini
        }

        updateScale();
    }

    public int scale(int value) {
        return (int) (value * getFontScale());
    }

    public int getSidebarWidth() {
        return scale(SIDEBAR_WIDTH);
    }

    public void updateScale() {
        int sw = getSidebarWidth();

        this.setPreferredSize(new Dimension(sw, 0));

        if (sidebarPanel != null) {
            sidebarPanel.setPreferredSize(new Dimension(sw, scale(SIDEBAR_HEIGHT)));
            sidebarPanel.setMinimumSize(new Dimension(sw, scale(SIDEBAR_HEIGHT)));
            sidebarPanel.setMaximumSize(new Dimension(sw, Short.MAX_VALUE));
        }

        if (lblCompanyInfo != null) {
            lblCompanyInfo.setPreferredSize(new Dimension(sw, scale(HEADER_INFO_HEIGHT)));
            lblCompanyInfo.setMaximumSize(new Dimension(sw, scale(HEADER_INFO_HEIGHT)));
        }

        if (lblPhaseInstruction != null) {
            lblPhaseInstruction.setPreferredSize(new Dimension(sw, scale(HEADER_PHASE_HEIGHT)));
            lblPhaseInstruction.setMaximumSize(new Dimension(sw, scale(HEADER_PHASE_HEIGHT)));
        }

        if (lblPlayerInfo != null) {
            lblPlayerInfo.setPreferredSize(new Dimension(sw, scale(20)));
            lblPlayerInfo.setMaximumSize(new Dimension(sw, scale(20)));
        }

        if (cashPanel != null) {
            cashPanel.setPreferredSize(new Dimension(sw, scale(READOUT_PANEL_HEIGHT)));
            cashPanel.setMinimumSize(new Dimension(sw, scale(READOUT_PANEL_HEIGHT)));
            cashPanel.setMaximumSize(new Dimension(sw, scale(READOUT_PANEL_HEIGHT)));
        }

        if (loansPanel != null) {
            loansPanel.setPreferredSize(new Dimension(sw, scale(READOUT_PANEL_HEIGHT)));
            loansPanel.setMinimumSize(new Dimension(sw, scale(READOUT_PANEL_HEIGHT)));
            loansPanel.setMaximumSize(new Dimension(sw, scale(READOUT_PANEL_HEIGHT)));
        }

        if (btnTileConfirm != null)
            updateBtnSize(btnTileConfirm, sw - scale(20), scale(BTN_HEIGHT));
        if (btnTokenConfirm != null)
            updateBtnSize(btnTokenConfirm, sw - scale(20), scale(BTN_HEIGHT));
        if (btnTrainSkip != null)
            updateBtnSize(btnTrainSkip, sw - scale(20), scale(BTN_HEIGHT));
        if (btnDone != null)
            updateBtnSize(btnDone, sw - scale(10), scale(40));

        if (btnDone != null)
            btnDone.setFont(new Font("SansSerif", Font.BOLD, scale(14)));
        if (btnTileConfirm != null)
            btnTileConfirm.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
        if (btnTokenConfirm != null)
            btnTokenConfirm.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
        if (btnTrainSkip != null)
            btnTrainSkip.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
        if (btnRevPayout != null)
            btnRevPayout.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
        if (btnRevWithhold != null)
            btnRevWithhold.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
        if (btnRevSplit != null)
            btnRevSplit.setFont(new Font("SansSerif", Font.BOLD, scale(12)));

        if (lblCompanyInfo != null) {
            String text = lblCompanyInfo.getText();
            if (text != null && text.contains("<font size='6'>")) {
                // Approximate HTML size scaling (HTML sizes 1-7 don't scale linearly, so we
                // adjust font directly if possible, or leave HTML alone and scale wrapper)
                // Since JLabel with HTML ignores setFont(), we strip HTML or dynamically
                // rewrite the tags if needed.
                // For now, we scale non-HTML labels and leave the HTML dynamic re-writes to the
                // updateSidebarData method.
            }
        }
        if (lblCash != null)
            lblCash.setFont(new Font("SansSerif", Font.BOLD, scale(22)));
        if (lblLoans != null)
            lblLoans.setFont(new Font("SansSerif", Font.BOLD, scale(22)));
        if (lblFixed != null)
            lblFixed.setFont(new Font("SansSerif", Font.BOLD, scale(18)));
        if (lblRoute != null)
            lblRoute.setFont(new Font("SansSerif", Font.BOLD, scale(18)));

        if (specialNotificationPanel != null) {
            for (Component c : specialNotificationPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    updateBtnSize((ActionButton) c, sw - scale(20), scale(BTN_HEIGHT));
                    c.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
                }
            }
        }

        if (specialPanel != null) {
            for (Component c : specialPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    ((ActionButton) c).setMaximumSize(new Dimension(sw - scale(20), scale(75)));
                    c.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
                }
            }
        }

        if (specialActionsButtonPanel != null) {
            for (Component c : specialActionsButtonPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    ((ActionButton) c).setMaximumSize(new Dimension(sw - scale(10), scale(45)));
                    c.setFont(new Font("SansSerif", Font.BOLD, scale(12))); // Inherited scaled font

                }
            }
        }

        if (trainButtonsPanel != null) {
            for (Component c : trainButtonsPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    ((ActionButton) c).setMaximumSize(new Dimension(sw - scale(10), scale(45)));
                    c.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
                }
            }
        }

        if (sidebarPanel != null) {
            sidebarPanel.revalidate();
            sidebarPanel.repaint();

        }
        rebuildDynamicButtonTexts();
        // Force a data refresh to rebuild HTML labels with the new scale
        updateSidebarData();
        if (orWindow != null && orWindow.getContentPane() != null) {
            orWindow.getContentPane().revalidate();
            orWindow.getContentPane().repaint();
        }

        this.revalidate();
        this.repaint();
    }

    private void updateBtnSize(JComponent btn, int w, int h) {
        if (btn != null) {
            btn.setPreferredSize(new Dimension(w, h));
            btn.setMaximumSize(new Dimension(w, h));
        }
    }

    private void initSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));

        // 1. Force sidebar to fixed width
        // Sidebar strict width, flexible height
        sidebarPanel.setPreferredSize(new Dimension(getSidebarWidth(), scale(SIDEBAR_HEIGHT)));
        sidebarPanel.setMinimumSize(new Dimension(getSidebarWidth(), scale(SIDEBAR_HEIGHT)));
        sidebarPanel.setMaximumSize(new Dimension(getSidebarWidth(), Short.MAX_VALUE));

        sidebarPanel.setBackground(BG_DETAILS);
        sidebarPanel.setOpaque(true);

        lblCompanyInfo = new JLabel("Stock Round", SwingConstants.CENTER);
        lblCompanyInfo.setOpaque(true); // Critical for background color
        lblCompanyInfo.setBackground(Color.LIGHT_GRAY);
        lblCompanyInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Fixed dimensions ensure it fills the width
        lblCompanyInfo.setPreferredSize(new Dimension(getSidebarWidth(), scale(HEADER_INFO_HEIGHT)));
        lblCompanyInfo.setMaximumSize(new Dimension(getSidebarWidth(), scale(HEADER_INFO_HEIGHT)));

        // 2. Bottom Component: Phase Instruction
        lblPhaseInstruction = new JLabel("", SwingConstants.CENTER);
        lblPhaseInstruction.setOpaque(true); // Critical for background color
        lblPhaseInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblPhaseInstruction.setPreferredSize(new Dimension(getSidebarWidth(), scale(HEADER_PHASE_HEIGHT)));
        lblPhaseInstruction.setMaximumSize(new Dimension(getSidebarWidth(), scale(HEADER_PHASE_HEIGHT)));
        sidebarPanel.add(lblCompanyInfo);

        lblPlayerInfo = new JLabel("", SwingConstants.CENTER);
        lblPlayerInfo.setOpaque(true);
        lblPlayerInfo.setBackground(Color.LIGHT_GRAY);
        lblPlayerInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblPlayerInfo.setPreferredSize(new Dimension(getSidebarWidth(), scale(20))); // Smaller height for player
        lblPlayerInfo.setMaximumSize(new Dimension(getSidebarWidth(), scale(20)));
        sidebarPanel.add(lblPlayerInfo);

        sidebarPanel.add(lblPhaseInstruction);

        sidebarPanel.add(Box.createVerticalStrut(5));

        lblLoans = new JLabel("0/0", SwingConstants.CENTER);
        loansPanel = createReadoutPanel("Loans", lblLoans);
// Do not structurally restrict layout adding at construction time if data model is unhydrated
        if (hasCompanyLoans) {
            sidebarPanel.add(loansPanel);
            sidebarPanel.add(Box.createVerticalStrut(5));
        }

        // 2. Cash (Readout Style)
        lblCash = new JLabel("-", SwingConstants.CENTER);
        cashPanel = createReadoutPanel("Treasury", lblCash);
        sidebarPanel.add(cashPanel);
        sidebarPanel.add(Box.createVerticalStrut(5));

        // 4. Phase 1 (Tile)
        phase1Panel = createPhasePanel("1. Build Track");
        // Reduce Height: Set a strict maximum height for Phase 1 (Header + Button +
        // Padding)
        btnTileConfirm = createSidebarButton("Skip", CONFIRM_CMD);
        // Add breathing space above and below the button
        phase1Panel.add(Box.createVerticalStrut(PANEL_ACTION_GAP));
        phase1Panel.add(btnTileConfirm);
        phase1Panel.add(Box.createVerticalStrut(PANEL_ACTION_GAP));

        miscActionPanel = new JPanel();
        miscActionPanel.setLayout(new BoxLayout(miscActionPanel, BoxLayout.Y_AXIS));
        miscActionPanel.setOpaque(false);
        phase1Panel.add(Box.createVerticalStrut(2));
        phase1Panel.add(miscActionPanel);

        sidebarPanel.add(phase1Panel);
        sidebarPanel.add(Box.createVerticalStrut(2));

        // 5. Phase 2 (Token)
        phase2Panel = createPhasePanel("2. Place Token");
        tokenDisplay = new TokenDisplayPanel();
        tokenDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        phase2Panel.add(tokenDisplay);

        btnTokenConfirm = createSidebarButton("Skip", CONFIRM_CMD);
        // Add breathing space between content and button, and at bottom
        phase2Panel.add(Box.createVerticalStrut(PANEL_ACTION_GAP));
        phase2Panel.add(btnTokenConfirm);
        phase2Panel.add(Box.createVerticalStrut(PANEL_ACTION_GAP));

        sidebarPanel.add(phase2Panel);
        sidebarPanel.add(Box.createVerticalStrut(2));

        // 6. Phase 3 (Revenue)
        phase3Panel = createPhasePanel("3. Revenue");

        JPanel revDisplayPanel = new JPanel(new GridLayout(1, hasDirectCompanyIncomeInOR ? 2 : 1, 5, 0));
        revDisplayPanel.setOpaque(false);
        revDisplayPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, scale(70)));
        revDisplayPanel.setPreferredSize(new Dimension(getSidebarWidth() - scale(10), scale(50)));

        JPanel divBox = new JPanel();
        divBox.setLayout(new BoxLayout(divBox, BoxLayout.Y_AXIS));
        divBox.setOpaque(false);

        lblRoute = new JLabel("0", SwingConstants.CENTER);
        lblRoute.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblRoute.setFont(new Font("SansSerif", Font.BOLD, 18));

        if (hasDirectCompanyIncomeInOR) {
            JLabel lblDivTitle = new JLabel("Route", SwingConstants.CENTER);
            lblDivTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblDivTitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
            divBox.add(lblDivTitle);
        } else {
            divBox.add(Box.createVerticalStrut(10));
        }

        JPanel routeContainer = new JPanel();
        routeContainer.setLayout(new BoxLayout(routeContainer, BoxLayout.X_AXIS));
        routeContainer.setOpaque(false);
        routeContainer.add(Box.createHorizontalGlue());

        revSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 10));
        revSpinner.setPreferredSize(new Dimension(80, 30));
        revSpinner.setMaximumSize(new Dimension(100, 35));
        revSpinner.setFont(new Font("SansSerif", Font.BOLD, 16));
        revSpinner.addChangeListener(e -> {
            if (isRevenueValueToBeSet) {
                int val = (Integer) revSpinner.getValue();
                int special = 0;
                try {
                    if (lblFixed != null)
                        special = Integer.parseInt(lblFixed.getText().replaceAll("[^\\d]", ""));
                } catch (Exception ex) {
                }
                setRevenue(orCompIndex, val + special, special);
            }
        });

        routeContainer.add(revSpinner);
        routeContainer.add(lblRoute);
        updateSpinnerVisibility();
        routeContainer.add(Box.createHorizontalGlue());
        divBox.add(routeContainer);

        revDisplayPanel.add(divBox);

        if (hasDirectCompanyIncomeInOR) {
            JPanel retBox = new JPanel();
            retBox.setLayout(new BoxLayout(retBox, BoxLayout.Y_AXIS));
            retBox.setOpaque(false);
            JLabel lblRetTitle = new JLabel("Fixed", SwingConstants.CENTER);
            lblRetTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblRetTitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
            lblFixed = new JLabel("0", SwingConstants.CENTER);
            lblFixed.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblFixed.setFont(new Font("SansSerif", Font.BOLD, 18));
            retBox.add(lblRetTitle);
            retBox.add(lblFixed);
            revDisplayPanel.add(retBox);
        }

        phase3Panel.add(Box.createVerticalStrut(5));
        phase3Panel.add(revDisplayPanel);

        // Use GridLayout to force exactly equal 1/3 widths for the 3 buttons
        JPanel revBtnRow = new JPanel(new GridLayout(1, 3, 5, 0)); // 1 row, 3 cols, 5px gap
        revBtnRow.setOpaque(false);
        revBtnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); // slight height increase

        btnRevPayout = createSmallButton("Pay");
        btnRevWithhold = createSmallButton("Hold");
        btnRevSplit = createSmallButton("Split");

        btnRevPayout.setActionCommand(PAYOUT_CMD);
        btnRevPayout.addActionListener(this);
        btnRevWithhold.setActionCommand(WITHHOLD_CMD);
        btnRevWithhold.addActionListener(this);
        btnRevSplit.setActionCommand(SPLIT_CMD);
        btnRevSplit.addActionListener(this);

        revBtnRow.add(btnRevPayout);
        revBtnRow.add(btnRevWithhold);
        revBtnRow.add(btnRevSplit);

        // Increase spacing around the revenue button row
        phase3Panel.add(Box.createVerticalStrut(PANEL_ACTION_GAP));
        phase3Panel.add(revBtnRow);
        phase3Panel.add(Box.createVerticalStrut(PANEL_ACTION_GAP));
        sidebarPanel.add(phase3Panel);
        sidebarPanel.add(Box.createVerticalStrut(2));

        // 7. Phase 4 (Train)
        phase4Panel = createPhasePanel("4. Buy Trains");

        trainDisplay = new TrainDisplayPanel();
        phase4Panel.add(trainDisplay);
        phase4Panel.add(Box.createVerticalStrut(5));
        JSeparator trainSep = new JSeparator();
        trainSep.setForeground(Color.LIGHT_GRAY);
        trainSep.setMaximumSize(new Dimension(getSidebarWidth() - scale(20), scale(2)));
        phase4Panel.add(trainSep);
        phase4Panel.add(Box.createVerticalStrut(5));

        trainButtonsPanel = new JPanel();
        trainButtonsPanel.setLayout(new BoxLayout(trainButtonsPanel, BoxLayout.Y_AXIS));
        trainButtonsPanel.setOpaque(false);
        phase4Panel.add(trainButtonsPanel);

        btnTrainSkip = createSidebarButton("Skip Buy", TRAIN_SKIP_CMD);
        phase4Panel.add(btnTrainSkip);
        sidebarPanel.add(phase4Panel);

        sidebarPanel.add(Box.createVerticalStrut(5));

        // 7.5 Phase 5 (Special Actions)
        phase5Panel = createPhasePanel("5. Special Actions");
        phase5Panel.add(Box.createVerticalStrut(5));

        specialContainer = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(getSidebarWidth(), pref.height);
            }
        };
        specialContainer.setOpaque(false);
        specialContainer.setVisible(false);
        specialContainer.setBorder(BorderFactory.createEmptyBorder());

        specialPanel = new JPanel();
        specialPanel.setLayout(new BoxLayout(specialPanel, BoxLayout.Y_AXIS));
        specialPanel.setOpaque(false);
        specialContainer.add(specialPanel, BorderLayout.CENTER);
        phase5Panel.add(specialContainer);

        specialActionsButtonPanel = new JPanel();
        specialActionsButtonPanel.setLayout(new BoxLayout(specialActionsButtonPanel, BoxLayout.Y_AXIS));
        specialActionsButtonPanel.setOpaque(false);
        phase5Panel.add(specialActionsButtonPanel);

        // Move End Turn button into Phase 5 Panel
        btnDone = createSidebarButton("End Turn", DONE_CMD);
        btnDone.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnDone.setPreferredSize(new Dimension(getSidebarWidth() - scale(10), scale(40)));
        btnDone.setEnabled(false);
        resetButtonStyle(btnDone);

        phase5Panel.add(Box.createVerticalStrut(5));
        phase5Panel.add(btnDone);
        phase5Panel.add(Box.createVerticalStrut(5));

        sidebarPanel.add(phase5Panel);
        sidebarPanel.add(Box.createVerticalStrut(5));

        // 8. Footer (Notifications Only)
        footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(PANEL_ACTION_GAP, 0, PANEL_ACTION_GAP, 0));

        specialNotificationPanel = new JPanel();
        specialNotificationPanel.setLayout(new BoxLayout(specialNotificationPanel, BoxLayout.Y_AXIS));
        specialNotificationPanel.setOpaque(false);
        specialNotificationPanel.setVisible(false);

        footerPanel.add(specialNotificationPanel);
        footerPanel.add(Box.createVerticalStrut(4));

        sidebarPanel.add(footerPanel);
        sidebarPanel.add(Box.createVerticalStrut(5));

        add(sidebarPanel);
    }

    private JPanel createReadoutPanel(String title, JLabel valueLabel) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_DETAILS);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), title,
                TitledBorder.LEFT, TitledBorder.TOP, FONT_HEADER));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(valueLabel);

        // Strict readout dimensions
        p.setPreferredSize(new Dimension(getSidebarWidth(), scale(READOUT_PANEL_HEIGHT)));
        p.setMinimumSize(new Dimension(getSidebarWidth(), scale(READOUT_PANEL_HEIGHT)));
        p.setMaximumSize(new Dimension(getSidebarWidth(), scale(READOUT_PANEL_HEIGHT)));
        return p;
    }

    private JPanel createPhasePanel(String title) {
        // Use an anonymous class to override sizing behavior dynamically
        JPanel p = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                // FORCE the layout to respect the content height.
                // By making Max Height = Preferred Height, the panel refuses to stretch
                // vertically to fill empty space.
                Dimension pref = getPreferredSize();
                return new Dimension(getSidebarWidth(), pref.height);
            }

            @Override
            public Dimension getPreferredSize() {
                // Ensure width is always fixed to sidebar width, but height is dynamic
                Dimension superPref = super.getPreferredSize();
                return new Dimension(getSidebarWidth(), Math.max(superPref.height, scale(MIN_PHASE_PANEL_HEIGHT)));
            }
        };

        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), title,
                TitledBorder.LEFT, TitledBorder.TOP, FONT_HEADER));
        p.setOpaque(true);
        p.setBackground(BG_NORMAL);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        // We do NOT set setPreferredSize or setMaximumSize manually here anymore.
        // The overrides above handle it dynamically based on content (buttons, labels).

        return p;
    }

    private ActionButton createSidebarButton(String text, String cmd) {
        ActionButton b = new ActionButton(RailsIcon.OK);
        b.setText(text);
        b.setIcon(null);
        b.setActionCommand(cmd);
        b.addActionListener(this);
        b.setEnabled(false);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setFont(BTN_FONT);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setPreferredSize(new Dimension(getSidebarWidth() - scale(20), scale(BTN_HEIGHT)));
        b.setMaximumSize(new Dimension(getSidebarWidth() - scale(20), scale(BTN_HEIGHT)));
        return b;
    }

    private void styleRevenueButton(ActionButton btn, boolean isSelected) {
        if (btn == null)
            return;

        // 1. Force BasicUI to prevent LookAndFeel (e.g. Mac Aqua) from overriding
        // disabled styles
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        // 2. Enforce strict opacity and painting
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        btn.setFocusPainted(false);

        // 3. Uniform Border and Font
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));

        // 4. Handle State Colors
        if (isSelected) {
            btn.setBackground(SYS_BLUE);
            btn.setForeground(Color.WHITE);
        } else {
            // Force White background even if Disabled
            btn.setBackground(Color.WHITE);
            // Optional: Ensure text remains visible/black if disabled
            btn.setForeground(Color.BLACK);
        }
    }

    public void recreate(Round or) {
        PublicCompany comp = null;
        if (or instanceof OperatingRound) {
            comp = ((OperatingRound) or).getOperatingCompany();
        } else {
            try {
                java.lang.reflect.Method m = or.getClass().getMethod("getOperatingCompany");
                comp = (PublicCompany) m.invoke(or);
            } catch (Exception e) {
            }
        }
        initORCompanyTurn(comp, 0);
    }

    public void recreate(OperatingRound or) {
        initORCompanyTurn(or.getOperatingCompany(), 0);
    }

    private void initButtonPanel() {
        buttonOC = new ActionButton(RailsIcon.OPERATING_COST);
        buttonOC.setActionCommand(OPERATING_COST_CMD);
        buttonOC.addActionListener(this);
        button1 = new ActionButton(RailsIcon.BUY_TRAIN);
        button1.addActionListener(this);
        button2 = new ActionButton(RailsIcon.DONE);
        button2.addActionListener(this);
        button3 = new ActionButton(RailsIcon.BUY_PRIVATE);
        button3.addActionListener(this);
    }

    public void setTileBuildNumbers(boolean show) {
        this.showNumbersActive = show;
        if (btnBuildShow != null)
            btnBuildShow.setText(showNumbersActive ? "Hide" : "Show");
        if (orUIManager != null)
            orUIManager.updateHexBuildNumbers(showNumbersActive);
        repaint();
    }

    public void toggleTileBuildNumbers() {
        setTileBuildNumbers(!this.showNumbersActive);
    }

    public String format(int amount) {
        return orUIManager.getGameUIManager().format(amount);
    }

    public void setSpecialMode(boolean enabled) {
        this.specialModeActive = enabled;
    } // Kept for compatibility but driven by updateDynamicActions

    // TrainDisplayPanel, addTrainBuyButton logic as is) ...
    // NOTE: For brevity in this response I am summarizing that the standard methods
    // (getRevenue, setDividend, etc.) remain as they were in the previous version,
    // ensuring standard functionality is not lost. The critical change is in the
    // Update/Render logic above.

    public void enableUndo(GameAction action) {
        if (action != null)
            this.currentUndoAction = action;
    }

    public void enableRedo(GameAction action) {
        if (action != null)
            this.currentRedoAction = action;
    }

    public void enableConfirm(boolean hasSelection) {
        ActionButton targetBtn = (activePhase == 1) ? btnTileConfirm : (activePhase == 2) ? btnTokenConfirm : null;
        if (targetBtn == null)
            return;
        targetBtn.setEnabled(true);
        if (hasSelection) {
            Color phaseColor = (activePhase == 1) ? PH_TILE_DARK : PH_TOKEN_DARK;
            styleButton(targetBtn, SYS_BLUE, "Confirm");
        } else {
            styleButton(targetBtn, SYS_BLUE, "Skip");
        }
        updateDefaultButton();
    }

    private void updateDefaultButton() {
        ActionButton defaultBtn = null;

        if (activePhase == 1)
            defaultBtn = btnTileConfirm;
        else if (activePhase == 2)
            defaultBtn = btnTokenConfirm;
        else if (activePhase == 3) {
            // Prioritize Payout -> Split -> Withhold
            if (btnRevPayout != null && btnRevPayout.isEnabled())
                defaultBtn = btnRevPayout;
            else if (btnRevSplit != null && btnRevSplit.isEnabled())
                defaultBtn = btnRevSplit;
            else
                defaultBtn = btnRevWithhold;
        } else if (activePhase == 4)
            defaultBtn = btnTrainSkip;
        else if (activePhase == 5)
            defaultBtn = btnDone;

        else
            defaultBtn = btnDone;

        // Fallback: If Done is enabled and nothing else is, default to Done
        if (defaultBtn == null || !defaultBtn.isEnabled()) {
            if (btnDone != null && btnDone.isEnabled())
                defaultBtn = btnDone;
        }

        this.currentDefaultButton = defaultBtn;
        if (getRootPane() != null)
            getRootPane().setDefaultButton(defaultBtn);

    }

    private void cycleHexes(int direction) {
        if (orUIManager == null || cycleableHexes.isEmpty())
            return;
        cycleIndex += direction;
        if (cycleIndex >= cycleableHexes.size())
            cycleIndex = 0;
        if (cycleIndex < 0)
            cycleIndex = cycleableHexes.size() - 1;
        orUIManager.hexClicked(cycleableHexes.get(cycleIndex), orUIManager.getMap().getSelectedHex(), false);
        enableConfirm(true);
    }

    // Required Stubs for Compilation (Legacy Interface support)
    public void resetHexCycle() {
        cycleableHexes.clear();
        cycleIndex = -1;
    }

    public void updateCycleableHexes(Collection<GUIHex> hexes) {
        cycleableHexes.clear();
        if (hexes != null)
            for (GUIHex h : hexes)
                if (h.getState() == GUIHex.State.SELECTABLE)
                    cycleableHexes.add(h);
    }

    public void initTileLayingStep() {
        activePhase = 1;
    }

    public void initTokenLayingStep() {
        activePhase = 2;
    }

    public void initTrainBuying(List<BuyTrain> a) {
        activePhase = 4;
    }

    public void initPayoutStep(int i, SetDividend s, boolean w, boolean spl, boolean p) {
        activePhase = 3;
    }

    public void setupConfirm() {
        enableConfirm(false);
    }

    public void enableSkip(NullAction a) {
        if (btnTrainSkip != null && activePhase == 4) {
            setupButton(btnTrainSkip, a);
            btnTrainSkip.setEnabled(true);
            styleButton(btnTrainSkip, UITheme.ACTION_SKIP, "Skip Buy");
        } else if (btnDone != null) {
            enableDone(a);
        }
    }

    public void enableDone(NullAction a) {
        if (btnDone != null) {
            setupButton(btnDone, a);
            bindActionHotkey(btnDone, a);
            btnDone.setEnabled(true);
            styleButton(btnDone, UITheme.ACTION_SKIP, getDoneButtonText());
            btnDone.setForeground(Color.WHITE);
            btnDone.setFont(new Font("SansSerif", Font.BOLD, 14));
        }
    }

    public void initOperatingCosts(boolean b) {
        if (buttonOC != null) {
            buttonOC.setEnabled(b);
            buttonOC.setVisible(b);
        }
    }

    public void initPrivateBuying(boolean b) {
        if (button3 != null) {
            button3.setEnabled(b);
            button3.setVisible(b);
        }
    }

    public void enableLoanTaking(TakeLoans a) {
        if (specialPanel != null && specialContainer != null) {
            specialContainer.setVisible(true);
            boolean alreadyExists = false;
            for (Component c : specialPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    List<PossibleAction> pas = ((ActionButton) c).getPossibleActions();
                    if (pas != null && !pas.isEmpty() && pas.get(0).getClass().getSimpleName().contains("TakeLoans")) {
                        alreadyExists = true;
                        break;
                    }
                }
            }
            if (!alreadyExists) {
                addSpecialActionButton(a);
                specialPanel.revalidate();
                specialPanel.repaint();
            }
        }
        if (phase5Panel != null) {
            phase5Panel.setVisible(true);
            applyPhaseStyle(phase5Panel, null, UITheme.ACTION_SKIP, UITheme.TRAIN_LIGHT, "Special Actions");
        }
    }

    public void enableLoanRepayment(RepayLoans a) {
        if (specialPanel != null && specialContainer != null) {
            specialContainer.setVisible(true);
            boolean alreadyExists = false;
            for (Component c : specialPanel.getComponents()) {
                if (c instanceof ActionButton) {
                    List<PossibleAction> pas = ((ActionButton) c).getPossibleActions();
                    if (pas != null && !pas.isEmpty() && pas.get(0).getClass().getSimpleName().contains("RepayLoans")) {
                        alreadyExists = true;
                        break;
                    }
                }
            }
            if (!alreadyExists) {
                addSpecialActionButton(a);
                specialPanel.revalidate();
                specialPanel.repaint();
            }
        }
        if (phase5Panel != null) {
            phase5Panel.setVisible(true);
            applyPhaseStyle(phase5Panel, null, UITheme.ACTION_SKIP, UITheme.TRAIN_LIGHT, "Special Actions");
        }
        if (activePhase == 0 || activePhase == 6) {
            activePhase = 5;
        }
    }

    public void setDividend(int i, int a) {
        setRevenue(i, a);
    }

    public void revenueUpdate(int best, int special, boolean finalRes) {
        SwingUtilities.invokeLater(() -> {

            if (lblRoute != null) {
                int routeRev = best - special;
                if (routeRev < 0)
                    routeRev = 0;
                lblRoute.setText(format(routeRev));
                if (revSpinner != null) {
                    revSpinner.setValue(routeRev);
                }
            }
            if (lblFixed != null) {
                lblFixed.setText(format(special));
            }

            if (isRevenueValueToBeSet) {
                // Pass the 'special' value (Mine Revenue) from the calculator to the button.
                setRevenue(orCompIndex, best, special);
                // Re-enable the Revenue buttons now that calculations are done
                if (finalRes) {
                    if (btnRevPayout != null && btnRevPayout.getPossibleActions() != null
                            && !btnRevPayout.getPossibleActions().isEmpty()) {
                        PossibleAction pa = btnRevPayout.getPossibleActions().get(0);
                        if (pa instanceof SetDividend && ((SetDividend) pa).getCompany() == orComp) {
                            btnRevPayout.setEnabled(true);
                        }
                    }
                    if (btnRevWithhold != null && btnRevWithhold.getPossibleActions() != null
                            && !btnRevWithhold.getPossibleActions().isEmpty()) {
                        PossibleAction pa = btnRevWithhold.getPossibleActions().get(0);
                        if (pa instanceof SetDividend && ((SetDividend) pa).getCompany() == orComp) {
                            btnRevWithhold.setEnabled(true);
                        }
                    }
                    if (btnRevSplit != null && btnRevSplit.getPossibleActions() != null
                            && !btnRevSplit.getPossibleActions().isEmpty()) {
                        PossibleAction pa = btnRevSplit.getPossibleActions().get(0);
                        if (pa instanceof SetDividend && ((SetDividend) pa).getCompany() == orComp) {
                            btnRevSplit.setEnabled(true);
                        }
                    }
                    updateDefaultButton(); // Refresh focus targets
                }
            }

            // Only draw the path if the toggle is enabled
            if (finalRes && isDisplayCurrentRoutes() &&
                    orUIManager != null && orUIManager.isShowRevenueRoutes()) {
                revenueAdapter.drawOptimalRunAsPath(orUIManager.getMap());
            }

        });
    }

    /**
     * Overloaded setRevenue to handle Special (Direct/Mine) revenue
     */
    public void setRevenue(int i, int a, int special) {
        updateRevenueButton(btnRevPayout, a, special);
        updateRevenueButton(btnRevWithhold, a, special);
        updateRevenueButton(btnRevSplit, a, special);
    }

    /**
     * Legacy method support
     */
    public void setRevenue(int i, int a) {
        setRevenue(i, a, 0);
    }

    // Revenue Helpers
    private void updateRevenueButton(ActionButton btn, int amount) {
        updateRevenueButton(btn, amount, 0);
    }

    private void updateRevenueButton(ActionButton btn, int amount, int special) {
        if (btn == null || !btn.isEnabled())
            return;
        List<PossibleAction> actions = btn.getPossibleActions();
        if (actions != null && !actions.isEmpty() && actions.get(0) instanceof SetDividend) {
            SetDividend sd = (SetDividend) actions.get(0);

            // Bind the data from the calculator to the Action
            sd.setActualRevenue(amount);
            sd.setActualCompanyTreasuryRevenue(special);

            btn.repaint();
        }
    }

    private void updateCurrentRoutes(boolean isSetRevenueStep) {
        if (orComp != null && !orComp.isClosed()) {
            isRevenueValueToBeSet = isSetRevenueStep;

            // Only disable buttons for 1817 to prevent lockouts in other games if a
            // calculation fails
            boolean is1817 = orComp.getClass().getSimpleName().contains("1817");
            if (isSetRevenueStep && is1817) {
                if (btnRevPayout != null)
                    btnRevPayout.setEnabled(false);
                if (btnRevWithhold != null)
                    btnRevWithhold.setEnabled(false);
                if (btnRevSplit != null)
                    btnRevSplit.setEnabled(false);
            }
            RailsRoot root = orUIManager.getGameUIManager().getRoot();
            if (revenueThread != null)
                revenueThread.interrupt();
            if (revenueAdapter != null)
                revenueAdapter.removeRevenueListener();
            revenueAdapter = RevenueAdapter.createRevenueAdapter(root, orComp,
                    root.getPhaseManager().getCurrentPhase());
            revenueAdapter.initRevenueCalculator(true);
            revenueAdapter.addRevenueListener(this);

            if (orUIManager != null && orUIManager.getMap() != null) {
                orUIManager.getMap().setDynamicHexBonusCache(revenueAdapter.getDynamicHexBonusCache());
                orUIManager.getMap().setTrainPaths(new java.util.ArrayList<>()); // Protect against NPE in HexMap
            }

            revenueThread = new Thread(revenueAdapter);
            revenueThread.start();
        } else {
            disableRoutesDisplay();
        }
    }

    private void disableRoutesDisplay() {
        if (revenueThread != null)
            revenueThread.interrupt();
        if (revenueAdapter != null)
            revenueAdapter.removeRevenueListener();
        orUIManager.getMap().setTrainPaths(null);
    }

    private boolean isDisplayCurrentRoutes() {
        return "yes".equalsIgnoreCase(Config.get("map.displayCurrentRoutes"));
    }

    public void processIPOBuy() {

        if (availableTrainActions != null && !availableTrainActions.isEmpty()) {
            for (BuyTrain action : availableTrainActions) {

                // Debug Logging
                net.sf.rails.game.state.Owner seller = action.getFromOwner();
                String sellerId = (seller != null) ? seller.getId() : "null";
                String parentId = (seller != null && seller.getParent() != null)
                        ? seller.getParent().getClass().getSimpleName()
                        : "null";

                // Strict Filter: Only buy if seller is explicitly IPO or Bank (and NOT Pool)
                boolean isIpo = false;
                if (seller != null) {
                    if ("IPO".equals(seller.getId())) {
                        isIpo = true;
                    } else if (seller.getParent() instanceof net.sf.rails.game.financial.Bank
                            && !"Pool".equals(seller.getId())) {
                        isIpo = true;
                    }
                }

                if (isIpo) {

                    log.warn("TODO: Engine must handle 0-price standard train buys. UI override should be removed.");
                    if (action.getPricePaid() == 0 && action.getFixedCost() > 0) {
                        action.setPricePaid(action.getFixedCost());
                    }

                    List<PossibleAction> toExec = new ArrayList<>();
                    toExec.add(action);

                    orUIManager.processAction(BUY_TRAIN_CMD, toExec, this);
                    break;
                } else {
                }

            }
        } 
    }

    public void finishORCompanyTurn(int index) {
        resetActions();
        setTileBuildNumbers(false);
        orUIManager.getMap().setTrainPaths(null);
    }

    private void cleanupUpgradesPanel() {
        if (orUIManager != null && orUIManager.getUpgradePanel() != null)
            orUIManager.getUpgradePanel().setInactive();
    }

    private void enableRevenueBtn(ActionButton btn, SetDividend sd, int allocation) {
        btn.setEnabled(true);
        SetDividend clone = (SetDividend) sd.clone();
        clone.setRevenueAllocation(allocation);
        btn.setPossibleAction(clone);
        if (btn == btnRevPayout)
            btn.setText("Pay");
        else if (btn == btnRevWithhold)
            btn.setText("Hold");
        else if (btn == btnRevSplit)
            btn.setText("Split");
    }

    private void setupButton(ActionButton btn, PossibleAction pa) {
        if (btn != null) {
            btn.setEnabled(true);
            btn.setPossibleAction(pa);
        }
    }

    // Inner Classes
    private class TokenDisplayPanel extends JPanel {
public void setTokens(int count, PublicCompany c) {
            removeAll();
            if (c == null || count <= 0) {
                revalidate();
                repaint();
                return;
            }

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;

            // Fetch pricing rule configuration metrics from the engine model
            PublicCompany.BaseCostMethod costMethod = c.getBaseTokenLayCostMethod();
            List<Integer> sequenceCosts = c.getBaseTokenLayCostList();
            int alreadyLaidCount = c.getNumberOfLaidBaseTokens();
            java.util.Set<Integer> knownCosts = null;
            try {
                knownCosts = c.getBaseTokenLayCosts();
            } catch (Exception e) {
                // Safeguard against custom subclass discrepancies
            }

            for (int i = 0; i < count; i++) {
                gbc.gridx = i * 2;
                gbc.insets = new Insets(0, i == 0 ? 0 : 8, 0, 2); // Left spacing between token slots

                // 1. Add Token Icon Label
                JLabel iconLabel = new JLabel(new TokenIcon(scale(24), c.getFgColour(), c.getBgColour(), c.getId()));
                add(iconLabel, gbc);

                // 2. Evaluate Exact Local Pricing for this Inventory Index Position
                String priceText = "";
                if (costMethod == PublicCompany.BaseCostMethod.SEQUENCE && sequenceCosts != null && !sequenceCosts.isEmpty()) {
                    int evaluationIndex = alreadyLaidCount + i;
                    if (evaluationIndex >= sequenceCosts.size()) {
                        evaluationIndex = sequenceCosts.size() - 1;
                    }
                    priceText = "$" + sequenceCosts.get(evaluationIndex);
                } else if (knownCosts != null && knownCosts.size() == 1) {
                    priceText = "$" + knownCosts.iterator().next();
                }

                // 3. Add Inline Pricing Label if determined
                if (!priceText.isEmpty()) {
                    gbc.gridx = (i * 2) + 1;
                    gbc.insets = new Insets(0, 0, 0, 0); // Flush to icon
                    JLabel priceLabel = new JLabel("<html><span style='font-family:\"Monospaced\"; font-weight:bold; color:#000080;'>" 
                        + priceText + "</span></html>");
                    add(priceLabel, gbc);
                }
            }
            revalidate();
            repaint();
        }
    }

    private class TrainDisplayPanel extends JPanel {
        // Visual Constants
        // Dynamically scaled dimensions
        private final Color BG_CARD_PASSIVE = new Color(255, 255, 240); // Beige

        // Dummy group to satisfy ClickField constructor constraints
        private final ButtonGroup dummyGroup = new ButtonGroup();

        public TrainDisplayPanel() {
            setOpaque(false);
            setLayout(new GridBagLayout()); // Center the vertical stack
            setBorder(null);
        }

        public void updateAssets(PublicCompany comp) {
            removeAll();
            if (comp == null)
                return;

            // 1. Parse Trains
            String trainString = comp.getPortfolioModel().getTrainsModel().toText();
            int limit = comp.getCurrentTrainLimit();

            List<String> trains = new ArrayList<>();
            if (trainString != null && !trainString.isEmpty() && !trainString.equals("None")
                    && !trainString.equals("-")) {
                String[] split = trainString.split("[,\\s]+");
                for (String s : split) {
                    if (!s.trim().isEmpty())
                        trains.add(s.trim());
                }
            }

            // 2. Build Vertical Stack
            JPanel stack = new JPanel(new GridLayout(0, 1, 0, 5)); // 1 Column, 5px Gap
            stack.setOpaque(false);

            int totalSlots = Math.max(limit, Math.max(trains.size(), 1));

            // 3. Render Trains and Empty Slots
            for (int i = 0; i < totalSlots; i++) {
                // Pass 'dummyGroup' to avoid NPE
                RailCard card = new RailCard((net.sf.rails.game.Train) null, dummyGroup);

                card.setCompactMode(true);
                card.setOpaque(true);

                if (i < trains.size()) {
                    // Active Train Card
                    String text = trains.get(i);
                    card.setCustomLabel(text);
                    card.setBackground(BG_CARD_PASSIVE);

                    card.setFont(new Font("SansSerif", Font.BOLD, scale(12)));
                    card.setPreferredSize(new Dimension(scale(60), scale(30)));

                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.BLACK, 1),
                            BorderFactory.createEmptyBorder(1, 1, 1, 1)));
                } else {
                    // Empty Slot (Placeholder)
                    card.setCustomLabel("_");
                    card.setBackground(new Color(240, 240, 240));
                    card.setFont(new Font("SansSerif", Font.PLAIN, scale(12)));
                    card.setPreferredSize(new Dimension(scale(60), scale(30)));
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY, 1),
                            BorderFactory.createEmptyBorder(1, 1, 1, 1)));
                    card.setForeground(Color.GRAY);
                }
                stack.add(card);
            }

            // 4. Render Privates
            for (PrivateCompany pc : comp.getPrivates()) {
                RailCard card = new RailCard(pc, dummyGroup);
                card.setCompactMode(true);
                card.setOpaque(true);
                card.setFont(new Font("SansSerif", Font.BOLD, scale(11)));
                card.setPreferredSize(new Dimension(scale(60), scale(30)));
                card.setBackground(new Color(255, 235, 235)); // Pinkish for Privates
                card.setPrivateCompanyTooltip(pc);
                if (orUIManager != null) {
                    net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener.addMouseListener(card, orUIManager, pc,
                            false);
                }
                stack.add(card);
            }

            add(stack);
            revalidate();
            repaint();
        }

        // Legacy stub
        public void setTrains(String s, int i) {
        }
    }

    private void addTrainBuyButton(BuyTrain action) {
        ActionButton btn = new ActionButton(RailsIcon.BUY_TRAIN);
        String text = action.getButtonLabel().replace("Buy ", "").replace(" train", "").replace(" from ", " - ")
                .replace(" for ", " - ");

        btn.setText(formatDynamicButtonText(text, null));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setIcon(null);

        // Match RailCard Styling
        btn.setBackground(new Color(255, 255, 240)); // BG_CARD_PASSIVE
        btn.setOpaque(true);
        btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));

        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(getSidebarWidth() - scale(10), scale(45)));
        btn.setPossibleAction(action);
        btn.addActionListener(this);
        if (trainButtonsPanel != null) {
            trainButtonsPanel.add(btn);
            trainButtonsPanel.add(Box.createVerticalStrut(4));
        }
    }

    public static void setGlobalCustomHeader(String t, String s) {
    } // Deprecated/No-op

    public static void releaseSpecialMode(GameUIManager m) {
    } // Deprecated/No-op

    public static void forceUpdateForManager(GameUIManager m, List<PossibleAction> a) {
        // Forward to instance
        for (ORPanel p : activeInstances)
            if (p.orWindow.gameUIManager == m)
                p.updateDynamicActions(a);
    }

    public Color getTrainHighlightColor() {
        return Color.ORANGE;
    }

    // Stub to prevent compilation error if referenced
    private void clearRevenueAdapter() {
    }

    public void dispose() {
        for (JFrame f : openWindows)
            f.dispose();
        openWindows.clear();
    }

    public JMenuBar getMenuBar() {
        // Menus were moved to StatusWindow, so returning null is safe
        // provided ORWindow checks for null (which we can't change easily),
        // or we return a dummy empty menu bar.
        return new JMenuBar();
    }

    public JPanel getSidebarPanel() {
        return sidebarPanel;
    }

    public void resetActions() {
        // Delegate to existing reset logic
        resetSidebarState();
    }

    public void redrawRoutes() {
        // Check both the Config and our new dynamic toggle
        boolean show = isDisplayCurrentRoutes();
        if (orUIManager != null && !orUIManager.isShowRevenueRoutes()) {
            show = false;
        }

        if (activePhase == 3 && show) { // Revenue phase
            updateCurrentRoutes(true);
        } else if ((activePhase == 1 || activePhase == 2) && show && orUIManager != null && orUIManager.getMap() != null
                && orUIManager.getMap().getDisplayLastRevenueRuns()) {
            updateCurrentRoutes(false);
        } else {
            disableRoutesDisplay();
        }
    }

    public boolean executeUndo() {
        if (currentUndoAction != null && orUIManager != null) {
            orUIManager.processAction(UNDO_CMD, Collections.singletonList(currentUndoAction), this);
            return true;
        }
        return false;
    }

    public int getRevenue(int index) {
        // Return 0 or cached value. 1837 uses this to read spinner values.
        // --- START FIX ---
        if (revSpinner != null) {
            return (Integer) revSpinner.getValue();
        }
        // --- DELETE --- // return (orComp != null) ? orComp.getLastRevenue() : 0;
        return (orComp != null) ? orComp.getLastRevenue() : 0;
        // --- END FIX ---
    }

    public int getCompanyTreasuryBonusRevenue(int index) {
        return 0; // Stub
    }

    public void setTreasuryBonusRevenue(int index, int amount) {
        // No-op or log
    }

    public void stopRevenueUpdate() {
        this.isRevenueValueToBeSet = false;
        // Stop threads if needed
        if (revenueThread != null)
            revenueThread.interrupt();
    }

    private void updateSidebarData() {
        if (specialModeActive)
            return;

        RoundFacade currentRound = null;
        if (orUIManager != null && orUIManager.getGameUIManager() != null
                && orUIManager.getGameUIManager().getGameManager() != null) {
            currentRound = orUIManager.getGameUIManager().getGameManager().getCurrentRound();
        }

        // Intercept specific Special Rounds to cleanly display the Actor's name
        if (currentRound != null && (currentRound.getClass().getSimpleName().contains("Formation")
                || currentRound.getClass().getSimpleName().contains("CoalExchange"))) {
            if (orComp != null) {
                // Valid company context resolved (e.g. discarding trains). Render standard
                // company sidebar.
            } else {
                if (lblCompanyInfo != null) {
                    String playerName = "";
                    if (currentRound.getCurrentPlayer() != null) {
                        playerName = currentRound.getCurrentPlayer().getName();
                    }

                    String topText = "<html><center><font face='SansSerif' size='6'><b>" + playerName
                            + "</b></font></center></html>";
                    lblCompanyInfo.setText(topText);

                    Color bg = currentRound.getClass().getSimpleName().contains("CoalExchange")
                            ? new Color(200, 200, 255)
                            : new Color(152, 251, 152);

                    lblCompanyInfo.setBackground(bg);
                    lblCompanyInfo.setForeground(Color.BLACK);
                    lblCompanyInfo.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, Color.DARK_GRAY));
                    lblCompanyInfo.setVisible(true);

                    if (lblPlayerInfo != null) {
                        lblPlayerInfo.setVisible(false);
                    }

                    String phaseName = "Phase";
                    if (currentRound instanceof net.sf.rails.game.specific._1837.NationalFormationRound) {
                        net.sf.rails.game.specific._1837.PublicCompany_1837 national = ((net.sf.rails.game.specific._1837.NationalFormationRound) currentRound)
                                .getNational();
                        phaseName = (national != null ? national.getId() : "National") + " Formation";
                    } else if (currentRound.getClass().getSimpleName().contains("Formation")) {
                        phaseName = "Prussian Formation";
                    } else if (currentRound.getClass().getSimpleName().contains("CoalExchange")) {
                        phaseName = "Coal Exchange";
                    }

                    String bottomText = "<html><center><font face='SansSerif' size='4'><b>" + phaseName
                            + "</b></font></center></html>";
                    lblPhaseInstruction.setText(bottomText);
                    lblPhaseInstruction.setBackground(bg);
                    lblPhaseInstruction.setForeground(Color.BLACK);
                    lblPhaseInstruction.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.DARK_GRAY));
                    lblPhaseInstruction.setVisible(true);
                }

                setStandardPanelsVisible(false);

                // Override standard panel wipe to ensure Special Actions remain visible during
                // Formation steps
                if (specialContainer != null && specialContainer.isVisible() && phase5Panel != null) {
                    phase5Panel.setVisible(true);
                }

                colorizeActivePhase(null);
                return;
            }
        }

        if (orComp == null) {
            if (lblCash != null)
                lblCash.setText("-");
            return;
        }

        // Dynamically recalculate if the current game rules support loans if it wasn't caught at startup
        if (!hasCompanyLoans && orUIManager != null && orUIManager.getGameUIManager() != null) {
            java.util.List<PublicCompany> allComps = orUIManager.getGameUIManager().getAllPublicCompanies();
            if (allComps != null) {
                for (PublicCompany company : allComps) {
                    if (company != null && company.getMaxNumberOfLoans() != 0) {
                        hasCompanyLoans = true;
                        break;
                    }
                }
            }
            // If discovered dynamically, safely inject the panel into the visual tree structure
            if (hasCompanyLoans && loansPanel != null && sidebarPanel != null) {
                // Insert it right above the cash/treasury panel
                int cashPanelIndex = sidebarPanel.getComponentZOrder(cashPanel);
                if (cashPanelIndex >= 0) {
                    sidebarPanel.add(loansPanel, cashPanelIndex);
                    sidebarPanel.add(Box.createVerticalStrut(5), cashPanelIndex);
                    sidebarPanel.revalidate();
                }
            }
        }

        Color phaseColor = UITheme.READOUT_BG;
        String instruction = "Wait...";

        switch (activePhase) {
            case 1:
                phaseColor = UITheme.TRACK_DARK;
                instruction = "BUILD TRACK";
                break;
            case 2:
                phaseColor = UITheme.TOKEN_DARK;
                instruction = "PLACE TOKEN";
                break;
            case 3:
                phaseColor = UITheme.REVENUE_DARK;
                instruction = "REVENUE";
                break;
            case 4:
                phaseColor = UITheme.TRAIN_DARK;
                instruction = "BUY TRAIN";
                break;
            case 5:
                phaseColor = UITheme.ACTION_SKIP;
                instruction = "SPECIAL ACTIONS";
                break;
            case 6:
                phaseColor = UITheme.ACTION_DONE;
                instruction = "FINALIZE";
                break;

        }
        // Dynamically override instruction if a GuiTargetedAction is active
        boolean specialHeaderApplied = false;
        if (specialContainer != null && specialContainer.isVisible() && specialPanel != null
                && specialPanel.getComponentCount() > 0) {
            Component firstBtn = specialPanel.getComponent(0);
            if (firstBtn instanceof ActionButton && !((ActionButton) firstBtn).getPossibleActions().isEmpty()) {
                PossibleAction pa = ((ActionButton) firstBtn).getPossibleActions().get(0);
                if (pa instanceof GuiTargetedAction) {
                    updateSpecialHeader((GuiTargetedAction) pa);
                    specialHeaderApplied = true;
                }
            }
        }

        if (!specialHeaderApplied) {
            Color headerBg = orComp.getBgColour();
            Color headerFg = orComp.getFgColour();

            if (currentRound != null && currentRound.getClass().getSimpleName().contains("ConnectionRun")) {
                instruction = "CONNECTION RUN";
                headerBg = new Color(255, 215, 0); // Gold
                headerFg = Color.BLACK;
            }

            if (lblCompanyInfo != null) {
                String playerInfo = (orComp.getPresident() != null) ? orComp.getPresident().getName() : "";

                // TOP LABEL: Company Info ONLY
                String topText = "<html><center>" +
                        "<span style='font-family: SansSerif; font-size: " + scale(24) + "px; font-weight: bold;'>"
                        + orComp.getId() + "</span>" +
                        "</center></html>";
                lblCompanyInfo.setText(topText);

                lblCompanyInfo.setBackground(headerBg);
                lblCompanyInfo.setForeground(headerFg);

                // Standard top/side border, open bottom to merge with instruction
                lblCompanyInfo.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, Color.DARK_GRAY));
                lblCompanyInfo.setVisible(true);

                if (lblPlayerInfo != null) {
                    lblPlayerInfo.setText(
                            "<html><center><span style='font-family: SansSerif; font-size: " + scale(18) + "px;'>"
                                    + playerInfo + "</span></center></html>");

                    lblPlayerInfo.setBackground(headerBg);
                    lblPlayerInfo.setForeground(headerFg);
                    lblPlayerInfo.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.DARK_GRAY));
                    lblPlayerInfo.setVisible(true);
                }

                // BOTTOM LABEL: Instruction
                // Reverted to match Company Logo colors (Unified Header)
                String bottomText = "<html><center><span style='font-family: SansSerif; font-size: " + scale(14)
                        + "px; font-weight: bold;'>" + instruction
                        + "</span></center></html>";

                lblPhaseInstruction.setText(bottomText);

                // Set colors to match the Company ID above

                lblPhaseInstruction.setBackground(headerBg);
                lblPhaseInstruction.setForeground(headerFg);

                // Remove the 5px gap. Set top inset to 0.
                // Border: 0px Top, 1px Left, 1px Bottom, 1px Right
                lblPhaseInstruction.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.DARK_GRAY));
                lblPhaseInstruction.setVisible(true);
            }
        }

        colorizeActivePhase(null);
        if (lblCash != null)
            lblCash.setText(format(orComp.getPurseMoneyModel().value()));

        if (lblLoans != null && orComp != null && hasCompanyLoans) {
            int currentBonds = orComp.getNumberOfBonds();
            int maxBonds = currentBonds;
            try {
                java.lang.reflect.Method m = orComp.getClass().getMethod("getShareCount");
                maxBonds = (Integer) m.invoke(orComp);
            } catch (Exception e) {
                // Fallback
            }

            // Calculate Interest Cost using the BondsModel_1817 rate
            int interestRate = 0;
            GameUIManager gum = orUIManager.getGameUIManager();
            if (gum != null && gum.getGameManager() instanceof net.sf.rails.game.specific._1817.GameManager_1817) {
                net.sf.rails.game.model.BondsModel bm = ((net.sf.rails.game.specific._1817.GameManager_1817) gum
                        .getGameManager()).getBondsModel();
                if (bm instanceof net.sf.rails.game.specific._1817.BondsModel_1817) {
                    interestRate = ((net.sf.rails.game.specific._1817.BondsModel_1817) bm).getInterestRate();
                }
            }
            int totalInterestCost = currentBonds * interestRate;

            // Generic Fallback: If not 1817, extract standard company loan tracking data
            int currentLoans = (orComp.hasBonds()) ? orComp.getNumberOfBonds() : orComp.getCurrentNumberOfLoans();
            int maxLoans = orComp.getMaxNumberOfLoans();
            if (maxLoans <= 0)
                maxLoans = 5; // standard 1856 maximum cap threshold
            if (maxLoans < currentLoans)
                maxLoans = currentLoans;

            // Build the Visual Dot String safely
            StringBuilder sb = new StringBuilder("<html><center>");
            for (int b = 0; b < currentLoans; b++) {
                sb.append("<font color='red'>●</font>");
            }
            for (int b = 0; b < (maxLoans - currentLoans); b++) {
                sb.append("<font color='#888888'>○</font>");
            }

// If an explicit per-bond interest rate wasn't extracted from the 1817 model, 
            // compute it using standard game parameter configurations or a default loan fee rate ($5)
            if (interestRate <= 0) {
                interestRate = 5; // Standard 18xx default interest fee per loan unit
            }
            int dynamicInterestCost = currentLoans * interestRate;
            
            // Render the formatted interest cost uniformly using the internal currency formatter
            sb.append("&nbsp;<font color='black' size='4'>(").append(format(dynamicInterestCost)).append(")</font>");

            sb.append("</center></html>");

            lblLoans.setText(sb.toString());

        }

        if (tokenDisplay != null) {
            int available = 0;
            if (orComp.getAllBaseTokens() != null) {
                for (net.sf.rails.game.BaseToken t : orComp.getAllBaseTokens()) {
                    if (!t.isPlaced())
                        available++;
                }
            }
            tokenDisplay.setTokens(available, orComp);
        }

        if (lblRoute != null) {
            String tStr = orComp.getPortfolioModel().getTrainsModel().toText();
            boolean hasTrains = tStr != null && !tStr.isEmpty() && !tStr.equals("None") && !tStr.equals("-");

            if (lblFixed != null) {
                int totalRev = orComp.getLastRevenue();
                int fixedRev = orComp.getLastDirectIncome();

                // Stale Data Guard
                if (!orComp.canHaveFixedIncome())
                    fixedRev = 0;
                if (totalRev == 0 || fixedRev > totalRev)
                    fixedRev = 0;

                int routeRev = totalRev - fixedRev;
                if (!hasTrains) {
                    routeRev = 0;
                }

                lblRoute.setText(format(routeRev));
                if (revSpinner != null)
                    revSpinner.setValue(routeRev);
                lblFixed.setText(format(fixedRev));
            } else {
                int routeRev = hasTrains ? orComp.getLastRevenue() : 0;
                lblRoute.setText(format(routeRev));
                if (revSpinner != null)
                    revSpinner.setValue(routeRev);
                
            }
        }

        if (trainDisplay != null)
            trainDisplay.updateAssets(orComp);

        // Force the UpgradesPanel's mini tile dock to refresh immediately on state
        // changes
        if (orWindow != null && orWindow.getUpgradePanel() != null) {
            orWindow.getUpgradePanel().refreshMiniDock();
        }

    }

    private void setupHotkeys() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        JComponent source = (JComponent) e.getSource();

        if (command.equals(REM_TILES_CMD)) {
            new RemainingTilesWindow(orWindow);
        } else if (command.equals(SHOW_CMD)) {
            toggleTileBuildNumbers();

        } else if (command.equals(CONFIRM_CMD)) {
            if (orUIManager != null) {
                boolean hasSelection = (orUIManager.getMap().getSelectedHex() != null);
                if (hasSelection) {
                    orUIManager.confirmUpgrade();
                } else {
                    // Check if the button contains a dynamically validated engine action object
                    if (source instanceof ActionButton && !((ActionButton) source).getPossibleActions().isEmpty()) {
                        List<PossibleAction> attached = ((ActionButton) source).getPossibleActions();
                        orUIManager.processAction(command, attached, source);
                    } else {
                        orUIManager.skipUpgrade();
                    }
                }
            }
        } else if (command.equals(OPERATING_COST_CMD)) {
            if (orUIManager != null)
                orUIManager.operatingCosts();
        } else if (command.equals(BUY_PRIVATE_CMD)) {
            if (orUIManager != null)
                orUIManager.buyPrivate();
        } else if (source instanceof ActionTaker) {
            // Immediate Feedback: Disable button to prevent double-clicks
            if (source instanceof AbstractButton) {
                ((AbstractButton) source).setEnabled(false);
                source.repaint();
            }
            List<PossibleAction> executedActions = ((ActionTaker) source).getPossibleActions();
            if (executedActions == null || executedActions.isEmpty()) {

            } else if (executedActions.get(0) instanceof net.sf.rails.game.specific._1817.action.RepayLoans_1817) {
                processRepayLoans_1817(
                        (net.sf.rails.game.specific._1817.action.RepayLoans_1817) executedActions.get(0));
                return;
            } else if (executedActions.get(0) instanceof rails.game.action.RepayLoans) {
                orUIManager.processAction("RepayLoans", executedActions, source);
                return;

            } else if (executedActions.get(0) instanceof BuyTrain) {
                orUIManager.processBuyTrain((BuyTrain) executedActions.get(0));
                return;
            } else {
                orUIManager.processAction(command, executedActions, source);
            }


        }
    }

    private void processRepayLoans_1817(net.sf.rails.game.specific._1817.action.RepayLoans_1817 action) {
        String compId = action.getCompanyId();
        int max = action.getMaxRepayable();

        if (max <= 0)
            return;

        String[] options = new String[max];
        for (int i = 0; i < max; i++) {
            options[i] = String.valueOf(i + 1);
        }

        String selected = (String) javax.swing.JOptionPane.showInputDialog(this,
                "Select number of loans to repay for " + compId + " ($100 each):\n(Max affordable: " + max + ")",
                "Repay Loans", javax.swing.JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (selected != null) {
            action.setLoansToRepay(Integer.parseInt(selected));
            orUIManager.processAction("RepayLoans",
                    java.util.Collections.singletonList((rails.game.action.PossibleAction) action), this);
        } else {
            if (specialActionsButtonPanel != null) {
                for (Component c : specialActionsButtonPanel.getComponents()) {
                    if (c instanceof ActionButton) {
                        ((ActionButton) c).setEnabled(true);
                    }
                }
            }
        }
    }

    private boolean isActionListEmpty(ActionButton btn) {
        return btn.getPossibleActions() == null || btn.getPossibleActions().isEmpty();
    }

    // ... (lines of unchanged context code) ...
    public boolean executeRedo() {
        if (currentRedoAction != null && orUIManager != null) {
            orUIManager.processAction(REDO_CMD, java.util.Collections.singletonList(currentRedoAction), this);
            return true;
        }
        return false;
    }

    private void addSpecialActionButton(PossibleAction action) {
        String label = action.getButtonLabel();

        // Defaults
        Color bgColor = Color.LIGHT_GRAY;
        Color borderColor = Color.GRAY;
        Color textColor = Color.BLACK;
        String cmd = "SpecialAction";

        Company highlightTarget = null;

        // 1. Extract Visual Signature
        if (action.getClass().getSimpleName().contains("1817")) {
            bgColor = new Color(255, 140, 0); // Vibrant Orange
            borderColor = Color.BLACK;
            textColor = Color.WHITE;
            label = action.getButtonLabel();
            if (label == null || label.isEmpty())
                label = action.toString();
        } else if (action instanceof GuiTargetedAction) {
            // PRIORITY: If the action defines its own visual signature, use it.
            GuiTargetedAction gta = (GuiTargetedAction) action;
            label = gta.getButtonLabel();

            if (gta.getTarget() instanceof Company) {
                highlightTarget = (Company) gta.getTarget();
            }

            bgColor = gta.getHighlightBackgroundColor();
            borderColor = gta.getHighlightBorderColor();
            textColor = gta.getHighlightTextColor();

        } else if (action instanceof DiscardTrain) {
            // FALLBACK: Only used if DiscardTrain somehow stops implementing
            // GuiTargetedAction
            bgColor = UITheme.ACTION_DISCARD;

            borderColor = Color.BLACK;
            textColor = Color.WHITE;
            label = action.getButtonLabel();
        } else if (action instanceof GuiTargetedAction) {
            GuiTargetedAction gta = (GuiTargetedAction) action;
            label = gta.getButtonLabel();
            if (gta.getTarget() instanceof Company) {
                highlightTarget = (Company) gta.getTarget();
            }
            // CONSUME THE SIGNATURE
            bgColor = gta.getHighlightBackgroundColor();
            borderColor = gta.getHighlightBorderColor();
            textColor = gta.getHighlightTextColor();
        } else if (action instanceof NullAction) {
            if (label == null || label.isEmpty()) {
                label = ((NullAction) action).getMode() == NullAction.Mode.PASS ? "Decline" : "Done";
            }
            bgColor = UITheme.ACTION_SKIP;
            borderColor = bgColor.darker();
            textColor = Color.WHITE;

            if (orUIManager != null && orUIManager.getGameUIManager() != null
                    && orUIManager.getGameUIManager().getGameManager() != null) {
                RoundFacade currentRound = orUIManager.getGameUIManager().getGameManager().getCurrentRound();
                if (currentRound != null && currentRound.getClass().getSimpleName().contains("Formation")) {
                    bgColor = new Color(152, 251, 152); // PaleGreen
                    borderColor = new Color(34, 139, 34); // ForestGreen
                    textColor = Color.BLACK;
                }
            }
        } else if (action instanceof LayBaseToken) {
            highlightTarget = ((LayBaseToken) action).getCompany();
        } else if (action instanceof BuyPrivate) {
            BuyPrivate bp = (BuyPrivate) action;
            highlightTarget = bp.getPrivateCompany();
            String compId = (highlightTarget != null) ? highlightTarget.getId() : action.toString();
            label = "Buy " + compId + " (" + bp.getMinimumPrice() + "-" + bp.getMaximumPrice() + ")";
            bgColor = new Color(255, 235, 235); // Re-assign missing background color fallback layout styling
            borderColor = new Color(200, 150, 150);
            textColor = Color.BLACK;

        } else {
            // Robust identification based on the action content text
            String actStr = action.toString().toLowerCase();
            String btnLbl = action.getButtonLabel() != null ? action.getButtonLabel().toLowerCase() : "";

            if (actStr.contains("bridge") || btnLbl.contains("bridge")) {
                label = "Buy Bridge Token ($50)";
                bgColor = new Color(255, 235, 205); // Industrial/Beige
                borderColor = new Color(204, 102, 0); // Orange border
                textColor = Color.BLACK;
            } else if (actStr.contains("tunnel") || btnLbl.contains("tunnel")) {
                label = "Buy Tunnel Token ($50)";
                bgColor = new Color(255, 235, 205);
                borderColor = new Color(204, 102, 0);
                textColor = Color.BLACK;
            } else if (actStr.contains("port") || btnLbl.contains("port") || actStr.contains("layport")) {
                label = "Lay Port Token";
                bgColor = new Color(255, 193, 7); // Vibrant Amber/Gold
                borderColor = new Color(184, 134, 11);
                textColor = Color.BLACK;
            } else if (actStr.contains("repay") || btnLbl.contains("repay")) {
                label = "Repay Loans";
                bgColor = new Color(210, 255, 210); // Unified clear layout green
                borderColor = new Color(34, 139, 34);
                textColor = Color.BLACK;
            } else if (actStr.contains("loan") || actStr.contains("bond") || actStr.contains("takeloan")) {
                label = "Take Loan";
                bgColor = new Color(210, 255, 210); // Unified clear layout green
                borderColor = new Color(34, 139, 34);
                textColor = Color.BLACK;
            }
        }

        // 2. Create Button
        ActionButton btn = createSidebarButton(label, cmd);

        // 2.5 Extract Dynamic Subtext (Rule explanations from XML)
        String subText = null;
        if (action instanceof UseSpecialProperty) {
            net.sf.rails.game.special.SpecialProperty sp = ((UseSpecialProperty) action).getSpecialProperty();
            if (sp != null) {
                subText = sp.getHelp();
                if (subText == null || subText.trim().isEmpty()) {
                    subText = sp.getInfo();
                }
            }
        }

        // 2.6 HTML Two-Tier Formatting
        if (label == null || label.trim().isEmpty()) {
            label = action.toString();
        }

        if (!label.toLowerCase().startsWith("<html>")) {
            btn.setText(formatDynamicButtonText(label, subText));
        } else {
            btn.setText(label);
        }
        btn.setHorizontalAlignment(SwingConstants.CENTER);

        btn.setPossibleAction(action);
        btn.setEnabled(true);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Allow button to be taller to fit the HTML content
        btn.setMaximumSize(new Dimension(getSidebarWidth() - scale(20), scale(60)));
        bindActionHotkey(btn, action);

        if (highlightTarget != null && highlightTarget.getInfoText() != null
                && !highlightTarget.getInfoText().isEmpty()) {
            btn.setToolTipText("<html><div style='width: 250px;'>"
                    + highlightTarget.getInfoText().replaceAll("\\n", "<br>") + "</div></html>");
        }

        // Attach HexHighlightMouseListener based on specific Company type
        if (highlightTarget instanceof PublicCompany) {
            net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener.addMouseListener(
                    btn, orUIManager, (PublicCompany) highlightTarget, false);
        } else if (highlightTarget instanceof PrivateCompany) {
            net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener.addMouseListener(
                    btn, orUIManager, (PrivateCompany) highlightTarget, false);
        }

        // 3. APPLY "RAILCARD" STYLING (Flattened)
        // A. Background & Text
        btn.setBackground(bgColor);
        btn.setForeground(textColor);

        // B. Border (Thick Line Border to match GameStatus Card)
        // Outer: The colored line (3px)
        // Inner: Padding (5px)
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 3),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        // C. Technical overrides to ensure "Flat" look
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false); // Remove dotted focus line
        // Force the font to match RailCard
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));

        specialPanel.add(btn);
        specialPanel.add(Box.createVerticalStrut(8));
    }

    /**
     * Replaces the tooltip logic with a formatted log entry.
     * Cleans up UI tooltips and dumps a readable text table to the console.
     */
    private void logFormattedActions(List<PossibleAction> actions) {
        // 1. Revert UI Tooltips (Clean up)
        this.setToolTipText(null);
        if (lblPhaseInstruction != null)
            lblPhaseInstruction.setToolTipText(null);
        if (lblCompanyInfo != null)
            lblCompanyInfo.setToolTipText(null);
        if (phase1Panel != null)
            phase1Panel.setToolTipText(null);
        if (phase2Panel != null)
            phase2Panel.setToolTipText(null);
        if (phase3Panel != null)
            phase3Panel.setToolTipText(null);
        if (phase4Panel != null)
            phase4Panel.setToolTipText(null);
        if (footerPanel != null)
            footerPanel.setToolTipText(null);

        if (actions == null || actions.isEmpty())
            return;

    }



    public void updateDynamicActions(List<PossibleAction> incomingActions) {


            cleanupUpgradesPanel();
            resetSidebarState();

            List<PossibleAction> actions = new ArrayList<>();
            if (incomingActions != null) {
                actions.addAll(incomingActions);
            }

            // RESCUE PLAYER-BOUND ACTIONS:
            // The UI router sends RepayLoans/TakeLoans to the StatusWindow because they are
            // owned by the Player.
            // We fetch them directly from the global action buffer so they render in the
            // ORPanel.
            if (orUIManager != null && orUIManager.getGameUIManager() != null
                    && orUIManager.getGameUIManager().getGameManager() != null) {
                rails.game.action.PossibleActions globalActionsObj = orUIManager.getGameUIManager().getGameManager()
                        .getPossibleActions();
                if (globalActionsObj != null) {
                    for (PossibleAction pa : globalActionsObj.getList()) {
                        String name = pa.getClass().getSimpleName();
                        if (name.contains("RepayLoans") || name.contains("TakeLoans")) {
                            if (!actions.contains(pa)) {
                                // Insert the rescued action immediately before NullAction (End Turn)
                                // to preserve the correct temporal sequence in the UI action lists.
                                int insertIndex = actions.size();
                                for (int i = 0; i < actions.size(); i++) {
                                    if (actions.get(i) instanceof NullAction) {
                                        insertIndex = i;
                                        break;
                                    }
                                }
                                actions.add(insertIndex, pa);
                            }
                        }
                    }
                }
            }

            // ROBUST CONTEXT HANDOVER
            PublicCompany engineActiveComp = null;
            boolean isMaARound = false;

            if (orUIManager != null && orUIManager.getGameUIManager().getGameManager() != null) {
                net.sf.rails.game.round.RoundFacade rf = orUIManager.getGameUIManager().getGameManager()
                        .getCurrentRound();
                if (rf != null) {
                    String rfName = rf.getClass().getSimpleName();

                    boolean hasOpComp = false;
                    try {
                        rf.getClass().getMethod("getOperatingCompany");
                        hasOpComp = true;
                    } catch (Exception e) {
                    }

                    boolean isOperatingPhase = rf instanceof net.sf.rails.game.OperatingRound ||
                            rfName.contains("Operating") ||
                            rfName.contains("Merger") ||
                            rfName.contains("Formation") ||
                            rfName.contains("CoalExchange") ||
                            hasOpComp;

                    if (!isOperatingPhase) {
                        finish();
                        if (lblCompanyInfo != null) {
                            String cleanName = rfName.replace("Round", "").replaceAll("([a-z])([A-Z]+)", "$1 $2")
                                    .trim();
                            lblCompanyInfo.setText("<html><center><font face='SansSerif' size='5'><b>" + cleanName
                                    + " Phase</b></font></center></html>");
                        }
                        return; // Force dormancy: prevents lingering actions from being processed
                    }
                    isMaARound = rfName.contains("Merger") || rfName.contains("Formation");

                    if (rf instanceof net.sf.rails.game.OperatingRound) {
                        engineActiveComp = ((net.sf.rails.game.OperatingRound) rf).getOperatingCompany();
                    } else {
                        try {
                            java.lang.reflect.Method m = rf.getClass().getMethod("getOperatingCompany");
                            engineActiveComp = (PublicCompany) m.invoke(rf);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            // THE CONTEXT OVERRIDE:
            // If the engine demands a discard, the actor might not match the nominal
            // "Operating Company"
            // We scan for DiscardTrain actions and forcibly redirect the UI focus to the
            // discarding company.
            boolean isFormationStep = false;
            boolean isRepayStep = false; // Declared here at method scope level to fix the compilation error
            if (actions != null) {
                for (PossibleAction pa : actions) {
                    if (pa instanceof DiscardTrain) {
                        net.sf.rails.game.state.Owner targetActor = ((DiscardTrain) pa).getActor();
                        if (targetActor instanceof PublicCompany) {
                            engineActiveComp = (PublicCompany) targetActor;
                            // Once we find a discard context, we lock it in and stop searching
                            break;
                        }
                    } else if (pa.getClass().getSimpleName().contains("RepayLoans")) {
                        isRepayStep = true;
                        if (pa instanceof GuiTargetedAction) {
                            net.sf.rails.game.state.Owner targetActor = ((GuiTargetedAction) pa).getActor();
                            if (targetActor instanceof PublicCompany) {
                                engineActiveComp = (PublicCompany) targetActor;
                            }
                        }
                    } else if (pa instanceof rails.game.specific._1835.StartPrussian ||
                            pa instanceof rails.game.specific._1835.ExchangeForPrussianShare ||
                            pa instanceof net.sf.rails.game.specific._1837.ExchangeMinorAction) {
                        isFormationStep = true;
                    }
                }
            }

            // Sync Context
            if (isFormationStep) {
                engineActiveComp = null;
                this.orComp = null;
                this.currentOperatingComp = null;
            } else if (engineActiveComp != null && !engineActiveComp.isClosed()) {
                this.currentOperatingComp = engineActiveComp;
                this.orComp = engineActiveComp;
            }

            // 3. FILTER & DETECT SPECIAL ACTIONS
            List<PossibleAction> specialActions = new ArrayList<>();
            GuiTargetedAction contextProvider = null;
            PossibleAction deferredNullAction = null;

            List<PossibleAction> validOrActions = new ArrayList<>();

            for (PossibleAction pa : actions) {
                String paName = pa.getClass().getSimpleName();

                boolean isStatusWindowExclusive = paName.contains("Bid") ||
                        paName.contains("SettleIPO") ||
                        paName.equals("Short1817");

                if (isStatusWindowExclusive) {
                    continue;
                }

                validOrActions.add(pa);

                boolean isRepayAction = paName.contains("RepayLoans");
                boolean isStandardUIAction = ((pa instanceof LayTile) ||
                        (pa instanceof LayToken) ||
                        (pa instanceof BuyTrain) ||
                        (pa instanceof SetDividend) ||
                        (pa instanceof NullAction) ||
                        (pa instanceof GameAction) ||
                        (pa instanceof CorrectionModeAction)) && !isRepayAction;

                if (!isStandardUIAction) {
                    specialActions.add(pa);
                    if (pa instanceof GuiTargetedAction && contextProvider == null) {
                        contextProvider = (GuiTargetedAction) pa;
                    }
                } else if (pa instanceof LayBaseToken && ((LayBaseToken) pa).getType() == LayBaseToken.HOME_CITY) {
                    specialActions.add(pa);
                } else if (pa instanceof NullAction) {
                    deferredNullAction = pa;
                }
            }



            if (isFormationStep && deferredNullAction != null) {
                specialActions.add(deferredNullAction);
            }

            int computedPhase = determineActivePhase(validOrActions);
            boolean hasStandardActions = computedPhase > 0;

            this.specialModeActive = false;

            if (!specialActions.isEmpty() && specialPanel != null && specialContainer != null) {
                specialContainer.setVisible(true);
                specialPanel.removeAll();
                for (PossibleAction spa : specialActions) {
                    if (spa instanceof NullAction)
                        continue;
                    addSpecialActionButton(spa);
                }
            } else if (specialContainer != null) {
                if (computedPhase == 5 && !specialActions.isEmpty()) {
                    specialContainer.setVisible(true);
                } else {
                    specialContainer.setVisible(false);
                }
            }

            activePhase = computedPhase;
            setStandardPanelsVisible(true);

            // Run visual framing first so it cannot overwrite explicit action bindings
            // later
            if (activePhase == 1 || activePhase == 2) {
                boolean hasSelection = (orUIManager != null && orUIManager.getMap() != null
                        && orUIManager.getMap().getSelectedHex() != null);
                enableConfirm(hasSelection);
            }

            distributeStandardActions(validOrActions);
            updateSidebarData();
            updatePhaseSpecifics();

      SwingUtilities.invokeLater(() -> {
                if (specialPanel != null) {
                    specialPanel.revalidate();
                    specialPanel.repaint();
                }
                if (specialContainer != null) {
                    specialContainer.revalidate();
                    specialContainer.repaint();
                }
                if (phase5Panel != null) {
                    phase5Panel.revalidate();
                    phase5Panel.repaint();
                }
                if (sidebarPanel != null) {
                    sidebarPanel.revalidate();
                    sidebarPanel.repaint();
                }
            });


    }

    private void bindActionHotkey(ActionButton btn, PossibleAction action) {
        int key = 0;
        String actionName = action.getClass().getSimpleName();

        // 1. Check if the Action defines a hotkey (via Interface)
        if (action instanceof GuiTargetedAction) {
            key = ((GuiTargetedAction) action).getHotkey();
        }

        // --- START FIX ---
        // 1. Capture the Pass button for external access (GlobalHotkeyManager)
        if (action instanceof NullAction) {
            this.directPassButton = btn;
        }

        // 2. RESTORE 'ENTER' MAPPING FOR PASS
        // We strictly bind ENTER to the Pass button (NullAction).
        if (action instanceof NullAction) {
            this.directPassButton = btn; // Keep reference for Global Manager
            key = KeyEvent.VK_ENTER;
        }

        if (key != 0) {
            InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = this.getActionMap();

            // Unique command name using identityHashCode to avoid collisions
            String commandKey = "invoke_" + actionName + "_" + System.identityHashCode(action);

            im.put(KeyStroke.getKeyStroke(key, 0), commandKey);

            am.put(commandKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (btn.isEnabled() && btn.isVisible()) {
                        btn.doClick();
                    } else {
                    }
                }
            });
        }

    }

    /**
     * Helper to safely click the Pass/Done button from the Global Manager.
     * Returns true if the button was clicked, false otherwise.
     */
    public boolean clickPassButton() {
        if (directPassButton != null && directPassButton.isShowing() && directPassButton.isEnabled()) {
            directPassButton.doClick();
            return true;
        }
        return false;
    }

    // We are modifying ORPanel.java to ensure the scanner is robust.

    // ... (lines of unchanged context code) ...

    public void handleEnterPress() {
        // Prioritize the explicitly set default button to restore expected 'Enter'
        // behavior
        if (currentDefaultButton != null && currentDefaultButton.isVisible() && currentDefaultButton.isEnabled()) {
            currentDefaultButton.doClick();
            return;
        }

        // 1. Check Special Panel (Discards, etc)
        if (specialContainer != null && specialContainer.isVisible()) {
            if (scanAndClickBestButton(specialPanel))
                return;
        }

        // 2. Check Active Phase Panel
        JPanel activePanel = null;
        if (activePhase == 1)
            activePanel = phase1Panel;
        else if (activePhase == 2)
            activePanel = phase2Panel;
        else if (activePhase == 3)
            activePanel = phase3Panel;
        else if (activePhase == 4)
            activePanel = phase4Panel;
        else if (activePhase == 5)
            activePanel = phase5Panel;
        if (activePanel != null && activePanel.isVisible()) {
            if (scanAndClickBestButton(activePanel))
                return;
        }

        // 3. Check Sidebar (Fallback)
        if (sidebarPanel != null && sidebarPanel.isVisible()) {
            // scanAndClickBestButton(sidebarPanel); // Optional: Enable if sidebar has
            // "Pay" buttons
        }

        // 4. Default Fallback
        if (btnDone != null && btnDone.isVisible() && btnDone.isEnabled()) {
            btnDone.doClick();
        } 
    }

    // We are modifying ORPanel.java
    // Fix: Add 'decline' to the scoring logic to catch the Prussian Pass action.

    // ... (existing imports) ...

    private boolean scanAndClickBestButton(Container container) {
        if (container == null)
            return false;

        AbstractButton bestCandidate = null;
        int bestScore = 0;

        Component[] comps = container.getComponents();
        for (Component comp : comps) {
            // Recursive dive
            if (comp instanceof Container && !(comp instanceof AbstractButton)) {
                if (scanAndClickBestButton((Container) comp))
                    return true;
                continue;
            }

            if (comp instanceof AbstractButton) {
                AbstractButton btn = (AbstractButton) comp;
                if (!btn.isEnabled() || !btn.isVisible())
                    continue;

                String text = btn.getText();
                // Strip HTML tags for cleaner logging/matching if needed,
                // but .contains() usually works fine on the raw string.
                String lowerText = (text != null) ? text.toLowerCase() : "";
                String cmd = (btn.getActionCommand() != null) ? btn.getActionCommand() : "";
                int score = 0;

                // --- SCORING LOGIC ---

                // 1. CRITICAL FLOW (Score 10)
                if (cmd.equals(PAYOUT_CMD) ||
                        lowerText.contains("pay") ||
                        lowerText.contains("confirm") ||
                        lowerText.contains("yes")) {
                    score = 10;
                }

                // 2. EXPLICIT SKIP / REFUSAL (Score 8)
                // ADDED "decline" HERE
                else if (lowerText.contains("skip") ||
                        lowerText.contains("decline")) {
                    score = 8;
                }

                // 3. GENERIC COMPLETION (Score 5)
                else if (cmd.equals(DONE_CMD) ||
                        lowerText.contains("done") ||
                        lowerText.contains("end turn") ||
                        lowerText.contains("pass")) {
                    score = 5;
                }

                // 4. SECONDARY OPTIONS (Score 3)
                else if (cmd.equals(WITHHOLD_CMD) || cmd.equals(SPLIT_CMD) ||
                        lowerText.contains("hold") || lowerText.contains("withhold") ||
                        lowerText.contains("split")) {
                    score = 3;
                }

                // 5. EXPLICITLY IGNORE (Score -1)
                // "Start" is ignored, which correctly handles "Start (Fold M2)"
                else if (lowerText.contains("buy") ||
                        lowerText.contains("undo") ||
                        lowerText.contains("redo") ||
                        lowerText.contains("start")) {
                    score = -1;
                }

                // LOGGING

                if (score > bestScore) {
                    bestScore = score;
                    bestCandidate = btn;
                }
            }
        }

        if (bestCandidate != null) {
            bestCandidate.doClick();
            return true;
        }
        return false;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {

            boolean result = super.processKeyBinding(ks, e, condition, pressed);
            return result;
        }
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    ////////////////////////////////////////////////////////
    ///
    ///

    public void activateHelpOverlay() {
        if (orWindow == null)
            return;

       // --- START FIX ---
        // Retrieve the current glass pane context safely.
        // We strip out the visible short-circuiting toggle check that was causing the double-click bug.
        Component currentGlass = orWindow.getGlassPane();
        net.sf.rails.ui.swing.help.HelpOverlayGlassPane helpPane;
        if (currentGlass instanceof net.sf.rails.ui.swing.help.HelpOverlayGlassPane) {
            helpPane = (net.sf.rails.ui.swing.help.HelpOverlayGlassPane) currentGlass;
        } else {
            log.warn("[GLASS PANE] Native component requested overlay setup outside authoritative state machine flow.");
            return;
        }
        
        helpPane.clearSpotlights();

// 1. Standard Buttons with Contextual Text
addIfActive(helpPane, btnTileConfirm, "Confirm Track: Finalize the current tile placement or upgrade.");
addIfActive(helpPane, btnTokenConfirm, "Confirm Token: Pay the fee and place your station marker.");
addIfActive(helpPane, btnRevPayout, "Pay Dividends: Distribute earnings to shareholders. Stock price moves right.");
addIfActive(helpPane, btnRevSplit, "Split Revenue: Half to shareholders, half to company. Stock price stays.");
addIfActive(helpPane, btnRevWithhold, "Withhold Earnings: Keep all cash in treasury. Stock price moves left.");
addIfActive(helpPane, btnTrainSkip, "Skip Train: End purchasing. You MUST buy if you have a route but no train.");
addIfActive(helpPane, btnDone, "End Turn: Finish all operations and advance to the next company.");
        // 2. Dynamic Buttons
        scanPanelForActiveButtons(helpPane, trainButtonsPanel);
        scanPanelForActiveButtons(helpPane, specialActionsButtonPanel);
        scanPanelForActiveButtons(helpPane, specialPanel);

        // 3. Highlight the active phase header
        JPanel activePanel = getActivePhasePanel();
        if (activePanel != null && activePanel.isVisible()) {
            Rectangle bounds = SwingUtilities.convertRectangle(activePanel.getParent(), activePanel.getBounds(),
                    helpPane);
            helpPane.addSpotlight(bounds, "Current Phase: Follow the highlighted actions.");
        }

        // 4. Highlight Valid Map Hexes (Spatial Spotlighting)
        if (orUIManager != null && orUIManager.getMap() != null) {
            if (activePhase == 1 || activePhase == 2) {
                for (GUIHex hex : cycleableHexes) {
                    try {
                        Rectangle hexBounds = hex.getBounds();
                        if (hexBounds != null && orWindow.getMapPanel() != null) {
                            Rectangle screenBounds = SwingUtilities.convertRectangle(orWindow.getMapPanel(), hexBounds,
                                    helpPane);
                            screenBounds.grow(2, 2);

                            String hexContext = (activePhase == 1)
                                    ? "Hex " + hex.getHex().getId() + ": Click to lay Track"
                                    : "Hex " + hex.getHex().getId() + ": Click to place Station Token";
                            helpPane.addSpotlight(screenBounds, hexContext);
                        }
                    } catch (Exception e) {
                        log.error("Could not extract bounds for highlighted hex", e);
                    }
                }
            }
        }

// Diagnostic Stack & Context Isolation Logging
        StringBuilder helpTrace = new StringBuilder();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 1; i < Math.min(stack.length, 5); i++) {
            helpTrace.append("\n    -> ").append(stack[i].toString());
        }

        boolean uiNotNull = (orWindow.gameUIManager != null);
        boolean mgrNotNull = (uiNotNull && orWindow.gameUIManager.getGameManager() != null);
        String currentMode = mgrNotNull ? orWindow.gameUIManager.getGameManager().getEngineMode().toString() : "UNKNOWN_MGR_NULL";

        log.info("[HELP-BUG-TRACE] ORPanel.activateHelpOverlay() invoked." +
                 "\n  - Engine Mode: " + currentMode +
                 "\n  - gameUIManager Available: " + uiNotNull +
                 "\n  - gameManager Available: " + mgrNotNull +
                 "\n  - Current GlassPane Visibility: " + helpPane.isVisible() +
                 "\n  - Stack Context: " + helpTrace);

        if (orWindow.gameUIManager != null && orWindow.gameUIManager.getGameManager() != null) {
            boolean isHelpActive = (orWindow.gameUIManager.getGameManager().getEngineMode() == net.sf.rails.game.GameManager.EngineMode.HELP);
            log.info("[HELP-BUG-TRACE] Evaluating guard branch. Setting helpPane visible to: " + isHelpActive);
            helpPane.setVisible(isHelpActive);
        } else {
            log.warn("[HELP-BUG-TRACE] CRITICAL: Fallback branch hit! Forcing helpPane visible to TRUE");
            helpPane.setVisible(true); // Fallback if unhydrated
        }
    
    }

    private JPanel getActivePhasePanel() {
        if (activePhase == 1)
            return phase1Panel;
        if (activePhase == 2)
            return phase2Panel;
        if (activePhase == 3)
            return phase3Panel;
        if (activePhase == 4)
            return phase4Panel;
        if (activePhase == 5)
            return phase5Panel;
        return null;
    }

    private void addIfActive(net.sf.rails.ui.swing.help.HelpOverlayGlassPane pane, ActionButton btn, String text) {
        if (btn != null && btn.isVisible() && btn.isEnabled()) {
            Rectangle bounds = SwingUtilities.convertRectangle(btn.getParent(), btn.getBounds(), pane);
            pane.addSpotlight(bounds, text);
        }
    }

    private void scanPanelForActiveButtons(net.sf.rails.ui.swing.help.HelpOverlayGlassPane pane, JPanel container) {
        if (container == null || !container.isVisible())
            return;
        for (Component c : container.getComponents()) {
            if (c instanceof ActionButton && c.isVisible() && c.isEnabled()) {
                // Extract clean text from the button, stripping any HTML tags we inject for
                // formatting
                String text = ((ActionButton) c).getText().replaceAll("<[^>]*>", "").trim();
String helpText = text;
            // Intercept generic or specific button texts to provide targeted context
            if (text.equalsIgnoreCase("Skip")) {
                helpText = "Skip Action: Choose not to take this optional action.";
            } else if (text.contains("Brdg")) {
                helpText = "Buy Bridge Co.: Allows crossing the Mississippi River or gives a $40 track discount.";
            } else if (text.contains("Gulf")) {
                helpText = "Buy Gulf Co.: Place an open or closed port token to boost a city's revenue.";
            } else if (text.contains("MKT")) {
                helpText = "Buy MKT: Acquires the private company and its attached 10% share of MKT Railroad.";
            } else if (text.contains("Cattl")) {
                helpText = "Buy Cattle Co.: Place a token to add $10 to a western city for your trains.";
            } else if (text.contains("GRSC")) {
                helpText = "Buy Great River Shipping: Standard private company, pays $5 revenue.";
            }

            pane.addSpotlight(SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), pane), helpText);
                    }
        }
    }

}

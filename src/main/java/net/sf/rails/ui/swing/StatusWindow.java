/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/StatusWindow.java,v 1.46 2010/06/15 20:16:54 evos Exp $*/
package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import rails.game.action.*;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.common.Config;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.EndOfGameRound;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.TreasuryShareRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.ui.swing.elements.ActionButton;
import net.sf.rails.ui.swing.elements.ActionCheckBoxMenuItem;
import net.sf.rails.ui.swing.elements.ActionMenuItem;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.util.GameLoader;
import rails.game.correct.CorrectionModeAction;
import rails.game.correct.CorrectionType;
import java.io.InputStream;
import java.util.Properties;
import rails.game.correct.ClosePrivate;
import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyType;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Closeable;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.PhaseManager;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import net.sf.rails.algorithms.NetworkAdapter;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.specific._1870.action.RedeemShare_1870;
import net.sf.rails.game.specific._1870.action.ReissueShares_1870;
import net.sf.rails.game.specific._1870.StockRound_1870;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * This is the Window used for displaying nearly all of the rails.game status.
 * This is also from where the ORWindow and StartRoundWindow are triggered.
 */
public class StatusWindow extends JFrame implements ActionListener, ActionPerformer {
    private static final long serialVersionUID = 1L;
    protected static final String QUIT_CMD = "Quit";
    protected static final String NEW_CMD = "New";
    protected static final String LOAD_CMD = "Load";
    protected static final String SAVE_CMD = "Save";
    protected static final String RELOAD_CMD = "Reload";
    protected static final String AUTOSAVELOAD_CMD = "AutoSaveLoad";
    protected static final String SAVESTATUS_CMD = "SaveGameStatus";
    protected static final String SAVEREPORT_CMD = "SaveReportFile";
    protected static final String EXPORT_CMD = "Export";
    protected static final String UNDO_CMD = "Undo"; //
    protected static final String FORCED_UNDO_CMD = "Undo!";
    protected static final String REDO_CMD = "Redo";
    protected static final String MAP_ZOOM_IN_CMD = "MapZoomIn";
    protected static final String MAP_ZOOM_OUT_CMD = "MapZoomOut";
    public static final String MARKET_CMD = "Market";
    public static final String AI_MOVE_CMD = "AIMove"; //
    protected static final String CORRECT_STOCK_CMD = "CorrectStock";

    protected static final String REM_TILES_CMD = "RemTiles";
    protected static final String SHOW_MAP_CMD = "ShowMap";
    protected static final String MAP_FIT_CMD = "MapFit";

    protected static final String TOGGLE_PAUSE_CMD = "TogglePause";
    protected static final String MAP_CMD = "Map";
    protected static final String REPORT_CMD = "Report";
    protected static final String CONFIG_CMD = "Config";
    protected static final String BUY_CMD = "Buy";
    protected static final String SELL_CMD = "Sell";
    protected static final String DONE_CMD = "Done";
    protected static final String PASS_CMD = "Pass";
    protected static final String AUTOPASS_CMD = "Autopass";
    protected static final String SHOW_PHASES_CMD = "ShowPhases";
    protected static final String REFRESH_NETWORK_CMD = "RefreshNetwork";

    private float currentBaseFontSize = 14f; // Default starting size
    private static final float SCALE_HEADER = 1.5f; // Relative size for "Thinking"
    private static final float SCALE_TIMER = 2.0f; // Relative size for Timer

    protected JPanel buttonPanel;
    protected GameStatus gameStatus;
    private boolean useAltStatus = false;
    private JScrollPane jspStatus;
    private boolean showPlayerWorth = false;

    public boolean isShowPlayerWorth() {
        return showPlayerWorth;
    }

    protected ActionButton passButton;
    protected ActionButton autopassButton;
    protected GameUIManager gameUIManager;
    protected RoundFacade currentRound;
    protected PossibleActions possibleActions;
    protected PossibleAction immediateAction = null;
    protected MoveMonitor moveMonitor = null;
    protected ActionRunner actionRunner = null; // --- ADDED FIELD ---
    protected JPanel pane = new JPanel(new BorderLayout());
    private JMenuBar menuBar;
    private JMenu fileMenu, optMenu, moveMenu, moderatorMenu, correctionMenu, developerMenu;
    ActionMenuItem undoItem;

    ActionMenuItem redoItem;
    protected static final String FONT_INCREASE_CMD = "FontIncrease";
    protected static final String FONT_DECREASE_CMD = "FontDecrease";
    // protected JLabel lastActionLabel;
    protected JLabel currentActorLabel;
    protected Font activityFont;
    // Added for Header Timer
    protected JLabel gameTimeLabel;
    protected javax.swing.Timer uiRefreshTimer;

    protected JPanel auctionPanel;
    protected JSpinner bidSpinner;
    protected JButton auctionBidButton;
    protected JButton auctionPassButton;

    protected String buildTimestamp = "Dev Build"; // Stores the build time
    private JScrollPane gameStatusPane; // <--- ADD THIS
    protected static final String CORRECT_SHARES_CMD = "CorrectShares";

    protected JPanel dynamicButtonPanel; // Panel for "Sell 1x", "Sell 2x", etc.
    // In StatusWindow.java (Near other static final commands, e.g., line 50)

    // In StatusWindow.java (Declaration section, near passButton/autopassButton)
    protected ActionButton aiButton;
    protected ActionButton pauseButton;

    protected ActionButton undoButton;
    protected ActionButton redoButton;

    // DESIGN LANGUAGE CONSTANTS

    // DESIGN LANGUAGE CONSTANTS
    // Primary Borders (Strong Indication)
    private static final Color SYS_BLUE = new Color(30, 144, 255); // DodgerBlue - Generic Interaction / Pass
    private static final Color SYS_CYAN = Color.CYAN; // Cyan - Special Action / Force / Scrap
    private static final Color SYS_RED = new Color(255, 69, 0); // OrangeRed - Selling / Destructive
    private static final Color SYS_GREEN = new Color(34, 139, 34); // ForestGreen - Buying / Acquisition
    private static final Color FG_WHITE = Color.WHITE;

    // Backgrounds (Pale Context Hints)
    private static final Color BG_BLUE = new Color(225, 240, 255); // Pale Blue
    private static final Color BG_RED = new Color(255, 235, 235); // Pale Red
    private static final Color BG_GREEN = new Color(230, 255, 230); // Pale Green
    private static final Color BG_BEIGE = new Color(245, 245, 220); // Beige - Special / Scrap

    // private String lastCompanySignature = null;

    // Helper to enforce Design Language on buttons
    private void styleStatusButton(ActionButton btn, Color bg) {
        if (btn == null)
            return;
        btn.setBackground(bg);
        btn.setForeground(FG_WHITE);
        btn.setOpaque(true);
        btn.setFont(new Font("SansSerif", Font.BOLD, (int) currentBaseFontSize));
        btn.setBorder(BorderFactory.createRaisedBevelBorder()); // Match ORPanel 3D look
    }

    // // New Helper for "Passive/Yield" buttons (gray + Black Text)
    // private void stylePassiveButton(ActionButton btn) {
    // if (btn == null)
    // return;
    // btn.setBackground(Color.LIGHT_GRAY);
    // btn.setForeground(Color.BLACK); // Black text for readability on gray
    // btn.setOpaque(true);
    // btn.setFont(new Font("SansSerif", Font.BOLD, 12));
    // btn.setBorder(BorderFactory.createRaisedBevelBorder());
    // }

    // // Helper to reset button to passive state
    // private void resetStatusButton(ActionButton btn) {
    // if (btn == null)
    // return;
    // btn.setBackground(UIManager.getColor("Button.background"));
    // btn.setForeground(Color.BLACK);
    // btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
    // btn.setBorder(UIManager.getBorder("Button.border"));
    // }

    // private void checkStructureChange() {
    // if (gameUIManager == null || gameUIManager.getGameManager() == null)
    // return;

    // // 1. Build a "Signature" of the current public companies
    // // Access CompanyManager via getRoot()
    // String currentSignature = "";
    // try {
    // List<PublicCompany> comps = gameUIManager.getGameManager()
    // .getRoot() // <--- ADDED THIS
    // .getCompanyManager()
    // .getAllPublicCompanies();

    // currentSignature = comps.stream()
    // .filter(c -> !c.isClosed())
    // .map(c -> c.getId())
    // .collect(Collectors.joining(","));
    // } catch (Exception e) {
    // // Safety fallback if Root or CompanyManager isn't ready
    // return;
    // }

    // // 2. Initialize on first run (Startup)
    // // We assume 'init()' has already set up the grid correctly for the start
    // state.
    // if (lastCompanySignature == null) {
    // lastCompanySignature = currentSignature;
    // return;
    // }

    // // 3. Compare and Recreate if needed
    // if (!currentSignature.equals(lastCompanySignature)) {
    // log.info("StatusWindow: Structure change detected ({} -> {}). Recreating
    // Dashboard.",
    // lastCompanySignature, currentSignature);
    // gameStatus.recreate();
    // lastCompanySignature = currentSignature;
    // } else {
    // // 4. Standard Refresh
    // gameStatus.refreshDashboard();
    // }
    // }

    private static final Logger log = LoggerFactory.getLogger(StatusWindow.class);

    // private JMenu infoMenu;
    private JMenu companiesMenu;
    private JMenu trainsMenu;
    private JMenu phasesMenu;
    private JMenu networkMenu;

    public void initMenu() {

        menuBar = new JMenuBar();

        // --- 1. FILE MENU ---
        fileMenu = new JMenu(LocalText.getText("FILE"));
        fileMenu.setMnemonic(KeyEvent.VK_F);

        // Get the cross-platform shortcut key (Cmd on Mac, Ctrl on Windows/Linux)
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        ActionMenuItem actionMenuItem = new ActionMenuItem(LocalText.getText("SAVE"));
        actionMenuItem.setActionCommand(SAVE_CMD);
        actionMenuItem.setMnemonic(KeyEvent.VK_S);
        actionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        actionMenuItem.addActionListener(this);
        actionMenuItem.setEnabled(true);
        actionMenuItem.setPossibleAction(new GameAction(gameUIManager.getRoot(), GameAction.Mode.SAVE));
        fileMenu.add(actionMenuItem);

        actionMenuItem = new ActionMenuItem(LocalText.getText("Reload"));
        actionMenuItem.setActionCommand(RELOAD_CMD);
        actionMenuItem.setMnemonic(KeyEvent.VK_R);
        actionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
        actionMenuItem.addActionListener(this);
        actionMenuItem.setEnabled(true);
        actionMenuItem.setPossibleAction(new GameAction(gameUIManager.getRoot(), GameAction.Mode.RELOAD));
        fileMenu.add(actionMenuItem);

        JMenuItem menuItem = new JMenuItem(LocalText.getText("AutoSaveLoad"));
        menuItem.setActionCommand(AUTOSAVELOAD_CMD);
        menuItem.setMnemonic(KeyEvent.VK_A);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
        menuItem.addActionListener(this);
        menuItem.setEnabled(true);
        fileMenu.add(menuItem);

        // Moved Config to File Menu
        menuItem = new JMenuItem(LocalText.getText("CONFIG"));
        menuItem.setName(CONFIG_CMD);
        menuItem.setActionCommand(CONFIG_CMD);
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem(LocalText.getText("QUIT"));
        menuItem.setActionCommand(QUIT_CMD);
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        menuBar.add(fileMenu);

        // --- 2. VIEW MENU (Formerly Options) ---
        optMenu = new JMenu(LocalText.getText("VIEW", "View"));
        optMenu.setMnemonic(KeyEvent.VK_V);

        // Window Toggles
        menuItem = new JCheckBoxMenuItem(LocalText.getText("MARKET"));
        menuItem.setName(MARKET_CMD);
        menuItem.setActionCommand(MARKET_CMD);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JCheckBoxMenuItem(LocalText.getText("MAP"));
        menuItem.setName(MAP_CMD);
        menuItem.setActionCommand(MAP_CMD);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JCheckBoxMenuItem(LocalText.getText("REPORT"));
        menuItem.setName(REPORT_CMD);
        menuItem.setActionCommand(REPORT_CMD);
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        // Toggle for the new modular visualization
        JCheckBoxMenuItem toggleStatusItem = new JCheckBoxMenuItem("Modular Status", useAltStatus);
        toggleStatusItem.addActionListener(e -> {
            useAltStatus = toggleStatusItem.isSelected();
            swapGameStatus();
        });
        optMenu.add(toggleStatusItem);

 // Read initial state directly from central config mapping
        boolean initialWorthState = Util.parseBoolean(net.sf.rails.common.Config.get("statusWindow.showPlayerWorth"));
        this.showPlayerWorth = initialWorthState;
        JCheckBoxMenuItem toggleWorthItem = new JCheckBoxMenuItem("Show Player Worth", initialWorthState);
        toggleWorthItem.setName("ShowPlayerWorth"); // Add this name to target it later
        toggleWorthItem.addActionListener(e -> {
            boolean isSelected = toggleWorthItem.isSelected();
            this.showPlayerWorth = isSelected;
            // Write to memory cache immediately so ConfigWindow mirrors this state change
            net.sf.rails.common.Config.set("statusWindow.showPlayerWorth", isSelected ? "yes" : "no");
            if (gameStatus != null) {
                gameStatus.recreate();
            }
        });

        optMenu.add(toggleWorthItem);

        optMenu.addSeparator();

        // Map Settings
        menuItem = new JCheckBoxMenuItem(LocalText.getText("HEX_NAMES"));
        menuItem.setName("ToggleHexNames");
        menuItem.setActionCommand("TOGGLE_HEX_NAMES");
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JCheckBoxMenuItem(LocalText.getText("HEX_NUMBERS"));
        menuItem.setName("ToggleHexNumbers");
        menuItem.setActionCommand("TOGGLE_HEX_NUMBERS");
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        optMenu.addSeparator();

        // Font Settings
        menuItem = new JMenuItem(LocalText.getText("IncreaseFont", "Increase Text Size"));
        menuItem.setActionCommand(FONT_INCREASE_CMD);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutMask));
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JMenuItem(LocalText.getText("DecreaseFont", "Decrease Text Size"));
        menuItem.setActionCommand(FONT_DECREASE_CMD);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutMask));
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        optMenu.addSeparator();

        // Map Zoom Settings
        JMenuItem item = new JMenuItem(LocalText.getText("ZoomMapIn", "Zoom Map In"));
        item.setActionCommand(MAP_ZOOM_IN_CMD);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK));
        item.addActionListener(this);
        optMenu.add(item);

        item = new JMenuItem(LocalText.getText("ZoomMapOut", "Zoom Map Out"));
        item.setActionCommand(MAP_ZOOM_OUT_CMD);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
        item.addActionListener(this);
        optMenu.add(item);

        item = new JMenuItem(LocalText.getText("FitMapWindow", "Fit Map to Window"));
        item.setActionCommand(MAP_FIT_CMD);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, ActionEvent.CTRL_MASK));
        item.addActionListener(this);
        optMenu.add(item);

        item = new JMenuItem(LocalText.getText("FitMapWidth", "Fit Map to Width"));
        item.setActionCommand("MapFitWidth");
        item.addActionListener(this);
        optMenu.add(item);

        item = new JMenuItem(LocalText.getText("FitMapHeight", "Fit Map to Height"));
        item.setActionCommand("MapFitHeight");
        item.addActionListener(this);
        optMenu.add(item);

        menuBar.add(optMenu);

        // --- 4. CHARTS MENU ---
        JMenu chartMenu = new JMenu(LocalText.getText("CHARTS", "Charts"));

        // Add the new Player Worth Chart
        JMenuItem worthChartItem = gameUIManager.getMenuChart();
        chartMenu.add(worthChartItem);

        // Add the Game End Report
        JMenuItem gameEndReportItem = gameUIManager.getMenuGameEndReport();
        chartMenu.add(gameEndReportItem);
        // Add the Company Payout Chart
        JMenuItem companyPayoutChartItem = gameUIManager.getMenuCompanyPayoutChart();
        chartMenu.add(companyPayoutChartItem);

        JMenuItem multiplierChartItem = gameUIManager.getMenuMultiplierChart();
        chartMenu.add(multiplierChartItem);

        menuBar.add(chartMenu);

        // --- 5. ACTIONS MENU (Formerly Move) ---
        moveMenu = new JMenu(LocalText.getText("ACTIONS", "Actions"));
        // CONSOLIDATED UNDO ITEM (Mapped to potentially " Undo" actions)
        undoItem = new ActionMenuItem(LocalText.getText("UNDO"));
        undoItem.setName(LocalText.getText("UNDO"));
        undoItem.setActionCommand(UNDO_CMD);
        undoItem.setMnemonic(KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        undoItem.addActionListener(this);
        undoItem.setEnabled(false);
        moveMenu.add(undoItem);

        redoItem = new ActionMenuItem(LocalText.getText("REDO"));
        redoItem.setName(LocalText.getText("REDO"));
        redoItem.setActionCommand(REDO_CMD);
        redoItem.setMnemonic(KeyEvent.VK_R);
        // Use proper shortcut mask for Redo as well
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutMask));
        redoItem.addActionListener(this);
        redoItem.setEnabled(false);
        moveMenu.add(redoItem);

        menuBar.add(moveMenu);

        // --- 7. INFO MENU ---
        JMenu infoMenu = new JMenu(LocalText.getText("INFO", "Info"));
        infoMenu.setMnemonic(KeyEvent.VK_I);

        // Remaining Tiles
        JMenuItem remTilesItem = new JMenuItem(LocalText.getText("REMAINING_TILES", "Remaining tiles"));
        remTilesItem.setActionCommand(net.sf.rails.ui.swing.ORPanel.REM_TILES_CMD);
        remTilesItem.addActionListener(this);
        infoMenu.add(remTilesItem);

        // Submenus (Content added in updateInfoMenu)
        companiesMenu = new JMenu(LocalText.getText("COMPANIES", "Companies"));
        infoMenu.add(companiesMenu);

        trainsMenu = new JMenu(LocalText.getText("TRAINS", "Trains"));
        infoMenu.add(trainsMenu);

        phasesMenu = new JMenu(LocalText.getText("PHASES", "Phases"));
        infoMenu.add(phasesMenu);

        networkMenu = new JMenu(LocalText.getText("NETWORK_INFO", "Network Info"));
        JMenuItem refreshItem = new JMenuItem(LocalText.getText("RefreshNetwork", "Refresh Network"));
        refreshItem.setActionCommand(REFRESH_NETWORK_CMD);
        refreshItem.addActionListener(this);
        networkMenu.add(refreshItem);
        infoMenu.add(networkMenu);

        // moderatorMenu = new JMenu(LocalText.getText("MODERATOR"));
        // moderatorMenu.setMnemonic(KeyEvent.VK_M);

        // ---7. CORRECTION MENU ---

        correctionMenu = new JMenu(LocalText.getText("CorrectionMainMenu"));
        correctionMenu.setName(LocalText.getText("CorrectionMainMenu"));
        correctionMenu.setMnemonic(KeyEvent.VK_C);
        correctionMenu.setEnabled(true);
        // menuBar.add(moderatorMenu);

        menuBar.add(correctionMenu); // Add as a top-level menu item

        // --- 9. DEVELOPER (Conditional) ---
        if (Config.isDevelop()) {
            developerMenu = new JMenu("Developer");
            developerMenu.setName("Developer");
            menuBar.add(developerMenu);

            developerMenu.add(infoMenu);
            developerMenu.addSeparator();

            ActionMenuItem actionRunnerItem = new ActionMenuItem("Force Actions!");
            actionRunnerItem.setName("Force Actions!");
            actionRunnerItem.setActionCommand("SHOW_ACTION_RUNNER");
            actionRunnerItem.addActionListener(this);
            developerMenu.add(actionRunnerItem);

            ActionMenuItem monitorItem = new ActionMenuItem("Move Monitor");
            monitorItem.setName("Move Monitor");
            monitorItem.setActionCommand("SHOW_MOVE_MONITOR");
            monitorItem.addActionListener(this);
            developerMenu.add(monitorItem);

            ActionMenuItem saveLogsItem = new ActionMenuItem("Save Logs");
            saveLogsItem.setName("Save Logs");
            saveLogsItem.setActionCommand("Save Logs");
            saveLogsItem.addActionListener(this);
            developerMenu.add(saveLogsItem);

            menuItem = new JMenuItem(LocalText.getText("SaveGameStatus"));
            menuItem.setActionCommand(SAVESTATUS_CMD);
            menuItem.setMnemonic(KeyEvent.VK_G);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
            menuItem.addActionListener(this);
            menuItem.setEnabled(true);
            developerMenu.add(menuItem);

            menuItem = new JMenuItem(LocalText.getText("SaveReportFile"));
            menuItem.setActionCommand(SAVEREPORT_CMD);
            menuItem.setMnemonic(KeyEvent.VK_R);
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
            menuItem.addActionListener(this);
            menuItem.setEnabled(true);
            developerMenu.add(menuItem);

            JMenuItem loadJsonItem = new JMenuItem("Load JSON State...");
            loadJsonItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select JSON Save File");
                    fileChooser.setCurrentDirectory(
                            new File(Config.get("save.directory", System.getProperty("user.dir"))));

                    if (fileChooser.showOpenDialog(StatusWindow.this) == JFileChooser.APPROVE_OPTION) {
                        File fileToLoad = fileChooser.getSelectedFile();
                        log.info("User selected JSON file: {}", fileToLoad.getAbsolutePath());

                        new Thread(() -> {
                            gameUIManager.closeGame();
                            // Call the standard load method, as your GameLoader now supports .json!
                            net.sf.rails.util.GameLoader.loadAndStartGame(fileToLoad);
                        }).start();
                    }
                }
            });
            developerMenu.add(loadJsonItem);

            JMenuItem saveJsonItem = new JMenuItem("Save JSON State...");
            saveJsonItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveJsonState();
                }
            });
            developerMenu.add(saveJsonItem);

        }

        // 10. HELP MENU
        JMenu helpMenu = new JMenu(LocalText.getText("HELP", "Help"));
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem hotkeysItem = new JMenuItem(LocalText.getText("HOTKEYS", "Hotkeys..."));
        hotkeysItem.setActionCommand("ShowHotkeys");
        hotkeysItem.setMnemonic(KeyEvent.VK_K);
        hotkeysItem.addActionListener(this);
        helpMenu.add(hotkeysItem);

        menuBar.add(helpMenu);

        JMenuItem rulesItem = new JMenuItem("Rules");
        rulesItem.addActionListener(e -> {
            // Retrieve the specific game name (e.g., "1835")
            String gameName = gameUIManager.getGameManager().getGameName();
            // Path relative to the classpath root
            String resourcePath = "/rules/" + gameName + "/" + gameName + ".pdf";

            try (java.io.InputStream pdfStream = getClass().getResourceAsStream(resourcePath)) {
                if (pdfStream == null) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Rules PDF not found for " + gameName + ".\nExpected in resources at: " + resourcePath,
                            "Missing Rules",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Create a temporary file so the system PDF viewer can access it
                java.io.File tempFile = java.io.File.createTempFile(gameName + "_rules_", ".pdf");
                tempFile.deleteOnExit();

                // Copy stream to temp file
                java.nio.file.Files.copy(pdfStream, tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Open using the Desktop API
                if (java.awt.Desktop.isDesktopSupported()
                        && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(tempFile);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Opening PDFs is not supported on this system configuration.",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "An error occurred while opening the rules:\n" + ex.getMessage(),
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        helpMenu.add(rulesItem);

        setJMenuBar(menuBar);

        if ("yes".equalsIgnoreCase(Config.get("report.window.open"))) {
            enableCheckBoxMenuItem(REPORT_CMD);
        }
    }

    public void initGameActions() {
        // Check the local Undo/Redo menu items,
        // which must always be up-to-date.
        undoItem.setEnabled(false);
        redoItem.setEnabled(false);
        if (undoButton != null)
            undoButton.setEnabled(false);
        if (redoButton != null)
            redoButton.setEnabled(false);
        // SAVE, RELOAD, AUTOSAVELOAD are always enabled
    }

    public void setGameActions() {
        List<GameAction> gameActions = possibleActions.getType(GameAction.class);
        if (gameActions != null) {
            for (GameAction na : gameActions) {
                switch (na.getMode()) {
                    case UNDO:
                    case FORCED_UNDO:
                        undoItem.setEnabled(true);
                        undoItem.setPossibleAction(na);
                        if (undoButton != null) {
                            undoButton.setEnabled(true);
                            undoButton.setPossibleAction(na);
                        }
                        break;

                    case REDO:
                        redoItem.setEnabled(true);
                        redoItem.setPossibleAction(na);
                        if (redoButton != null) {
                            redoButton.setEnabled(true);
                            redoButton.setPossibleAction(na);
                        }

                        break;
                    default:
                        break;
                }
            }
        }
    }

    // --- PINPOINT CHANGE: Replace the existing setCorrectionMenu method ---
    public void setCorrectionMenu() {
        // Reset the menu
        correctionMenu.removeAll();
        correctionMenu.setEnabled(true);

        // 1. Get currently active correction modes from the engine
        List<CorrectionModeAction> activeModes = possibleActions.getType(CorrectionModeAction.class);

        // 2. Iterate over ALL defined CorrectionTypes to build the menu dynamically
        for (CorrectionType type : CorrectionType.values()) {

            boolean isActive = false;
            // Check if this specific type is currently active in the engine
            if (activeModes != null) {
                for (CorrectionModeAction a : activeModes) {
                    if (a.getCorrectionType() == type && a.isActive()) {
                        isActive = true;
                        break;
                    }
                }
            }

            // Create a new action to toggle this mode (click flips the boolean)
            CorrectionModeAction toggleAction = new CorrectionModeAction(
                    gameUIManager.getRoot(),
                    type,
                    isActive);

            ActionCheckBoxMenuItem item = new ActionCheckBoxMenuItem(LocalText.getText(type.name()));
            item.addActionListener(this);
            item.addPossibleAction(toggleAction);
            item.setSelected(isActive);
            item.setEnabled(true);

            correctionMenu.add(item);
        }
        // Re-add the Debug Item (Must be done here because removeAll() clears it)
        correctionMenu.addSeparator();
        JMenuItem forceSkipItem = new JMenuItem("Force Skip Stuck Turn (Debug)");
        forceSkipItem.setToolTipText("Use this ONLY if the game hangs on a closed company (Zombie Turn).");
        forceSkipItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(
                        StatusWindow.this,
                        "This is a debug tool to bypass a stuck turn (e.g., a closed company acting).\n"
                                + "It forcibly advances the internal company index.\n\n"
                                + "Are you sure you want to force the engine to skip the current actor?",
                        "Force Skip Confirmation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    if (gameUIManager != null && gameUIManager.getGameManager() != null) {
                        gameUIManager.getGameManager().forceSkipStuckCompany();
                    }
                }
            }
        });
        correctionMenu.add(forceSkipItem);

        if ("1870".equals(gameUIManager.getGameManager().getGameName())) {
            JMenuItem forceDestItem = new JMenuItem("Force Connection Run (1870)");
            forceDestItem.setToolTipText("Forces the current operating company to complete its destination run.");
            forceDestItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gameUIManager != null && gameUIManager.getGameManager() != null) {
                        RoundFacade currentRound = gameUIManager.getGameManager().getCurrentRound();
                        if (currentRound instanceof net.sf.rails.game.specific._1870.OperatingRound_1870) {
                            ((net.sf.rails.game.specific._1870.OperatingRound_1870) currentRound).forceConnectionRun();
                            gameUIManager.updateUI();
                        }
                    }
                }
            });
            correctionMenu.add(forceDestItem);
        }

    }

    public boolean setupFor(RoundFacade round) {
        currentRound = round;
        // We must call refreshDashboard/recreate here to ensure the UI
        // reflects the new list of companies (M2, M4, etc. closed) BEFORE
        // initCashCorrectionActions runs its button-clearing loop.
        if (gameStatus != null) {
            gameStatus.refreshDashboard();
        }

        if (round instanceof StartRound) {
            disableCheckBoxMenuItem(MAP_CMD);
            disableCheckBoxMenuItem(MARKET_CMD);
        } else if (round instanceof StockRound) {
            enableCheckBoxMenuItem(MARKET_CMD);
            disableCheckBoxMenuItem(MAP_CMD);
        } else if (round instanceof OperatingRound) {
            enableCheckBoxMenuItem(MAP_CMD);
            disableCheckBoxMenuItem(MARKET_CMD);
        }

        // Initialize BOTH Cash and Train actions
        // Note: These methods are defined in GameStatus.java, we just call them here.
        boolean c = gameStatus.initCashCorrectionActions();
        boolean t = gameStatus.initTrainCorrectionActions();
        // Include Stock Correction Mode in the override check

        // Return true if EITHER mode is active.
        // This tells the UI manager to keep the Status Window visible.
        return c || t;
    }

    public void disableButtons() {
        passButton.setEnabled(false);
        autopassButton.setEnabled(false);
    }

    /** Stub, may be overridden in game-specific subclasses */
    protected boolean updateGameSpecificSettings() {
        return false;
    }

    private void enableCheckBoxMenuItem(String name) {
        for (int x = 0; x < optMenu.getMenuComponentCount(); x++) {
            try {
                if (optMenu.getMenuComponent(x).getName().equals(name)) {
                    ((JCheckBoxMenuItem) optMenu.getMenuComponent(x)).setSelected(true);
                }
            } catch (NullPointerException e) {
                // The separator has null name. Har Har Har.
            }
        }
    }

    private void disableCheckBoxMenuItem(String name) {
        for (int x = 0; x < optMenu.getMenuComponentCount(); x++) {
            try {
                if (optMenu.getMenuComponent(x).getName().equals(name)) {
                    ((JCheckBoxMenuItem) optMenu.getMenuComponent(x)).setSelected(false);
                }
            } catch (NullPointerException e) {
                // The separator has null name. Har Har Har.
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent actor) {
        String command = actor.getActionCommand();
        List<PossibleAction> actions = null;
        if (actor.getSource() instanceof ActionTaker) {
            actions = ((ActionTaker) actor.getSource()).getPossibleActions();
        }
        PossibleAction executedAction = null;

        if (actions != null && !actions.isEmpty()) {
            executedAction = actions.get(0);
        }

        // ++ ADD PAUSE BUTTON HANDLING ++
        if (command.equals(TOGGLE_PAUSE_CMD)) {
            if (gameUIManager.isTimerPaused()) {
                gameUIManager.resumeTimer();
            } else {
                gameUIManager.pauseTimer();
            }
        } else if (command.equals(BUY_CMD)) {
            process(executedAction);
        } else if (command.equals(SELL_CMD)) {
            process(executedAction);
        } else if (command.equals(DONE_CMD) || command.equals(PASS_CMD) || command.equals(AUTOPASS_CMD)) {
            if (gameUIManager.isGameOver()) {
                System.exit(0);
            }
            process(executedAction);

        } else if (executedAction instanceof UseSpecialProperty
                || executedAction instanceof RequestTurn) {
            process(executedAction);

        } else if (executedAction instanceof AdjustSharePrice) {
            // Not sure where this should be handled.
            // Will proably also be accessible from OR instances,
            // so let GameUIManager handle it.
            gameUIManager.adjustSharePrice((AdjustSharePrice) executedAction);

        } else if (command.equals(QUIT_CMD)) {
            gameUIManager.terminate();
        } else if (command.equals(NEW_CMD)) {
            // TODO
        } else if (command.equals(LOAD_CMD)) {
            // TODO: does this really belong here?
            String saveDirectory = Config.get("save.directory");
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File(saveDirectory));
            jfc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    // TODO: need to filter like GameSetupController.isOurs() does
                    return true;
                }

                @Override
                public String getDescription() {
                    return null;
                }
            });

            if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                // close the existing game

                final File selectedFile = jfc.getSelectedFile();
                // start in new thread so that swing thread is not used for game setup
                new Thread(() -> {
                    // close the existing game (which ironically will include us
                    gameUIManager.closeGame();
                    // start the new game
                    GameLoader.loadAndStartGame(selectedFile);
                }).start();
            }
        } else if (command.equals(REPORT_CMD)) {
            // Logic: If Checkbox, toggle. If Button (Info Menu), force Show.
            boolean show = true;
            if (actor.getSource() instanceof JCheckBoxMenuItem) {
                show = ((JCheckBoxMenuItem) actor.getSource()).isSelected();
            }
            gameUIManager.reportWindow.setVisible(show);
            if (show)
                gameUIManager.reportWindow.scrollDown();

        } else if (command.equals(MARKET_CMD)) {
            boolean show = true;
            if (actor.getSource() instanceof JCheckBoxMenuItem) {
                show = ((JCheckBoxMenuItem) actor.getSource()).isSelected();
            }
            gameUIManager.stockChartWindow.setVisible(show);

        } else if (command.equals(MAP_CMD)) {
            boolean show = true;
            if (actor.getSource() instanceof JCheckBoxMenuItem) {
                show = ((JCheckBoxMenuItem) actor.getSource()).isSelected();
            }
            gameUIManager.orWindow.setVisible(show);

        } else if (command.equals(CONFIG_CMD)) {
            boolean show = true;
            if (actor.getSource() instanceof JCheckBoxMenuItem) {
                show = ((JCheckBoxMenuItem) actor.getSource()).isSelected();
            }
            gameUIManager.configWindow.setVisible(show);

        } else if (command.equals(AUTOSAVELOAD_CMD)) {
            gameUIManager.autoSaveLoadGame();
        } else if (command.equals(SAVESTATUS_CMD)) {
            gameUIManager.saveGameStatus();
        } else if (command.equals("Save Logs")) {
            gameUIManager.saveLogs();
        } else if (command.equals(SAVEREPORT_CMD)) {
            gameUIManager.saveReportFile();
        } else if (command.equals(AI_MOVE_CMD)) {

            // ++ ADD CASE FOR NEW OR BUTTON ++
            // log.info("AI_MOVE_CMD detected by StatusWindow. Delegating to
            // GameUIManager.");
            enableAIButton(false); // Disable button immediately
            gameUIManager.performAIMove(); // Call the central AI handler

        } else if (command.equals(MAP_ZOOM_IN_CMD)) {
            gameUIManager.zoomMap(true); // Call zoom in
        } else if (command.equals(MAP_ZOOM_OUT_CMD)) {
            gameUIManager.zoomMap(false); // Call zoom out
        } else if (command.equals(MAP_FIT_CMD)) {
            gameUIManager.fitMapToWindow(); // Call fit
        } else if (command.equals("MapFitWidth")) {
            gameUIManager.fitMapToWidth(); // Need to add this

        } else if (command.equals("MapFitHeight")) {
            gameUIManager.fitMapToHeight(); // Need to add this
        } else if (command.equals("ShowHotkeys")) {
            showHotkeysDialog();

        } else if (command.equals(FONT_INCREASE_CMD)) {
            updateFonts(currentBaseFontSize + 1f);
        } else if (command.equals(FONT_DECREASE_CMD)) {
            updateFonts(Math.max(8f, currentBaseFontSize - 1f));
        } else if (command.equals("SHOW_MOVE_MONITOR")) {
            showMoveMonitor();
        } else if (command.equals("SHOW_ACTION_RUNNER")) {
            showActionRunner();

        } else if (command.equals(REM_TILES_CMD) || command.equals(ORPanel.REM_TILES_CMD)) {

            // Route through ORUIManager
            if (gameUIManager.getORUIManager() != null) {
                // Pass the correct command string "RemainingTiles" defined in ORPanel
                gameUIManager.getORUIManager().processAction(ORPanel.REM_TILES_CMD, null, this);
            } else {
                log.info("StatusWindow: ORUIManager is null (not in OR?). Cannot open Tiles window.");
            }
            return;
        }

        else if (executedAction == null) {

        } else if (executedAction instanceof GameAction) {
            switch (((GameAction) executedAction).getMode()) {
                case SAVE:
                    gameUIManager.saveGame((GameAction) executedAction);
                    break;
                case RELOAD:
                    gameUIManager.reloadGame((GameAction) executedAction);
                    break;
                case EXPORT:
                    gameUIManager.exportGame((GameAction) executedAction);
                    break;
                default:
                    process(executedAction);
                    break;
            }
        }

        else {
            // Unknown action, let UIManager catch it
            process(executedAction);
        }
    }

    @Override
    public boolean process(PossibleAction executedAction) {
        if (executedAction == null) {
            JOptionPane.showMessageDialog(this, "ERROR: no action found!");
            return false;
        }

        return gameUIManager.processAction(executedAction);
    }

    @Override
    public boolean processImmediateAction() {

        return true;
    }

    public void setPassButton(NullAction action) {
        if (action != null) {
            NullAction.Mode mode = action.getMode();
            if (mode == NullAction.Mode.PASS) {
                passButton.setText("Pass");
                passButton.setIcon(null);
                styleStatusButton(passButton, SYS_BLUE);
            } else if (mode == NullAction.Mode.DONE) {
                passButton.setText("Done");
                passButton.setIcon(null);
                styleStatusButton(passButton, SYS_BLUE);
            }
            passButton.setEnabled(true);
            passButton.setVisible(true);
            passButton.addPossibleAction(action);
        } else {
            passButton.setEnabled(false);
            passButton.setVisible(false);
            passButton.clearPossibleActions();
        }
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void uncheckMenuItemBox(String itemName) {
        int count = optMenu.getMenuComponentCount();

        for (int i = 0; i < count; i++) {
            try {
                if (optMenu.getMenuComponent(i).getName().equalsIgnoreCase(itemName)) {
                    ((JCheckBoxMenuItem) optMenu.getMenuComponent(i)).setSelected(false);
                    optMenu.invalidate();
                }
            } catch (NullPointerException e) {
                // Seperators are null
            }
        }
    }

    public void finishRound() {
        setTitle("Rails Evolution - " + gameUIManager.getGameManager().getGameName() + " - "
                + LocalText.getText("GAME_STATUS_TITLE") + " - " + buildTimestamp);

        gameStatus.recreate();
        gameStatus.initTurn(-1, true);
        passButton.setEnabled(false);
    }

    public void endOfGame() {

        setTitle("Rails Evolution - " + gameUIManager.getGameManager().getGameName() + " - "
                + LocalText.getText("EoGTitle") + " - " + buildTimestamp);

        // Enable Passbutton
        passButton.setEnabled(true);
        passButton.setRailsIcon(RailsIcon.END_OF_GAME_CLOSE_ALL_WINDOWS);

        gameUIManager.orWindow.finish();

        if (gameTimeLabel != null) {
            gameTimeLabel.setVisible(false);
        }
        if (uiRefreshTimer != null) {
            uiRefreshTimer.stop();
        }
    }

    public Player getCurrentPlayer() {
        return gameUIManager.getCurrentPlayer();
    }

    public void updatePlayerOrder(List<String> newPlayerNames) {
        gameStatus.updatePlayerOrder(newPlayerNames);
    }

    /**
     * Enables or disables the AI OR Move button based on game state.
     * 
     * @param enable Generally true if the button should be considered, false
     *               otherwise.
     */
    public void enableAIButton(boolean enable) {
        if (aiButton != null) {
            aiButton.setEnabled(true);
        }
    }

    private void setupHotkeys() {
        // Define a name for our action
        String CONFIRM_ACTION_KEY = "confirmAction";

        // Create the Action (Necessary for 'Enter' key mapping, though only used for
        // PASS/DONE)
        AbstractAction confirmAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check for the "Pass" or "Done" button (passButton)
                if (passButton != null && passButton.isEnabled()) {
                    passButton.doClick(); // Clicks the visible button (PASS or DONE)
                    return;
                }
            }
        };

        // Get the InputMap for the root pane when it's in a focused window
        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Get the ActionMap for the root pane
        ActionMap actionMap = this.getRootPane().getActionMap();
        int MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // --- 1. System Hotkeys (Required for basic functionality/Moderator) ---

        // --- 4. Font Scaling Hotkeys (+ / -) ---
        String FONT_INC = "fontIncrease";
        String FONT_DEC = "fontDecrease";

        // Increase: Equals (=), Shift+Equals (+), and Numpad Add (+)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), FONT_INC);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), FONT_INC);

        actionMap.put(FONT_INC, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Read current fraction, increment by 5%, cap at 2.0 (200%)
                double currentFactor = 1.0;
                try {
                    currentFactor = Double.parseDouble(net.sf.rails.common.Config.get("statusWindow.zoom"));
                    if (currentFactor >= 5.0)
                        currentFactor /= 100.0;
                } catch (Exception ex) {
                }
                double newFactor = Math.min(2.0, currentFactor + 0.05);
                net.sf.rails.common.Config.set("statusWindow.zoom", String.valueOf(newFactor));
                updateFontsFromConfig();

            }
        });

        // Decrease: Minus (-) and Numpad Subtract (-)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), FONT_DEC);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), FONT_DEC);

        actionMap.put(FONT_DEC, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Read current fraction, decrement by 5%, floor at 0.5 (50%)
                double currentFactor = 1.0;
                try {
                    currentFactor = Double.parseDouble(net.sf.rails.common.Config.get("statusWindow.zoom"));
                    if (currentFactor >= 5.0)
                        currentFactor /= 100.0;
                } catch (Exception ex) {
                }
                double newFactor = Math.max(0.5, currentFactor - 0.05);
                net.sf.rails.common.Config.set("statusWindow.zoom", String.valueOf(newFactor));
                updateFontsFromConfig();

            }
        });

    }

    public void init(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        this.possibleActions = gameUIManager.getGameManager().getPossibleActions();

        // Extracted instantiation to createGameStatus() to allow overrides
        gameStatus = createGameStatus();

        // String gameStatusClassName =
        // gameUIManager.getClassName(GuiDef.ClassName.GAME_STATUS);
        // try {
        // Class<? extends GameStatus> gameStatusClass =
        // Class.forName(gameStatusClassName)
        // .asSubclass(GameStatus.class);
        // gameStatus = gameStatusClass.newInstance();
        // } catch (Exception e) {
        // log.error("Cannot instantiate class {}", gameStatusClassName, e);
        // System.exit(1);
        // }

        gameStatus.init(this, gameUIManager);

        gameStatusPane = new JScrollPane(gameStatus);
        gameStatusPane.getVerticalScrollBar().setUnitIncrement(16);
        gameStatusPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        gameStatusPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // --- BUTTONS (SOUTH) ---
        // 1. Single Row Grid (1 Row, 5 Cols) -> HGap=12 for "tiny bit more space"
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // 2. Define Buttons
        // We use RailsIcon.PASS as a placeholder for the constructor, then strip it for
        // Text-Only mode.

        // PAUSE
        pauseButton = new ActionButton(RailsIcon.PAUSE);
        pauseButton.setText("Pause");
        pauseButton.setIcon(null);
        pauseButton.setToolTipText("Pause/Resume the player timer");
        pauseButton.setActionCommand(TOGGLE_PAUSE_CMD);
        pauseButton.addActionListener(this);

        // UNDO
        undoButton = new ActionButton(RailsIcon.PASS);
        undoButton.setText("Undo");
        undoButton.setIcon(null);
        undoButton.setActionCommand(UNDO_CMD);
        undoButton.addActionListener(this);
        undoButton.setEnabled(false);

        // REDO
        redoButton = new ActionButton(RailsIcon.PASS);
        redoButton.setText("Redo");
        redoButton.setIcon(null);
        redoButton.setActionCommand(REDO_CMD);
        redoButton.addActionListener(this);
        redoButton.setEnabled(false);

        // AI
        aiButton = new ActionButton(RailsIcon.AI_MOVE);
        aiButton.setText("AI");
        aiButton.setIcon(null);
        aiButton.setActionCommand(AI_MOVE_CMD);
        aiButton.setToolTipText("Let AI make the next Operating Round move");
        aiButton.addActionListener(this);

        // PASS
        passButton = new ActionButton(RailsIcon.PASS);
        passButton.setText("Pass");
        passButton.setIcon(null);
        passButton.setMnemonic(KeyEvent.VK_P);
        passButton.setActionCommand(DONE_CMD);
        passButton.addActionListener(this);

        // We do NOT add this to the buttonPanel (keeping your 5-button layout),
        // but we must initialize it so updateStatus() doesn't crash.
        autopassButton = new ActionButton(RailsIcon.DONE);
        autopassButton.setText("Auto Pass");
        autopassButton.setIcon(null);
        autopassButton.setActionCommand(AUTOPASS_CMD);
        autopassButton.addActionListener(this);

        // 3. Add to Panel in Order: Pause, Undo, Redo, AI, Pass
        buttonPanel.add(pauseButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(redoButton);
        buttonPanel.add(aiButton);
        buttonPanel.add(passButton);

        // 4. Style & Size
        styleStatusButton(pauseButton, SYS_BLUE);
        styleStatusButton(undoButton, SYS_BLUE);
        styleStatusButton(redoButton, SYS_BLUE);
        styleStatusButton(aiButton, SYS_BLUE);
        styleStatusButton(passButton, SYS_BLUE);

        // Force Taller Buttons (45px height)
        Dimension btnDim = new Dimension(60, 35);
        pauseButton.setPreferredSize(btnDim);
        undoButton.setPreferredSize(btnDim);
        redoButton.setPreferredSize(btnDim);
        aiButton.setPreferredSize(btnDim);
        passButton.setPreferredSize(new Dimension(113, 45)); // 1.41x wider

        setSize(600, 300);

        buttonPanel.setBorder(null);
        buttonPanel.setOpaque(false);

        String loadedBuildTime = "Dev Build";
        try (InputStream input = StatusWindow.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                loadedBuildTime = prop.getProperty("buildTimestamp", loadedBuildTime);
            }
        } catch (Exception ex) {
        }

        this.buildTimestamp = loadedBuildTime;
        setTitle("Rails Evolution - " + gameUIManager.getGameManager().getGameName() + " - "
                + LocalText.getText("GAME_STATUS_TITLE") + " - " + buildTimestamp);

        pane.setLayout(new BorderLayout());
        initMenu();

        // 2. Timer Label
        gameTimeLabel = new JLabel("00:00:00");
        gameTimeLabel.setForeground(Color.BLACK);
        gameTimeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gameTimeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // 3. Apply Initial Fonts

        // Restore this call, but calculate size based on the saved global scale
        float savedScale = (float) gameUIManager.getFontScale();
        // Default 14f * 1.0 = 14f. If saved 0.8 -> 11.2f
        this.currentBaseFontSize = 14f * savedScale;
        updateFonts(currentBaseFontSize);

        // 2. Setup UI Timer
        if (uiRefreshTimer != null && uiRefreshTimer.isRunning()) {
            uiRefreshTimer.stop();
        }
        uiRefreshTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTimeLabel();
            }
        });
        uiRefreshTimer.start();

        pane.add(gameStatusPane, BorderLayout.CENTER); // Table in Center

        // Bottom Container
        JPanel southContainer = new JPanel(new BorderLayout(10, 0));
        southContainer.setBorder(BorderFactory.createEtchedBorder());
        southContainer.add(buttonPanel, BorderLayout.WEST);

        dynamicButtonPanel = new JPanel();
        dynamicButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        dynamicButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        dynamicButtonPanel.setOpaque(false);
        southContainer.add(dynamicButtonPanel, BorderLayout.CENTER);
        southContainer.add(gameTimeLabel, BorderLayout.EAST);

        pane.add(southContainer, BorderLayout.SOUTH); // Buttons on Bottom

        pane.setOpaque(true);
        setContentPane(pane);
        setGlassPane(new PauseOverlay());
        gameUIManager.setMeVisible(this, true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupHotkeys();

        final JFrame frame = this;
        final GameUIManager guiMgr = gameUIManager;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (GameUIManager.confirmQuit(frame)) {
                    frame.dispose();
                    guiMgr.terminate();
                }
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                guiMgr.getWindowSettings().set(frame);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                guiMgr.getWindowSettings().set(frame);

                if (gameStatus != null) {
                    

                    int availableWidth = frame.getWidth() - 40; 
                    int contentWidth = gameStatus.getPreferredSize().width;

                    if (contentWidth > 0 && availableWidth > 0) {
                        // Calculate exact multiplier needed to fit content inside the window width
                        float ratio = (float) availableWidth / (float) contentWidth;
                        float targetFontSize = currentBaseFontSize * ratio;
                        
                        // Clamp bounds to prevent entirely unreadable text or massive overflows
                        targetFontSize = Math.max(6f, Math.min(36f, targetFontSize));

                        // Update only if difference > 0.5f to eliminate feedback loops during smooth drag
                        if (Math.abs(targetFontSize - currentBaseFontSize) > 0.5f) {
                            updateFonts(targetFontSize);
                        }
                    }


                }
            }
        });

        gameUIManager.packAndApplySizing(this);
        enforceDynamicMinimumSize();
    }

    /**
     * Factory method to create the GameStatus panel.
     * Subclasses (like StatusWindow_1856) override this to provide custom panels.
     */
    protected GameStatus createGameStatus() {

        if (useAltStatus) {
            return new GameStatus_Alt();
        } else {

            // Explicitly inject the 1817 GameStatus if the game matches
            if ("1817".equals(gameUIManager.getGameManager().getGameName())) {
                // Use direct compile-time instantiation instead of reflection
                return new net.sf.rails.ui.swing.gamespecific._1817.GameStatus_1817();
            }

            String gameStatusClassName = gameUIManager.getClassName(GuiDef.ClassName.GAME_STATUS);
            try {
                Class<? extends GameStatus> gameStatusClass = Class.forName(gameStatusClassName)
                        .asSubclass(GameStatus.class);
                return gameStatusClass.newInstance();
            } catch (Exception e) {
                log.error("Cannot instantiate class {}", gameStatusClassName, e);
                System.exit(1);
                return null;
            }
        }
    }

    // --- START FIX ---
    private void swapGameStatus() {
        // 1. Create the new component based on the useAltStatus toggle
        gameStatus = createGameStatus();

        // 2. Re-initialize the new component
        gameStatus.init(this, gameUIManager);

        // 3. Attach it to the existing scroll pane (which is named gameStatusPane)
        if (gameStatusPane != null) {
            gameStatusPane.setViewportView(gameStatus);
        }

        // 4. Sync the new UI with the current game data
        gameStatus.refreshDashboard();

        // 5. Force layout refresh
        revalidate();
        repaint();
    }

    /**
     * Provides GameStatus access to the panel that holds dynamic SR/IR buttons.
     * 
     * @return The dynamic button panel.
     */
    public JPanel getDynamicButtonPanel() {
        return dynamicButtonPanel;
    }



// ... (lines of unchanged context code) ...
    private void enforceDynamicMinimumSize() {
        if (gameStatus != null && gameStatusPane != null) {
            // 1. Force the ScrollPane to behave as a simple container without scrollbars
            gameStatusPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            gameStatusPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            // 2. We no longer force a minimum size via setMinimumSize().
            // Instead, we let the container size itself naturally based on the content.
        }

        // 3. Pack the frame to fit the current preferred size of the GameStatus panel.
        // This will automatically shrink or grow the window when the zoom changes.
        // this.pack();
    }
// ... (rest of the method) ...








    private void updateComponentTreeFont(Component comp, Font font) {
        comp.setFont(font);
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateComponentTreeFont(child, font);
            }
        }
    }

    private void refreshTimeLabel() {
        if (gameUIManager == null || gameUIManager.getGameManager() == null)
            return;

        String timeText = "00:00:00";
        Color color = Color.BLACK;

        Player p = gameUIManager.getCurrentPlayer();
        if (p != null) {
            int val = gameUIManager.getDisplayedTime(p);
            int absVal = Math.abs(val);
            timeText = String.format("%s: %s%02d:%02d",
                    p.getName(),
                    (val < 0 ? "-" : ""),
                    absVal / 60,
                    absVal % 60);
            if (val < 0)
                color = SYS_RED;
        } else {
            net.sf.rails.game.GameManager gm = gameUIManager.getGameManager();
            gm.incrementTotalGameTime();
            timeText = gm.getFormattedGameTime();
        }

        if (gameUIManager.isTimerPaused()) {
            getGlassPane().setVisible(true);
        } else {
            getGlassPane().setVisible(false);
        }

        if (gameTimeLabel != null) {
            gameTimeLabel.setText(timeText);
            gameTimeLabel.setForeground(color);
        }

        if (pauseButton != null) {
            if (gameUIManager.isTimerPaused()) {
                if (!"Resume".equals(pauseButton.getText())) {
                    pauseButton.setText("Resume");
                    pauseButton.setBackground(Color.YELLOW);
                    pauseButton.setForeground(Color.BLACK);
                }
            } else {
                if (!"Pause".equals(pauseButton.getText())) {
                    pauseButton.setText("Pause");
                    pauseButton.setBackground(UIManager.getColor("Button.background"));
                    pauseButton.setForeground(Color.BLACK);
                }
            }
        }
    }

    private String currentMetadata = "";

    public void updateMetadata(String meta) {
        this.currentMetadata = meta;
        // Force an immediate update so it doesn't lag by 1 second
        refreshTimeLabel();
    }

    @Override
    public void updateStatus(boolean myTurn) {

        if (moveMonitor != null && moveMonitor.isVisible()) {
            moveMonitor.refresh();
        }

        if (actionRunner != null && actionRunner.isVisible()) {
            actionRunner.refresh();
        }

        try {
            // 1. Debug: Log Entry and Player States
            Player uiPlayer = gameUIManager.getCurrentPlayer();
            Player enginePlayer = null;
            if (gameUIManager.getGameManager() != null) {
                enginePlayer = gameUIManager.getGameManager().getCurrentPlayer();
            }

            // 2. Refresh Actions from Engine
            if (gameUIManager.getGameManager() != null) {
                this.possibleActions = gameUIManager.getGameManager().getPossibleActions();
                this.currentRound = gameUIManager.getGameManager().getCurrentRound();

            }

            // 3. Delegate to Subclass (Polymorphism)
            // We removed the hardcoded 1835 PFR check.
            updateGameSpecificHighlights();

            // RESOLVE EFFECTIVE PLAYER (Centralized Fix)
            // If the Engine reports NULL (during interruptions like Discard Train),
            // we infer the active player from the Operating Company.
            Player effectivePlayer = getCurrentPlayer();
            if (effectivePlayer == null && gameUIManager.getGameManager() != null) {
                RoundFacade rf = gameUIManager.getGameManager().getCurrentRound();
                if (rf instanceof OperatingRound) {
                    PublicCompany pc = ((OperatingRound) rf).getOperatingCompany();
                    if (pc != null && pc.getPresident() != null) {
                        effectivePlayer = pc.getPresident();
                        log.info("StatusWindow: Interruption detected. Inferred Active Player: {}",
                                effectivePlayer.getName());
                    }
                }
            }

            if (currentRound instanceof net.sf.rails.game.specific._1817.AuctionRound_1817) {
                net.sf.rails.game.specific._1817.AuctionRound_1817 ar = (net.sf.rails.game.specific._1817.AuctionRound_1817) currentRound;
                if (ar.getActingPlayer() != null) {
                    effectivePlayer = ar.getActingPlayer();
                }
            } else if (currentRound instanceof net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) {
                net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817 mar = (net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) currentRound;
                net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep step = mar.getCurrentStep();
                if (step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_AUCTION ||
                        step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_SELECT_BUYER
                        ||
                        step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_FRIENDLY) {
                    if (mar.getActingPlayer() != null) {
                        effectivePlayer = mar.getActingPlayer();
                    }
                }
            }

            // Use this safe index for ALL initTurn calls below
            final int effectivePlayerIndex = (effectivePlayer != null) ? effectivePlayer.getIndex() : -1;

            // 1. Highlight Logic
            if (!myTurn) {
                gameStatus.initTurn(effectivePlayerIndex, false);
            } else {
                // Unconditionally call initTurn for the active player.
                gameStatus.initTurn(effectivePlayerIndex, true);
            }

            if (currentRound instanceof net.sf.rails.game.specific._1817.AuctionRound_1817) {
                net.sf.rails.game.specific._1817.AuctionRound_1817 ar = (net.sf.rails.game.specific._1817.AuctionRound_1817) currentRound;
                if (ar.getAuctionedCompany() != null) {
                    highlightRailCard(ar.getAuctionedCompany(), null, "Auction");
                }
            } else if (currentRound instanceof net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) {
                net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817 mar = (net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) currentRound;
                net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep step = mar.getCurrentStep();
                if (step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_AUCTION ||
                        step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_SELECT_BUYER
                        ||
                        step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_FRIENDLY) {
                    if (mar.getOperatingCompany() != null) {
                        highlightRailCard(mar.getOperatingCompany(), null, "Sale");
                    }
                }
            }

            // Register the Global Hotkey Manager
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher(new GlobalHotkeyManager(gameUIManager));

            // Reset the menu items to disabled
            initGameActions();

            // 3. Enable them if the new actions list contains Undo/Redo
            setGameActions();

            // 4. Rebuild Correction Menu
            setCorrectionMenu();
            updateInfoMenu(); // Rebuild the lists

            passButton.setEnabled(false);
            autopassButton.setEnabled(false);

            // Sync local caching state flag with authoritative memory map
            this.showPlayerWorth = Util.parseBoolean(net.sf.rails.common.Config.get("statusWindow.showPlayerWorth"));

            // Crash-Proof Dashboard Sync ---
            try {
                // 1. FORCE RECREATE (Fixes "Green Ghost" and "Stale Names")
                // Instead of just refreshing values, we destroy and rebuild the grid.
                // This ensures all "Active" states are wiped clean and player order is
                // re-fetched.
                if (gameStatus != null) {
                    gameStatus.recreate();
                }

                // Re-apply the current scaled font to the newly created buttons
                updateFonts(this.currentBaseFontSize);

                // Always push the Priority Deal state to the UI, regardless of Round Type.
                if (gameUIManager.getPriorityPlayer() != null) {
                    gameStatus.setPriorityPlayer(gameUIManager.getPriorityPlayer().getIndex());
                } else {
                    gameStatus.setPriorityPlayer(-1);
                }

                // 2. Highlight Logic
                if (!myTurn) {
                    gameStatus.initTurn(effectivePlayerIndex, false);
                } else if (currentRound instanceof StockRound
                        || currentRound instanceof StartRound
                        || currentRound instanceof ShareSellingRound
                        || currentRound instanceof TreasuryShareRound) {

                    gameStatus.initTurn(effectivePlayerIndex, true);
                }

            } catch (Exception e) {
                // 3. DESYNC DETECTED
                log.warn("StatusWindow: Dashboard Desync detected ({}). Attempting Hard Reset...",
                        e.getClass().getSimpleName());

                try {
                    // 4. HARD RESET: Destroy old instance, create NEW instance
                    if (gameStatus != null) {

                        // 1. Snapshot timing data before recreation
                        // NOTE: This relies on GameStatus.java having getLastPlayerTimes()
                        int[] savedTimes = gameStatus.getLastPlayerTimes();

                        // Create fresh instance via Reflection
                        Class<? extends GameStatus> clazz = gameStatus.getClass();
                        gameStatus = clazz.getDeclaredConstructor().newInstance();

                        // Initialize it
                        gameStatus.init(this, gameUIManager);

                        // 2. Restore timing data if needed
                        // NOTE: This relies on GameStatus.java having setLastPlayerTimes()
                        if (savedTimes != null) {
                            gameStatus.setLastPlayerTimes(savedTimes);
                        }

                        // Hot-swap it into the UI
                        if (gameStatusPane != null) {
                            gameStatusPane.setViewportView(gameStatus);
                        }

                        // 5. Retry Logic on the FRESH instance
                        if (!myTurn) {
                            gameStatus.initTurn(effectivePlayerIndex, false);
                        } else {
                            if (currentRound instanceof StockRound
                                    || currentRound instanceof ShareSellingRound
                                    || currentRound instanceof TreasuryShareRound) {
                                int priority = -1;
                                if (gameUIManager.getPriorityPlayer() != null) {
                                    priority = gameUIManager.getPriorityPlayer().getIndex();
                                }
                                gameStatus.initTurn(effectivePlayerIndex, true);
                                if (priority >= 0)
                                    gameStatus.setPriorityPlayer(priority);
                            }
                        }
                        log.info("StatusWindow: Hard Reset successful. UI restored.");
                    }
                } catch (Exception ex) {
                    // 6. FAIL SAFE: If Hard Reset fails, keep game running without dashboard
                    // highlights
                    log.error("StatusWindow: Hard Reset failed. Dashboard may be static.", ex);
                }
            }

            // 1. Highlight Logic

            if (!myTurn) {
                gameStatus.initTurn(effectivePlayerIndex, false);
            } else {
                // Unconditionally call initTurn for the active player.
                gameStatus.initTurn(effectivePlayerIndex, true);
            }

            Player currentPlayer = effectivePlayer;
            String activityText = (currentPlayer != null) ? "Thinking: " + currentPlayer.getName() : "Thinking...";

            if (currentRound instanceof StockRound) {
                float certCount = currentPlayer.getPortfolioModel().getCertificateCount();
                int certLimit = gameUIManager.getGameManager().getPlayerCertificateLimit(currentPlayer);
                if (certCount > certLimit) {
                    activityText = "!! " + currentPlayer.getName() + " MUST SELL shares (Over Limit " + certCount + "/"
                            + certLimit + ")";
                }
            }

            updateActivityPanel(activityText);

            boolean enableSRButton = (currentRound instanceof StockRound) && myTurn;
            enableAIButton(enableSRButton);

            String customTitle = currentRound.getOwnWindowTitle();
            String titlePrefix = "Rails Evolution - " + gameUIManager.getGameManager().getGameName() + " - ";
            if (Util.hasValue(customTitle)) {
                setTitle(titlePrefix + customTitle + " - " + buildTimestamp);
            }

            if (currentRound instanceof TreasuryShareRound) {
                if (!Util.hasValue(customTitle)) {
                    setTitle(titlePrefix + LocalText.getText(
                            "TRADE_TREASURY_SHARES_TITLE",
                            ((TreasuryShareRound) currentRound).getOperatingCompany().getId(),
                            String.valueOf(gameUIManager.getGameManager().getORId())) + " - " + buildTimestamp);
                }
            } else if ((currentRound instanceof ShareSellingRound)) {
                if (!Util.hasValue(customTitle)) {
                    setTitle(titlePrefix + LocalText.getText(
                            "EMERGENCY_SHARE_SELLING_TITLE",
                            (((ShareSellingRound) currentRound).getCompanyNeedingCash().getId())) + " - "
                            + buildTimestamp);
                }
                // int cash = ((ShareSellingRound) currentRound).getRemainingCashToRaise();
                // JOptionPane.showMessageDialog(this, LocalText.getText(
                // "YouMustRaiseCash", getCurrentPlayer(),
                // gameUIManager.format(cash)), "",
                // JOptionPane.OK_OPTION);

            } else if (currentRound instanceof StockRound) {
                if (!Util.hasValue(customTitle)) {
                    setTitle(titlePrefix + LocalText.getText(
                            "STOCK_ROUND_TITLE",
                            String.valueOf(((StockRound) currentRound).getStockRoundNumber())) + " - "
                            + buildTimestamp);
                }
            }

            if (dynamicButtonPanel != null) {
                dynamicButtonPanel.removeAll();

                if (currentRound instanceof net.sf.rails.game.specific._1817.AuctionRound_1817) {
                    net.sf.rails.game.specific._1817.AuctionRound_1817 auction = (net.sf.rails.game.specific._1817.AuctionRound_1817) currentRound;

                    // Visual container setup
                    dynamicButtonPanel.setBackground(new Color(230, 240, 255));
                    dynamicButtonPanel.setOpaque(true);
                    dynamicButtonPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));

                    dynamicButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
                    // 1. Display Highest Bidder and Amount
                    String highBidderName = (auction.getHighestBidder() != null) ? auction.getHighestBidder().getName()
                            : "None";
                    JLabel highBidLabel = new JLabel("High: " + highBidderName + " ($" + auction.getCurrentBid() + ")");
                    highBidLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

                    highBidLabel.setForeground(new Color(0, 102, 204));
                    dynamicButtonPanel.add(highBidLabel);

                    // Vertical separator for visual clarity
                    JSeparator sep = new JSeparator(JSeparator.VERTICAL);
                    sep.setPreferredSize(new Dimension(2, 25));
                    dynamicButtonPanel.add(sep);

                    String actorName = (auction.getActingPlayer() != null) ? auction.getActingPlayer().getName()
                            : "Someone";
                    String companyId = (auction.getAuctionedCompany() != null) ? auction.getAuctionedCompany().getId()
                            : "Company";

                    if (bidSpinner == null) {
                        bidSpinner = new JSpinner(new SpinnerNumberModel(5, 5, 10000, 5));
                        bidSpinner.setPreferredSize(new Dimension(65, 30));
                    }

                    net.sf.rails.game.specific._1817.action.Bid1817IPO bidAction = null;
                    NullAction passAction = null;

                    java.util.List<net.sf.rails.game.specific._1817.action.SettleIPO_1817> settleActions = new java.util.ArrayList<>();

                    if (possibleActions != null && possibleActions.getList() != null) {
                        for (PossibleAction pa : possibleActions.getList()) {
                            if (pa instanceof net.sf.rails.game.specific._1817.action.Bid1817IPO)
                                bidAction = (net.sf.rails.game.specific._1817.action.Bid1817IPO) pa;
                            else if (pa instanceof NullAction && ((NullAction) pa).getMode() == NullAction.Mode.PASS)
                                passAction = (NullAction) pa;
                            else if (pa instanceof net.sf.rails.game.specific._1817.action.SettleIPO_1817)
                                settleActions.add((net.sf.rails.game.specific._1817.action.SettleIPO_1817) pa);
                        }
                    }

                    if (!settleActions.isEmpty()) {
                        if (myTurn) {
                            int totalBid = settleActions.get(0).getCashAmount(); // Initially holds the full bid

                            JPanel settlePanel = new JPanel(new java.awt.BorderLayout(10, 5));
                            settlePanel.setOpaque(false);

                            JLabel settlePrompt = new JLabel(
                                    actorName + ", settle " + companyId + " IPO for $" + totalBid);
                            settlePrompt.setFont(new Font("SansSerif", Font.BOLD, 13));
                            settlePanel.add(settlePrompt, java.awt.BorderLayout.NORTH);

                            JPanel privatesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                            privatesPanel.setOpaque(false);

                            java.util.List<JCheckBox> privateBoxes = new java.util.ArrayList<>();
                            JLabel summaryLabel = new JLabel("Privates: $0 | Cash due: $" + totalBid);
                            summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                            summaryLabel.setForeground(new Color(153, 0, 0));

                            // Find owned privates

                            for (net.sf.rails.game.PrivateCompany pc : gameUIManager.getRoot().getCompanyManager()
                                    .getAllPrivateCompanies()) {
                                // Match the owner's name to the auction winner's name (actorName)
                                if (pc.getOwner() != null && pc.getOwner().getId().equals(actorName)
                                        && !pc.isClosed()) {
                                    int faceValue = pc.getBasePrice();
                                    JCheckBox cb = new JCheckBox(pc.getId() + " ($" + faceValue + ")");

                                    cb.putClientProperty("pc_id", pc.getId());
                                    cb.putClientProperty("pc_value", faceValue);
                                    cb.setOpaque(false);

                                    cb.addItemListener(e -> {
                                        int selectedValue = 0;
                                        for (JCheckBox box : privateBoxes) {
                                            if (box.isSelected()) {
                                                selectedValue += (Integer) box.getClientProperty("pc_value");
                                            }
                                        }
                                        int cashDue = Math.max(0, totalBid - selectedValue);
                                        summaryLabel
                                                .setText("Privates: $" + selectedValue + " | Cash due: $" + cashDue);
                                    });

                                    privateBoxes.add(cb);
                                    privatesPanel.add(cb);
                                }
                            }

                            JPanel centerPanel = new JPanel(new java.awt.BorderLayout());
                            centerPanel.setOpaque(false);
                            if (!privateBoxes.isEmpty()) {
                                centerPanel.add(new JLabel("Select Private Companies to use at face value:"),
                                        java.awt.BorderLayout.NORTH);
                                centerPanel.add(privatesPanel, java.awt.BorderLayout.CENTER);
                            }
                            centerPanel.add(summaryLabel, java.awt.BorderLayout.SOUTH);
                            settlePanel.add(centerPanel, java.awt.BorderLayout.CENTER);

                            JPanel buttonBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                            buttonBox.setOpaque(false);

                            for (final net.sf.rails.game.specific._1817.action.SettleIPO_1817 sa : settleActions) {
                                JButton settleButton = new JButton("Settle as " + sa.getShareSize() + "-Share");
                                settleButton.setPreferredSize(new Dimension(140, 35));

                                settleButton.addActionListener(e -> {
                                    int selectedValue = 0;
                                    java.util.List<String> selectedIds = new java.util.ArrayList<>();
                                    for (JCheckBox box : privateBoxes) {
                                        if (box.isSelected()) {
                                            selectedValue += (Integer) box.getClientProperty("pc_value");
                                            selectedIds.add((String) box.getClientProperty("pc_id"));
                                        }
                                    }
                                    int cashDue = Math.max(0, totalBid - selectedValue);

                                    sa.setPrivateCompanyIds(selectedIds);
                                    sa.setCashAmount(cashDue);

                                    process(sa);
                                });
                                buttonBox.add(settleButton);
                            }

                            settlePanel.add(buttonBox, java.awt.BorderLayout.EAST);

                            dynamicButtonPanel.add(settlePanel);
                        } else {
                            dynamicButtonPanel.add(
                                    new JLabel("Waiting for " + actorName + " to settle " + companyId + " IPO..."));
                        }
                    } else {

                        if (myTurn) {
                            int minBid = (bidAction != null) ? bidAction.getBidAmount() : 5;
                            ((SpinnerNumberModel) bidSpinner.getModel()).setMinimum(minBid);
                            bidSpinner.setValue(minBid);

                            JLabel bidPrompt = new JLabel(actorName + " bids: $");
                            bidPrompt.setFont(new Font("SansSerif", Font.PLAIN, 13));
                            dynamicButtonPanel.add(bidPrompt);
                            dynamicButtonPanel.add(bidSpinner);

                            if (auctionBidButton == null)
                                auctionBidButton = new JButton("Place Bid");
                            auctionBidButton.setPreferredSize(new Dimension(90, 28));

                            final net.sf.rails.game.specific._1817.action.Bid1817IPO finalBid = bidAction;
                            for (java.awt.event.ActionListener al : auctionBidButton.getActionListeners())
                                auctionBidButton.removeActionListener(al);
                            auctionBidButton.addActionListener(e -> {
                                if (finalBid != null) {
                                    finalBid.setBidAmount((Integer) bidSpinner.getValue());
                                    process(finalBid);
                                }
                            });
                            dynamicButtonPanel.add(auctionBidButton);

                            // if (auctionPassButton == null)
                            // auctionPassButton = new JButton("Pass");

                            // auctionPassButton.setPreferredSize(new Dimension(70, 28));
                            // auctionPassButton.setForeground(Color.RED);
                            // final NullAction finalPass = passAction;
                            // for (java.awt.event.ActionListener al :
                            // auctionPassButton.getActionListeners())
                            // auctionPassButton.removeActionListener(al);
                            // auctionPassButton.addActionListener(e -> process(finalPass));
                            // dynamicButtonPanel.add(auctionPassButton);
                        } else {
                            dynamicButtonPanel
                                    .add(new JLabel("Waiting for " + actorName + " to bid for " + companyId + "..."));
                        }
                    }

                    dynamicButtonPanel.revalidate();
                    dynamicButtonPanel.repaint();

                } else if (currentRound instanceof net.sf.rails.game.specific._1870.StockRound_1870) {
                    dynamicButtonPanel.setBackground(new Color(230, 240, 255));
                    dynamicButtonPanel.setOpaque(true);
                    dynamicButtonPanel.setBorder(BorderFactory.createLineBorder(SYS_BLUE, 1));
                    dynamicButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

                    boolean isProtecting = false;

                    if (possibleActions != null && possibleActions.getList() != null) {
                        for (PossibleAction pa : possibleActions.getList()) {

                            if (pa instanceof net.sf.rails.game.specific._1870.StockRound_1870.ProtectShare_1870) {
                                ActionButton btn = new ActionButton(null);
                                btn.setText("Protect Shares");
                                btn.setPossibleAction(pa);
                                btn.addActionListener(this);
                                styleStatusButton(btn, SYS_BLUE);
                                dynamicButtonPanel.add(btn);
                                isProtecting = true;

                            } else if (pa instanceof net.sf.rails.game.specific._1870.StockRound_1870.DeclineProtection_1870) {
                                ActionButton btn = new ActionButton(null);
                                btn.setText("Decline Protection");
                                btn.setPossibleAction(pa);
                                btn.addActionListener(this);
                                styleStatusButton(btn, Color.RED);
                                dynamicButtonPanel.add(btn);
                                isProtecting = true;
                            } else if (pa instanceof net.sf.rails.game.specific._1870.action.RedeemShare_1870) {
                                ActionButton btn = new ActionButton(null);
                                btn.setText(pa.getButtonLabel() != null ? pa.getButtonLabel()
                                        : pa.getClass().getSimpleName());
                                btn.setPossibleAction(pa);
                                btn.addActionListener(this);
                                styleStatusButton(btn, SYS_GREEN);
                                dynamicButtonPanel.add(btn);
                            } else if (pa instanceof net.sf.rails.game.specific._1870.action.ReissueShares_1870) {
                                ActionButton btn = new ActionButton(null);
                                btn.setText(pa.getButtonLabel() != null ? pa.getButtonLabel()
                                        : pa.getClass().getSimpleName());
                                btn.setPossibleAction(pa);
                                btn.addActionListener(this);
                                styleStatusButton(btn, SYS_RED);
                                dynamicButtonPanel.add(btn);
                            } else if (pa instanceof net.sf.rails.game.specific._1870.action.ExchangeMKT_1870) {
                                ActionButton btn = new ActionButton(null);
                                btn.setText(pa.getButtonLabel() != null ? pa.getButtonLabel()
                                        : pa.getClass().getSimpleName());
                                btn.setPossibleAction(pa);
                                btn.addActionListener(this);
                                styleStatusButton(btn, SYS_BLUE);
                                dynamicButtonPanel.add(btn);
                            }

                        }
                    }

                    // Shift to alert colors if a protection decision is pending
                    if (isProtecting) {
                        dynamicButtonPanel.setBackground(new Color(255, 230, 230));
                        dynamicButtonPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                    }

                    dynamicButtonPanel.setVisible(true);
                    dynamicButtonPanel.revalidate();
                    dynamicButtonPanel.repaint();

                } else if (currentRound instanceof net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817
                        && (((net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) currentRound)
                                .getCurrentStep() == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_AUCTION
                                ||
                                ((net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) currentRound)
                                        .getCurrentStep() == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_SELECT_BUYER)) {

                    net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817 maRound = (net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817) currentRound;
                    net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep step = maRound
                            .getCurrentStep();

                    dynamicButtonPanel.setBackground(new Color(230, 240, 255));
                    dynamicButtonPanel.setOpaque(true);
                    dynamicButtonPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
                    dynamicButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

                    String highBidderName = (maRound.getHighestBiddingPlayer() != null)
                            ? maRound.getHighestBiddingPlayer().getName()
                            : "None";

                    String companyId = (maRound.getOperatingCompany() != null) ? maRound.getOperatingCompany().getId()
                            : "Company";
                    String saleType = "friendly sale";
                    if (maRound.getOperatingCompany() != null && maRound.getOperatingCompany().hasStockPrice()) {
                        int price = maRound.getOperatingCompany().getMarketPrice();
                        if (price == 0)
                            saleType = "liquidation";
                        else if (price <= 30)
                            saleType = "acquisition";
                    }

                    JLabel highBidLabel = new JLabel(companyId + " (" + saleType + ") | High: " + highBidderName + " ($"
                            + maRound.getHighestBid() + ")");
                    highBidLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
                    highBidLabel.setForeground(new Color(0, 102, 204));
                    dynamicButtonPanel.add(highBidLabel);

                    JSeparator sep = new JSeparator(JSeparator.VERTICAL);
                    sep.setPreferredSize(new Dimension(2, 25));
                    dynamicButtonPanel.add(sep);

                    String actorName = (maRound.getActingPlayer() != null) ? maRound.getActingPlayer().getName()
                            : "Someone";
                    if (step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_SELECT_BUYER) {
                        actorName = highBidderName;
                    }

                    net.sf.rails.game.specific._1817.action.BidOnCompany_1817 bidAction = null;
                    NullAction passAction = null;

                    if (possibleActions != null && possibleActions.getList() != null) {
                        for (PossibleAction pa : possibleActions.getList()) {
                            if (pa instanceof net.sf.rails.game.specific._1817.action.BidOnCompany_1817)
                                bidAction = (net.sf.rails.game.specific._1817.action.BidOnCompany_1817) pa;
                            else if (pa instanceof NullAction && ((NullAction) pa).getMode() == NullAction.Mode.PASS)
                                passAction = (NullAction) pa;
                        }
                    }

                    if (step == net.sf.rails.game.specific._1817.MergerAndAcquisitionRound_1817.MaAStep.SALES_SELECT_BUYER) {
                        if (myTurn) {
                            dynamicButtonPanel
                                    .add(new JLabel(actorName + ", select company to purchase " + companyId + ":"));
                            if (possibleActions != null && possibleActions.getList() != null) {
                                for (PossibleAction pa : possibleActions.getList()) {
                                    if (pa instanceof net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817) {
                                        final net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817 sel = (net.sf.rails.game.specific._1817.action.SelectPurchasingCompany_1817) pa;
                                        JButton btn = new JButton(sel.getCompanyId());
                                        btn.addActionListener(e -> process(sel));
                                        dynamicButtonPanel.add(btn);
                                    }
                                }
                            }
                        } else {
                            dynamicButtonPanel.add(new JLabel("Waiting for " + actorName
                                    + " to select purchasing company for " + companyId + "..."));
                        }
                    } else {
                        if (myTurn) {
                            if (bidAction != null) {
                                if (bidSpinner == null) {
                                    bidSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 10000, 10));
                                    bidSpinner.setPreferredSize(new Dimension(65, 30));
                                }

                                int currentHighestBid = maRound.getHighestBid();
                                boolean hasBidder = maRound.getHighestBiddingPlayer() != null;
                                int minNextBid = (currentHighestBid == 0 && !hasBidder) ? currentHighestBid
                                        : currentHighestBid + 10;
                                if (currentHighestBid > 0 && !hasBidder)
                                    minNextBid = currentHighestBid;

                                int maxNextBid = bidAction.getMaxBid();

                                ((SpinnerNumberModel) bidSpinner.getModel()).setMinimum(minNextBid);
                                ((SpinnerNumberModel) bidSpinner.getModel()).setMaximum(maxNextBid);

                                // Always snap to the minimum valid bid when it becomes this player's turn to
                                // act
                                bidSpinner.setValue(minNextBid);

                                JLabel bidPrompt = new JLabel(actorName + " bids: $");
                                bidPrompt.setFont(new Font("SansSerif", Font.PLAIN, 13));
                                dynamicButtonPanel.add(bidPrompt);
                                dynamicButtonPanel.add(bidSpinner);

                                if (auctionBidButton == null)
                                    auctionBidButton = new JButton("Place Bid");
                                auctionBidButton.setPreferredSize(new Dimension(90, 28));

                                final net.sf.rails.game.specific._1817.action.BidOnCompany_1817 finalBid = bidAction;
                                for (java.awt.event.ActionListener al : auctionBidButton.getActionListeners())
                                    auctionBidButton.removeActionListener(al);
                                auctionBidButton.addActionListener(e -> {
                                    finalBid.setBidAmount((Integer) bidSpinner.getValue());
                                    process(finalBid);
                                });
                                dynamicButtonPanel.add(auctionBidButton);
                            } else {
                                JLabel noBidLabel = new JLabel("(No eligible companies to bid)");
                                noBidLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
                                noBidLabel.setForeground(Color.GRAY);
                                dynamicButtonPanel.add(noBidLabel);
                            }

                            // if (passAction != null) {
                            // if (auctionPassButton == null)
                            // auctionPassButton = new JButton("Pass");
                            // auctionPassButton.setPreferredSize(new Dimension(70, 28));
                            // auctionPassButton.setForeground(Color.RED);

                            // final NullAction finalPass = passAction;
                            // for (java.awt.event.ActionListener al :
                            // auctionPassButton.getActionListeners())
                            // auctionPassButton.removeActionListener(al);
                            // auctionPassButton.addActionListener(e -> process(finalPass));
                            // dynamicButtonPanel.add(auctionPassButton);
                            // }

                        } else {
                            dynamicButtonPanel
                                    .add(new JLabel("Waiting for " + actorName + " to bid for " + companyId + "..."));
                        }
                    }

                    dynamicButtonPanel.revalidate();
                    dynamicButtonPanel.repaint();

                } else {

                    boolean hasSpecialActions = false;
                    java.util.List<PossibleAction> specialActions = new java.util.ArrayList<>();
                    GuiTargetedAction contextProvider = null;

                    if (possibleActions != null && possibleActions.getList() != null
                            && "1817".equals(gameUIManager.getGameManager().getGameName())) {
                        for (PossibleAction pa : possibleActions.getList()) {
                            // Skip Short1817 as it is handled visually on the OSI grid
                            if (pa.getClass().getSimpleName().equals("Short1817")) {
                                continue;
                            }

                            if (pa instanceof GuiTargetedAction) {
                                specialActions.add(pa);
                                hasSpecialActions = true;
                                if (contextProvider == null)
                                    contextProvider = (GuiTargetedAction) pa;
                            }
                        }
                    }

                    if (hasSpecialActions) {
                        dynamicButtonPanel.setBackground(null);
                        dynamicButtonPanel.setOpaque(false);
                        dynamicButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                        dynamicButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

                        if (contextProvider != null) {
                            String actorName = (effectivePlayer != null) ? effectivePlayer.getName() : "Player";
                            JLabel promptLabel = new JLabel(
                                    "<html><b>" + actorName + "</b>: " + contextProvider.getGroupLabel() + "</html>");
                            promptLabel.setForeground(new Color(0, 102, 204));
                            dynamicButtonPanel.add(promptLabel);

                            for (PossibleAction spa : specialActions) {
                                GuiTargetedAction gta = (GuiTargetedAction) spa;
                                ActionButton btn = new ActionButton(null);
                                btn.setText(gta.getButtonLabel());
                                btn.setPossibleAction(spa);
                                btn.addActionListener(this);
                                dynamicButtonPanel.add(btn);
                            }
                        }
                    } else {
                        dynamicButtonPanel.setBackground(null);
                        dynamicButtonPanel.setOpaque(false);
                        dynamicButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                    }

                }
            }

            boolean passFound = false;
            boolean blockGlobalPass = false; // NEVER block the global pass button

            List<NullAction> inactiveItems = possibleActions.getType(NullAction.class);

            if (inactiveItems != null && !blockGlobalPass) {

                // if (inactiveItems != null) {
                for (NullAction na : inactiveItems) {
                    switch (na.getMode()) {
                        case PASS:
                            passButton.setRailsIcon(null);
                            passButton.setEnabled(true);
                            passButton.setActionCommand(PASS_CMD);
                            passButton.setMnemonic(KeyEvent.VK_P);
                            passButton.setPossibleAction(na);

                            // 1. Force Enable/Visible
                            passFound = true;
                            passButton.setEnabled(true);
                            passButton.setVisible(true);

                            // 2. Contextual Text
                            if (currentRound instanceof net.sf.rails.game.specific._1835.PrussianFormationRound) {
                                passButton.setText("Decline Exchange");
                                styleStatusButton(passButton, SYS_BLUE); // Exception: Keep Blue for "Decision"
                            } else {
                                passButton.setText("Pass");
                                styleStatusButton(passButton, SYS_BLUE);
                            }

                            break;
                        case DONE:
                            passButton.setRailsIcon(null);
                            passButton.setEnabled(true);
                            passButton.setActionCommand(DONE_CMD);
                            passButton.setMnemonic(KeyEvent.VK_D);
                            passButton.setPossibleAction(na);

                            passFound = true;
                            passButton.setEnabled(true);
                            passButton.setVisible(true);

                            passButton.setText("Done");
                            styleStatusButton(passButton, SYS_BLUE); // Standard: Blue

                            break;
                        case AUTOPASS:
                            autopassButton.setEnabled(true);
                            autopassButton.setPossibleAction(na);
                            break;
                        default:
                            break;
                    }
                }
            }

            // Only disable if we genuinely found no action
            if (!passFound) {
                passButton.setEnabled(false);
            }

            // "Default Move" Logic: Map the main "Done/Pass" button to the most logical
            // next step
            // Labels: "Pay out" (Rev), "Skip" (Tile/Token/Train), "Finished" (OR End),
            // "Done" (SR)

            boolean defaultActionHandled = false;

            // 1. Analyze Phase Context
            boolean hasLayTile = false;
            boolean hasLayToken = false;
            boolean hasBuyTrain = false;

            if (possibleActions != null && possibleActions.getList() != null) {
                for (PossibleAction pa : possibleActions.getList()) {
                    if (pa instanceof LayTile)
                        hasLayTile = true;
                    else if (pa instanceof LayToken)
                        hasLayToken = true;
                    else if (pa instanceof BuyTrain)
                        hasBuyTrain = true;
                }
            }

            // ... (lines of unchanged context code) ...
            // 2. REVENUE PHASE ("Pay out" / "Split" / "Withhold")
            List<SetDividend> revenueActions = possibleActions.getType(SetDividend.class);
            if (revenueActions != null && !revenueActions.isEmpty()) {
                SetDividend sourceAction = revenueActions.get(0);
                SetDividend bestOption = null;

                // Priority: PAYOUT -> SPLIT -> WITHHOLD -> ANY
                if (sourceAction.isAllocationAllowed(SetDividend.PAYOUT)) {
                    bestOption = (SetDividend) sourceAction.clone();
                    bestOption.setRevenueAllocation(SetDividend.PAYOUT);
                } else if (sourceAction.isAllocationAllowed(SetDividend.SPLIT)) {
                    bestOption = (SetDividend) sourceAction.clone();
                    bestOption.setRevenueAllocation(SetDividend.SPLIT);
                } else if (sourceAction.isAllocationAllowed(SetDividend.WITHHOLD)) {
                    bestOption = (SetDividend) sourceAction.clone();
                    bestOption.setRevenueAllocation(SetDividend.WITHHOLD);
                } else {
                    int[] allowed = sourceAction.getAllowedRevenueAllocations();
                    if (allowed != null && allowed.length > 0) {
                        bestOption = (SetDividend) sourceAction.clone();
                        bestOption.setRevenueAllocation(allowed[0]);
                    }
                }

                if (bestOption != null) {
                    passButton.setEnabled(true);
                    passButton.setVisible(true);
                    passButton.setRailsIcon(null);
                    passButton.setActionCommand(DONE_CMD);
                    passButton.setPossibleAction(bestOption);

                    // Critical: Set Text AFTER setPossibleAction
                    int alloc = bestOption.getRevenueAllocation();

                    if (alloc == SetDividend.PAYOUT) {
                        passButton.setText("Pay out");
                    } else if (alloc == SetDividend.SPLIT) {
                        passButton.setText("Split");
                    } else {
                        passButton.setText("Withhold");
                    }

                    // Unified Blue Color (as requested)
                    styleStatusButton(passButton, SYS_BLUE);
                    defaultActionHandled = true;
                }
            }

            // 3. OTHER PHASES ("Pass" / "Done" / "Skip" / "Finished")
            if (!defaultActionHandled) {
                List<NullAction> nullActions = possibleActions.getType(NullAction.class);
                if (nullActions != null && !nullActions.isEmpty()) {
                    NullAction bestNull = null;

                    boolean isStock = (currentRound instanceof net.sf.rails.game.financial.StockRound);
                    boolean isStepPhase = (hasLayTile || hasLayToken); // Phases where we typically "Skip" steps

                    for (NullAction na : nullActions) {
                        if (na.getMode() == NullAction.Mode.PASS || na.getMode() == NullAction.Mode.DONE) {
                            if (bestNull == null) {
                                bestNull = na;
                            } else {
                                if (isStock) {
                                    // Stock Round: Prefer PASS
                                    if (na.getMode() == NullAction.Mode.PASS)
                                        bestNull = na;
                                } else if (isStepPhase) {
                                    // OR Step (Build/Token): Prefer PASS (Skip Step)
                                    if (na.getMode() == NullAction.Mode.PASS)
                                        bestNull = na;
                                } else {
                                    // OR Final (Train/End): Prefer DONE (End Turn)
                                    if (na.getMode() == NullAction.Mode.DONE)
                                        bestNull = na;
                                }
                            }
                        }
                    }

                    if (bestNull != null) {
                        passButton.setEnabled(true);
                        passButton.setVisible(true);
                        passButton.setRailsIcon(null);
                        passButton.setActionCommand(passButton.getActionCommand());
                        passButton.setPossibleAction(bestNull);

                        String label = "Done"; // Default fallback

                        if (currentRound instanceof net.sf.rails.game.financial.StockRound) {
                            // Stock Round: Explicitly map PASS to "Pass"
                            if (bestNull.getMode() == NullAction.Mode.PASS) {
                                label = "Pass";
                            } else {
                                label = "Done";
                            }
                        } else {
                            if (hasLayTile) {
                                label = "Skip Build";
                            } else if (hasLayToken) {
                                label = "Skip Token";
                            } else if (hasBuyTrain) {
                                label = "Skip Buy"; // User Request: Always "Skip Buy" if purchase is possible
                            } else {
                                label = "End Turn";
                            }
                        }

                        passButton.setText(label);
                        styleStatusButton(passButton, SYS_BLUE);
                        defaultActionHandled = true;
                    }
                }
            }
            // ... (rest of the method) ...

            gameStatus.setBackground(UIManager.getColor("Panel.background"));
            gameStatus.setOpaque(false);
            gameStatus.repaint();

            if (currentRound instanceof EndOfGameRound)
                endOfGame();

            gameUIManager.packAndApplySizing(this);
            enforceDynamicMinimumSize();

        } catch (Exception e) {
            log.error("CRITICAL ERROR in StatusWindow.updateStatus", e);
        }
    }

    /**
     * Hook for subclasses (e.g., StatusWindow_1835) to apply specific highlighting
     * without polluting the generic parent class.
     */
    protected void updateGameSpecificHighlights() {
        // Default implementation does nothing.
    }

    public void updateActivityPanel(String text) {
        return;
    }

    public void updateInfoMenu() {
        if (gameUIManager == null || gameUIManager.getGameManager() == null)
            return;

        // 1. Remaining Tiles (Always present)
        // (Handled in initMenu, static item)

        // 2. Build Dynamic Sub-Menus
        buildCompaniesMenu();
        buildTrainsMenu();
        buildPhasesMenu();
        buildNetworkMenu();
    }

    protected void buildCompaniesMenu() {
        companiesMenu.removeAll();

        // Add Market Shortcut
        JMenuItem marketItem = new JMenuItem(LocalText.getText("StockMarket", "Stock Market"));
        marketItem.setActionCommand(MARKET_CMD);
        marketItem.addActionListener(this);
        companiesMenu.add(marketItem);
        companiesMenu.addSeparator();

        // Build Hierarchical Menu
        CompanyManager cm = gameUIManager.getGameManager().getRoot().getCompanyManager();
        List<CompanyType> comps = cm.getCompanyTypes();
        JMenu menu, item;

        for (CompanyType type : comps) {
            menu = new JMenu(LocalText.getText(type.getId()));
            companiesMenu.add(menu);

            for (Company comp : type.getCompanies()) {
                item = new JMenu(comp.getId());
                JMenuItem menuItem = new JMenuItem(comp.getInfoText());

                // Add Highlighting Listener if ORUIManager is available
                // (Allows hovering over menu to light up tokens on map)
                if (gameUIManager.getORUIManager() != null) {
                    if (comp instanceof PrivateCompany) {
                        HexHighlightMouseListener.addMouseListener(menuItem,
                                gameUIManager.getORUIManager(), (PrivateCompany) comp, true);
                        HexHighlightMouseListener.addMouseListener(item,
                                gameUIManager.getORUIManager(), (PrivateCompany) comp, true);
                    } else if (comp instanceof PublicCompany) {
                        HexHighlightMouseListener.addMouseListener(menuItem,
                                gameUIManager.getORUIManager(), (PublicCompany) comp, true);
                        HexHighlightMouseListener.addMouseListener(item,
                                gameUIManager.getORUIManager(), (PublicCompany) comp, true);
                    }
                }

                item.add(menuItem);
                menu.add(item);
            }
        }
    }

    protected void buildTrainsMenu() {
        trainsMenu.removeAll();

        // Add Report Shortcut
        JMenuItem reportItem = new JMenuItem(LocalText.getText("TrainReport", "Train Report"));
        reportItem.setActionCommand(REPORT_CMD);
        reportItem.addActionListener(this);
        trainsMenu.add(reportItem);
        trainsMenu.addSeparator();

        TrainManager tm = gameUIManager.getRoot().getTrainManager();
        List<TrainType> types = tm.getTrainTypes();
        JMenu item;

        for (TrainType type : types) {
            item = new JMenu(LocalText.getText("N_Train", type.getName()));
            item.add(new JMenuItem(type.getInfo()));
            trainsMenu.add(item);
        }
    }

    protected void buildPhasesMenu() {
        phasesMenu.removeAll();

        PhaseManager pm = gameUIManager.getRoot().getPhaseManager();
        List<Phase> phases = pm.getPhases();
        JMenu item;
        StringBuffer b = new StringBuffer("<html>");

        for (Phase phase : phases) {
            b.setLength(6);
            appendInfoText(b, LocalText.getText("PhaseTileColours", phase.getTileColoursString()));
            appendInfoText(b, LocalText.getText("PhaseNumberOfORs", phase.getNumberOfOperatingRounds()));
            appendInfoText(b, LocalText.getText("PhaseOffBoardStep", phase.getOffBoardRevenueStep()));
            appendInfoText(b, LocalText.getText("PhaseTrainLimitStep", phase.getTrainLimitStep()));
            if (phase.doPrivatesClose()) {
                appendInfoText(b, LocalText.getText("PhaseClosesAllPrivates"));
            }
            if (phase.getClosedObjects() != null) {
                for (Closeable object : phase.getClosedObjects()) {
                    if (Util.hasValue(object.getClosingInfo())) {
                        appendInfoText(b,
                                LocalText.getText("PhaseRemoves", Util.lowerCaseFirst(object.getClosingInfo())));
                    }
                }
            }
            if (Util.hasValue(phase.getInfo())) {
                appendInfoText(b, phase.getInfo());
            }

            // Mark current phase

            boolean isCurrent = phase.equals(gameUIManager.getRoot().getPhaseManager().getCurrentPhase());
            String prefix = isCurrent ? "> " : "";
            item = new JMenu(prefix + LocalText.getText("PhaseX", phase.toText()));
            item.add(new JMenuItem(b.toString()));
            phasesMenu.add(item);
        }

        phasesMenu.addSeparator();
        JMenuItem detailsItem = new JMenuItem("Show Details...");
        detailsItem.setActionCommand(SHOW_PHASES_CMD);
        detailsItem.addActionListener(this);
        phasesMenu.add(detailsItem);
    }

    protected void buildNetworkMenu() {
        networkMenu.removeAll();

        JMenuItem refreshItem = new JMenuItem(LocalText.getText("RefreshNetwork", "Refresh Network"));
        refreshItem.setActionCommand(REFRESH_NETWORK_CMD);
        refreshItem.addActionListener(this);
        networkMenu.add(refreshItem);
        networkMenu.addSeparator();

        boolean route_highlight = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.ROUTE_HIGHLIGHT);
        boolean revenue_suggest = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.REVENUE_SUGGEST);

        if (!route_highlight && !revenue_suggest)
            return;

        // Developer Graph
        if (route_highlight && Config.isDevelop()) {
            JMenuItem item = new JMenuItem("Network");
            item.addActionListener(this);
            item.setActionCommand(REFRESH_NETWORK_CMD); // Re-uses the refresh command to trigger listener logic
            // Note: We handle the specific "Network" string check in execution
            networkMenu.add(item);
        }

        if (revenue_suggest) {
            CompanyManager cm = gameUIManager.getGameManager().getRoot().getCompanyManager();
            for (PublicCompany comp : cm.getAllPublicCompanies()) {
                if (!comp.hasFloated() || comp.isClosed())
                    continue;
                JMenuItem item = new JMenuItem(comp.getId());
                item.addActionListener(this);
                // We use a custom action command or handle via item text
                item.setActionCommand("SHOW_NETWORK_GRAPH");
                networkMenu.add(item);
            }
        }
    }

    private void appendInfoText(StringBuffer b, String text) {
        if (text == null || text.length() == 0)
            return;
        if (b.length() > 6)
            b.append("<br>");
        b.append(text);
    }

    protected void executeNetworkInfo(String companyName) {
        RailsRoot root = gameUIManager.getRoot();

        // Safety check: Needs ORUIManager for map interaction
        if (gameUIManager.getORUIManager() == null || gameUIManager.getORUIManager().getMap() == null) {
            log.info("Cannot display Network Graph: Map is not initialized.");
            return;
        }

        if (companyName.equals("Network")) {
            NetworkAdapter network = NetworkAdapter.create(root);
            NetworkGraph mapGraph = network.getMapGraph();
            mapGraph.optimizeGraph();
            JFrame mapWindow = mapGraph.visualize("Optimized Map Network");
            // StatusWindow doesn't track open windows to close them, relying on user to
            // close
        } else {
            CompanyManager cm = root.getCompanyManager();
            PublicCompany company = cm.getPublicCompany(companyName);
            if (company == null)
                return;

            if (Config.getBoolean("map.route.window.display", true)) {
                NetworkAdapter network = NetworkAdapter.create(root);
                NetworkGraph routeGraph = network.getRevenueGraph(company, new ArrayList<>());
                routeGraph.visualize("Route Network for " + company);
            }

            // Calculate Revenue and Show Dialog
            RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(root, company,
                    root.getPhaseManager().getCurrentPhase());
            ra.initRevenueCalculator(true);
            int revenueValue = ra.calculateRevenue();

            try {
                ra.drawOptimalRunAsPath(gameUIManager.getORUIManager().getMap());
            } catch (Exception e) {
            }

            JOptionPane.showMessageDialog(this,
                    LocalText.getText("NetworkInfoDialogMessage", company.getId(),
                            gameUIManager.format(revenueValue)),
                    LocalText.getText("NetworkInfoDialogTitle", company.getId()),
                    JOptionPane.INFORMATION_MESSAGE);

            // Cleanup paths after dialog closes
            gameUIManager.getORUIManager().getMap().setTrainPaths(null);
        }
    }

    private void showHotkeysDialog() {

        String msg = "<html><h3>Keyboard Shortcuts</h3>" +
                "<table border='0' cellpadding='4'>" +
                "<tr><td><b>Key</b></td><td><b>Action</b></td></tr>" +
                "<tr><td colspan='2'><hr></td></tr>" +

                "<tr><td colspan='2'><b>Global / Interface</b></td></tr>" +
                "<tr><td><b>A</b></td><td>AI Move (Executes the AI logic for the current step)</td></tr>" +
                "<tr><td><b>T</b></td><td>Toggle Timer (Pauses/Resumes the game timer)</td></tr>" +
                "<tr><td><b>Space</b> or <b>Enter</b></td><td>Done / Confirm (Presses the 'Done' button)</td></tr>" +
                "<tr><td><b>Cmd/Ctrl + Z</b></td><td>Undo</td></tr>" +
                "<tr><td><b>Cmd/Ctrl + Y</b></td><td>Redo</td></tr>" +
                "<tr><td><b>Cmd/Ctrl + +/-</b></td><td>Increase / Decrease Font Size</td></tr>" +

                "<tr><td colspan='2'><br><b>Operating Round (Map & Tiles)</b></td></tr>" +
                "<tr><td><b>S / D</b></td><td>Cycle available tiles for the selected hex (Previous / Next)</td></tr>" +
                "<tr><td><b>E</b></td><td>Cycle tile upgrades (if multiple tile types fit the hex)</td></tr>" +
                "<tr><td><b>R</b></td><td>Rotate the currently selected tile on the map</td></tr>" +
                "<tr><td><b>L</b></td><td>Buy Train (Auto-selects the cheapest available train from IPO/Pool)</td></tr>"
                +
                "<tr><td><b>Space bar</b></td><td>Toggle Tile Numbers (Show/Hide build numbers on the map)</td></tr>" +

                "<tr><td colspan='2'><br><b>Operating Round (Revenue)</b></td></tr>" +
                "<tr><td><b>P</b></td><td>Payout Revenue</td></tr>" +
                "<tr><td><b>H</b></td><td>Half / Split Revenue</td></tr>" +
                "<tr><td><b>W</b></td><td>Withhold Revenue</td></tr>" +
                "</table></html>";
        JOptionPane.showMessageDialog(this, msg, "Hotkeys", JOptionPane.INFORMATION_MESSAGE);
    }

    private void highlightRailCard(net.sf.rails.game.Company company, PossibleAction action, String actionName) {
        net.sf.rails.ui.swing.elements.RailCard card = findRailCardRecursive(gameStatus, company);

        if (card != null) {

            // Visual: Color Code based on Action
            applyActionColor(card, action);

            // Functional: Attach Action
            card.setPossibleAction(action);
            // Listen
            card.removeActionListener(this);
            card.addActionListener(this);

            card.setToolTipText("<html><b>Click to " + actionName + "</b><br>" + company.getId() + "</html>");
            card.repaint();
        } else {
        }
    }

    private net.sf.rails.ui.swing.elements.RailCard findRailCardRecursive(Container parent,
            net.sf.rails.game.Company target) {
        if (parent == null)
            return null;

        for (Component c : parent.getComponents()) {
            if (c instanceof net.sf.rails.ui.swing.elements.RailCard) {
                net.sf.rails.ui.swing.elements.RailCard card = (net.sf.rails.ui.swing.elements.RailCard) c;

                // Debug every card we check to see if we find M2
                // We use getId() for safer comparison than object equality
                String cardText = card.getText();
                String targetId = target.getId();

                // 1. Check Direct Company
                if (card.getCompany() != null && card.getCompany().getId().equals(targetId)) {
                    return card;
                }

                // 2. Check Certificates (Crucial for "Owner" card in Player Table)
                for (net.sf.rails.game.financial.Certificate cert : card.getCertificates()) {
                    if (cert instanceof net.sf.rails.game.financial.PublicCertificate) {
                        if (((net.sf.rails.game.financial.PublicCertificate) cert).getCompany().getId()
                                .equals(targetId)) {
                            return card;
                        }
                    } else if (cert instanceof net.sf.rails.game.PrivateCompany) {
                        if (cert.getId().equals(targetId)) {
                            return card;
                        }
                    }
                }

            } else if (c instanceof Container) {
                net.sf.rails.ui.swing.elements.RailCard found = findRailCardRecursive((Container) c, target);
                if (found != null)
                    return found;
            }
        }

        // --- DELETE ---
        /*
         * if (container instanceof net.sf.rails.ui.swing.elements.RailCard) {
         * net.sf.rails.ui.swing.elements.RailCard card =
         * (net.sf.rails.ui.swing.elements.RailCard) container;
         * log.
         * info("[DEBUG-SEARCH] Inspecting RailCard. Text: '{}'. Attached Company: {}",
         * card.getText(),
         * (card.getCompany() != null ? card.getCompany().getId() : "NULL"));
         * }
         */

        return null;
    }

    /**
     * Protected helper to allow subclasses (like 1835) to find specific cards
     * to apply highlighting logic.
     */
    protected net.sf.rails.ui.swing.elements.RailCard findRailCardRecursive(Container parent,
            net.sf.rails.game.Company targetCompany,
            net.sf.rails.game.financial.Certificate targetCert) {

        if (parent == null)
            return null;

        for (Component c : parent.getComponents()) {
            if (c instanceof net.sf.rails.ui.swing.elements.RailCard) {
                net.sf.rails.ui.swing.elements.RailCard card = (net.sf.rails.ui.swing.elements.RailCard) c;

                // 1. Match by Certificate Object (Most Robust)
                if (targetCert != null && card.holdsCertificate(targetCert)) {
                    return card;
                }

                // 2. Match by Company ID (Fallback)
                if (targetCompany != null && card.getCompany() != null &&
                        card.getCompany().getId().equals(targetCompany.getId())) {
                    return card;
                }
            } else if (c instanceof Container) {
                net.sf.rails.ui.swing.elements.RailCard found = findRailCardRecursive((Container) c, targetCompany,
                        targetCert);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private void highlightRailCard(net.sf.rails.game.Company company,
            net.sf.rails.game.financial.Certificate specificCert,
            PossibleAction action, String actionName) {

        // Use the new finder signature
        net.sf.rails.ui.swing.elements.RailCard card = findRailCardRecursive(gameStatus, company, specificCert);

        // Fallback: Direct Grid Lookup via GameStatus if recursive search failed
        if (card == null && gameStatus != null && company instanceof PublicCompany && specificCert != null) {
            if (specificCert.getOwner() instanceof Player) {
                int pIdx = ((Player) specificCert.getOwner()).getIndex();
                int cIdx = ((PublicCompany) company).getPublicNumber();

                card = gameStatus.getRailCardFor(cIdx, pIdx);

            }
        }

        if (card != null) {
            applyActionColor(card, action);
            card.setPossibleAction(action);
            card.removeActionListener(this);
            card.addActionListener(this);
            card.setToolTipText("<html><b>Click to " + actionName + "</b><br>" + company.getId() + "</html>");
            card.repaint();
        } else {
        }
    }

    /**
     * WATERTIGHT SOLUTION:
     * A generic, robust method to find a specific certificate on screen,
     * highlight it, and attach an action to it.
     * * @param targetCert The specific certificate object to find.
     * 
     * @param action  The action to execute when clicked.
     * @param tooltip (Optional) Text to show on hover.
     * @return true if the card was found and highlighted.
     */
    public boolean attachActionToCertificate(net.sf.rails.game.financial.Certificate targetCert,
            PossibleAction action,
            String tooltip) {
        if (targetCert == null || action == null)
            return false;

        // 1. Find the Company associated with the cert
        net.sf.rails.game.Company company = null;
        if (targetCert instanceof net.sf.rails.game.financial.PublicCertificate) {
            company = ((net.sf.rails.game.financial.PublicCertificate) targetCert).getCompany();
        } else if (targetCert instanceof net.sf.rails.game.PrivateCompany) {
            company = (net.sf.rails.game.Company) targetCert;
        }

        // 2. Use our Robust Recursive Finder
        // (Reusing the existing findRailCardRecursive logic we built)
        net.sf.rails.ui.swing.elements.RailCard card = findRailCardRecursive(gameStatus, company, targetCert);

        // 3. Fallback: Grid Lookup (The "Fast Fix" logic, formalized)
        if (card == null && gameStatus != null && company instanceof PublicCompany) {
            if (targetCert.getOwner() instanceof Player) {
                int pIdx = ((Player) targetCert.getOwner()).getIndex();
                int cIdx = ((PublicCompany) company).getPublicNumber();
                card = gameStatus.getRailCardFor(cIdx, pIdx);
            }
        }

        // 4. Apply the Visuals
        if (card != null) {
            applyActionColor(card, action);
            card.setPossibleAction(action);

            // Clean listeners to prevent double-clicks
            card.removeActionListener(this);
            card.addActionListener(this);

            if (tooltip != null) {
                card.setToolTipText("<html><b>" + tooltip + "</b><br>" + company.getId() + "</html>");
            }
            card.repaint();
            return true;
        }

        return false;
    }

    private void applyActionColor(net.sf.rails.ui.swing.elements.RailCard card, PossibleAction action) {
        if (card == null)
            return;

        // Default Highlight (Generic Selection)
        Color borderColor = SYS_BLUE;
        Color bgColor = BG_BLUE;

        if (action instanceof SellShares) {
            // SELLING -> RED (Destructive)
            borderColor = SYS_RED;
            bgColor = BG_RED;

        } else if (action instanceof DiscardTrain) {
            // SCRAPPING -> CYAN + BEIGE (Special / Mandatory Action)
            borderColor = SYS_CYAN;
            bgColor = BG_BEIGE;

        } else if (action instanceof BuyCertificate || action instanceof BuyTrain || action instanceof BuyPrivate) {
            // BUYING -> GREEN (Acquisition)
            borderColor = SYS_GREEN;
            bgColor = BG_GREEN;
        }

        // Apply State & Colors
        card.setState(net.sf.rails.ui.swing.elements.RailCard.State.HIGHLIGHTED);
        card.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        card.setOpaque(true);
        card.setBackground(bgColor);
        card.repaint();
    }

    private void showMoveMonitor() {
        if (moveMonitor == null) {
            moveMonitor = new MoveMonitor(gameUIManager);
        }
        moveMonitor.setVisible(true);
        moveMonitor.refresh();
    }

    private class MoveMonitor extends JFrame {
        private JTextArea area;
        private GameUIManager gui;

        public MoveMonitor(GameUIManager gui) {
            super("Engine Move Monitor");
            this.gui = gui;

            // Make Sticky (Always on top)
            setAlwaysOnTop(true);
            setLayout(new BorderLayout());

            area = new JTextArea();
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.BOLD, 13));
            area.setBackground(new Color(30, 30, 30)); // Dark background
            area.setForeground(new Color(0, 255, 0)); // Matrix Green

            add(new JScrollPane(area), BorderLayout.CENTER);
            setSize(600, 500);
        }

        public void refresh() {
            GameManager gm = gui.getGameManager();
            if (gm == null)
                return;

            StringBuilder sb = new StringBuilder();
            int move = gm.getActionCountModel().value();
            Player p = gm.getCurrentPlayer();
            RoundFacade r = gm.getCurrentRound();

            sb.append("==================================================\n");
            sb.append(String.format(" MOVE: #%-5d | PLAYER: %s\n", move, (p != null ? p.getName() : "None")));
            sb.append(String.format(" ROUND: %-13s | CLASS: %s\n",
                    (r != null ? r.getId() : "None"),
                    (r != null ? r.getClass().getSimpleName() : "N/A")));

            // --- START FIX: DETECT COMPANY VIA REFLECTION ---
            // Previously: only checked (r instanceof OperatingRound)
            // Now: Checks any round for "getOperatingCompany" method
            String companyId = "None";
            if (r != null) {
                try {
                    // Try to find the method on whatever round class this is (CER, OR, etc.)
                    java.lang.reflect.Method method = r.getClass().getMethod("getOperatingCompany");
                    Object result = method.invoke(r);
                    if (result instanceof PublicCompany) {
                        companyId = ((PublicCompany) result).getId();
                    }
                } catch (Exception e) {
                    // Method not found or accessible, ignore
                }
            }
            sb.append(String.format(" ACTIVE COMPANY (Reflect): %s\n", companyId));
            sb.append("==================================================\n\n");

            sb.append(String.format(" %-20s | %-10s | %s\n", "ACTION TYPE", "OWNER", "DETAILS"));
            sb.append(" ---------------------+------------+----------------------\n");

            List<PossibleAction> actions = gm.getPossibleActions().getList();
            if (actions != null) {
                for (PossibleAction pa : actions) {
                    if (pa instanceof GameAction || pa instanceof CorrectionModeAction || pa.isCorrection()) {
                        continue;
                    }

                    // HOUSE RULE: Hide "Sell Short" buttons from the sidebar.
                    // They are now handled by the OSI grid with the Green Border.
                    if (pa.getClass().getName().endsWith("Short1817")) {
                        continue;
                    }

                    // --- START FIX: SHOW ACTION OWNER ---
                    String owner = "-";
                    // Try standard "company" field if available (PossibleORAction)
                    if (pa instanceof rails.game.action.PossibleORAction) {
                        PublicCompany c = ((rails.game.action.PossibleORAction) pa).getCompany();
                        if (c != null)
                            owner = c.getId();
                    }
                    // Fallback: Check for getPlayer()
                    else if (pa instanceof rails.game.action.PossibleAction) {
                        Player pl = pa.getPlayer();
                        if (pl != null)
                            owner = pl.getName();
                    }

                    sb.append(String.format(" %-20s | %-10s | %s\n",
                            pa.getClass().getSimpleName(),
                            owner,
                            pa.toString()));
                }
            }
            area.setText(sb.toString());
            area.setCaretPosition(0);
        }

    }

    private void showActionRunner() {
        if (actionRunner == null) {
            actionRunner = new ActionRunner();
        }
        actionRunner.setVisible(true);
        actionRunner.refresh();
    }

    private class ActionRunner extends JFrame {
        private static final long serialVersionUID = 1L;
        private JPanel buttonPanel;

        public ActionRunner() {
            super("Action Runner (Debug Force)");

            // Layout: Scrollable Vertical List
            setLayout(new BorderLayout());
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

            JScrollPane scrollPane = new JScrollPane(buttonPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
            add(scrollPane, BorderLayout.CENTER);

            setSize(400, 600);
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }

        public void refresh() {
            buttonPanel.removeAll();

            // Safety Check
            if (possibleActions == null || possibleActions.getList() == null) {
                buttonPanel.revalidate();
                buttonPanel.repaint();
                return;
            }

            for (final PossibleAction pa : possibleActions.getList()) {

                if (pa instanceof GameAction || pa instanceof rails.game.correct.CorrectionAction
                        || pa.isCorrection()) {
                    continue;
                }

                // Create a button for every single action
                JButton btn = new JButton(pa.toString());
                btn.setAlignmentX(Component.LEFT_ALIGNMENT);
                btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 30)); // Full width

                // Color Code "NullActions" (Pass/Done) vs Real Moves
                if (pa instanceof NullAction) {
                    btn.setForeground(SYS_BLUE);
                }

                btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // FORCE THE MOVE
                        log.info("ActionRunner: Forcing action -> {}", pa);
                        gameUIManager.processAction(pa);
                    }
                });

                buttonPanel.add(btn);
            }

            buttonPanel.revalidate();
            buttonPanel.repaint();
        }
    }

    private void saveJsonState() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save JSON State");
        fileChooser.setCurrentDirectory(new File(Config.get("save.directory", System.getProperty("user.dir"))));
        if (fileChooser.showSaveDialog(StatusWindow.this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().endsWith(".json")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
            }
            final File finalFile = fileToSave;
            new Thread(() -> {
                try {
                    net.sf.rails.game.ai.snapshot.JsonStateSerializer.serialize(gameUIManager.getGameManager(),
                            finalFile.getAbsolutePath());
                    log.info("Successfully saved JSON state to {}", finalFile.getAbsolutePath());
                } catch (Exception ex) {
                    log.error("Failed to save JSON state", ex);
                    JOptionPane.showMessageDialog(StatusWindow.this, "Failed to save JSON: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }).start();
        }
    }

    /**
     * A translucent overlay to display a massive "GAME PAUSED" text across the
     * entire window.
     */
    private class PauseOverlay extends JComponent {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean contains(int x, int y) {
            // CRITICAL: Let all mouse clicks pass through so the user can still click
            // "Resume"
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            // 1. Semi-transparent dark background to dim the game
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 2. Setup Font
            String text = "GAME PAUSED";
            g2d.setFont(new Font("SansSerif", Font.BOLD, 60));
            FontMetrics fm = g2d.getFontMetrics();

            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

            // 3. Draw Shadow
            g2d.setColor(Color.BLACK);
            g2d.drawString(text, x + 4, y + 4);

            // 4. Draw Text
            g2d.setColor(SYS_RED);
            g2d.drawString(text, x, y);

            g2d.dispose();
        }
    }

    // We are modifying updateFontsFromConfig to treat the zoom value strictly as a
    // percentage integer (50-200)
    /**
     * Reads the updated font values directly from memory configuration maps
     * and refreshes the application UI layout dynamically.
     */
    public void updateFontsFromConfig() {
        try {
            // 1. Resolve standard UI font family settings
            String fontName = net.sf.rails.common.Config.get("font.ui.name");
            if (fontName == null || fontName.trim().isEmpty()) {
                fontName = "SansSerif";
            }



        } catch (Exception e) {
            log.error("Failed to dynamically propagate configuration fonts", e);
        }
    }

    /**
     * Safely synchronizes the StatusWindow UI with the latest configuration values
     * without triggering a full game state updateStatus() pass.
     */
    public void refreshConfigState() {
        // 1. Pull the authoritative configuration value
        this.showPlayerWorth = Util.parseBoolean(net.sf.rails.common.Config.get("statusWindow.showPlayerWorth"));
        
        // 2. Visually update the menu tick box
        if (this.showPlayerWorth) {
            enableCheckBoxMenuItem("ShowPlayerWorth");
        } else {
            disableCheckBoxMenuItem("ShowPlayerWorth");
        }
        
        // 3. Rebuild the dashboard to show/hide the worth data
        if (gameStatus != null) {
            gameStatus.recreate();
        }
        
        // 4. Update fonts (handles size scaling changes)
        updateFontsFromConfig();
    }

    private void updateFonts(float baseSize) {
        // Safe tracking fallback for Hotkey step zooming (+/- actions)
        String standardFamily = net.sf.rails.common.Config.get("font.ui.name");
        if (standardFamily == null || standardFamily.trim().isEmpty()) {
            standardFamily = net.sf.rails.common.Config.get("font.name");
        }
        if (standardFamily == null || standardFamily.trim().isEmpty()) {
            standardFamily = "SansSerif";
        }
        updateFonts(standardFamily, baseSize);
    }

    private void updateFonts(String standardFamily, float baseSize) {
        this.currentBaseFontSize = baseSize;

        // 1. Resolve standard display family and custom money font settings from Config
        // Manager maps
        String moneyFamily = net.sf.rails.common.Config.get("font.currency");
        if (moneyFamily == null || moneyFamily.trim().isEmpty()) {
            moneyFamily = "Monospaced"; // Retain default compatibility fallback
        }

        Font baseFont = new Font(standardFamily, Font.PLAIN, (int) baseSize);

        // Push configuration configurations downstream onto your custom game table
        // layout
        if (gameStatus != null) {
            gameStatus.setFont(baseFont);
            updateComponentTreeFont(gameStatus, baseFont);
            gameStatus.recreate(); // Force layout metrics rebuild with true loaded fonts
        }

        // 2. Update Header (Thinking Indicator) -> 1.5x scale factor
        if (currentActorLabel != null) {
            currentActorLabel.setFont(baseFont.deriveFont(Font.BOLD, baseSize * SCALE_HEADER));
        }

        // 3. Update Timer layout elements -> 2.0x scale factor
        if (gameTimeLabel != null) {
            gameTimeLabel.setFont(baseFont.deriveFont(Font.BOLD, baseSize * SCALE_TIMER));
        }

        // 4. Update Button labels uniform formatting
        if (buttonPanel != null) {
            updateComponentTreeFont(buttonPanel, baseFont);

            Font smallerFont = baseFont.deriveFont(Font.BOLD, Math.max(8f, baseSize - 4f));
            if (pauseButton != null)
                pauseButton.setFont(smallerFont);
            if (undoButton != null)
                undoButton.setFont(smallerFont);
            if (redoButton != null)
                redoButton.setFont(smallerFont);
        }
        enforceDynamicMinimumSize();
    }
}

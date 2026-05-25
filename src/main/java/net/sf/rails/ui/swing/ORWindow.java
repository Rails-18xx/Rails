package net.sf.rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import net.sf.rails.game.financial.StockRound;
import java.awt.KeyboardFocusManager;
import net.sf.rails.common.Config;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Round;
import net.sf.rails.ui.swing.elements.DockingFrame;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.round.RoundFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import javax.swing.BoxLayout; // Add this import
import javax.swing.JPanel; // Add this import
import java.awt.Color; // Add this
import java.awt.Dimension; // Add this
import java.awt.Component;

public class ORWindow extends DockingFrame implements ActionPerformer {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ORWindow.class);

    protected final GameUIManager gameUIManager;
    protected ORUIManager orUIManager;
    protected final MapPanel mapPanel;
    protected final ORPanel orPanel;
    protected final UpgradesPanel upgradePanel;
    protected RemainingTilesWindow remainingTilesWindow; // Add this field
    protected final MessagePanel messagePanel; // Kept for logic, removed from GUI

    protected Rectangle lastBounds;

    public ORWindow(GameUIManager gameUIManager, SplashWindow splashWindow) {
        super("yes".equals(Config.get("or.window.dockablePanels")), splashWindow);
        this.gameUIManager = gameUIManager;

        splashWindow.notifyOfStep(SplashWindow.STEP_OR_INIT_PANELS);

        String orUIManagerClassName = gameUIManager.getClassName(GuiDef.ClassName.OR_UI_MANAGER);
        try {
            Class<? extends ORUIManager> orUIManagerClass = Class.forName(orUIManagerClassName)
                    .asSubclass(ORUIManager.class);
            orUIManager = orUIManagerClass.newInstance();
        } catch (Exception e) {
            log.error("Cannot instantiate class {}", orUIManagerClassName, e);
            System.exit(1);
        }
        gameUIManager.setORUIManager(orUIManager);
        orUIManager.setGameUIManager(gameUIManager);

        // Initialize components
        messagePanel = new MessagePanel(); // Kept in memory but NOT added to UI
        upgradePanel = new UpgradesPanel(orUIManager, isDockingFrameworkEnabled());
        // The UpgradesPanel has its own Confirm/Skip buttons. We want to use the
        // main Sidebar buttons instead. We iterate through them and hide them.
        if (upgradePanel.getButtons() != null) {
            for (javax.swing.AbstractButton btn : upgradePanel.getButtons()) {
                btn.setVisible(false);
            }
        }

        mapPanel = new MapPanel(gameUIManager);
        orPanel = new ORPanel(this, orUIManager);

        if (isDockingFrameworkEnabled()) {
            // orPanel.addToButtonPanel(upgradePanel.getButtons(), 0);
            splashWindow.notifyOfStep(SplashWindow.STEP_OR_INIT_TILES);
            remainingTilesWindow = new RemainingTilesWindow(this);
            JScrollPane remainingTilesPanelSlider = remainingTilesWindow.getScrollPane();

            splashWindow.notifyOfStep(SplashWindow.STEP_OR_APPLY_DOCKING_FRAME);
            // Removed messagePanel dockable
            addDockable(upgradePanel, "Dockable.orWindow.upgradePanel", 0, 10, 20, 70, DockableProperty.STANDARD);
            addDockable(mapPanel, "Dockable.orWindow.mapPanel", 20, 10, 80, 70, DockableProperty.STANDARD);
            addDockable(remainingTilesPanelSlider, "Dockable.orWindow.remainingTilesPanel", 100, 0, 120, 100,
                    DockableProperty.INITIALLY_HIDDEN);
            addDockable(orPanel, "Dockable.orWindow.orPanel", 0, 80, 100, 15, DockableProperty.STANDARD);
            // addDockable(orPanel.getButtonPanel(), "Dockable.orWindow.buttonPanel", 0, 95,
            // 100, 5, DockableProperty.STANDARD);
            deployDockables();

            JMenuBar menuBar = orPanel.getMenuBar();
            addDockingFrameMenu(menuBar);

        } else {
            // The ORPanel object itself is now just a component holder.
            // We pull its contents (Sidebar and Upgrades) directly into the main window.

            // 1. Sidebar Wrapper (WEST): Holds ONLY the Action Buttons (ORPanel sidebar)
            // We strip the upgrade panel out so the sidebar gets full height.

            JPanel sidebarWrapper = new JPanel(new BorderLayout());

            // VERIFICATION LOG: Confirm the width is actually 300 (ORPanel.SIDEBAR_WIDTH)

            sidebarWrapper.add(orPanel.getSidebarPanel(), BorderLayout.CENTER);

            // 2. Map Wrapper (CENTER): Vertically stacks Map (Center) and Tiles (South)
            JPanel mapWrapper = new JPanel(new BorderLayout());
            mapWrapper.add(mapPanel, BorderLayout.CENTER);

            // Place the Tiles horizontally underneath the map
            if (upgradePanel != null) {
                mapWrapper.add(upgradePanel, BorderLayout.SOUTH);
            }

            // 3. Main Layout Assembly
            getContentPane().add(sidebarWrapper, BorderLayout.WEST);
            getContentPane().add(mapWrapper, BorderLayout.CENTER);
            // The menu bar is already fully populated inside orPanel (infoMenu, zoomMenu,
            // etc.)
            // We set it as the official menu bar for the ORWindow frame.
            // setJMenuBar(orPanel.getMenuBar());

        }

        orUIManager.init(this);
        // setTitle(LocalText.getText("MapWindowTitle"));
        String baseTitle = LocalText.getText("MapWindowTitle").replace("Rails: Map: ", "").replace("Rails: ", "");
        setTitle("Rails Evolution - " + gameUIManager.getGameManager().getGameName() + " - " + baseTitle);
        setLocation(10, 10);
        setVisible(false);

        if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.NO_MAP_MODE)) {
            mapPanel.setVisible(false);
            upgradePanel.setVisible(false);
            setSize(800, 500);
        } else {
            // Set fixed window size and prevent dynamic resize ---
            setSize(1000, 700); // Use a fixed size larger than the standard 800x600.
            setMinimumSize(new Dimension(800, 600)); // Enforce minimum size.
        }

        final JFrame frame = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveLayout();
                gameUIManager.uncheckMenuItemBox(StatusWindow.MAP_CMD);
                if (!isDockingFrameworkEnabled()) {
                    frame.dispose();
                } else {
                    setVisible(false);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                orPanel.dispose();
            }
        });

        setupGlobalHotkeys();

        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                if (!isDockingFrameworkEnabled()) {
                    mapPanel.fitToWindow();
                } else {
                    setSize(new Dimension(600, 500));
                }
                // WindowSettings ws = getGameUIManager().getWindowSettings();
                // Rectangle bounds = ws.getBounds(ORWindow.this);
                // if (bounds.x != -1 && bounds.y != -1) setLocation(bounds.getLocation());
                // if (bounds.width != -1 && bounds.height != -1) setSize(bounds.getSize());
                // ws.set(frame);

                gameUIManager.packAndApplySizing(frame);

                if (isDockingFrameworkEnabled()) {
                    initLayout();
                }
            }
        });
    }

    public ORUIManager getORUIManager() {
        return orUIManager;
    }

    public GameUIManager getGameUIManager() {
        return gameUIManager;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public ORPanel getORPanel() {
        return orPanel;
    }

    // Add a getter to allow ORPanel to reach it
    public RemainingTilesWindow getRemainingTilesWindow() {
        return remainingTilesWindow;
    }

    public UpgradesPanel getUpgradePanel() {
        return upgradePanel;
    }

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    @Override
    public boolean process(PossibleAction action) {
        return gameUIManager.processAction(action);
    }

    @Override
    public boolean processImmediateAction() {
        return true;
    }

    public void repaintORPanel() {
        if (!isDockingFrameworkEnabled()) {
            orPanel.revalidate();
        }
    }

    public void activate(Round or) {
        orPanel.recreate(or);
        setMapWindowTitle(or);

        // 2. VISIBILITY GUARD:
        // Enhanced Logging
        RoundFacade current = gameUIManager.getCurrentRound();
        String rName = (current != null) ? current.getClass().getSimpleName() : "NULL";

        if (current instanceof StartRound || rName.contains("StartRound") || rName.contains("FormationRound")) {
            return;
        }

        if (!isDockingFrameworkEnabled()) {
            // Keep the window fixed size and re-apply fit to window ---
            mapPanel.fitToWindow();
            if (lastBounds != null) {
                // Ensure we don't accidentally resize the width/height of the window
                // but keep the location fixed if possible.
                setBounds(lastBounds.x, lastBounds.y, getWidth(), getHeight());
            }
        }
        setVisible(true);
        // Do not steal focus if we are actually in a Start Round (e.g. dummy OR for
        // minor token lay)
        if (!(gameUIManager.getCurrentRound() instanceof StartRound)) {
            // Use invokeLater to ensure this runs after the visibility change is fully
            // processed
            SwingUtilities.invokeLater(() -> {
                toFront();
                requestFocus();
            });
        }

    }

    protected void setMapWindowTitle(Round round) {
        GameManager gameManager = gameUIManager.getGameManager();
        String localizedTitle = LocalText.getText("MapWindowORTitle",
                gameManager.getORId(),
                gameManager.getRelativeORNumber(),
                gameManager.getNumOfORs());

        localizedTitle = localizedTitle.replace("Rails: Map: ", "").replace("Rails: ", "");
        setTitle("Rails Evolution - " + gameManager.getGameName() + " - " + localizedTitle);
    }

    @Override
    public void updateStatus(boolean myTurn) {
        if (!(gameUIManager.getCurrentRound() instanceof OperatingRound))
            return;
        orUIManager.updateStatus(myTurn);
        // Explicitly block focus stealing if we are in a Start Round
        if (gameUIManager.getCurrentRound() instanceof StartRound)
            return;
        requestFocus();
    }

    public void finish() {
        lastBounds = getBounds();
        orPanel.finish();
        messagePanel.setMessage("");
        String localizedTitle = LocalText.getText("MapWindowTitle").replace("Rails: Map: ", "").replace("Rails: ", "");
        setTitle("Rails Evolution - " + gameUIManager.getGameManager().getGameName() + " - " + localizedTitle);
    }

    @Override
    protected String getLayoutFileName() {
        return getClass().getSimpleName() + "_" + gameUIManager.getRoot().getGameName();
    }

    // --- START FIX ---
    private void setupGlobalHotkeys() {
        JComponent rootPane = this.getRootPane();

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Increase Font (Cmd = and Cmd +)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask), "increaseFont");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask), "increaseFont");
        actionMap.put("increaseFont", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (orPanel != null)
                    orPanel.adjustFontScale(0.1);
                if (remainingTilesWindow != null)
                    remainingTilesWindow.adjustFontScale(0.1);
                if (upgradePanel != null)
                    upgradePanel.adjustFontScale(0.1);
            }
        });

        // Decrease Font (Cmd -)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask), "decreaseFont");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask), "decreaseFont");
        actionMap.put("decreaseFont", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (orPanel != null)
                    orPanel.adjustFontScale(-0.1);
                if (remainingTilesWindow != null)
                    remainingTilesWindow.adjustFontScale(-0.1);
                if (upgradePanel != null)
                    upgradePanel.adjustFontScale(-0.1);

            }
        });

        // // SPACE KEY: toggle through visuals
        // String SHOW_NUMBERS_KEY = "showNumbersAction";
        // inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), SHOW_NUMBERS_KEY);
        // actionMap.put(SHOW_NUMBERS_KEY, new AbstractAction() {
        // public void actionPerformed(ActionEvent e) {
        // if (orPanel != null) {
        // orPanel.toggleTileBuildNumbers();
        // orUIManager.toggleCompanyHighlights();
        // orUIManager.toggleMapMarkings();
        // // Force a full map repaint to ensure the costs appear/disappear instantly
        // if (orUIManager.getMap() != null) {
        // orUIManager.getMap().repaintAll(new
        // Rectangle(orUIManager.getMap().getSize()));
        // }
        // }
        // }
        // });

        // --- 2. MAP & UPGRADE ACTIONS ---

        // R: Rotate Tile
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "rotateTile");
        actionMap.put("rotateTile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check if we are in Tile Phase OR if Upgrade panel is visible
                if (getUpgradePanel() != null && (orPanel.activePhase == 1 || getUpgradePanel().isVisible())) {
                    getUpgradePanel().nextSelection();
                    orPanel.enableConfirm(true);
                    // Force Map Repaint
                    if (orUIManager.getMap() != null) {
                        orUIManager.getMap().repaintTiles(new java.awt.Rectangle(orUIManager.getMap().getSize()));
                    }
                }
            }
        });

        // E: Cycle Tile Type (The one that was failing)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "cycleTile");
        actionMap.put("cycleTile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check phase and visibility
                if (getUpgradePanel() != null && (orPanel.activePhase == 1 || getUpgradePanel().isVisible())) {
                    // Aggressive Safety Block to prevent crashes on empty upgrade lists
                    try {
                        getUpgradePanel().nextUpgrade();
                        orPanel.enableConfirm(true);

                        if (orUIManager.getMap() != null) {
                            orUIManager.getMap().repaintTiles(new java.awt.Rectangle(orUIManager.getMap().getSize()));
                        }
                    } catch (Exception ex) {
                        // Catch NoSuchElementException and any other runtime issues silently
                        log.warn("Hotkey 'E' ignored: Upgrade cycling failed (likely no upgrades available). Details: "
                                + ex.toString());
                    }
                }
            }

        });

        // S: Select Hex (Cycle Clockwise)
        String CYCLE_HEX_CW_KEY = "cycleHexActionCW";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), CYCLE_HEX_CW_KEY);
        actionMap.put(CYCLE_HEX_CW_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (orUIManager == null || orPanel.cycleableHexes == null || orPanel.cycleableHexes.isEmpty())
                    return;

                orPanel.cycleIndex++;
                if (orPanel.cycleIndex >= orPanel.cycleableHexes.size())
                    orPanel.cycleIndex = 0;

                GUIHex hexToSelect = orPanel.cycleableHexes.get(orPanel.cycleIndex);
                orUIManager.hexClicked(hexToSelect, orUIManager.getMap().getSelectedHex(), false);
                orPanel.enableConfirm(true);
            }
        });

        // D: Select Hex (Cycle Anti-Clockwise) OR Done
        String CYCLE_HEX_ACW_KEY = "cycleHexActionACW";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), CYCLE_HEX_ACW_KEY);
        actionMap.put(CYCLE_HEX_ACW_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // If we have hexes to cycle, do that
                if (orUIManager != null && orPanel.cycleableHexes != null && !orPanel.cycleableHexes.isEmpty()) {
                    orPanel.cycleIndex--;
                    if (orPanel.cycleIndex < 0)
                        orPanel.cycleIndex = orPanel.cycleableHexes.size() - 1;

                    GUIHex hexToSelect = orPanel.cycleableHexes.get(orPanel.cycleIndex);
                    orUIManager.hexClicked(hexToSelect, orUIManager.getMap().getSelectedHex(), false);
                    orPanel.enableConfirm(true);
                }
                // OTHERWISE: Behave as "Done" button if enabled
                else if (orPanel.btnDone != null && orPanel.btnDone.isEnabled()) {
                    orPanel.btnDone.doClick();
                }
            }
        });
        // P: Payout Revenue
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "payRevenue");
        actionMap.put("payRevenue", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (orPanel != null && orPanel.activePhase == 3 && orPanel.btnRevPayout != null
                        && orPanel.btnRevPayout.isEnabled()) {
                    orPanel.btnRevPayout.doClick();
                }
            }
        });

        // W: Withhold Revenue
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "withholdRevenue");
        actionMap.put("withholdRevenue", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (orPanel != null && orPanel.activePhase == 3 && orPanel.btnRevWithhold != null
                        && orPanel.btnRevWithhold.isEnabled()) {
                    orPanel.btnRevWithhold.doClick();
                }
            }
        });

        // H: Half / Split Revenue
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "splitRevenue");
        actionMap.put("splitRevenue", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (orPanel != null && orPanel.activePhase == 3 && orPanel.btnRevSplit != null
                        && orPanel.btnRevSplit.isEnabled()) {
                    orPanel.btnRevSplit.doClick();
                }
            }
        });

    }

    @Override
    public void setVisible(boolean b) {
        // If trying to show window, block it if Start Round is locking focus
        if (b && gameUIManager.isStartRoundActive()) {
            return;
        }
        super.setVisible(b);
    }

    @Override
    public void toFront() {

        // Prevent Focus War: If we are in a Stock Round, the OR Window should not
        // aggressively pop to the front during model updates.
        // We check the GameManager's current round to be precise.
        if (gameUIManager.getGameManager().getCurrentRound() instanceof StockRound) {
            return;
        }

        super.toFront();
    }

    @Override
    public void requestFocus() {
        // detailed "Focus Spy" logs showed ORWindow grabbing focus after every buy.
        // We block programmatic focus requests during Stock Round.
        if (gameUIManager.getGameManager().getCurrentRound() instanceof StockRound) {
            return;
        }

        super.requestFocus();
    }

}

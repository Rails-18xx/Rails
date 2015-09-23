/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/StatusWindow.java,v 1.46 2010/06/15 20:16:54 evos Exp $*/
package net.sf.rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.sf.rails.common.Config;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.TreasuryShareRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.ui.swing.elements.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;
import rails.game.correct.CorrectionModeAction;


/**
 * This is the Window used for displaying nearly all of the rails.game status.
 * This is also from where the ORWindow and StartRoundWindow are triggered.
 */
public class StatusWindow extends JFrame implements ActionListener,
KeyListener, ActionPerformer {
    private static final long serialVersionUID = 1L;

    protected static final String QUIT_CMD = "Quit";

    protected static final String SAVE_CMD = "Save";

    protected static final String RELOAD_CMD = "Reload";

    protected static final String AUTOSAVELOAD_CMD = "AutoSaveLoad";

    protected static final String SAVESTATUS_CMD = "SaveGameStatus";

    protected static final String EXPORT_CMD = "Export";

    protected static final String UNDO_CMD = "Undo";

    protected static final String FORCED_UNDO_CMD = "Undo!";

    protected static final String REDO_CMD = "Redo";

    protected static final String MARKET_CMD = "Market";

    protected static final String MAP_CMD = "Map";

    protected static final String REPORT_CMD = "Report";

    protected static final String CONFIG_CMD = "Config";

    protected static final String BUY_CMD = "Buy";

    protected static final String SELL_CMD = "Sell";

    protected static final String DONE_CMD = "Done";

    protected static final String PASS_CMD = "Pass";
    protected static final String AUTOPASS_CMD = "Autopass";

    protected JPanel buttonPanel;

    protected GameStatus gameStatus;

    protected ActionButton passButton;
    protected ActionButton autopassButton;

    protected GameUIManager gameUIManager;

    protected RoundFacade currentRound;

    protected PossibleActions possibleActions;
    protected PossibleAction immediateAction = null;

    protected JPanel pane = new JPanel(new BorderLayout());

    private JMenuBar menuBar;

    private static JMenu fileMenu, optMenu, moveMenu, moderatorMenu,
    specialMenu, correctionMenu;

    private JMenuItem menuItem;

    private ActionMenuItem actionMenuItem;

    private ActionMenuItem undoItem, forcedUndoItem, redoItem, redoItem2;

    protected static Logger log =
            LoggerFactory.getLogger(StatusWindow.class);

    //    GraphicsConfiguration graphicsConfiguration;

    //    public StatusWindow(GraphicsConfiguration gc) {
    //        super(gc);
    //        this.graphicsConfiguration = gc;
    //    }

    public void initMenu() {
        menuBar = new JMenuBar();
        fileMenu = new JMenu(LocalText.getText("FILE"));
        optMenu = new JMenu(LocalText.getText("OPTIONS"));
        moveMenu = new JMenu(LocalText.getText("MOVE"));
        moderatorMenu = new JMenu(LocalText.getText("MODERATOR"));
        specialMenu = new JMenu(LocalText.getText("SPECIAL"));

        fileMenu.setMnemonic(KeyEvent.VK_F);
        optMenu.setMnemonic(KeyEvent.VK_O);
        moveMenu.setMnemonic(KeyEvent.VK_V);
        moderatorMenu.setMnemonic(KeyEvent.VK_M);
        specialMenu.setMnemonic(KeyEvent.VK_S);

        actionMenuItem = new ActionMenuItem(LocalText.getText("SAVE"));
        actionMenuItem.setActionCommand(SAVE_CMD);
        actionMenuItem.setMnemonic(KeyEvent.VK_S);
        actionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.ALT_MASK));
        actionMenuItem.addActionListener(this);
        actionMenuItem.setEnabled(true);
        actionMenuItem.setPossibleAction(new GameAction(GameAction.Mode.SAVE));
        fileMenu.add(actionMenuItem);

        actionMenuItem = new ActionMenuItem(LocalText.getText("Reload"));
        actionMenuItem.setActionCommand(RELOAD_CMD);
        actionMenuItem.setMnemonic(KeyEvent.VK_R);
        actionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                ActionEvent.ALT_MASK));
        actionMenuItem.addActionListener(this);
        actionMenuItem.setEnabled(true);
        actionMenuItem.setPossibleAction(new GameAction(GameAction.Mode.RELOAD));
        fileMenu.add(actionMenuItem);

        menuItem = new JMenuItem(LocalText.getText("AutoSaveLoad"));
        menuItem.setActionCommand(AUTOSAVELOAD_CMD);
        menuItem.setMnemonic(KeyEvent.VK_A);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                ActionEvent.ALT_MASK));
        menuItem.addActionListener(this);
        menuItem.setEnabled(true);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem(LocalText.getText("SaveGameStatus"));
        menuItem.setActionCommand(SAVESTATUS_CMD);
        menuItem.setMnemonic(KeyEvent.VK_G);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                ActionEvent.ALT_MASK));
        menuItem.addActionListener(this);
        menuItem.setEnabled(true);
        fileMenu.add(menuItem);

        // export menu item
        //        exportItem = new ActionMenuItem(LocalText.getText("EXPORT"));
        //        exportItem.setActionCommand(EXPORT_CMD);
        //        exportItem.addActionListener(this);
        //        exportItem.setEnabled(true);
        //        exportItem.setPossibleAction(new GameAction(GameAction.EXPORT));
        //        fileMenu.add(exportItem);
        //        fileMenu.addSeparator();


        menuItem = new JMenuItem(LocalText.getText("QUIT"));
        menuItem.setActionCommand(QUIT_CMD);
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                ActionEvent.ALT_MASK));
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        menuBar.add(fileMenu);

        menuItem = new JMenuItem(LocalText.getText("SET_SCALE"));
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.addActionListener(this);
        menuItem.setEnabled(false); // XXX: Setting to disabled until we
        // implement this
        optMenu.add(menuItem);

        optMenu.addSeparator();

        menuItem = new JCheckBoxMenuItem(LocalText.getText("MARKET"));
        menuItem.setName(MARKET_CMD);
        menuItem.setActionCommand(MARKET_CMD);
        menuItem.setMnemonic(KeyEvent.VK_K);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JCheckBoxMenuItem(LocalText.getText("MAP"));
        menuItem.setName(MAP_CMD);
        menuItem.setActionCommand(MAP_CMD);
        menuItem.setMnemonic(KeyEvent.VK_M);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
                ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JCheckBoxMenuItem(LocalText.getText("REPORT"));
        menuItem.setName(REPORT_CMD);
        menuItem.setActionCommand(REPORT_CMD);
        menuItem.setMnemonic(KeyEvent.VK_R);
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuItem = new JCheckBoxMenuItem(LocalText.getText("CONFIG"));
        menuItem.setName(CONFIG_CMD);
        menuItem.setActionCommand(CONFIG_CMD);
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.addActionListener(this);
        optMenu.add(menuItem);

        menuBar.add(optMenu);

        undoItem = new ActionMenuItem(LocalText.getText("UNDO"));
        undoItem.setName(LocalText.getText("UNDO"));
        undoItem.setActionCommand(UNDO_CMD);
        undoItem.setMnemonic(KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                ActionEvent.CTRL_MASK));
        undoItem.addActionListener(this);
        undoItem.setEnabled(false);
        moveMenu.add(undoItem);

        redoItem = new ActionMenuItem(LocalText.getText("REDO"));
        redoItem.setName(LocalText.getText("REDO"));
        redoItem.setActionCommand(REDO_CMD);
        redoItem.setMnemonic(KeyEvent.VK_R);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                ActionEvent.CTRL_MASK));
        redoItem.addActionListener(this);
        redoItem.setEnabled(false);
        moveMenu.add(redoItem);

        menuBar.add(moveMenu);

        forcedUndoItem = new ActionMenuItem(LocalText.getText("FORCED_UNDO"));
        forcedUndoItem.setName(LocalText.getText("FORCED_UNDO"));
        forcedUndoItem.setActionCommand(FORCED_UNDO_CMD);
        forcedUndoItem.setMnemonic(KeyEvent.VK_F);
        forcedUndoItem.addActionListener(this);
        forcedUndoItem.setEnabled(false);
        moderatorMenu.add(forcedUndoItem);

        redoItem2 = new ActionMenuItem(LocalText.getText("REDO"));
        redoItem2.setName(LocalText.getText("REDO"));
        redoItem2.setActionCommand(REDO_CMD);
        redoItem2.setMnemonic(KeyEvent.VK_R);
        redoItem2.addActionListener(this);
        redoItem2.setEnabled(false);
        moderatorMenu.add(redoItem2);

        correctionMenu = new JMenu(LocalText.getText("CorrectionMainMenu"));
        correctionMenu.setName(LocalText.getText("CorrectionMainMenu"));
        correctionMenu.setMnemonic(KeyEvent.VK_C);
        correctionMenu.setEnabled(false);
        moderatorMenu.add(correctionMenu);

        menuBar.add(moderatorMenu);

        specialMenu.setBackground(Color.YELLOW); // Normally not seen
        // because menu is not
        // opaque
        menuBar.add(specialMenu);

        setJMenuBar(menuBar);

        if ("yes".equalsIgnoreCase(Config.get("report.window.open"))) {
            enableCheckBoxMenuItem(REPORT_CMD);
        }
    }

    public StatusWindow() {

    }

    public void init(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        this.possibleActions = gameUIManager.getGameManager().getPossibleActions();

        String gameStatusClassName = gameUIManager.getClassName(GuiDef.ClassName.GAME_STATUS);
        try {
            Class<? extends GameStatus> gameStatusClass =
                Class.forName(gameStatusClassName).asSubclass(GameStatus.class);
            gameStatus = gameStatusClass.newInstance();
        } catch (Exception e) {
            log.error("Cannot instantiate class " + gameStatusClassName, e);
            System.exit(1);
        }

        gameStatus.init(this, gameUIManager);
        // put gameStatus into a JScrollPane
        JScrollPane gameStatusPane = new JScrollPane(gameStatus);

        buttonPanel = new JPanel();

        passButton = new ActionButton(RailsIcon.PASS);
        passButton.setMnemonic(KeyEvent.VK_P);
        buttonPanel.add(passButton);
        passButton.setActionCommand(DONE_CMD);
        passButton.addActionListener(this);

        autopassButton = new ActionButton(RailsIcon.AUTOPASS);
        autopassButton.setMnemonic(KeyEvent.VK_A);
        buttonPanel.add(autopassButton);
        autopassButton.setActionCommand(AUTOPASS_CMD);
        autopassButton.addActionListener(this);

        setSize(800, 300);

        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        buttonPanel.setOpaque(false);

        setTitle(LocalText.getText("GAME_STATUS_TITLE"));
        pane.setLayout(new BorderLayout());
        initMenu();
        pane.add(gameStatusPane, BorderLayout.CENTER);
        pane.add(buttonPanel, BorderLayout.SOUTH);
        pane.setOpaque(true);
        setContentPane(pane);
        gameUIManager.setMeVisible(this, true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gameStatus.addKeyListener(this);
        buttonPanel.addKeyListener(this);
        addKeyListener(this);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE );
        final JFrame frame = this;
        final GameUIManager guiMgr = gameUIManager;
        addWindowListener(new WindowAdapter () {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(frame, LocalText.getText("CLOSE_WINDOW"), LocalText.getText("Select"), JOptionPane.OK_CANCEL_OPTION)
                        == JOptionPane.OK_OPTION) {
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
            }
        });

        gameUIManager.packAndApplySizing(this);
    }

    public void initGameActions() {

        // Check the local Undo/Redo menu items,
        // which must always be up-to-date.
        undoItem.setEnabled(false);
        forcedUndoItem.setEnabled(false);
        redoItem.setEnabled(false);
        redoItem2.setEnabled(false);
        // SAVE, RELOAD, AUTOSAVELOAD are always enabled
    }

    public void setGameActions() {
        List<GameAction> gameActions =
            possibleActions.getType(GameAction.class);
        if (gameActions != null) {
            for (GameAction na : gameActions) {
                switch (na.getMode()) {
                case UNDO:
                    undoItem.setEnabled(true);
                    undoItem.setPossibleAction(na);
                    break;
                case FORCED_UNDO:
                    forcedUndoItem.setEnabled(true);
                    forcedUndoItem.setPossibleAction(na);
                    break;
                case REDO:
                    redoItem.setEnabled(true);
                    redoItem.setPossibleAction(na);
                    redoItem2.setEnabled(true);
                    redoItem2.setPossibleAction(na);
                    break;
                default:
                    break;
                }
            }
        }
    }

    public void setCorrectionMenu() {

        // Update the correction  menu
        correctionMenu.removeAll();
        correctionMenu.setEnabled(false);

        // currently only shows CorrectionModeActions
        List<CorrectionModeAction> corrections =
            possibleActions.getType(CorrectionModeAction.class);


        if (corrections != null && !corrections.isEmpty()) {
            for (CorrectionModeAction a : corrections) {
                ActionCheckBoxMenuItem item = new ActionCheckBoxMenuItem (
                        LocalText.getText(a.getCorrectionName()));
                item.addActionListener(this);
                item.addPossibleAction(a);
                item.setEnabled(true);
                item.setSelected(a.isActive());
                correctionMenu.add(item);
            }
            correctionMenu.setEnabled(true);
        }
    }

    public boolean setupFor(RoundFacade round) {

        currentRound = round;

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

        // correction actions always possible
        return gameStatus.initCashCorrectionActions();

    }

    public void updateStatus(boolean myTurn) {

        if (!(currentRound instanceof StockRound || currentRound instanceof EndOfGameRound))
            return;

        log.debug ("MyTurn="+myTurn);
        if (!myTurn) {
            gameStatus.initTurn(getCurrentPlayer().getIndex(), false);
            return;
        }

        // Moved here from StatusWindow_1856. It's getting generic...
        if (possibleActions.contains(DiscardTrain.class)) {
            immediateAction = possibleActions.getType(DiscardTrain.class).get(0);
            return;
        }


        if (currentRound instanceof TreasuryShareRound) {

            setTitle(LocalText.getText(
                    "TRADE_TREASURY_SHARES_TITLE",
                    ((TreasuryShareRound) currentRound).getOperatingCompany().getId()));
            gameStatus.initTurn(-1, true);

        } else if ((currentRound instanceof ShareSellingRound)) {
            setTitle(LocalText.getText(
                    "EMERGENCY_SHARE_SELLING_TITLE",
                    (((ShareSellingRound) currentRound).getCompanyNeedingCash().getId())));
            gameStatus.initTurn(getCurrentPlayer().getIndex(), true);
            gameStatus.setPriorityPlayer(gameUIManager.getPriorityPlayer().getIndex());

            int cash =
                ((ShareSellingRound) currentRound).getRemainingCashToRaise();
            if (!possibleActions.contains(SellShares.class)) {
                // should not occur anymore
                //                JOptionPane.showMessageDialog(this, LocalText.getText(
//                        "YouMustRaiseCashButCannot", Currency.format(this, cash)), "",
                //                        JOptionPane.OK_OPTION);
                /*
                 * For now assume that this ends the game (not true in all
                 * games)
                 * sfy: changed now
                 */
                //                JOptionPane.showMessageDialog(this,
                //                        gameUIManager.getGameManager().getGameReport(), "", JOptionPane.OK_OPTION);
                /*
                 * All other wrapping up has already been done when calling
                 * getSellableCertificates, so we can just finish now.
                 */
                //                finish();
                //                return;
            } else {
                JOptionPane.showMessageDialog(this, LocalText.getText(
                        "YouMustRaiseCash", gameUIManager.format(cash)), "",
                        JOptionPane.OK_OPTION);
            }
        } else if (currentRound instanceof StockRound && !updateGameSpecificSettings()) {

            setTitle(LocalText.getText(
                    "STOCK_ROUND_TITLE",
                    String.valueOf(((StockRound) currentRound).getStockRoundNumber())));
            gameStatus.initTurn(getCurrentPlayer().getIndex(), true);
            gameStatus.setPriorityPlayer(gameUIManager.getPriorityPlayer().getIndex());
        }

        // New special action handling
        List<ActionMenuItem> specialActionItems =
            new ArrayList<ActionMenuItem>();

        // Special properties
        List<UseSpecialProperty> sps =
            possibleActions.getType(UseSpecialProperty.class);
        for (ActionMenuItem item : specialActionItems) {
            item.removeActionListener(this);
        }
        specialMenu.removeAll();
        specialActionItems.clear();
        for (UseSpecialProperty sp : sps) {
            ActionMenuItem item = new ActionMenuItem(sp.toMenu());
            item.addActionListener(this);
            item.setEnabled(false);
            item.addPossibleAction(sp);
            item.setEnabled(true);
            specialActionItems.add(item);
            specialMenu.add(item);
        }

        // Request turn
        if (possibleActions.contains(RequestTurn.class)) {
            for (RequestTurn action : possibleActions.getType(RequestTurn.class)) {
                ActionMenuItem item = new ActionMenuItem(action.toMenu());
                item.addActionListener(this);
                item.setEnabled(false);
                item.addPossibleAction(action);
                item.setEnabled(true);
                specialActionItems.add(item);
                specialMenu.add(item);
            }
        }

        // Must Special menu be enabled?
        boolean enabled = specialActionItems.size() > 0;
        specialMenu.setOpaque(enabled);
        specialMenu.setEnabled(enabled);
        specialMenu.repaint();

        passButton.setEnabled(false);
        autopassButton.setEnabled(false);

        List<NullAction> inactiveItems =
            possibleActions.getType(NullAction.class);
        if (inactiveItems != null) {

            for (NullAction na : inactiveItems) {
                switch (na.getMode()) {
                case PASS:
                    passButton.setRailsIcon(RailsIcon.PASS);
                    passButton.setEnabled(true);
                    passButton.setActionCommand(PASS_CMD);
                    passButton.setMnemonic(KeyEvent.VK_P);
                    passButton.setPossibleAction(na);
                    break;
                case DONE:
                    passButton.setRailsIcon(RailsIcon.DONE);
                    passButton.setEnabled(true);
                    passButton.setActionCommand(DONE_CMD);
                    passButton.setMnemonic(KeyEvent.VK_D);
                    passButton.setPossibleAction(na);
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

        if (currentRound instanceof EndOfGameRound) endOfGame();

        pack();

        toFront();
    }

    public void disableButtons () {
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

    public void actionPerformed(ActionEvent actor) {
        String command = actor.getActionCommand();
        List<PossibleAction> actions = null;
        if (actor.getSource() instanceof ActionTaker) {
            actions = ((ActionTaker) actor.getSource()).getPossibleActions();
        }
        PossibleAction executedAction = null;
        if (actions != null && actions.size() > 0) {
            executedAction = actions.get(0);
        }

        if (command.equals(BUY_CMD)) {
            process(executedAction);
        } else if (command.equals(SELL_CMD)) {
            process(executedAction);
        } else if (command.equals(DONE_CMD) || command.equals(PASS_CMD)
                || command.equals(AUTOPASS_CMD)) {
            if (gameUIManager.isGameOver()) {
                System.exit(0);
            }
            process(executedAction);

        } else if (executedAction instanceof UseSpecialProperty
                || executedAction instanceof RequestTurn) {
            process(executedAction);

        } else if (command.equals(QUIT_CMD)) {
            gameUIManager.terminate();
        } else if (command.equals(REPORT_CMD)) {
            gameUIManager.reportWindow.setVisible(((JMenuItem) actor.getSource()).isSelected());
            gameUIManager.reportWindow.scrollDown();
            return;
        } else if (command.equals(MARKET_CMD)) {
            gameUIManager.stockChart.setVisible(((JMenuItem) actor.getSource()).isSelected());
        } else if (command.equals(MAP_CMD)) {
            gameUIManager.orWindow.setVisible(((JMenuItem) actor.getSource()).isSelected());
        } else if (command.equals(CONFIG_CMD)) {
            gameUIManager.configWindow.setVisible(((JMenuItem) actor.getSource()).isSelected());
        } else if (command.equals(AUTOSAVELOAD_CMD)) {
            gameUIManager.autoSaveLoadGame();
        } else if (command.equals(SAVESTATUS_CMD)) {
            gameUIManager.saveGameStatus();
        } else if (executedAction == null) {
            ;
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
        } else {
            // Unknown action, let UIManager catch it
            process (executedAction);
        }
    }

    public boolean process(PossibleAction executedAction) {

        if (executedAction == null) {
            JOptionPane.showMessageDialog(this, "ERROR: no action found!");
            return false;
        }

        return gameUIManager.processAction(executedAction);
    }

    public boolean processImmediateAction() {
        if (immediateAction instanceof DiscardTrain) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            DiscardTrain nextAction = (DiscardTrain) immediateAction;
            immediateAction = null;
            gameUIManager.discardTrains (nextAction);
        }
        return true;
    }

    public void setPassButton(NullAction action) {
        if (action != null) {
            NullAction.Mode mode = action.getMode();
            if (mode == NullAction.Mode.PASS) {
                passButton.setRailsIcon(RailsIcon.PASS);
            } else if (mode == NullAction.Mode.DONE) {
                passButton.setRailsIcon(RailsIcon.DONE);
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

    public static void uncheckMenuItemBox(String itemName) {
        int count = optMenu.getMenuComponentCount();

        for (int i = 0; i < count; i++) {
            try {
                if (optMenu.getMenuComponent(i).getName().equalsIgnoreCase(
                        itemName)) {
                    ((JCheckBoxMenuItem) optMenu.getMenuComponent(i)).setSelected(false);
                    optMenu.invalidate();
                }
            } catch (NullPointerException e) {
                // Seperators are null
            }
        }
    }

    public void finishRound() {
        setTitle(LocalText.getText("GAME_STATUS_TITLE"));
        gameStatus.initTurn(-1, true);
        passButton.setEnabled(false);
    }

    /**
     * End of Game processing
     */
    public void endOfGame() {
        //        setVisible(true);
        //        gameUIManager.reportWindow.setVisible(true);
        //        gameUIManager.stockChart.setVisible(true);

        setTitle(LocalText.getText("EoGTitle"));

        // Enable Passbutton
        passButton.setEnabled(true);
        passButton.setRailsIcon(RailsIcon.END_OF_GAME_CLOSE_ALL_WINDOWS);

        gameUIManager.orWindow.finish();
    }

    public Player getCurrentPlayer () {
        return gameUIManager.getCurrentPlayer();
    }

    public void endOfGameReport() {

        GameManager gm = gameUIManager.getGameManager();

        if (gm.getGameOverReportedUI())
            return;
        else
            gm.setGameOverReportedUI(true);

        JOptionPane.showMessageDialog(this,
                LocalText.getText("EoGPressButton"),
                LocalText.getText("EoGFinalRanking"),
                JOptionPane.PLAIN_MESSAGE
        );


        if (!Config.getDevelop()) {
            // show game report line by line
            List<String> gameReport = gm.getGameReport();
            Collections.reverse(gameReport);
            StringBuilder report = new StringBuilder();
            for (String s:gameReport) {
                report.insert(0, s + "\n");
                JOptionPane.showMessageDialog(this,
                        report,
                        LocalText.getText("EoGFinalRanking"),
                        JOptionPane.PLAIN_MESSAGE
                );
            }
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public void updatePlayerOrder(List<String> newPlayerNames) {
       gameStatus.updatePlayerOrder(newPlayerNames);
    }


}

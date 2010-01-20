/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/StatusWindow.java,v 1.36 2010/01/20 19:52:43 evos Exp $*/
package rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.ActionButton;
import rails.ui.swing.elements.ActionMenuItem;
import rails.util.Config;
import rails.util.LocalText;

/**
 * This is the Window used for displaying nearly all of the rails.game status.
 * This is also from where the ORWindow and StartRoundWindow are triggered.
 */
public class StatusWindow extends JFrame implements ActionListener,
        KeyListener, ActionPerformer {
    private static final long serialVersionUID = 1L;

    protected static final String QUIT_CMD = "Quit";

    protected static final String SAVE_CMD = "Save";

    protected static final String UNDO_CMD = "Undo";

    protected static final String FORCED_UNDO_CMD = "Undo!";

    protected static final String REDO_CMD = "Redo";

    protected static final String MARKET_CMD = "Market";

    protected static final String MAP_CMD = "Map";

    protected static final String REPORT_CMD = "Report";

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

    protected RoundI currentRound;

    protected PossibleActions possibleActions = PossibleActions.getInstance();
    protected PossibleAction immediateAction = null;

    JPanel pane = new JPanel(new BorderLayout());

    private JMenuBar menuBar;

    private static JMenu fileMenu, optMenu, moveMenu, moderatorMenu,
            specialMenu;

    private JMenuItem menuItem;

    private ActionMenuItem saveItem;

    private ActionMenuItem undoItem, forcedUndoItem, redoItem, redoItem2;

    protected static Logger log =
            Logger.getLogger(StatusWindow.class.getPackage().getName());

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

        saveItem = new ActionMenuItem(LocalText.getText("SAVE"));
        saveItem.setActionCommand(SAVE_CMD);
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                ActionEvent.ALT_MASK));
        saveItem.addActionListener(this);
        saveItem.setEnabled(true);
        saveItem.setPossibleAction(new GameAction(GameAction.SAVE));
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

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

        String gameStatusClassName = gameUIManager.getClassName(GuiDef.ClassName.GAME_STATUS);
        try {
            Class<? extends GameStatus> gameStatusClass =
                Class.forName(gameStatusClassName).asSubclass(GameStatus.class);
            gameStatus = gameStatusClass.newInstance();
        } catch (Exception e) {
            log.fatal("Cannot instantiate class " + gameStatusClassName, e);
            System.exit(1);
        }
        gameStatus.init(this, gameUIManager);

        buttonPanel = new JPanel();

        passButton = new ActionButton(LocalText.getText("PASS"));
        passButton.setMnemonic(KeyEvent.VK_P);
        buttonPanel.add(passButton);
        passButton.setActionCommand(DONE_CMD);
        passButton.addActionListener(this);

        autopassButton = new ActionButton(LocalText.getText("Autopass"));
        autopassButton.setMnemonic(KeyEvent.VK_A);
        buttonPanel.add(autopassButton);
        autopassButton.setActionCommand(AUTOPASS_CMD);
        autopassButton.addActionListener(this);

        setSize(800, 300);
        setLocation(25, 450);

        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        buttonPanel.setOpaque(false);

        setTitle(LocalText.getText("GAME_STATUS_TITLE"));
        pane.setLayout(new BorderLayout());
        initMenu();
        pane.add(gameStatus, BorderLayout.NORTH);
        pane.add(buttonPanel, BorderLayout.CENTER);
        pane.setOpaque(true);
        setContentPane(pane);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gameStatus.addKeyListener(this);
        buttonPanel.addKeyListener(this);
        addKeyListener(this);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE );
        final JFrame frame = this;
        addWindowListener(new WindowAdapter () {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(frame, "Do you really want to exit the game?", "Select", JOptionPane.OK_CANCEL_OPTION)
                        == JOptionPane.OK_OPTION) {
                    frame.dispose();
                    System.exit(0);
                }
            }
        });


        pack();
    }

    public void setGameActions() {

        // Check the local Undo/Redo menu items,
        // which must always be up-to-date.
        undoItem.setEnabled(false);
        forcedUndoItem.setEnabled(false);
        redoItem.setEnabled(false);
        redoItem2.setEnabled(false);
        // SAVE is always enabled

        List<GameAction> gameActions =
                possibleActions.getType(GameAction.class);
        if (gameActions != null) {
            for (GameAction na : gameActions) {
                switch (na.getMode()) {
                // SAVE is now enabled by default
                //case GameAction.SAVE:
                //    saveItem.setPossibleAction(na);
                //    break;
                case GameAction.UNDO:
                    undoItem.setEnabled(true);
                    undoItem.setPossibleAction(na);
                    break;
                case GameAction.FORCED_UNDO:
                    forcedUndoItem.setEnabled(true);
                    forcedUndoItem.setPossibleAction(na);
                    break;
                case GameAction.REDO:
                    redoItem.setEnabled(true);
                    redoItem.setPossibleAction(na);
                    redoItem2.setEnabled(true);
                    redoItem2.setPossibleAction(na);
                    break;
                }
            }
        }
    }

    public void setupFor(RoundI round) {

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
    }

    public void updateStatus() {
        if (!(currentRound instanceof StockRound)) {
            return;
        }

        if (currentRound instanceof TreasuryShareRound) {

            setTitle(LocalText.getText(
                    "TRADE_TREASURY_SHARES_TITLE",
                    ((TreasuryShareRound) currentRound).getOperatingCompany().getName()));
            gameStatus.initTurn(-1);
            passButton.setEnabled(true);

        } else if ((currentRound instanceof ShareSellingRound)) {
            setTitle(LocalText.getText(
                    "EMERGENCY_SHARE_SELLING_TITLE",
                    (((ShareSellingRound) currentRound).getCompanyNeedingCash().getName())));
            gameStatus.initTurn(gameUIManager.getCurrentPlayer().getIndex());
            gameStatus.setPriorityPlayer(gameUIManager.getPriorityPlayer().getIndex());

            passButton.setEnabled(false);
            int cash =
                    ((ShareSellingRound) currentRound).getRemainingCashToRaise();
            if (!possibleActions.contains(SellShares.class)) {
                JOptionPane.showMessageDialog(this, LocalText.getText(
                        "YouAreBankrupt", Bank.format(cash)), "",
                        JOptionPane.OK_OPTION);
                /*
                 * For now assume that this ends the game (not true in all
                 * games)
                 */
                JOptionPane.showMessageDialog(this,
                        gameUIManager.getGameManager().getGameReport(), "", JOptionPane.OK_OPTION);
                /*
                 * All other wrapping up has already been done when calling
                 * getSellableCertificates, so we can just finish now.
                 */
                finish();
                return;
            } else {
                JOptionPane.showMessageDialog(this, LocalText.getText(
                        "YouMustRaiseCash", Bank.format(cash)), "",
                        JOptionPane.OK_OPTION);
            }
        } else if (!updateGameSpecificSettings()) {

            setTitle(LocalText.getText(
                    "STOCK_ROUND_TITLE",
                    String.valueOf(((StockRound) currentRound).getStockRoundNumber())));
            gameStatus.initTurn(gameUIManager.getCurrentPlayer().getIndex());
            gameStatus.setPriorityPlayer(gameUIManager.getPriorityPlayer().getIndex());

            passButton.setEnabled(true);
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
                ActionMenuItem item = new ActionMenuItem(action.toString());
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
                case NullAction.PASS:
                    passButton.setText(LocalText.getText("PASS"));
                    passButton.setEnabled(true);
                    passButton.setActionCommand(PASS_CMD);
                    passButton.setMnemonic(KeyEvent.VK_P);
                    passButton.setPossibleAction(na);
                    break;
                case NullAction.DONE:
                    passButton.setText(LocalText.getText("Done"));
                    passButton.setEnabled(true);
                    passButton.setActionCommand(DONE_CMD);
                    passButton.setMnemonic(KeyEvent.VK_D);
                    passButton.setPossibleAction(na);
                    break;
                case NullAction.AUTOPASS:
                    autopassButton.setEnabled(true);
                    autopassButton.setPossibleAction(na);
                    break;
                }
            }
        }

        pack();

        toFront();
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
            System.exit(0);
        } else if (command.equals(REPORT_CMD)) {
            gameUIManager.reportWindow.setVisible(((JMenuItem) actor.getSource()).isSelected());
            return;
        } else if (command.equals(MARKET_CMD)) {
            gameUIManager.stockChart.setVisible(((JMenuItem) actor.getSource()).isSelected());
        } else if (command.equals(MAP_CMD)) {
            gameUIManager.orWindow.setVisible(((JMenuItem) actor.getSource()).isSelected());
        } else if (executedAction == null) {
            ;
        } else if (executedAction instanceof GameAction) {
            switch (((GameAction) executedAction).getMode()) {
            case GameAction.SAVE:
                gameUIManager.saveGame((GameAction) executedAction);
                break;

            case GameAction.UNDO:
            case GameAction.FORCED_UNDO:
            case GameAction.REDO:
                process(executedAction);
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

        return gameUIManager.processOnServer(executedAction);
    }

    public boolean processImmediateAction() {
        // No such actions here
        return true;
    }

    public void setPassButton(NullAction action) {
        if (action != null) {
            int mode = action.getMode();
            if (mode == NullAction.PASS) {
                passButton.setText(LocalText.getText("PASS"));
            } else if (mode == NullAction.DONE) {
                passButton.setText(LocalText.getText("Done"));
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
        gameStatus.initTurn(-1);
        passButton.setEnabled(false);
    }

    public void reportGameOver() {
        /* End of rails.game checks */

        JOptionPane.showMessageDialog(this, "GAME OVER", "",
                JOptionPane.OK_OPTION);
        JOptionPane.showMessageDialog(this,
                GameManager.getInstance().getGameReport(), "",
                JOptionPane.OK_OPTION);
        /*
         * All other wrapping up has already been done when calling
         * getSellableCertificates, so we can just finish now.
         */
        finish();
    }

    public void reportBankBroken() {

        /* The message must become configuration-depedent */
        JOptionPane.showMessageDialog(this,
                "Bank is broken. The rails.game will be over after the current set of ORs.");
    }

    /**
     * Finish the application.
     */
    public void finish() {
        setVisible(true);
        gameUIManager.reportWindow.setVisible(true);
        gameUIManager.stockChart.setVisible(true);

        /* Disable all buttons */
        passButton.setEnabled(true);
        passButton.setText(LocalText.getText("END_OF_GAME_CLOSE_ALL_WINDOWS"));
        gameUIManager.orWindow.finish();

        toFront();

    }

    public void keyReleased(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameUIManager.getHelp());
            e.consume();
        }
    }

    public void keyTyped(KeyEvent e) {}


}

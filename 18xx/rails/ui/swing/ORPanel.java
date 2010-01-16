/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORPanel.java,v 1.40 2010/01/16 15:06:44 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.*;
import rails.game.special.SpecialPropertyI;
import rails.ui.swing.elements.*;
import rails.util.LocalText;
import rails.util.Util;

public class ORPanel extends GridPanel
implements ActionListener, KeyListener {

    private static final long serialVersionUID = 1L;

    public static final String BUY_PRIVATE_CMD = "BuyPrivate";
    public static final String BUY_TRAIN_CMD = "BuyTrain";
    private static final String WITHHOLD_CMD = "Withhold";
    private static final String SPLIT_CMD = "Split";
    public static final String PAYOUT_CMD = "Payout";
    public static final String SET_REVENUE_CMD = "SetRevenue";
    private static final String LAY_TILE_CMD = "LayTile";
    private static final String DONE_CMD = "Done";
    private static final String UNDO_CMD = "Undo";
    private static final String REDO_CMD = "Redo";
    public static final String REM_TILES_CMD = "RemainingTiles";
    public static final String TAKE_LOANS_CMD = "TakeLoans";
    public static final String REPAY_LOANS_CMD = "RepayLoans";

    ORWindow orWindow;
    ORUIManager orUIManager;

    private JPanel statusPanel;
    private JPanel buttonPanel;

    private JMenuBar menuBar;
    private JMenu infoMenu;
    private JMenuItem remainingTilesMenuItem;
    private JMenu privatesInfoMenu;
    private JMenu specialMenu;
    private JMenu loansMenu;
    private JMenu zoomMenu;
    private JMenuItem zoomIn, zoomOut;
    private ActionMenuItem takeLoans;
    private ActionMenuItem repayLoans;

    // Grid elements per function
    private Caption leftCompName[];
    private int leftCompNameXOffset, leftCompNameYOffset;
    private Caption rightCompName[];
    private int rightCompNameXOffset, rightCompNameYOffset;
    private Field president[];
    private int presidentXOffset, presidentYOffset;
    private Field sharePrice[];
    private int sharePriceXOffset, sharePriceYOffset;
    private Field cash[];
    private int cashXOffset, cashYOffset;
    private Field privates[];
    private int privatesXOffset, privatesYOffset;
    private Field newPrivatesCost[];
    private Field[] compLoans;
    private int loansXOffset, loansYOffset;
    private Field tiles[];
    private int tilesXOffset, tilesYOffset;
    private Field tileCost[];
    private Field tokens[];
    private Field tokenCost[];
    private Field tokensLeft[];
    private Field tokenBonus[];
    private int tokensXOffset, tokensYOffset;
    private Field revenue[];
    private Spinner revenueSelect[];
    private Field decision[];
    private int revXOffset, revYOffset;
    private Field trains[];
    private int trainsXOffset, trainsYOffset;
    private Field newTrainCost[];

    private boolean privatesCanBeBought = false;
    private boolean bonusTokensExist = false;
    private boolean hasCompanyLoans = false;

    private Caption tileCaption, tokenCaption, revenueCaption, trainCaption,
            privatesCaption, loansCaption;

    private ActionButton button1;
    private ActionButton button2;
    private ActionButton button3;
    private ActionButton undoButton;
    private ActionButton redoButton;

    // Current state
    private int playerIndex = -1;
    private int orCompIndex = -1;

    private PublicCompanyI orComp = null;

    protected static Logger log =
            Logger.getLogger(ORPanel.class.getPackage().getName());

    public ORPanel(ORWindow parent, ORUIManager orUIManager) {
        super();

        orWindow = parent;
        this.orUIManager = orUIManager;
        GameUIManager gameUIManager = parent.gameUIManager;

        statusPanel = new JPanel();
        gb = new GridBagLayout();
        statusPanel.setLayout(gb);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setOpaque(true);

        gridPanel = statusPanel;
        parentFrame = parent;

        round = gameUIManager.getCurrentRound();
        privatesCanBeBought = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES);
        bonusTokensExist = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.DO_BONUS_TOKENS_EXIST);
        hasCompanyLoans = gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.HAS_ANY_COMPANY_LOANS);

        initButtonPanel();
        gbc = new GridBagConstraints();

        players = gameUIManager.getPlayers().toArray(new Player[0]);

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies();
            nc = companies.length;
        }

        initFields();

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        menuBar = new JMenuBar();

        infoMenu = new JMenu(LocalText.getText("Info"));
        infoMenu.setEnabled(true);
        remainingTilesMenuItem =
                new JMenuItem(LocalText.getText("RemainingTiles"));
        remainingTilesMenuItem.addActionListener(this);
        remainingTilesMenuItem.setActionCommand(REM_TILES_CMD);
        infoMenu.add(remainingTilesMenuItem);
        menuBar.add(infoMenu);

        addPrivatesInfo();
        
        specialMenu = new JMenu(LocalText.getText("SPECIAL"));
        specialMenu.setBackground(Color.YELLOW);
        // Normally not seen because menu is not opaque
        specialMenu.setEnabled(false);
        menuBar.add(specialMenu);

        if (hasCompanyLoans) {
            loansMenu = new JMenu (LocalText.getText("LOANS"));
            loansMenu.setEnabled(true);

            takeLoans = new ActionMenuItem (LocalText.getText("TakeLoans"));
            takeLoans.addActionListener(this);
            takeLoans.setEnabled(false);
            loansMenu.add(takeLoans);
            menuItemsToReset.add(takeLoans);

            repayLoans = new ActionMenuItem (LocalText.getText("RepayLoans"));
            repayLoans.addActionListener(this);
            repayLoans.setEnabled(false);
            loansMenu.add(repayLoans);
            menuItemsToReset.add(repayLoans);

            menuBar.add(loansMenu);
        }

        zoomMenu = new JMenu("Zoom");
        zoomMenu.setEnabled(true);
        zoomIn = new JMenuItem("In");
        zoomIn.addActionListener(this);
        zoomIn.setEnabled(true);
        zoomMenu.add(zoomIn);
        zoomOut = new JMenuItem("Out");
        zoomOut.addActionListener(this);
        zoomOut.setEnabled(true);
        zoomMenu.add(zoomOut);
        menuBar.add(zoomMenu);

        add(menuBar, BorderLayout.NORTH);

        setVisible(true);

        addKeyListener(this);
    }

    public void recreate(OperatingRound or) {
        log.debug("ORPanel.recreate() called");

        companies = or.getOperatingCompanies();
        nc = companies.length;

        // Remove old fields. Don't forget to deregister the Observers
        deRegisterObservers();
        statusPanel.removeAll();

        // Create new fields
        initFields();
        repaint();
    }

    private void initButtonPanel() {
        buttonPanel = new JPanel();

        button1 = new ActionButton(LocalText.getText("LayTile"));
        button1.setActionCommand(LAY_TILE_CMD);
        button1.setMnemonic(KeyEvent.VK_T);
        button1.addActionListener(this);
        button1.setEnabled(true);
        buttonPanel.add(button1);

        button2 = new ActionButton(LocalText.getText("BUY_PRIVATE"));
        button2.setActionCommand(BUY_PRIVATE_CMD);
        button2.setMnemonic(KeyEvent.VK_V);
        button2.addActionListener(this);
        button2.setEnabled(false);
        button2.setVisible(false);
        buttonPanel.add(button2);

        button3 = new ActionButton(LocalText.getText("Done"));
        button3.setActionCommand(DONE_CMD);
        button3.setMnemonic(KeyEvent.VK_D);
        button3.addActionListener(this);
        button3.setEnabled(true);
        buttonPanel.add(button3);

        undoButton = new ActionButton(LocalText.getText("UNDO"));
        undoButton.setActionCommand(UNDO_CMD);
        undoButton.setMnemonic(KeyEvent.VK_U);
        undoButton.addActionListener(this);
        undoButton.setEnabled(false);
        buttonPanel.add(undoButton);

        redoButton = new ActionButton(LocalText.getText("REDO"));
        redoButton.setActionCommand(REDO_CMD);
        redoButton.setMnemonic(KeyEvent.VK_R);
        redoButton.addActionListener(this);
        redoButton.setEnabled(false);
        buttonPanel.add(redoButton);

        buttonPanel.setOpaque(true);
    }

    private void initFields() {
        leftCompName = new Caption[nc];
        rightCompName = new Caption[nc];
        president = new Field[nc];
        sharePrice = new Field[nc];
        cash = new Field[nc];
        trains = new Field[nc];
        privates = new Field[nc];
        tiles = new Field[nc];
        tileCost = new Field[nc];
        tokens = new Field[nc];
        tokenCost = new Field[nc];
        tokensLeft = new Field[nc];
        if (bonusTokensExist) tokenBonus = new Field[nc];
        if (hasCompanyLoans) compLoans = new Field[nc];
        revenue = new Field[nc];
        revenueSelect = new Spinner[nc];
        decision = new Field[nc];
        newTrainCost = new Field[nc];
        newPrivatesCost = new Field[nc];

        leftCompNameXOffset = 0;
        leftCompNameYOffset = 2;
        int currentXOffset = leftCompNameXOffset;
        int lastXWidth = 0;

        /* Top titles */
        addField(new Caption("Company"), 0, 0, lastXWidth = 1, 2,
                WIDE_BOTTOM + WIDE_RIGHT);

        presidentXOffset = currentXOffset += lastXWidth;
        presidentYOffset = leftCompNameYOffset;
        addField(new Caption("President"), presidentXOffset, 0, lastXWidth = 1,
                2, WIDE_BOTTOM);

        sharePriceXOffset = currentXOffset += lastXWidth;
        sharePriceYOffset = leftCompNameYOffset;
        addField(new Caption("<html>Share<br>value</html>"), sharePriceXOffset,
                0, lastXWidth = 1, 2, WIDE_BOTTOM);

        cashXOffset = currentXOffset += lastXWidth;
        cashYOffset = leftCompNameYOffset;
        addField(new Caption("Treasury"), cashXOffset, 0, lastXWidth = 1, 2,
                WIDE_BOTTOM + WIDE_RIGHT);

        if (privatesCanBeBought) {
            privatesXOffset = currentXOffset += lastXWidth;
            privatesYOffset = leftCompNameYOffset;
            addField(privatesCaption = new Caption("Privates"),
                    privatesXOffset, 0, lastXWidth = 2, 1, WIDE_RIGHT);
            addField(new Caption("owned"), privatesXOffset, 1, 1, 1,
                    WIDE_BOTTOM);
            addField(new Caption("cost"), privatesXOffset + 1, 1, 1, 1,
                    WIDE_BOTTOM + WIDE_RIGHT);
        }

        if (hasCompanyLoans) {
            loansXOffset = currentXOffset += lastXWidth;
            loansYOffset = leftCompNameYOffset;
            addField (loansCaption = new Caption(LocalText.getText("LOANS")),
                    loansXOffset, 0, lastXWidth = 1, 2, WIDE_RIGHT);
        }

        tilesXOffset = currentXOffset += lastXWidth;
        tilesYOffset = leftCompNameYOffset;
        addField(tileCaption = new Caption("Tiles"), tilesXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tilesXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tilesXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                                                                 + WIDE_RIGHT);

        tokensXOffset = currentXOffset += lastXWidth;
        tokensYOffset = leftCompNameYOffset;
        lastXWidth = bonusTokensExist ? 4 : 3;
        addField(tokenCaption = new Caption("Tokens"), tokensXOffset, 0,
                lastXWidth, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tokensXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tokensXOffset + 1, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("left"), tokensXOffset + 2, 1, 1, 1,
                WIDE_BOTTOM + (bonusTokensExist ? 0 : WIDE_RIGHT));
        if (bonusTokensExist) {
            addField(new Caption("bonus"), tokensXOffset + 3, 1, 1, 1,
                    WIDE_BOTTOM + WIDE_RIGHT);
        }

        revXOffset = currentXOffset += lastXWidth;
        revYOffset = leftCompNameYOffset;
        addField(revenueCaption = new Caption("Revenue"), revXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("earned"), revXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("payout"), revXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                                                                 + WIDE_RIGHT);

        trainsXOffset = currentXOffset += lastXWidth;
        trainsYOffset = leftCompNameYOffset;
        addField(trainCaption = new Caption("Trains"), trainsXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("owned"), trainsXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), trainsXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                                                                  + WIDE_RIGHT);

        rightCompNameXOffset = currentXOffset += lastXWidth;
        rightCompNameYOffset = leftCompNameYOffset;
        addField(new Caption("Company"), rightCompNameXOffset, 0, 1, 2,
                WIDE_BOTTOM);

        fields = new JComponent[1+currentXOffset][2+nc];
        rowVisibilityObservers = new RowVisibility[nc];

        for (int i = 0; i < nc; i++) {
            c = companies[i];
            rowVisibilityObservers[i]
                    = new RowVisibility (this, leftCompNameYOffset + i, c.getClosedModel());
            observers.add(rowVisibilityObservers[i]);

            boolean visible = !c.isClosed();

            f = leftCompName[i] = new Caption(c.getName());
            f.setBackground(c.getBgColour());
            f.setForeground(c.getFgColour());
            addField(f, leftCompNameXOffset, leftCompNameYOffset + i, 1, 1,
                    WIDE_RIGHT, visible);

            f =
                    president[i] =
                            new Field(c.hasStarted() && !c.isClosed()
                                    ? c.getPresident().getNameAndPriority() : "");
            addField(f, presidentXOffset, presidentYOffset + i, 1, 1, 0, visible);

            f = sharePrice[i] = new Field(c.getCurrentPriceModel());
            addField(f, sharePriceXOffset, sharePriceYOffset + i, 1, 1, 0, visible);

            f = cash[i] = new Field(c.getCashModel());
            addField(f, cashXOffset, cashYOffset + i, 1, 1, WIDE_RIGHT, visible);

            if (privatesCanBeBought) {
                f =
                        privates[i] =
                                new Field(
                                        c.getPortfolio().getPrivatesOwnedModel());
                addField(f, privatesXOffset, privatesYOffset + i, 1, 1,
                        WIDE_RIGHT, visible);

                f =
                        newPrivatesCost[i] =
                                new Field(c.getPrivatesSpentThisTurnModel());
                addField(f, privatesXOffset + 1, privatesYOffset + i, 1, 1,
                        WIDE_RIGHT, visible);
            }

            if (hasCompanyLoans) {
                //if (c.canLoan()) {
            	if (c.getLoanValueModel() != null) {
                    f = compLoans[i] = new Field (c.getLoanValueModel());
                } else {
                    f = compLoans[i] = new Field ("");
                }
                addField (f, loansXOffset, loansYOffset + i, 1, 1, WIDE_RIGHT, visible);
            }

            f = tiles[i] = new Field(c.getTilesLaidThisTurnModel());
            addField(f, tilesXOffset, tilesYOffset + i, 1, 1, 0, visible);

            f = tileCost[i] = new Field(c.getTilesCostThisTurnModel());
            addField(f, tilesXOffset + 1, tilesYOffset + i, 1, 1, WIDE_RIGHT, visible);

            f = tokens[i] = new Field(c.getTokensLaidThisTurnModel());
            addField(f, tokensXOffset, tokensYOffset + i, 1, 1, 0, visible);

            f = tokenCost[i] = new Field(c.getTokensCostThisTurnModel());
            addField(f, tokensXOffset + 1, tokensYOffset + i, 1, 1, 0, visible);

            f = tokensLeft[i] = new Field(c.getBaseTokensModel());
            addField(f, tokensXOffset + 2, tokensYOffset + i, 1, 1,
                    bonusTokensExist ? 0 : WIDE_RIGHT, visible);

            if (bonusTokensExist) {
                f = tokenBonus[i] = new Field(c.getBonusTokensModel());
                addField(f, tokensXOffset + 3, tokensYOffset + i, 1, 1,
                        WIDE_RIGHT, visible);
            }

            f = revenue[i] = new Field(c.getLastRevenueModel());
            addField(f, revXOffset, revYOffset + i, 1, 1, 0, visible);
            f = revenueSelect[i] = new Spinner(0, 0, 0, 10);
            addField(f, revXOffset, revYOffset + i, 1, 1, 0,  false);
            revenue[i].addDependent(revenueSelect[i]);

            f = decision[i] = new Field(c.getLastRevenueAllocationModel());
            addField(f, revXOffset + 1, revYOffset + i, 1, 1, WIDE_RIGHT,  visible);

            f = trains[i] = new Field(c.getPortfolio().getTrainsModel());
            addField(f, trainsXOffset, trainsYOffset + i, 1, 1, 0,  visible);

            f = newTrainCost[i] = new Field(c.getTrainsSpentThisTurnModel());
            addField(f, trainsXOffset + 1, trainsYOffset + i, 1, 1, WIDE_RIGHT,  visible);

            f = rightCompName[i] = new Caption(c.getName());
            f.setBackground(companies[i].getBgColour());
            f.setForeground(companies[i].getFgColour());
            addField(f, rightCompNameXOffset, rightCompNameYOffset + i, 1, 1, 0,  visible);

        }

    }
    
    protected void addPrivatesInfo () {
        
        List<PrivateCompanyI> privates = orWindow.gameUIManager.getGameManager().getAllPrivateCompanies();
        if (privates == null || privates.isEmpty()) return;
        
        privatesInfoMenu = new JMenu(LocalText.getText("PRIVATES"));
        privatesInfoMenu.setEnabled(true);
        infoMenu.add(privatesInfoMenu);
        
        JMenu item;
        List<SpecialPropertyI> sps;
        StringBuffer b;

        for (PrivateCompanyI p : privates) {
            sps = p.getSpecialProperties();
            b = new StringBuffer("<html>");
            if (Util.hasValue(p.getLongName())) {
                b.append(p.getLongName());
            }
            if (sps == null || sps.isEmpty()) {
                if (b.length() > 6) b.append("<br>");
                b.append(LocalText.getText("NoSpecialProperty"));
            } else {
                for (SpecialPropertyI sp : sps) {
                    if (b.length() > 6) b.append("<br>");
                    b.append(sp.toString());
                }
            }
            item = new JMenu (p.getName());
            item.setEnabled(true);
            item.add(new JMenuItem(b.toString()));
            privatesInfoMenu.add(item);
        }
    }

    public void finish() {

        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);

    }

    public void actionPerformed(ActionEvent actor) {

        // What kind action has been taken?
        JComponent source = (JComponent) actor.getSource();
        String command = actor.getActionCommand();
        List<PossibleAction> executedActions = null;
        PossibleAction executedAction = null;

        if (source instanceof ActionTaker) {
            executedActions = ((ActionTaker) source).getPossibleActions();
            // In most cases we have only one
            if (!executedActions.isEmpty()) {
                executedAction = executedActions.get(0);
                // In all cases, the actions in the list must be
                // instances of the same class
                log.debug("Action taken is " + executedAction.toString());
            }

            if (executedAction instanceof SetDividend) {
                // Hide the spinner here, because we might not return
                // via InitPayoutStep, where this would otherwise be done.
                setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                        false);
            }

            orUIManager.processAction(command, executedActions);
        } else if (source == zoomIn) {
        	orWindow.getMapPanel().zoomIn();
        } else if (source == zoomOut) {
        	orWindow.getMapPanel().zoomOut();
        } else {
            orUIManager.processAction(command, null);
        }

    }

    public int getRevenue(int orCompIndex) {
        return ((Integer) revenueSelect[orCompIndex].getValue()).intValue();

    }

    public void setRevenue(int orCompIndex, int amount) {
        revenue[orCompIndex].setText(Bank.format(amount));
    }

    public void resetActions() {
        tileCaption.setHighlight(false);
        tokenCaption.setHighlight(false);
        revenueCaption.setHighlight(false);
        trainCaption.setHighlight(false);
        if (privatesCanBeBought) privatesCaption.setHighlight(false);
        for (int i = 0; i < president.length; i++) {
            president[i].setHighlight(false);
        }

        for (JMenuItem item : menuItemsToReset) {
            item.setEnabled(false);
            if (item instanceof ActionMenuItem) {
                ((ActionMenuItem)item).clearPossibleActions();
            }
        }
    }

    public void resetORCompanyTurn(int orCompIndex) {

        for (int i = 0; i < nc; i++) {
            setSelect(revenue[i], revenueSelect[i], false);
        }
    }

    public void initORCompanyTurn(int orCompIndex) {

        this.orCompIndex = orCompIndex;
        president[orCompIndex].setHighlight(true);

        button1.clearPossibleActions();
        button2.clearPossibleActions();
        button3.clearPossibleActions();
        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
    }

    public void initTileLayingStep() {

        tileCaption.setHighlight(true);
        button1.setVisible(false);
    }

    public void initTokenLayingStep() {

        tokenCaption.setHighlight(true);
        button1.setEnabled(false);
        button1.setVisible(false);
        button3.setEnabled(false);
    }

    public void initRevenueEntryStep(int orCompIndex, SetDividend action) {

        revenueCaption.setHighlight(true);
        revenueSelect[orCompIndex].setValue(action.getPresetRevenue());
        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], true);

        button1.setText(LocalText.getText("SET_REVENUE"));
        button1.setActionCommand(SET_REVENUE_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);
    }

    public void initPayoutStep(int orCompIndex, SetDividend action,
            boolean withhold, boolean split, boolean payout) {

        SetDividend clonedAction;

        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex], false);

        if (withhold) {
            button1.setText(LocalText.getText("WITHHOLD"));
            button1.setActionCommand(WITHHOLD_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.WITHHOLD);
            button1.setPossibleAction(clonedAction);
            button1.setMnemonic(KeyEvent.VK_W);
            button1.setEnabled(true);
            button1.setVisible(true);
        } else {
            button1.setVisible(false);
        }

        if (split) {
            button2.setText(LocalText.getText("SPLIT"));
            button2.setActionCommand(SPLIT_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.SPLIT);
            button2.setPossibleAction(clonedAction);
            button2.setMnemonic(KeyEvent.VK_S);
            button2.setEnabled(true);
            button2.setVisible(true);
        } else {
            button2.setVisible(false);
        }

        if (payout) {
            button3.setText(LocalText.getText("PAYOUT"));
            button3.setActionCommand(PAYOUT_CMD);
            clonedAction = (SetDividend) action.clone();
            clonedAction.setRevenueAllocation(SetDividend.PAYOUT);
            button3.setPossibleAction(clonedAction);
            button3.setMnemonic(KeyEvent.VK_P);
            button3.setEnabled(true);
        } else {
            button3.setVisible(false);
        }
    }

    public void initTrainBuying(boolean enabled) {

        trainCaption.setHighlight(true);

        button1.setText(LocalText.getText("BUY_TRAIN"));
        button1.setActionCommand(BUY_TRAIN_CMD);
        button1.setMnemonic(KeyEvent.VK_T);
        button1.setEnabled(enabled);
        button1.setVisible(true);
    }

    public void initPrivateBuying(boolean enabled) {

        if (privatesCanBeBought) {
            if (enabled) {
                button2.setText(LocalText.getText("BUY_PRIVATE"));
                button2.setActionCommand(BUY_PRIVATE_CMD);
                button2.setMnemonic(KeyEvent.VK_V);
            }
            button2.setEnabled(enabled);
            button2.setVisible(enabled);
            privatesCaption.setHighlight(enabled);
        } else {
            button2.setVisible(false);
        }
    }

    public void initSpecialActions() {

        specialMenu.removeAll();
        specialMenu.setEnabled(false);
        specialMenu.setOpaque(false);
    }

    public void addSpecialAction(PossibleAction action, String text) {

        ActionMenuItem item = new ActionMenuItem(text);
        item.addActionListener(this);
        item.addPossibleAction(action);
        specialMenu.add(item);
        specialMenu.setEnabled(true);
        specialMenu.setOpaque(true);
    }

    public void enableDone(NullAction action) {

        button3.setText(LocalText.getText("Done"));
        button3.setActionCommand(DONE_CMD);
        button3.setMnemonic(KeyEvent.VK_D);
        button3.setPossibleAction(action);
        button3.setEnabled(true);
    }

    public void enableUndo(GameAction action) {
        undoButton.setEnabled(action != null);
        if (action != null) undoButton.setPossibleAction(action);
    }

    public void enableRedo(GameAction action) {
        redoButton.setEnabled(action != null);
        if (action != null) redoButton.setPossibleAction(action);
    }

    public void enableLoanTaking (TakeLoans action) {
        if (action != null) takeLoans.addPossibleAction(action);
        takeLoans.setEnabled(action != null);
    }

    public void enableLoanRepayment (RepayLoans action) {

        repayLoans.setPossibleAction(action);
        repayLoans.setEnabled(true);

        loansCaption.setHighlight(true);

        button1.setText(LocalText.getText("RepayLoans"));
        button1.setActionCommand(REPAY_LOANS_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);
    }

    public void finishORCompanyTurn(int orCompIndex) {

        president[orCompIndex].setHighlight(false);

        button1.setEnabled(false);

        orCompIndex = -1;
    }

    // TEMPORARY
    public PublicCompanyI getORComp() {
        return orComp;
    }

    public String getORPlayer() {
        if (playerIndex >= 0)
            return players[playerIndex].getName();
        else
            return "";
    }

    private void setSelect(JComponent f, JComponent s, boolean active) {
        f.setVisible(!active);
        s.setVisible(active);
    }

    public PublicCompanyI[] getOperatingCompanies() {
        return companies;
    }

}

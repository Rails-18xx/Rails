package rails.ui.swing;

import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.*;
import rails.ui.swing.hexmap.HexMap;
import rails.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.List;

public class ORPanel extends JPanel implements ActionListener, KeyListener {

    private static final int NARROW_GAP = 1;

    private static final int WIDE_GAP = 3;

    private static final int WIDE_LEFT = 1;

    private static final int WIDE_RIGHT = 2;

    private static final int WIDE_TOP = 4;

    private static final int WIDE_BOTTOM = 8;
    
    private static final String BUY_PRIVATE_CMD = "BuyPrivate";
    private static final String BUY_TRAIN_CMD = "BuyTrain";
    private static final String WITHHOLD_CMD = "Withhold";
    private static final String SPLIT_CMD = "Split";
    private static final String PAYOUT_CMD = "Payout";
    private static final String SET_REVENUE_CMD = "SetRevenue";
    private static final String LAY_TILE_CMD = "LayTile";
    //private static final String LAY_TOKEN_CMD = "LayToken";
    private static final String DONE_CMD = "Done";
    private static final String UNDO_CMD = "Undo";
    private static final String FORCED_UNDO_CMD = "Undo!";
    private static final String REDO_CMD = "Redo";
    
    ORWindow orWindow;

    private JPanel statusPanel;

    private JPanel buttonPanel;

    private GridBagLayout gb;

    private GridBagConstraints gbc;

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
    private Field tiles[];
    private int tilesXOffset, tilesYOffset;
    private Field tileCost[];
    private Field tokens[];
    private Field tokenCost[];
    private Field tokensLeft[];
    private int tokensXOffset, tokensYOffset;
    private Field revenue[];
    private Spinner revenueSelect[];
    private Field decision[];
    private int revXOffset, revYOffset;
    private Field trains[];
    private int trainsXOffset, trainsYOffset;
    private Field newTrainCost[];

    private boolean privatesCanBeBought = false;

    private boolean privatesCanBeBoughtNow = false;

    private Caption tileCaption, tokenCaption, revenueCaption, trainCaption,
            privatesCaption;

    private ActionButton button1;

    private ActionButton button2;

    private ActionButton button3;
    
    private ActionButton undoButton;
    private ActionButton redoButton;

    //private int np = 0; // Number of players

    private int nc = 0; // Number of companies

    private Player[] players;

    private PublicCompanyI[] companies;

    private RoundI round, previousRound;

    private OperatingRound oRound;

    //private Player p;

    private PublicCompanyI c;

    private JComponent f;

    private List<JComponent> observers = new ArrayList<JComponent>();

    // Current state
    private int playerIndex = -1;

    private int orCompIndex = -1;

    private PublicCompanyI orComp = null;

    private String orCompName = "";

    private PossibleActions possibleActions = PossibleActions.getInstance();
    
    // Temporary fixtures: must OperatingRound be accessed at the start of updateStatus()?
    // This will disappear once all info is paseed via PossibleAction objects.
    private int orStep = 0;
    private boolean retrieveStep = true;
    
	protected static Logger log = Logger.getLogger(ORPanel.class.getPackage().getName());

    public ORPanel(ORWindow parent) {
        super();
        
        orWindow = parent;
        
        statusPanel = new JPanel();
        gb = new GridBagLayout();
        statusPanel.setLayout(gb);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setOpaque(true);

        round = GameManager.getInstance().getCurrentRound();
        privatesCanBeBought = GameManager.getCompaniesCanBuyPrivates();

        initButtonPanel();
        gbc = new GridBagConstraints();

        players = Game.getPlayerManager().getPlayersArray();
        //np = GameManager.getNumberOfPlayers();

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies();
            nc = companies.length;
        }

        initFields();

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);

        updateStatus();

        addKeyListener(this);
    }

    public void recreate() {
        log.debug ("ORPanel.recreate() called"/*, new Exception("TRACE")*/);
        round = GameManager.getInstance().getCurrentRound();
        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies();
            nc = companies.length;

            // Remove old fields. Don't forget to deregister the Observers
            deRegisterObservers();
            statusPanel.removeAll();

            // Create new fields
            initFields();
            repaint();
         }
    }

    private void deRegisterObservers() {
        log.debug ("Deregistering observers");
        if (StatusWindow.useObserver) {
            for (Iterator it = observers.iterator(); it.hasNext();) {
                ((ViewObject) it.next()).deRegister();
            }
        }
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
        addField(new Caption("Company"), 0, 0, lastXWidth = 1, 2, WIDE_BOTTOM
                + WIDE_RIGHT);

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

        tilesXOffset = currentXOffset += lastXWidth;
        tilesYOffset = leftCompNameYOffset;
        addField(tileCaption = new Caption("Tiles"), tilesXOffset, 0,
                lastXWidth = 2, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tilesXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tilesXOffset + 1, 1, 1, 1, WIDE_BOTTOM
                + WIDE_RIGHT);

        tokensXOffset = currentXOffset += lastXWidth;
        tokensYOffset = leftCompNameYOffset;
        addField(tokenCaption = new Caption("Tokens"), tokensXOffset, 0,
                lastXWidth = 3, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tokensXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tokensXOffset+1, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("left"), tokensXOffset+2, 1, 1, 1, WIDE_BOTTOM
                + WIDE_RIGHT);

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

        for (int i = 0; i < nc; i++) {
            c = companies[i];
            f = leftCompName[i] = new Caption(c.getName());
            f.setBackground(c.getBgColour());
            f.setForeground(c.getFgColour());
            addField(f, leftCompNameXOffset, leftCompNameYOffset + i, 1, 1,
                    WIDE_RIGHT);

            f = president[i] = new Field(c.hasStarted() ? c.getPresident()
                    .getName() : "");
            addField(f, presidentXOffset, presidentYOffset + i, 1, 1, 0);

            f = sharePrice[i] = new Field(c.getCurrentPriceModel());
            addField(f, sharePriceXOffset, sharePriceYOffset + i, 1, 1, 0);

            f = cash[i] = new Field(c.getCashModel());
            addField(f, cashXOffset, cashYOffset + i, 1, 1, WIDE_RIGHT);

            if (privatesCanBeBought) {
                f = privates[i] = new Field(c.getPortfolio().getPrivatesOwnedModel());
                addField(f, privatesXOffset, privatesYOffset + i, 1, 1,
                        WIDE_RIGHT);

                f = newPrivatesCost[i] = new Field(c.getPrivatesSpentThisTurnModel());
                addField(f, privatesXOffset + 1, privatesYOffset + i, 1, 1,
                        WIDE_RIGHT);
            }

            f = tiles[i] = new Field(c.getTilesLaidThisTurnModel());
            addField(f, tilesXOffset, tilesYOffset + i, 1, 1, 0);

            f = tileCost[i] = new Field(c.getTilesCostThisTurnModel());
            addField(f, tilesXOffset + 1, tilesYOffset + i, 1, 1, WIDE_RIGHT);

            f = tokens[i] = new Field(c.getTokensLaidThisTurnModel());
            addField(f, tokensXOffset, tokensYOffset + i, 1, 1, 0);

            f = tokenCost[i] = new Field(c.getTokensCostThisTurnModel());
            addField(f, tokensXOffset + 1, tokensYOffset + i, 1, 1, 0);

            f = tokensLeft[i] = new Field(c.getBaseTokensModel());
            addField(f, tokensXOffset + 2, tokensYOffset + i, 1, 1, WIDE_RIGHT);

            f = revenue[i] = new Field(c.getLastRevenueModel());
            addField(f, revXOffset, revYOffset + i, 1, 1, 0);
            f = revenueSelect[i] = new Spinner(0, 0, 0, 10);
            addField(f, revXOffset, revYOffset + i, 1, 1, 0);

            f = decision[i] = new Field(c.getLastRevenueAllocationModel());
            addField(f, revXOffset + 1, revYOffset + i, 1, 1, WIDE_RIGHT);

            f = trains[i] = new Field(c.getPortfolio().getTrainsModel());
            addField(f, trainsXOffset, trainsYOffset + i, 1, 1, 0);

            f = newTrainCost[i] = new Field(c.getTrainsSpentThisTurnModel());
            addField(f, trainsXOffset + 1, trainsYOffset + i, 1, 1, WIDE_RIGHT);

            f = rightCompName[i] = new Caption(c.getName());
            f.setBackground(companies[i].getBgColour());
            f.setForeground(companies[i].getFgColour());
            addField(f, rightCompNameXOffset, rightCompNameYOffset + i, 1, 1, 0);

        }

    }

    private void addField(JComponent comp, int x, int y, int width, int height,
            int wideGapPositions) {

        int padTop, padLeft, padBottom, padRight;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
        padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
        padBottom = (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP
                : NARROW_GAP;
        padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
        gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

        statusPanel.add(comp, gbc);

        if (StatusWindow.useObserver && comp instanceof ViewObject
                && ((ViewObject) comp).getModel() != null) {
            observers.add(comp);
        }
    }

    public void updateStatus() {
        
        updateStatus (null);
        
    }
    
    public void updateStatus (PossibleAction actionToComplete) {
        
        /* End of rails.game checks */
        if (GameManager.isGameOver()) {

            JOptionPane.showMessageDialog(this, "GAME OVER", "",
                    JOptionPane.OK_OPTION);
            JOptionPane.showMessageDialog(this, GameManager.getInstance()
                    .getGameReport(), "", JOptionPane.OK_OPTION);
            /*
             * All other wrapping up has already been done when calling
             * getSellableCertificates, so we can just finish now.
             */
            GameUILoader.statusWindow.finish();
            return;
        } else if (Bank.isJustBroken()) {
            /* The message must become configuration-depedent */
            JOptionPane
                    .showMessageDialog(this,
                            "Bank is broken. The rails.game will be over after the current set of ORs.");
        }

        round = GameManager.getInstance().getCurrentRound();
        if (round instanceof OperatingRound) {

            oRound = (OperatingRound) round;

            setHighlightsOff();
            /* Reorder the companies if the round has changed */
            if (round != previousRound)
                recreate();
            previousRound = round;

            //For debugging : log all possible actions
            List<PossibleAction> as = possibleActions.getList();
            if (as.isEmpty()) {
                log.debug ("No possible actions!!");
            } else {
                for (PossibleAction a : as) {
                    if (a instanceof LayTile) {
                        log.debug ("PossibleAction: "+((LayTile)a));
                    } else {
                        log.debug ("PossibleAction: "+a);
                    }
                }
            }
            if (actionToComplete != null) {
                log.debug("ExecutedAction: "+actionToComplete);
            }
            // End of possible action debug listing 

            if (retrieveStep) orStep = oRound.getStep();
            if (oRound.getOperatingCompanyIndex() != orCompIndex) {
                setORCompanyTurn(oRound.getOperatingCompanyIndex());
            }

            president[orCompIndex].setHighlight(true);

            privatesCanBeBoughtNow = possibleActions.contains(BuyPrivate.class);
            
            button1.clearPossibleActions();
            button2.clearPossibleActions();
            button3.clearPossibleActions();
            button3.setEnabled(false);
 
            if (possibleActions.contains(LayTile.class)) {
                tileCaption.setHighlight(true);
                button1.setVisible(false);

                orWindow.requestFocus();
                
                log.debug ("Tiles can be laid");
                orWindow.enableTileLaying(true);
                GameUILoader.getMapPanel().setAllowedTileLays (possibleActions.getType(LayTile.class));

                if (privatesCanBeBought) {
	                button2.setText(LocalText.getText("BUY_PRIVATE"));
	                button2.setActionCommand(BUY_PRIVATE_CMD);
	                button2.setMnemonic(KeyEvent.VK_V);
	                button2.setEnabled(privatesCanBeBoughtNow);
	                button2.setVisible(privatesCanBeBoughtNow);
	                privatesCaption.setHighlight(privatesCanBeBoughtNow);
                } else {
                	button2.setVisible(false);
                }

            } else if (possibleActions.contains(LayToken.class)) {
 
                orWindow.requestFocus();
                orWindow.enableTileLaying(false);
                orWindow.enableBaseTokenLaying(true);

                tokenCaption.setHighlight(true);

                log.debug ("Tokens can be laid");
                orWindow.enableBaseTokenLaying(true);
                GameUILoader.getMapPanel().setAllowedTokenLays (possibleActions.getType(LayToken.class));

                button1.setEnabled(false);
                button1.setVisible(false);
                button3.setEnabled(false);

                orWindow.updateMessage();

            } else if (possibleActions.contains(SetDividend.class)
                    && orStep == OperatingRound.STEP_CALC_REVENUE) {
                
                SetDividend action = (SetDividend) possibleActions.getType(SetDividend.class).get(0);
                
                revenueCaption.setHighlight(true);
                revenueSelect[orCompIndex].setValue(action.getPresetRevenue());
                setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                        true);

                button1.setText(LocalText.getText("SET_REVENUE"));
                button1.setActionCommand(SET_REVENUE_CMD);
                button1.setPossibleAction(action);
                button1.setMnemonic(KeyEvent.VK_R);
                button1.setEnabled(true);
                button1.setVisible(true);
                orWindow.setMessage(LocalText.getText("EnterRevenue"));
                orStep = OperatingRound.STEP_PAYOUT;
                    
            } else if (possibleActions.contains(SetDividend.class)
                    && orStep == OperatingRound.STEP_PAYOUT) {
                
                setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                        false);
                
                SetDividend action;
                if (actionToComplete != null) {
                    action = (SetDividend) actionToComplete;
                } else {
                    action = (SetDividend) possibleActions.getType(SetDividend.class).get(0);
                }
                SetDividend clonedAction;
                log.debug("Payout action before cloning: "+action);
                
                if (action.isAllocationAllowed(SetDividend.WITHHOLD)) {
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

                if (action.isAllocationAllowed(SetDividend.SPLIT)) {
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

                if (action.isAllocationAllowed(SetDividend.PAYOUT)) {
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

                orWindow.setMessage(LocalText.getText("SelectPayout"));

            } else if (possibleActions.contains(BuyTrain.class)) {
            	
                trainCaption.setHighlight(true);

                button1.setText(LocalText.getText("BUY_TRAIN"));
                button1.setActionCommand(BUY_TRAIN_CMD);
                button1.setMnemonic(KeyEvent.VK_T);
                button1.setEnabled(oRound.getOperatingCompany().mayBuyTrains());
                button1.setVisible(true);

                if (privatesCanBeBought) {
	                button2.setText(LocalText.getText("BUY_PRIVATE"));
	                button2.setActionCommand(BUY_PRIVATE_CMD);
	                button2.setMnemonic(KeyEvent.VK_V);
	                button2.setEnabled(privatesCanBeBoughtNow);
	                button2.setVisible(privatesCanBeBoughtNow);
	                privatesCaption.setHighlight(privatesCanBeBoughtNow);
                } else {
                    button2.setVisible(false);
                }

                orWindow.setMessage(LocalText.getText("BuyTrain"));

            } else if (possibleActions.contains(DiscardTrain.class)) {
                
            } else if (orStep == OperatingRound.STEP_FINAL) {
                button1.setEnabled(false);
            }
            
            boolean enableUndo = false;
            boolean enableRedo = false;
            
            if (possibleActions.contains(NullAction.class)) {
                
                List<NullAction> actions = possibleActions.getType(NullAction.class);
                for (NullAction action : actions) {
                    switch (action.getMode()) {
                    case NullAction.DONE:
                        button3.setText(LocalText.getText("Done"));
                        button3.setActionCommand(DONE_CMD);
                        button3.setMnemonic(KeyEvent.VK_D);
                        button3.setEnabled(true);
                        button3.setPossibleAction(action);
                        break;
                    case NullAction.UNDO:
                        enableUndo = true;
                        undoButton.setPossibleAction(action);
                        break;
                    case NullAction.REDO:
                        enableRedo = true;
                        redoButton.setPossibleAction(action);
                        break;
                    }
                }
            }
            
            undoButton.setEnabled (enableUndo);
            redoButton.setEnabled (enableRedo);

            if (StatusWindow.useObserver) {
                revalidate();
            } else {
                repaint();
            }
        } else if (!(round instanceof ShareSellingRound)) {
            deRegisterObservers();
            setORCompanyTurn(-1);
        }

    }

    public void actionPerformed(ActionEvent actor) {
        if (!(round instanceof OperatingRound))
            return;
        oRound = (OperatingRound) round;
        retrieveStep = true;

        // What kind action has been taken? 
        JComponent source = (JComponent) actor.getSource();
        String command = actor.getActionCommand();
        List<PossibleAction> executedActions = null;
        PossibleAction executedAction = null;
        PossibleAction executedActionToComplete = null;
        Class executedActionType = null;
        
        if (source instanceof ActionTaker) {
            executedActions = ((ActionTaker)source).getPossibleActions();
            // In most cases we have only one
            if (!executedActions.isEmpty()) {
	            executedAction = executedActions.get(0);
	            // In all cases, the actions in the list must be
	            // instances of the same class
	            executedActionType = executedAction.getClass();
	            log.debug("Action taken is "+executedAction.toString());
            }
        }
        
        int amount;

        if (executedActionType == SetDividend.class) {
            SetDividend action = (SetDividend) executedAction;
             if (command.equals(SET_REVENUE_CMD)) {
                amount = ((Integer) revenueSelect[orCompIndex].getValue()).intValue();
                log.debug ("Set revenue amount is "+amount);
                action.setActualRevenue(amount);
                if (action.getRevenueAllocation() != SetDividend.UNKNOWN) {
                    process (action);
                } else {
                    orStep = OperatingRound.STEP_PAYOUT;
                    retrieveStep = false;
                    executedActionToComplete = action;
                }
            } else {
                // The revenue allocation has been selected
                process (action);
            }
        } else if (command.equals(BUY_TRAIN_CMD)) {
            
            buyTrain();

        } else if (command.equals(BUY_PRIVATE_CMD)) {

            buyPrivate();

        } else if (executedActionType == NullAction.class) {
            
            process (executedAction);
            
        }

        ReportWindow.addLog();

        updateStatus(executedActionToComplete);

        if (!(GameManager.getInstance().getCurrentRound() instanceof OperatingRound)) {
            ORWindow.updateORWindow();
        }
    }

    protected boolean process (PossibleAction action) {

        // Add the actor for safety checking in the server 
        action.setPlayerName(getORPlayer());
        if (action instanceof PossibleORAction) {
            ((PossibleORAction)action).setCompany(orComp);
        }
        // Process the action
        boolean result = oRound.process(action);
        // Display any error message
        displayMessage();
        
        return result;
    }
    

    private void buyTrain()
	{

        List<String> prompts = new ArrayList<String>();
        Map<String, BuyTrain> promptToTrain = new HashMap<String, BuyTrain>();
        TrainI train;

        BuyTrain selectedTrain;
        String prompt;
        StringBuffer b;
        int cost;
        Portfolio from;
        
        List<BuyTrain> buyableTrains = possibleActions.getType(BuyTrain.class);
        for (BuyTrain bTrain : buyableTrains)
		{
            train = bTrain.getTrain();
            cost = bTrain.getFixedCost();
            from = bTrain.getFromPortfolio();
            
            /* Create a prompt per buying option */
            b = new StringBuffer();
            
			b.append(LocalText.getText("BUY_TRAIN_FROM", new String[] {
					        train.getName(),
                            from.getName()}));
			if (bTrain.isForExchange())
			{
				b.append(" (").append(LocalText.getText("EXCHANGED")).append(")");
            }
			if (cost > 0)
			{
				b.append(" ").append(LocalText.getText("AT_PRICE",Bank.format(cost)));
            }
			if (bTrain.mustPresidentAddCash())
			{
				b.append(" ").append(LocalText.getText("YOU_MUST_ADD_CASH",
				        Bank.format(bTrain.getPresidentCashToAdd())));
			}
			else if (bTrain.mayPresidentAddCash())
			{
				b.append(" ").append(LocalText.getText("YOU_MAY_ADD_CASH",
				        Bank.format(bTrain.getPresidentCashToAdd())));
            }
            prompt = b.toString();
            prompts.add(prompt);
            promptToTrain.put(prompt, bTrain);
        }

		if (prompts.size() == 0) {
			JOptionPane.showMessageDialog(this, 
					LocalText.getText("CannotBuyTrain"));
			return;
		}

        String boughtTrain;
 		boughtTrain = (String) JOptionPane.showInputDialog(this,
			LocalText.getText("BUY_WHICH_TRAIN"),
			LocalText.getText("WHICH_TRAIN"),
			JOptionPane.QUESTION_MESSAGE,
			null,
			prompts.toArray(),
            prompts.get(0));
		if (!Util.hasValue(boughtTrain))
			return;
        
        selectedTrain = (BuyTrain) promptToTrain.get(boughtTrain);
		if (selectedTrain == null)
			return;
        
        train = selectedTrain.getTrain();
        Portfolio seller = selectedTrain.getFromPortfolio();
        int price = selectedTrain.getFixedCost();

        if (price == 0 && seller.getOwner() instanceof PublicCompanyI) {
            prompt = LocalText.getText ("WHICH_TRAIN_PRICE",
                    new String [] {orComp.getName(), train.getName(), seller.getName()});
            String response;
            for (;;) {
            	response = JOptionPane.showInputDialog(this,
                    prompt, LocalText.getText("WHICH_PRICE"),
                    JOptionPane.QUESTION_MESSAGE);
	            if (response == null) return; // Cancel
	            try {
	                price = Integer.parseInt(response);
	            } catch (NumberFormatException e) {
	                // Price stays 0, this is handled below
	            }
	            if (price > 0) break; // Got a good (or bad, but valid) price.
	            
            	if (!prompt.startsWith("Please")) {
            	    prompt = LocalText.getText("ENTER_PRICE_OR_CANCEL")
            	        + "\n" + prompt;
            	}
            }
        }

        TrainI exchangedTrain = null;
		if (train != null && selectedTrain.isForExchange())
		{
            List<TrainI> oldTrains = selectedTrain.getTrainsForExchange();
            List<String> oldTrainOptions = new ArrayList<String>(oldTrains.size());
            String[] options = new String[oldTrains.size() + 1];
            options[0] = LocalText.getText("None");
			for (int j = 0; j < oldTrains.size(); j++)
			{
                options[j + 1] = LocalText.getText("N_Train", oldTrains.get(j).getName());
                oldTrainOptions.add(options[j+1]);
            }
			String exchangedTrainName = (String) JOptionPane.showInputDialog(this,
					LocalText.getText("WHICH_TRAIN_EXCHANGE_FOR",
                                    Bank.format(price)),
					LocalText.getText("WHICH_TRAIN_EXCHANGE"),
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[1]);
            int index = oldTrainOptions.indexOf(exchangedTrainName);
            if (index >= 0)
			{
				exchangedTrain = oldTrains.get(index);
            }

        }

		if (train != null)
		{
		    // Remember the old off-board revenue step
		    int oldOffBoardRevenueStep = PhaseManager.getInstance().getCurrentPhase().getOffBoardRevenueStep();

		    selectedTrain.setPricePaid(price);
		    selectedTrain.setExchangedTrain(exchangedTrain);

		    if (process (selectedTrain)) {
		    	
                // Check if any trains must be discarded
				// Keep looping until all relevant companies have acted
                while (possibleActions.contains(DiscardTrain.class))
				{
                    // We expect one company at a time
                    DiscardTrain dt = (DiscardTrain)possibleActions.getType(DiscardTrain.class).get(0);
                        
                    PublicCompanyI c = dt.getCompany();
                    String playerName = dt.getPlayerName();
                    List<TrainI> trains = dt.getOwnedTrains();
                    List<String> trainOptions = new ArrayList<String>(trains.size());
                    String[] options = new String[trains.size()];

                    for (int i=0; i<options.length; i++) {
                        options[i] = LocalText.getText("N_Train", trains.get(i).getName());
                        trainOptions.add(options[i]);
                    }
					String discardedTrainName = (String) JOptionPane.showInputDialog (this,
					        LocalText.getText("HAS_TOO_MANY_TRAINS", new String[] {
                                    playerName,
                                    c.getName()
                            }),
							LocalText.getText("WhichTrainToDiscard"),
                            JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[0]);
					if (discardedTrainName != null)
					{
                        TrainI discardedTrain = trains.get(trainOptions.indexOf(discardedTrainName));
                        
                        dt.setDiscardedTrain(discardedTrain);
                        
                        process (dt);
                    }
                }
			}
		    
		    int newOffBoardRevenueStep = PhaseManager.getInstance().getCurrentPhase().getOffBoardRevenueStep();
		    if (newOffBoardRevenueStep != oldOffBoardRevenueStep) {
		        HexMap.updateOffBoardToolTips();
		    }

		}

    }    

	private void buyPrivate() {

        int amount, index;
        List<String> privatesForSale = new ArrayList<String>();
        List<BuyPrivate> privates =  possibleActions.getType(BuyPrivate.class);
        String chosenOption;
        BuyPrivate chosenAction = null;
        int minPrice = 0, maxPrice = 0;

        for (BuyPrivate action : privates) {
            privatesForSale.add(LocalText.getText("BuyPrivatePrompt", new String[] {
            		action.getPrivateCompany().getName(),
            		action.getPrivateCompany().getPortfolio().getName(),
            		Bank.format(action.getMinimumPrice()),
            		Bank.format(action.getMaximumPrice())
            }));
        }

        if (privatesForSale.size() > 0) {
            chosenOption = (String) JOptionPane.showInputDialog(this, 
            		LocalText.getText("BUY_WHICH_PRIVATE"), 
            		LocalText.getText("WHICH_PRIVATE"),
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    privatesForSale.toArray(), 
                    privatesForSale.get(0));
            if (chosenOption != null) {
                index = privatesForSale.indexOf(chosenOption);
                chosenAction = privates.get(index);
                minPrice = chosenAction.getMinimumPrice();
                maxPrice = chosenAction.getMaximumPrice();
                String price = (String) JOptionPane.showInputDialog(this,
                        LocalText.getText("WHICH_PRIVATE_PRICE", new String[] {
                                chosenOption,
                                Bank.format(minPrice), 
                                Bank.format(maxPrice)}),
                        LocalText.getText("WHICH_PRICE"),
                        JOptionPane.QUESTION_MESSAGE);
                try {
                    amount = Integer.parseInt(price);
                } catch (NumberFormatException e) {
                    amount = 0; // This will generally be refused.
                }
                chosenAction.setPrice(amount);

                if (process (chosenAction)) {
                    orWindow.updateMessage();
                }
            }
        }

    }


    private void setHighlightsOff() {
        tileCaption.setHighlight(false);
        tokenCaption.setHighlight(false);
        revenueCaption.setHighlight(false);
        trainCaption.setHighlight(false);
        if (privatesCanBeBought) privatesCaption.setHighlight(false);
        if (orCompIndex >= 0)
            president[orCompIndex].setHighlight(false);
    }

    public int getOrCompIndex() {
        return orCompIndex;
    }

    public void setORCompanyTurn(int orCompIndex) {
        int j;

        if ((j = this.orCompIndex) >= 0) {
            setSelect(revenue[j], revenueSelect[j], false);
        }

        this.orCompIndex = orCompIndex;
        orComp = orCompIndex >= 0 ? companies[orCompIndex] : null;
        orCompName = orComp != null ? orComp.getName() : "";

        if ((j = this.orCompIndex) >= 0) {
            // Give a new company the turn.
            this.playerIndex = companies[orCompIndex].getPresident().getIndex();
        }
    }

    public String getORPlayer() {
        if (playerIndex >= 0)
            return players[playerIndex].getName();
        else
            return "";
    }

    public RoundI getRound() {
        return round;
    }

    private void setSelect(JComponent f, JComponent s, boolean active) {
        f.setVisible(!active);
        s.setVisible(active);
    }

    public void displayPopup(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    public void displayMessage() {
        String[] message = DisplayBuffer.get();
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(GameManager.getInstance().getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void finish() {

        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);
    }

    public PublicCompanyI[] getOperatingCompanies() {
        return companies;
    }

}

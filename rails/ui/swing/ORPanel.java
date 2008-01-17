/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORPanel.java,v 1.18 2008/01/17 21:13:48 evos Exp $*/
package rails.ui.swing;

import rails.game.*;
import rails.game.action.*;
import rails.ui.swing.elements.*;
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
    
    ORWindow orWindow;
    ORUIManager orUIManager;

    private JPanel statusPanel;

    private JPanel buttonPanel;

    private JMenuBar menuBar;
    private JMenu infoMenu;
    private JMenuItem remainingTilesMenuItem;
	private JMenu specialMenu;

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

    private Caption tileCaption, tokenCaption, revenueCaption, trainCaption,
            privatesCaption;

    private ActionButton button1;

    private ActionButton button2;

    private ActionButton button3;
    
    private ActionButton undoButton;
    private ActionButton redoButton;

    private int nc = 0; // Number of companies

    private Player[] players;

    private PublicCompanyI[] companies;

    private RoundI round;

    private PublicCompanyI c;

    private JComponent f;

    private List<JComponent> observers = new ArrayList<JComponent>();

    // Current state
    private int playerIndex = -1;

    private int orCompIndex = -1;

    private PublicCompanyI orComp = null;

	protected static Logger log = Logger.getLogger(ORPanel.class.getPackage().getName());

    public ORPanel(ORWindow parent,ORUIManager orUIManager) {
        super();
        
        orWindow = parent;
        this.orUIManager = orUIManager;
        
        statusPanel = new JPanel();
        gb = new GridBagLayout();
        statusPanel.setLayout(gb);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setOpaque(true);

        round = GameManager.getInstance().getCurrentRound();
        privatesCanBeBought = GameManager.getCompaniesCanBuyPrivates();
        bonusTokensExist = GameManager.doBonusTokensExist();

        initButtonPanel();
        gbc = new GridBagConstraints();

        players = Game.getPlayerManager().getPlayers().toArray(new Player[0]);

        if (round instanceof OperatingRound) {
            companies = ((OperatingRound) round).getOperatingCompanies();
            nc = companies.length;
        }

        initFields();

        setLayout(new BorderLayout());
        add(statusPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

		menuBar = new JMenuBar();
		
		infoMenu = new JMenu (LocalText.getText("Info"));
		infoMenu.setEnabled(true);
		remainingTilesMenuItem = new JMenuItem(LocalText.getText("RemainingTiles"));
		remainingTilesMenuItem.addActionListener(this);
		remainingTilesMenuItem.setActionCommand(REM_TILES_CMD);
		infoMenu.add(remainingTilesMenuItem);
		menuBar.add(infoMenu);
		
		specialMenu = new JMenu (LocalText.getText("SPECIAL"));
        specialMenu.setBackground(Color.YELLOW); // Normally not seen because menu is not opaque
		specialMenu.setEnabled(false);
		menuBar.add(specialMenu);
		add(menuBar, BorderLayout.NORTH);

        setVisible(true);

        addKeyListener(this);
    }

    public void recreate(OperatingRound or) {
        log.debug ("ORPanel.recreate() called"/*, new Exception("TRACE")*/);

        companies = (or).getOperatingCompanies();
        nc = companies.length;

        // Remove old fields. Don't forget to deregister the Observers
        deRegisterObservers();
        statusPanel.removeAll();

        // Create new fields
        initFields();
        repaint();
    }
    
    public void redisplay() {
        if (StatusWindow.useObserver) {
            revalidate();
        } else {
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
        if (bonusTokensExist) tokenBonus = new Field[nc];
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
        lastXWidth = bonusTokensExist ? 4 : 3;
        addField(tokenCaption = new Caption("Tokens"), tokensXOffset, 0,
                lastXWidth, 1, WIDE_RIGHT);
        addField(new Caption("laid"), tokensXOffset, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("cost"), tokensXOffset+1, 1, 1, 1, WIDE_BOTTOM);
        addField(new Caption("left"), tokensXOffset+2, 1, 1, 1, WIDE_BOTTOM
                + (bonusTokensExist ? 0 : WIDE_RIGHT));
        if (bonusTokensExist) {
        	addField(new Caption("bonus"), tokensXOffset+3, 1, 1, 1, WIDE_BOTTOM + WIDE_RIGHT);
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
            addField(f, tokensXOffset + 2, tokensYOffset + i, 1, 1, bonusTokensExist ? 0 : WIDE_RIGHT);

            if (bonusTokensExist) {
            	f = tokenBonus[i] = new Field(c.getBonusTokensModel());
            	addField(f, tokensXOffset + 3, tokensYOffset + i, 1, 1, WIDE_RIGHT);
            }

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

    public void finish() {

        button1.setEnabled(false);
        button2.setEnabled(false);
        button3.setEnabled(false);

        round = GameManager.getInstance().getCurrentRound();
        if (!(round instanceof ShareSellingRound)) {
            orUIManager.setORCompanyTurn(-1);
        }
    }

    public void actionPerformed(ActionEvent actor) {

        // What kind action has been taken? 
        JComponent source = (JComponent) actor.getSource();
        String command = actor.getActionCommand();
        List<PossibleAction> executedActions = null;
        PossibleAction executedAction = null;
        
        if (source instanceof ActionTaker) {
            executedActions = ((ActionTaker)source).getPossibleActions();
            // In most cases we have only one
            if (!executedActions.isEmpty()) {
	            executedAction = executedActions.get(0);
	            // In all cases, the actions in the list must be
	            // instances of the same class
	            log.debug("Action taken is "+executedAction.toString());
            }
            
            orUIManager.processAction (command, executedActions);
        } else {
        	orUIManager.processAction (command, null);
        }
        
    }
    
    public int getRevenue (int orCompIndex) {
    	return ((Integer) revenueSelect[orCompIndex].getValue()).intValue();

    }
    
    public void setRevenue (int orCompIndex, int amount) {
        revenue[orCompIndex].setText(Bank.format(amount));
    }

    public void setHighlightsOff() {
        tileCaption.setHighlight(false);
        tokenCaption.setHighlight(false);
        revenueCaption.setHighlight(false);
        trainCaption.setHighlight(false);
        if (privatesCanBeBought) privatesCaption.setHighlight(false);
        if (orCompIndex >= 0)
            president[orCompIndex].setHighlight(false);
    }

    
    public void resetORCompanyTurn (int orCompIndex) {
        
        int j;

        if ((j = this.orCompIndex) >= 0) {
            setSelect(revenue[j], revenueSelect[j], false);
        }
    }
    
    public void initORCompanyTurn (int orCompIndex) {
        
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
        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                true);

        button1.setText(LocalText.getText("SET_REVENUE"));
        button1.setActionCommand(SET_REVENUE_CMD);
        button1.setPossibleAction(action);
        button1.setMnemonic(KeyEvent.VK_R);
        button1.setEnabled(true);
        button1.setVisible(true);
    }
    
    public void initPayoutStep (int orCompIndex,
            SetDividend action,
            boolean withhold, boolean split, boolean payout) {
        
        SetDividend clonedAction;
        
        setSelect(revenue[orCompIndex], revenueSelect[orCompIndex],
                false);

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
    
    public void initTrainBuying (boolean enabled) {
        
        trainCaption.setHighlight(true);

        button1.setText(LocalText.getText("BUY_TRAIN"));
        button1.setActionCommand(BUY_TRAIN_CMD);
        button1.setMnemonic(KeyEvent.VK_T);
        button1.setEnabled(enabled);
        button1.setVisible(true);
    }
    
    public void initPrivateBuying (boolean enabled) {
        
        if (privatesCanBeBought) {
            button2.setText(LocalText.getText("BUY_PRIVATE"));
            button2.setActionCommand(BUY_PRIVATE_CMD);
            button2.setMnemonic(KeyEvent.VK_V);
            button2.setEnabled(enabled);
            button2.setVisible(enabled);
            privatesCaption.setHighlight(enabled);
        } else {
            button2.setVisible(false);
        }
    }
    
    public void initSpecialActions () {
        
        specialMenu.removeAll();
        specialMenu.setEnabled(false);
        specialMenu.setOpaque(false);
    }
    
    public void addSpecialAction (PossibleAction action, String text) {
        
        ActionMenuItem item = new ActionMenuItem (text);
    	item.addActionListener(this);
    	item.addPossibleAction(action);
    	specialMenu.add(item);
        specialMenu.setEnabled(true);
        specialMenu.setOpaque(true);
    }
    
    public void enableDone (NullAction action) {
        
        button3.setText(LocalText.getText("Done"));
        button3.setActionCommand(DONE_CMD);
        button3.setMnemonic(KeyEvent.VK_D);
        button3.setPossibleAction(action);
        button3.setEnabled(true);
    }
    
    public void enableUndo (GameAction action) {
        undoButton.setEnabled(action != null);
        if (action != null) undoButton.setPossibleAction(action);
    }
    
    public void enableRedo (GameAction action) {
        redoButton.setEnabled (action != null);
        if (action != null) redoButton.setPossibleAction(action);
    }
    
    public void finishORCompanyTurn (int orCompIndex) {
        
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

    public void displayPopup(String text) {
        JOptionPane.showMessageDialog(this, text);
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

    public PublicCompanyI[] getOperatingCompanies() {
        return companies;
    }

}

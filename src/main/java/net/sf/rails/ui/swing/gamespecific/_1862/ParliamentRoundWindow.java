package net.sf.rails.ui.swing.gamespecific._1862;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.HelpWindow;
import net.sf.rails.ui.swing.StartRoundWindow;
import net.sf.rails.ui.swing.elements.NonModalDialog;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.swing.*;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.specific._1862.BidParliamentAction; 
import net.sf.rails.game.specific._1862.BuyParliamentAction;
import net.sf.rails.game.specific._1862.ParliamentBiddableItem;
import net.sf.rails.game.specific._1862.ParliamentRound;
import net.sf.rails.game.specific._1862.PublicCompany_1862;
import net.sf.rails.ui.swing.elements.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;

import com.google.common.collect.ImmutableList;

public class ParliamentRoundWindow extends StartRoundWindow {

    private static final long serialVersionUID = 1L;

    // Gap sizes between screen cells, in pixels
    private static final int NARROW_GAP = 1;
    private static final int WIDE_GAP = 3;
    // Bits for specifying where to apply wide gaps
    private static final int WIDE_LEFT = 1;
    private static final int WIDE_RIGHT = 2;
    private static final int WIDE_TOP = 4;
    private static final int WIDE_BOTTOM = 8;

    private static final Color buyableColour = new Color(0, 128, 0);
    private static final Color soldColour = new Color(128, 128, 128);
    private static final Color defaultColour = Color.BLACK;

    private StatusPanel statusPanel;
    private ButtonPanel buttonPanel;

    private ParliamentBiddableItem selectedBiddable;
    
    private GridBagConstraints gbc;


    private ImageIcon infoIcon = null;

    private Player[] players;
    private ParliamentRound round;
    private GameUIManager gameUIManager;

    // For the non-modal dialog to ask for a company starting share price.
    protected JDialog currentDialog = null;
    protected PossibleAction currentDialogAction = null;
    protected SortedSet<StockSpace> startSpaces = null;

    public static final String PAR_DIALOG = "ParDialog";
    public static final String NUM_SHARES_DIALOG = "NumSharesDialog";

    /** @see StartItem.statusName */
    public static final String[] itemStatusTextKeys = new String[] {
            "Status_Unavailable", "Status_Biddable", "Status_Buyable",
            "Status_Selectable", "Status_Auctioned",
            "Status_NeedingSharePrice", "Status_Sold" };

    // Current state
    private int playerIndex = -1;

    protected PossibleActions possibleActions;
    protected PossibleAction immediateAction = null;

    private final ButtonGroup itemGroup = new ButtonGroup();

    private ArrayList<ParliamentBiddableItem> biddables;


    protected static Logger log =
            LoggerFactory.getLogger(ParliamentRoundWindow.class);

    public class StatusPanel extends JPanel implements ActionListener {
        private static final long serialVersionUID = 1L;
        private GridBagLayout gb;
        private ImmutableList<Player> players;
        private ArrayList<ParliamentBiddableItem> biddables;
        private ParliamentRoundWindow parent;

        private Caption[] upperPlayerCaption;
        private Caption[] lowerPlayerCaption;
        private Field[] playerCash;
        private Caption itemName[];
        private ClickField itemNameButton[];
        private Field bidPerPlayer[][];
        private Field info[]; // TODO: Needed?
        
        public StatusPanel(ParliamentRoundWindow parent, ImmutableList<Player> players, ArrayList<ParliamentBiddableItem> biddables) {
            super();
            this.parent = parent;
            this.players = players;
            this.biddables = biddables;
            
            gb = new GridBagLayout();
            setLayout(gb);
            setBorder(BorderFactory.createEtchedBorder());
            
            gbc = new GridBagConstraints();
            
            addFields();
            
            setOpaque(true);
        }
        
        private void addFields() {
            addStaticCaptions();
            addPlayerCaptions();
            addCompanies();
        }
        
        private void addStaticCaptions() {
            addField(new Caption(LocalText.getText("ITEM")), 0, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM);
            addField(new Caption(LocalText.getText("PLAYERS")), 1, 0, players.size(), 1, 0);
            addField(new Caption(LocalText.getText("CASH")), 0, biddables.size()+3, 1, 1, WIDE_RIGHT + WIDE_TOP);
        }
        
        private void addPlayerCaptions() {
            upperPlayerCaption  = new Caption[players.size()];
            lowerPlayerCaption  = new Caption[players.size()];
            playerCash = new Field[players.size()];
            for (int i = 0; i < players.size(); i++) {
                upperPlayerCaption[i] = new Caption(players.get(i).getId());
                lowerPlayerCaption[i] = new Caption(players.get(i).getId());
                playerCash[i] = new Field(players.get(i).getWallet());
                addField(upperPlayerCaption[i], i+1, 1, 1, 1, WIDE_BOTTOM);
                addField(lowerPlayerCaption[i], i+1, biddables.size()+2, 1, 1, WIDE_BOTTOM);
                addField(playerCash[i], i+1, biddables.size()+3, 1, 1, 0);
            }            
        }        
        
        private void addCompanies() {
            itemName = new Caption[biddables.size()];
            itemNameButton = new ClickField[biddables.size()];
            bidPerPlayer = new Field[biddables.size()][players.size()];
            info = new Field[biddables.size()];
            infoIcon = createInfoIcon();

            for (int i = 0; i < biddables.size(); i++) {
                PublicCompany_1862 company = biddables.get(i).getCompany();
                itemName[i] = new Caption(company.getLongName());
                addField(itemName[i], 0, 2 + i, 1, 1, WIDE_RIGHT);
                itemNameButton[i] = new ClickField(company.getLongName(), "", "", this, itemGroup);
                addField(itemNameButton[i], 0, 2 + i, 1, 1, WIDE_RIGHT);
                itemName[i].setPreferredSize(itemNameButton[i].getPreferredSize());
                
                itemName[i].setVisible(false);
                itemNameButton[i].setVisible(true);
                String tooltip = "bar"; // TODO:
                itemName[i].setToolTipText(tooltip);
                itemNameButton[i].setToolTipText("");

                itemName[i].setForeground(defaultColour);
                itemNameButton[i].setForeground(buyableColour);
                
                for (int j = 0; j < players.size(); j++) {
                    bidPerPlayer[i][j] = new Field(biddables.get(i).getBidModels().get(j));
                    addField(bidPerPlayer[i][j], j+1, i+2, 1, 1, 0);
                }
                
                info[i] = new Field(infoIcon);
                info[i].setToolTipText("foo");
                addField(info[i], players.size()+1, i+2, 1, 1, WIDE_LEFT);

            }
        }
        
        public void actionPerformed(ActionEvent actor) {
            JComponent source = (JComponent) actor.getSource();
            for (int i = 0; i < itemNameButton.length; i++) {
                if (source == itemNameButton[i]) {
                    parent.biddableSelected(biddables.get(i));
                }
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
            padBottom = (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP : NARROW_GAP;
            padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
            gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

            add(comp, gbc);
        }

        public void highlightPlayer(int playerIndex) {
            for (int i = 0; i < players.size(); i++) {
                if (i == playerIndex) {
                    upperPlayerCaption[i].setHighlight(true);
                    lowerPlayerCaption[i].setHighlight(true);
                } else {
                    upperPlayerCaption[i].setHighlight(false);
                    lowerPlayerCaption[i].setHighlight(false);
                }
            }
        }
        
        public void resetAllBiddables() {
            for (int i = 0; i < biddables.size(); i++) {
                itemNameButton[i].setEnabled(false);
                itemNameButton[i].setSelected(false);
                itemName[i].setForeground (
                        biddables.get(i).hasBeenSold() ? soldColour : defaultColour);
//                int status = Integer.parseInt(itemStatus[i].getText());
//                String tooltip = LocalText.getText(itemStatusTextKeys[status]);
//
//                itemName[i].setToolTipText(clickable ? "" : tooltip);
//                itemNameButton[i].setToolTipText(clickable ? tooltip : "");
            }
        }
        
        
        public void setBiddablePickable(ParliamentBiddableItem biddable) {
            for (int i = 0; i < biddables.size(); i++) {
                if (biddables.get(i) == biddable) {
                    itemNameButton[i].setEnabled(true);
                }
            }
        }
        
        public void setBiddableSelected(ParliamentBiddableItem biddable) {
            for (int i = 0; i < biddables.size(); i++) {
                if (biddables.get(i) == biddable) {
                    itemNameButton[i].setSelected(true);
                    parent.biddableSelected(biddable);
                }
            }
        }

        public void setBiddableUnSelected(ParliamentBiddableItem biddable) {
            for (int i = 0; i < biddables.size(); i++) {
                if (biddables.get(i) == biddable) {
                    itemNameButton[i].setSelected(false);
                    parent.biddableSelected(biddable);
                }
            }         
        }
        
        
    }
    
    public class ButtonPanel extends JPanel implements ActionListener {
        private static final long serialVersionUID = 1L;
        private ActionButton bidButton;
        private JSpinner bidAmount;
        private SpinnerNumberModel spinnerModel;
        private ActionButton passButton;
        private ParliamentRoundWindow parent;

        public ButtonPanel(ParliamentRoundWindow parent) {
            super();
            this.parent = parent;
            
            bidButton = new ActionButton(RailsIcon.BID);
            bidButton.setMnemonic(KeyEvent.VK_D);
            bidButton.addActionListener(this);
            bidButton.setEnabled(false);
            add(bidButton);
            
            spinnerModel = new SpinnerNumberModel(new Integer(999), new Integer(0), null, new Integer(1));
            bidAmount = new JSpinner(spinnerModel);
            bidAmount.setPreferredSize(new Dimension(50, 28));
            bidAmount.setEnabled(false);
            add(bidAmount);

            passButton = new ActionButton(RailsIcon.PASS);
            passButton.setMnemonic(KeyEvent.VK_P);
            passButton.addActionListener(this);
            passButton.setEnabled(false);            
            add(passButton);

            setOpaque(true);
        }

        public void actionPerformed(ActionEvent actor) {
            if (actor.getSource() == bidButton) {
                parent.bidPlaced(spinnerModel.getNumber().intValue());
            } else if (actor.getSource() == passButton) {
                parent.passPerformed();
            }
        }

        public void startBidding(int minimumBid, Number bidIncrement) {
            bidButton.setEnabled(true);
            spinnerModel.setMinimum(minimumBid);
            spinnerModel.setStepSize(bidIncrement);
            spinnerModel.setValue(minimumBid);   
            bidAmount.setEnabled(true);
        }
        
        public void passEnabled(boolean enabled) {
            passButton.setEnabled(enabled);
        }

        public void disableButtons() {
            bidButton.setEnabled(false);
            passButton.setEnabled(false);            
        }
    }

    public void init(StartRound round, GameUIManager parent) {
        this.round = (ParliamentRound) round;
        gameUIManager = parent;
        possibleActions = gameUIManager.getGameManager().getPossibleActions();
        biddables = this.round.getBiddables();
        players = gameUIManager.getRoot().getPlayerManager().getPlayers().toArray(new Player[0]);
        
        setTitle(LocalText.getText("START_ROUND_TITLE"));
        getContentPane().setLayout(new BorderLayout());

        statusPanel = new StatusPanel(this, gameUIManager.getRoot().getPlayerManager().getPlayers(), biddables);
        buttonPanel = new ButtonPanel(this);

        getContentPane().add(statusPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Rails: Parliament Round");
        setLocation(300, 150);
        setSize(275, 325);
        gameUIManager.setMeVisible(this, true);
        requestFocus();

        addKeyListener(this);

        // set closing behavior and listener
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        final JFrame thisFrame = this;
        final GameUIManager guiMgr = gameUIManager;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(thisFrame,
                        LocalText.getText("CLOSE_WINDOW"),
                        LocalText.getText("Select"),
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    thisFrame.dispose();
                    guiMgr.terminate();
                }
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                guiMgr.getWindowSettings().set(thisFrame);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                guiMgr.getWindowSettings().set(thisFrame);
            }
        });

        gameUIManager.packAndApplySizing(this);
    }


    public void updateStatus(boolean myTurn) {
        if (!myTurn) return;

        // For debugging
        for (PossibleAction action : possibleActions.getList()) {
//            System.out.println(action.getPlayerName() + " may: " + action);
            log.debug(action.getPlayerName() + " may: " + action);
        }

        if (possibleActions == null || possibleActions.isEmpty()) {
            close();
            return;
        }

        List<BuyParliamentAction> buyActions = possibleActions.getType(BuyParliamentAction.class);
        if ((buyActions != null) && (!buyActions.isEmpty())) {
            requestParValue(buyActions.get(0));
            return;
        }
        
        statusPanel.resetAllBiddables();
        
        List<BidParliamentAction> bidActions = possibleActions.getType(BidParliamentAction.class);
        for (BidParliamentAction bidAction : bidActions) {
            statusPanel.setBiddablePickable(bidAction.getBiddable());
        }
        if (bidActions.size() == 1) {
            statusPanel.setBiddableSelected(bidActions.get(0).getBiddable());
        }
        
        List<NullAction> inactiveItems = possibleActions.getType(NullAction.class);
        if ((inactiveItems != null) && (!inactiveItems.isEmpty())) {
            buttonPanel.passEnabled(true);
        } else {
            buttonPanel.passEnabled(false);
        }

        pack(); // to avoid not displaying after label size changes
        requestFocus();
    }


    
    public void biddableSelected(ParliamentBiddableItem biddable) {
        selectedBiddable = biddable;
        int startBid = (biddable.hasBeenBidOn() ? biddable.getCurrentBid() + biddable.getBidIncrement() : biddable.getMinimumBid());
        buttonPanel.startBidding(startBid, biddable.getBidIncrement());
    }
    
    public void bidPlaced(int i) {
        for (PossibleAction action : possibleActions.getList()) {
            if (action instanceof BidParliamentAction) {
                BidParliamentAction bpa = (BidParliamentAction) action;
                if (bpa.getBiddable() == selectedBiddable) {
                    bpa.setActualBid(i);
                    process(bpa);
                    break;
                }
            }
        }
    }
    
    public void passPerformed() {
        List<NullAction> inactiveItems = possibleActions.getType(NullAction.class);
        NullAction passAction = inactiveItems.get(0); // Only one is pass.
        process(passAction);
    }

    public BidParliamentAction findAction(ParliamentBiddableItem biddable) {
        for (PossibleAction action : possibleActions.getList()) {
            if ((action instanceof BidParliamentAction) && 
                    (((BidParliamentAction) action).getBiddable() == biddable)) {
                return (BidParliamentAction) action;
            }
        }
        return null;
    }

    
    private boolean requestParValue(BuyParliamentAction action) {
        ArrayList<Integer> parValues = action.getBiddable().getPossibleParValues();
        String [] pv = new String[parValues.size()];
        for (int i = 0; i < parValues.size(); i++) {
            pv[i] = Integer.toString(parValues.get(i));
        }
        RadioButtonDialog dialog = new RadioButtonDialog(
                PAR_DIALOG, this, this, // TODO
                LocalText.getText("PleaseSelect"),
                LocalText.getText("WHICH_PAR_PRICE", action.getPlayerName(), action.getBiddable().getCompany().getLongName()),
                        pv, 0);
        setCurrentDialog (dialog, action);
        return true;
    }

    private void handleParValue() {
        RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
        BuyParliamentAction action = (BuyParliamentAction) currentDialogAction;

        int index = dialog.getSelectedOption();
        if (index >= 0) {
            action.setParPrice(action.getBiddable().getPossibleParValues().get(index));  
            requestNumShares(action);
        } 
    }
    
    private boolean requestNumShares(BuyParliamentAction action) {
        String [] numShares = new String[3];
        numShares[0] = "3";
        numShares[1] = "4";
        numShares[2] = "5";
        RadioButtonDialog dialog = new RadioButtonDialog(
                NUM_SHARES_DIALOG, this, this, // TODO
                LocalText.getText("PleaseSelect"),       
                LocalText.getText("HOW_MANY_SHARES"),
                        numShares, 0);
        setCurrentDialog (dialog, action);
        return true;
    }
    
    private void handleNumShares() {
        RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
        BuyParliamentAction action = (BuyParliamentAction) currentDialogAction;

        int index = dialog.getSelectedOption();
        if (index >= 0) {
            action.setSharesPurchased(index + 3);
            statusPanel.setBiddableUnSelected(action.getBiddable());
            process(action);
        } 
    }

    public JDialog getCurrentDialog() {
        return currentDialog;
    }

    public PossibleAction getCurrentDialogAction() {
        return currentDialogAction;
    }

    public void setCurrentDialog(JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = dialog;
        currentDialogAction = action;
        disableButtons();
    }

    public void dialogActionPerformed() {
        String key="";
        
        if (currentDialog instanceof NonModalDialog) {
            key = ((NonModalDialog) currentDialog).getKey();
        }
        
        if (PAR_DIALOG.equals(key)) {
            handleParValue();
        } else if (NUM_SHARES_DIALOG.equals(key)) {
            handleNumShares();
            
        }
        return;
    }

    protected void disableButtons() {
        buttonPanel.disableButtons();
    }

    public void close() {
        this.dispose();
    }

    public void setSRPlayerTurn(int selectedPlayerIndex) {
        playerIndex = selectedPlayerIndex;
        statusPanel.highlightPlayer(playerIndex);
    }

    public String getSRPlayer() {
        if (playerIndex >= 0)
            return players[playerIndex].getId();
        else
            return "";
    }

    private ImageIcon createInfoIcon() {
        return RailsIcon.INFO.smallIcon;
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameUIManager.getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public boolean process(PossibleAction action) {
        return gameUIManager.processAction(action);
    }
    
}
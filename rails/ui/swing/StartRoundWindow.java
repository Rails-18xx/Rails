/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/StartRoundWindow.java,v 1.38 2010/06/10 20:54:31 stefanfrey Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.action.*;
import rails.game.special.SpecialPropertyI;
import rails.ui.swing.elements.*;
import rails.util.LocalText;

/**
 * This displays the Auction Window
 */
public class StartRoundWindow extends JFrame implements ActionListener,
        KeyListener, ActionPerformer {
    private static final long serialVersionUID = 1L;

    // Gap sizes between screen cells, in pixels
    private static final int NARROW_GAP = 1;
    private static final int WIDE_GAP = 3;
    // Bits for specifying where to apply wide gaps
    private static final int WIDE_LEFT = 1;
    private static final int WIDE_RIGHT = 2;
    private static final int WIDE_TOP = 4;
    private static final int WIDE_BOTTOM = 8;

    private static final Color buyableColour = new Color (0, 128, 0);
    private static final Color soldColour = new Color (128, 128, 128);
    private static final Color defaultColour = Color.BLACK;

    private final JPanel statusPanel;
    private final JPanel buttonPanel;

    private final GridBagLayout gb;
    private GridBagConstraints gbc;

    // Grid elements per function
    private Caption itemName[];
    private ClickField itemNameButton[];
    private int itemNameXOffset, itemNameYOffset;
    private Field basePrice[];
    private int basePriceXOffset, basePriceYOffset;
    private Field minBid[];
    private int minBidXOffset, minBidYOffset;
    private Field bidPerPlayer[][];
    private int bidPerPlayerXOffset, bidPerPlayerYOffset;
    private Field playerBids[];
    private int playerBidsXOffset, playerBidsYOffset;
    private Field playerFree[];
    private int playerFreeCashXOffset, playerFreeCashYOffset;
    private Field info[];
    private int infoXOffset, infoYOffset;
    private Field itemStatus[]; // Remains invisible, only used for status tooltip

    private Caption[] upperPlayerCaption;
    private Caption[] lowerPlayerCaption;

    private ActionButton bidButton;
    private final ActionButton buyButton;
    private JSpinner bidAmount;
    private SpinnerNumberModel spinnerModel;
    private final ActionButton passButton;

    private ImageIcon infoIcon = null;

    private final int np; // Number of players
    private final int ni; // Number of start items
    private final Player[] players;
    private final StartItem[] items;
    private final StartItemAction[] actionableItems;
    private final StartPacket packet;
    private final int[] crossIndex;
    private final StartRound round;
    private final GameUIManager gameUIManager;

    private StartItem si;
    private JComponent f;

    /** @see StartItem.statusName */
    public static final String[] itemStatusTextKeys =
        new String[] { "Status_Unavailable", "Status_Biddable", "Status_Buyable",
                "Status_Selectable", "Status_Auctioned",
                "Status_NeedingSharePrice", "Status_Sold" };

    // Current state
    private int playerIndex = -1;

    private final PossibleActions possibleActions =
            PossibleActions.getInstance();
    private PossibleAction immediateAction = null;

    private final ButtonGroup itemGroup = new ButtonGroup();
    private ClickField dummyButton; // To be selected if none else is.

    private final boolean includeBidding;
    private final boolean showBasePrices;

//    private boolean repacked = false;

    protected static Logger log =
            Logger.getLogger(StartRoundWindow.class.getPackage().getName());

    public StartRoundWindow(StartRound round, GameUIManager parent) {
        super();
        this.round = round;
        includeBidding = round.hasBidding();
        showBasePrices = round.hasBasePrices();
        gameUIManager = parent;
        setTitle(LocalText.getText("START_ROUND_TITLE"));
        getContentPane().setLayout(new BorderLayout());

        statusPanel = new JPanel();
        gb = new GridBagLayout();
        statusPanel.setLayout(gb);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setOpaque(true);

        buttonPanel = new JPanel();

        buyButton = new ActionButton(LocalText.getText("BUY"));
        buyButton.setMnemonic(KeyEvent.VK_B);
        buyButton.addActionListener(this);
        buyButton.setEnabled(false);
        buttonPanel.add(buyButton);

        if (includeBidding) {
            bidButton = new ActionButton(LocalText.getText("BID") + ":");
            bidButton.setMnemonic(KeyEvent.VK_D);
            bidButton.addActionListener(this);
            bidButton.setEnabled(false);
            buttonPanel.add(bidButton);

            spinnerModel =
                    new SpinnerNumberModel(new Integer(999), new Integer(0),
                            null, new Integer(1));
            bidAmount = new JSpinner(spinnerModel);
            bidAmount.setPreferredSize(new Dimension(50, 28));
            bidAmount.setEnabled(false);
            buttonPanel.add(bidAmount);
        }

        passButton = new ActionButton(LocalText.getText("PASS"));
        passButton.setMnemonic(KeyEvent.VK_P);
        passButton.addActionListener(this);
        passButton.setEnabled(false);
        buttonPanel.add(passButton);

        buttonPanel.setOpaque(true);

        gbc = new GridBagConstraints();

        players = gameUIManager.getGameManager().getPlayers().toArray(new Player[0]);
        np = gameUIManager.getGameManager().getNumberOfPlayers();
        packet = round.getStartPacket();
        crossIndex = new int[packet.getNumberOfItems()];

        items = round.getStartItems().toArray(new StartItem[0]);
        ni = items.length;
        StartItem item;
        for (int i = 0; i < ni; i++) {
            item = items[i];
            crossIndex[item.getIndex()] = i;
        }

        actionableItems = new StartItemAction[ni];

        infoIcon = createInfoIcon();

        init();

        getContentPane().add(statusPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setTitle("Rails: Start Round");
        setLocation(300, 150);
        setSize(275, 325);
        setVisible(true);
        requestFocus();

        addKeyListener(this);

        // set closing behavior and listener
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE );
        final JFrame thisFrame = this;
        final GameUIManager guiMgr = gameUIManager;
        addWindowListener(new WindowAdapter () {
            @Override
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(thisFrame, LocalText.getText("CLOSE_WINDOW"), LocalText.getText("Select"), JOptionPane.OK_CANCEL_OPTION)
                        == JOptionPane.OK_OPTION) {
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

        pack();

        WindowSettings ws = gameUIManager.getWindowSettings();
        Rectangle bounds = ws.getBounds(this);
        if (bounds.x != -1 && bounds.y != -1) setLocation(bounds.getLocation());
        if (bounds.width != -1 && bounds.height != -1) setSize(bounds.getSize());
        ws.set(thisFrame);
    }

    private void init() {
        int lastX = -1;
        int lastY = 1;

        itemName = new Caption[ni];
        itemNameButton = new ClickField[ni];
        basePrice = new Field[ni];
        minBid = new Field[ni];
        bidPerPlayer = new Field[ni][np];
        info = new Field[ni];
        itemStatus = new Field[ni];
        upperPlayerCaption = new Caption[np];
        lowerPlayerCaption = new Caption[np];
        playerBids = new Field[np];
        playerFree = new Field[np];

        itemNameXOffset = ++lastX;
        itemNameYOffset = ++lastY;
        if (showBasePrices) {
            basePriceXOffset = ++lastX;
            basePriceYOffset = lastY;
        }
        if (includeBidding) {
            minBidXOffset = ++lastX;
            minBidYOffset = lastY;
        }
        bidPerPlayerXOffset = ++lastX;
        bidPerPlayerYOffset = lastY;

        infoXOffset = bidPerPlayerXOffset + np;
        infoYOffset = lastY;

        // Bottom rows
        lastY += (ni - 1);
        if (includeBidding) {
            playerBidsXOffset = bidPerPlayerXOffset;
            playerBidsYOffset = ++lastY;
        }
        playerFreeCashXOffset = bidPerPlayerXOffset;
        playerFreeCashYOffset = ++lastY;

        addField(new Caption(LocalText.getText("ITEM")), 0, 0, 1, 2,
                WIDE_RIGHT + WIDE_BOTTOM);
        if (showBasePrices) {
            addField(new Caption(LocalText.getText(includeBidding
                    ? "BASE_PRICE" : "PRICE")), basePriceXOffset, 0, 1, 2,
                    WIDE_BOTTOM);
        }
        if (includeBidding) {
            addField(new Caption(LocalText.getText("MINIMUM_BID")),
                    minBidXOffset, 0, 1, 2, WIDE_BOTTOM + WIDE_RIGHT);
        }
        addField(new Caption(LocalText.getText("PLAYERS")),
                bidPerPlayerXOffset, 0, np, 1, 0);
        for (int i = 0; i < np; i++) {
            f = upperPlayerCaption[i] = new Caption(players[i].getName());
            addField(f, bidPerPlayerXOffset + i, 1, 1, 1, WIDE_BOTTOM);
        }

        for (int i = 0; i < ni; i++) {
            si = items[i];
            f = itemName[i] = new Caption(si.getName());
            addField(f, itemNameXOffset, itemNameYOffset + i, 1, 1, WIDE_RIGHT);
            f =
                    itemNameButton[i] =
                            new ClickField(si.getName(), "", "", this,
                                    itemGroup);
            addField(f, itemNameXOffset, itemNameYOffset + i, 1, 1, WIDE_RIGHT);
            // Prevent row height resizing after every buy action
            itemName[i].setPreferredSize(itemNameButton[i].getPreferredSize());

            if (showBasePrices) {
                f = basePrice[i] = new Field(si.getBasePriceModel());
                addField(f, basePriceXOffset, basePriceYOffset + i, 1, 1, 0);
            }

            if (includeBidding) {
                f = minBid[i] = new Field(round.getMinimumBidModel(i));
                addField(f, minBidXOffset, minBidYOffset + i, 1, 1, WIDE_RIGHT);
            }

            for (int j = 0; j < np; j++) {
                f = bidPerPlayer[i][j] = new Field(round.getBidModel(i, j));
                addField(f, bidPerPlayerXOffset + j, bidPerPlayerYOffset + i,
                        1, 1, 0);
            }

            f = info[i] = new Field (infoIcon);
            f.setToolTipText(getStartItemDescription(si));
            addField (f, infoXOffset, infoYOffset + i, 1, 1, WIDE_LEFT);

            // Invisible field, only used to hold current item status.
            f = itemStatus[i] = new Field (si.getStatusModel());
        }

        // Player money
        boolean firstBelowTable = true;
        if (includeBidding) {
            addField(new Caption(LocalText.getText("BID")),
                    playerBidsXOffset - 1, playerBidsYOffset, 1, 1,
                    WIDE_TOP + WIDE_RIGHT);
            for (int i = 0; i < np; i++) {
                f = playerBids[i] = new Field(round.getBlockedCashModel(i));
                addField(f, playerBidsXOffset + i, playerBidsYOffset, 1, 1,
                        WIDE_TOP);
            }
            firstBelowTable = false;
        }

        addField(new Caption(
                LocalText.getText(includeBidding ? "FREE" : "CASH")),
                playerFreeCashXOffset - 1, playerFreeCashYOffset, 1, 1,
                WIDE_RIGHT + (firstBelowTable ? WIDE_TOP : 0));
        for (int i = 0; i < np; i++) {
            f =
                    playerFree[i] =
                            new Field(includeBidding
                                    ? round.getFreeCashModel(i)
                                    : players[i].getCashModel());
            addField(f, playerFreeCashXOffset + i, playerFreeCashYOffset, 1, 1,
                    firstBelowTable ? WIDE_TOP : 0);
        }

        for (int i = 0; i < np; i++) {
            f = lowerPlayerCaption[i] = new Caption(players[i].getName());
            addField(f, playerFreeCashXOffset + i, playerFreeCashYOffset + 1,
                    1, 1, WIDE_TOP);
        }

        dummyButton = new ClickField("", "", "", this, itemGroup);

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
        padBottom =
                (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP : NARROW_GAP;
        padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
        gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

        statusPanel.add(comp, gbc);

    }

    public void updateStatus(boolean myTurn) {
        StartItem item;
        int i, j;

        for (i = 0; i < ni; i++) {
            setItemNameButton(i, false);
            actionableItems[i] = null;
        }
        // Unselect the selected private
        dummyButton.setSelected(true);
        
        if (!myTurn) return;

        // For debugging
        for (PossibleAction action : possibleActions.getList()) {
            log.debug(action.getPlayerName() + " may: " + action);
        }

        List<StartItemAction> actions =
                possibleActions.getType(StartItemAction.class);

        if (actions == null || actions.isEmpty()) {
            close();
            return;
        }

        int nextPlayerIndex =
                ((PossibleAction) actions.get(0)).getPlayerIndex();
        setSRPlayerTurn(nextPlayerIndex);

        boolean passAllowed = false;
        boolean buyAllowed = false;
        boolean bidAllowed = false;

        boolean selected = false;

        BuyStartItem buyAction;

        for (StartItemAction action : actions) {
            j = action.getItemIndex();
            i = crossIndex[j];
            actionableItems[i] = action;
            item = action.getStartItem();

            if (action instanceof BuyStartItem) {

                buyAction = (BuyStartItem) action;
                if (!buyAction.setSharePriceOnly()) {

                    selected = buyAction.isSelected();
                    if (selected) {
                        buyButton.setPossibleAction(action);
                    } else {
                        //itemNameButton[i].setToolTipText(LocalText.getText("ClickToSelectForBuying"));
                        itemNameButton[i].setPossibleAction(action);
                    }
                    itemNameButton[i].setSelected(selected);
                    itemNameButton[i].setEnabled(!selected);
                    setItemNameButton(i, true);
                    if (includeBidding && showBasePrices)
                        minBid[i].setText("");
                    buyAllowed = selected;

                } else {
                    PossibleAction lastAction = gameUIManager.getLastAction();
                    if (lastAction instanceof GameAction
                        && (((GameAction) lastAction).getMode() == GameAction.UNDO || ((GameAction) lastAction).getMode() == GameAction.FORCED_UNDO)) {
                        // If we come here via an Undo, we should not start
                        // with a modal dialog, as that would prevent further
                        // Undos.
                        // So there is an extra step: let the player press Buy
                        // first.
                        setItemNameButton(i, true);
                        itemNameButton[i].setSelected(true);
                        itemNameButton[i].setEnabled(false);
                        buyButton.setPossibleAction(action);
                        buyAllowed = true;

                    } else {
                        immediateAction = action;
                    }
                }

            } else if (action instanceof BidStartItem) {

                BidStartItem bidAction = (BidStartItem) action;
                selected = bidAction.isSelected();
                if (selected) {
                    bidButton.addPossibleAction(action);
                    bidButton.setPossibleAction(action);
                    int mb = bidAction.getMinimumBid();
                    spinnerModel.setMinimum(mb);
                    spinnerModel.setStepSize(bidAction.getBidIncrement());
                    spinnerModel.setValue(mb);
                } else {
                    itemNameButton[i].setPossibleAction(action);
                }
                bidAllowed = selected;
                itemNameButton[i].setSelected(selected);
                itemNameButton[i].setEnabled(!selected);
                setItemNameButton(i, true);
                minBid[i].setText(Bank.format(item.getMinimumBid()));
            }
        }

        passAllowed = false;

        List<NullAction> inactiveItems =
                possibleActions.getType(NullAction.class);
        if (inactiveItems != null && !inactiveItems.isEmpty()) {
            // only one NullAction is allowed
            NullAction na = inactiveItems.get(0);
            // nullActions differ in text to display
            passButton.setText(LocalText.getText(na.toString()));
            passAllowed = true;
            passButton.setPossibleAction(na);
            passButton.setMnemonic(KeyEvent.VK_P);
        }

        buyButton.setEnabled(buyAllowed);
        if (includeBidding) {
            bidButton.setEnabled(bidAllowed);
            bidAmount.setEnabled(bidAllowed);
        }
        passButton.setEnabled(passAllowed);

        pack(); // to avoid not displaying after label size changes
        requestFocus();
    }

    public boolean processImmediateAction() {

        log.debug("ImmediateAction=" + immediateAction);
        if (immediateAction != null) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            PossibleAction nextAction = immediateAction;
            immediateAction = null;
            if (nextAction instanceof StartItemAction) {
                StartItemAction action = (StartItemAction) nextAction;
                if (action instanceof BuyStartItem) {
                    requestStartPrice((BuyStartItem) action);
                    return process(action);
                }
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent actor) {
        JComponent source = (JComponent) actor.getSource();

        if (source instanceof ClickField) {
            gbc = gb.getConstraints(source);
            StartItemAction currentActiveItem =
                    (StartItemAction) ((ClickField) source).getPossibleActions().get(
                            0);

            if (currentActiveItem instanceof BuyStartItem) {
                buyButton.setEnabled(true);
                buyButton.setPossibleAction(currentActiveItem);
                if (includeBidding) {
                    bidButton.setEnabled(false);
                    bidAmount.setEnabled(false);
                }
            } else if (currentActiveItem instanceof BidStartItem) {
                BidStartItem bidAction = (BidStartItem) currentActiveItem;
                buyButton.setEnabled(false);

                if (bidAction.isSelectForAuction()) {
                    // In this case, "Pass" becomes "Select, don't buy"
                    passButton.setPossibleAction(currentActiveItem);
                    passButton.setEnabled(true);
                    passButton.setText(LocalText.getText("SelectNoBid"));
                    passButton.setVisible(true);
//                    if (!repacked) {
                        pack();
//                        repacked = true;
//                    }

                }
                if (includeBidding) {
                    bidButton.setEnabled(true);
                    bidButton.setPossibleAction(currentActiveItem);
                    bidAmount.setEnabled(true);
                    int minBid = bidAction.getMinimumBid();
                    spinnerModel.setMinimum(minBid);
                    spinnerModel.setStepSize(bidAction.getBidIncrement());
                    spinnerModel.setValue(minBid);
                }
            }
        } else if (source instanceof ActionButton) {
            PossibleAction activeItem =
                    ((ActionButton) source).getPossibleActions().get(0);

            if (source == buyButton) {
                if (activeItem instanceof BuyStartItem
                    && ((BuyStartItem) activeItem).hasSharePriceToSet()) {
                    if (requestStartPrice((BuyStartItem) activeItem))
                        process(activeItem);
                } else {
                    process(activeItem);
                }
            } else if (source == bidButton) {

                ((BidStartItem) activeItem).setActualBid(((Integer) spinnerModel.getValue()));
                process(activeItem);

            } else if (source == passButton) {

                if (activeItem != null && activeItem instanceof BidStartItem
                    && ((BidStartItem) activeItem).isSelectForAuction()) {
                    ((BidStartItem) activeItem).setActualBid(-1);
                }
                process(activeItem);

            }
        }

    }

    private boolean requestStartPrice(BuyStartItem activeItem) {

        if (activeItem.hasSharePriceToSet()) {
            String compName = activeItem.getCompanyToSetPriceFor();
            StockMarketI stockMarket = gameUIManager.getGameManager().getStockMarket();

            // Get a sorted prices List
            // TODO: should be included in BuyStartItem
            List<StockSpaceI> startSpaces = stockMarket.getStartSpaces();
            Map<Integer, StockSpaceI> spacePerPrice =
                    new HashMap<Integer, StockSpaceI>();
            int[] prices = new int[startSpaces.size()];
            StockSpaceI[] options = new StockSpaceI[startSpaces.size()];
            for (int i = 0; i < startSpaces.size(); i++) {
                prices[i] = startSpaces.get(i).getPrice();
                spacePerPrice.put(prices[i], startSpaces.get(i));
            }
            Arrays.sort(prices);
            for (int i = 0; i < startSpaces.size(); i++) {
                options[i] = spacePerPrice.get(prices[i]);
            }

            StockSpace sp =
                    (StockSpace) JOptionPane.showInputDialog(this,
                            LocalText.getText("WHICH_START_PRICE",
                                    activeItem.getPlayerName(),
                                    compName),
                            LocalText.getText("WHICH_PRICE"),
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[0]);
            if (sp == null) {
                return false;
            }
            int price = sp.getPrice();
            activeItem.setAssociatedSharePrice(price);
        }
        return true;
    }

    public void close() {
        this.dispose();
    }

    public void setSRPlayerTurn(int selectedPlayerIndex) {
        int j;

        if ((j = this.playerIndex) >= 0) {
            upperPlayerCaption[j].setHighlight(false);
            lowerPlayerCaption[j].setHighlight(false);
        }
        this.playerIndex = selectedPlayerIndex;
        if ((j = this.playerIndex) >= 0) {
            upperPlayerCaption[j].setHighlight(true);
            lowerPlayerCaption[j].setHighlight(true);
        }
    }

    public String getSRPlayer() {
        if (playerIndex >= 0)
            return players[playerIndex].getName();
        else
            return "";
    }

    private void setItemNameButton(int i, boolean clickable) {

        itemName[i].setVisible(!clickable);
        itemNameButton[i].setVisible(clickable);

        int status = Integer.parseInt(itemStatus[i].getText());
        String tooltip = LocalText.getText(itemStatusTextKeys[status]);

        itemName[i].setToolTipText(clickable ? "" : tooltip);
        itemNameButton[i].setToolTipText(clickable ? tooltip : "");

        itemName[i].setForeground (
                status == StartItem.SOLD ? soldColour : defaultColour);
        itemNameButton[i].setForeground (
                status == StartItem.BUYABLE ? buyableColour : defaultColour);
    }

   private String getStartItemDescription (StartItem item) {
            StringBuffer b = new StringBuffer("<html>");
            b.append (item.getPrimary().toString());
            if (item.getPrimary() instanceof PrivateCompany) {
                PrivateCompany priv = (PrivateCompany) item.getPrimary();
                b.append ("<br>Revenue: ").append(Bank.formatIntegerArray(priv.getRevenue()));
                List<MapHex> blockedHexes = priv.getBlockedHexes();
                if (blockedHexes == null) {
                } else if (blockedHexes.size() == 1) {
                    b.append("<br>Blocked hex: ").append(blockedHexes.get(0).getName());
                } else if (blockedHexes.size() > 1) {
                    b.append("<br>Blocked hexes:");
                    for (MapHex hex : blockedHexes) {
                        b.append(" ").append(hex.getName());
                    }
                }
                if (priv.hasSpecialProperties()) {
                    b.append("<br><b>Special properties:</b>");
                    for (SpecialPropertyI sp : priv.getSpecialProperties()) {
                        b.append("<br>").append(sp.toString());
                   }
                }
                // sfy 1889
                List<String> preventClosingConditions = priv.getPreventClosingConditions();
                if (!preventClosingConditions.isEmpty()) {
                    b.append("<br><b>Prevent closing conditions:</b>");
                    for (String condition : preventClosingConditions) {
                        b.append("<br>").append(condition);
                   }
                }

            }
            if (item.getSecondary() != null) {
                b.append("<br><b>Also contains:</b><br>");
                b.append(item.getSecondary().toString());
            }
            return b.toString();
    }

    private ImageIcon createInfoIcon() {
        return RailsIcon.INFO.icon;
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

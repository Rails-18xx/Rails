package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.util.Util;
import net.sf.rails.ui.swing.ORPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;

import com.google.common.collect.Iterables;

/**
 * This displays the Auction Window.
 * Refactored to remove separate Info column/button and integrate details into
 * RailCard tooltips.
 */
public class StartRoundWindow extends JFrame implements ActionListener, KeyListener, ActionPerformer, DialogOwner {

    private static final long serialVersionUID = 1L;

    // Gap sizes between screen cells, in pixels
    protected static final int NARROW_GAP = 2;
    protected static final int WIDE_GAP = 5;
    // Bits for specifying where to apply wide gaps
    protected static final int WIDE_LEFT = 1;
    protected static final int WIDE_RIGHT = 2;
    protected static final int WIDE_TOP = 4;
    protected static final int WIDE_BOTTOM = 8;

    protected ActionButton undoButton;

    protected static final String[] itemStatusTextKeys = new String[] { "Status_Unavailable", "Status_Biddable",
            "Status_Buyable",
            "Status_Selectable", "Status_Auctioned",
            "Status_NeedingSharePrice", "Status_Sold" };

    /* Keys of dialogs owned by this class */
    public static final String COMPANY_START_PRICE_DIALOG = "CompanyStartPrice";

    private static final Logger log = LoggerFactory.getLogger(StartRoundWindow.class);

    protected JPanel statusPanel;
    protected JPanel buttonPanel;

    protected GridBagLayout gb;
    protected GridBagConstraints gbc;

    // Grid elements per function
    protected Caption[] itemName;
    protected int[] itemNameXOffset;
    protected int itemNameYOffset;
    protected Field[] basePrice;
    protected int[] basePriceXOffset;
    protected int basePriceYOffset;
    protected Field[] minBid;
    protected int[] minBidXOffset;
    protected int minBidYOffset;

    // Separator Fields
    protected JComponent[] verticalSeparators;
    protected int[] separatorXOffset;

    protected Field[][] bidPerPlayer;
    protected int[] bidPerPlayerXOffset;
    protected int bidPerPlayerYOffset;
    protected Field[] playerBids;
    protected int[] playerBidsXOffset;
    protected int playerBidsYOffset;
    protected Field[] playerFree;
    protected int[] playerFreeCashXOffset;
    protected int playerFreeCashYOffset;

    protected Field[] itemStatus;

    protected int[] playerCaptionXOffset;
    protected int upperPlayerCaptionYOffset, lowerPlayerCaptionYOffset;
    protected Field[][] upperPlayerCaption;
    protected Field[] lowerPlayerCaption;
    protected JComponent[][] fields;
    protected int currentFontSize = 14;

    protected ActionButton bidButton;
    protected ActionButton buyButton;

    protected ActionButton aiIRButton;
    protected JSpinner bidAmount;
    protected SpinnerNumberModel spinnerModel;
    protected ActionButton passButton;

    protected RailCard[] cards;
    protected PlayerManager players;

    protected int[] crossIndex;
    protected StartRound round;
    protected GameUIManager gameUIManager;
    protected StartPacket startPacket;
    protected boolean multipleColumns;
    protected int numberOfColumns;
    protected int numberOfRows;
    protected int columnWidth = 0;

    protected JDialog currentDialog;
    protected PossibleAction currentDialogAction;
    protected SortedSet<StockSpace> startSpaces;

    protected PossibleActions possibleActions;
    protected PossibleAction immediateAction;

    protected final ButtonGroup itemGroup = new ButtonGroup();
    protected ClickField dummyButton;

    protected StartRound.Bidding includeBidding;
    protected boolean includeBuying;
    protected boolean showBasePrices;

    protected ORUIManager orUIManager;
    protected int selectedItemIndex = -1;
    protected JPanel[] cardWrappers;
    protected JPanel[] playerInventoryPanels; // New field for player inventories
    protected static final Color COLOR_AVAILABLE = new Color(204, 255, 204);
    protected static final Color COLOR_SOLD = new Color(220, 220, 220);
    protected static final Color COLOR_HIGHLIGHT = new Color(160, 32, 240); // Prominent Purple

    public StartRoundWindow() {
    }


    private void addField(JComponent comp, int x, int y, int width, int height, int wideGapPositions) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;

        int padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
        int padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
        int padBottom = (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP : NARROW_GAP;
        int padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;

        gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

        statusPanel.add(comp, gbc);
        if (fields != null && x < fields.length && y < fields[0].length) {
            fields[x][y] = comp;
        }
    }

    public void updateFonts(int size) {
        if (size < 8)
            size = 8;
        if (size > 48)
            size = 48;
        currentFontSize = size;

        Font f = new Font("SansSerif", Font.BOLD, currentFontSize);

        // Helper logic to update all arrays if they exist
        if (basePrice != null)
            for (Field c : basePrice)
                if (c != null)
                    c.setFont(f);
        if (minBid != null)
            for (Field c : minBid)
                if (c != null)
                    c.setFont(f);
        if (playerBids != null)
            for (Field c : playerBids)
                if (c != null)
                    c.setFont(f);
        if (playerFree != null)
            for (Field c : playerFree)
                if (c != null)
                    c.setFont(f);
        if (lowerPlayerCaption != null)
            for (Field c : lowerPlayerCaption)
                if (c != null)
                    c.setFont(f);

        if (upperPlayerCaption != null) {
            for (Field[] row : upperPlayerCaption) {
                if (row != null)
                    for (Field c : row)
                        if (c != null)
                            c.setFont(f);
            }
        }
        if (bidPerPlayer != null) {
            for (Field[] row : bidPerPlayer) {
                if (row != null)
                    for (Field c : row)
                        if (c != null)
                            c.setFont(f);
            }
        }

        // Re-pack window to accommodate new size
        if (gameUIManager != null)
            gameUIManager.packAndApplySizing(this);
    }

    @Override
    public boolean processImmediateAction() {
        if (immediateAction != null) {
            log.debug("ImmediateAction = {}", immediateAction);
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            PossibleAction nextAction = immediateAction;
            immediateAction = null;
            if (nextAction instanceof StartItemAction) {
                StartItemAction action = (StartItemAction) nextAction;
                if (action instanceof BuyStartItem) {
                    requestStartPrice((BuyStartItem) action);
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean requestStartPrice(BuyStartItem activeItem) {
        if (activeItem.hasSharePriceToSet()) {
            String compName = activeItem.getCompanyToSetPriceFor();
            StockMarket stockMarket = gameUIManager.getRoot().getStockMarket();

            // Get a sorted prices List
            // TODO: should be included in BuyStartItem

            if (activeItem.containsStartSpaces()) {
                startSpaces = new TreeSet<StockSpace>();
                for (String s : activeItem.startSpaces()) {
                    startSpaces.add(stockMarket.getStockSpace(s));
                }
            } else {
                startSpaces = stockMarket.getStartSpaces();
            }

            String[] options = new String[startSpaces.size()];
            int i = 0;
            for (StockSpace space : startSpaces) {
                options[i++] = gameUIManager.format(space.getPrice());
            }

            RadioButtonDialog dialog = new RadioButtonDialog(
                    COMPANY_START_PRICE_DIALOG,
                    this,
                    this,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText("WHICH_START_PRICE",
                            players.getCurrentPlayer().getId(),
                            compName),
                    options,
                    -1);

            setCurrentDialog(dialog, activeItem);
        }

        return true;
    }

    @Override
    public JDialog getCurrentDialog() {
        return currentDialog;
    }

    @Override
    public PossibleAction getCurrentDialogAction() {
        return currentDialogAction;
    }

    @Override
    public void setCurrentDialog(JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        currentDialog = dialog;
        currentDialogAction = action;

        disableButtons();
    }

    @Override
    public void dialogActionPerformed() {
        if (currentDialog instanceof RadioButtonDialog && currentDialogAction instanceof BuyStartItem) {
            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            BuyStartItem action = (BuyStartItem) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index >= 0) {
                int price = Iterables.get(startSpaces, index).getPrice();
                action.setAssociatedSharePrice(price);
                process(action);
            } else {
                // No selection done - no action
                return;
            }
        }
    }

    protected void disableButtons() {
        if (includeBidding != StartRound.Bidding.NO) {
            bidButton.setEnabled(false);
        }

        if (includeBuying) {
            buyButton.setEnabled(false);
        }

        passButton.setEnabled(false);
    }

    public void close() {
        this.dispose();
    }

    public void setSRPlayerTurn() {
        int playerIndex = players.getCurrentPlayer().getIndex();
        for (int i = 0; i < players.getNumberOfPlayers(); i++) {
            for (int j = 0; j < numberOfColumns; j++) {
if (upperPlayerCaption[j][i] != null) {
                    upperPlayerCaption[j][i].setHighlight(i == playerIndex);
                }            }
            lowerPlayerCaption[i].setHighlight(i == playerIndex);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public boolean process(PossibleAction action) {
        return gameUIManager.processAction(action);
    }

    public void updatePlayerOrder(List<String> newPlayerNames) {

        // Multiple columns are here ignored for now.
        // When is this called?
        int np = players.getNumberOfPlayers();

        int[] xref = new int[np];
        List<String> oldPlayerNames = gameUIManager.getCurrentGuiPlayerNames();
        for (int i = 0; i < np; i++) {
            xref[i] = oldPlayerNames.indexOf(newPlayerNames.get(i));
        }

        JComponent[] cells = new Cell[np];
        GridBagConstraints[] constraints = new GridBagConstraints[np];
        JComponent f;
        for (int y = upperPlayerCaptionYOffset; y <= lowerPlayerCaptionYOffset; y++) {
            for (int i = 0, x = playerCaptionXOffset[0]; i < np; i++, x++) {
                cells[i] = fields[x][y];
                constraints[i] = gb.getConstraints(cells[i]);
                statusPanel.remove(cells[i]);
            }
            for (int i = 0, x = playerCaptionXOffset[0]; i < np; i++, x++) {
                f = fields[x][y] = cells[xref[i]];
                statusPanel.add(f, constraints[i]);
            }
        }
        for (int i = 0, x = playerCaptionXOffset[0]; i < np; i++, x++) {
            for (int col = 0; col < numberOfColumns; col++) {
                upperPlayerCaption[col][i] = (Field) fields[x][upperPlayerCaptionYOffset];
            }
            lowerPlayerCaption[i] = (Field) fields[x][lowerPlayerCaptionYOffset];
        }

        gameUIManager.packAndApplySizing(this);
    }

    // Method for the Start Round (IR) AI button
    public void enableAIIRButton(boolean enable) {
        // Assuming aiIRbutton is defined in ORPanel.java
        if (aiIRButton != null) {
            aiIRButton.setEnabled(enable);
            aiIRButton.setVisible(enable);
        }
    }

    public void init(StartRound round, GameUIManager parent, ORUIManager orUIManager) {
        this.round = round;
        this.orUIManager = orUIManager;
        startPacket = round.getStartPacket();
        multipleColumns = startPacket.isMultipleColumns();
        if (multipleColumns) {
            numberOfColumns = startPacket.getNumberOfColumns();
            numberOfRows = startPacket.getNumberOfRows();
        } else {
            numberOfRows = round.getNumberOfStartItems();
            numberOfColumns = 1;
        }
        includeBidding = round.hasBidding();
        includeBuying = round.hasBuying();
        showBasePrices = round.hasBasePrices();
        gameUIManager = parent;
        possibleActions = gameUIManager.getGameManager().getPossibleActions();

        setTitle(LocalText.getText("START_ROUND_TITLE",
                String.valueOf(round.getStartRoundNumber())));
        getContentPane().setLayout(new BorderLayout());

        statusPanel = new JPanel();
        gb = new GridBagLayout();
        statusPanel.setLayout(gb);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        statusPanel.setOpaque(true);

        buttonPanel = new JPanel();

        if (includeBuying) {
            buyButton = new ActionButton(RailsIcon.AUCTION_BUY);
            buyButton.setMnemonic(KeyEvent.VK_U);
            buyButton.addActionListener(this);
            buyButton.setEnabled(false);
            buttonPanel.add(buyButton);
        }

        if (includeBidding != StartRound.Bidding.NO) {
            bidButton = new ActionButton(RailsIcon.BID);
            bidButton.setMnemonic(KeyEvent.VK_B);
            bidButton.addActionListener(this);
            bidButton.setEnabled(false);
            buttonPanel.add(bidButton);

            spinnerModel = new SpinnerNumberModel(999, 0, null, 1);
            bidAmount = new JSpinner(spinnerModel);
            bidAmount.setPreferredSize(new Dimension(50, 28));
            bidAmount.setEnabled(false);
            buttonPanel.add(bidAmount);
        }

        passButton = new ActionButton(RailsIcon.PASS);
        passButton.setMnemonic(KeyEvent.VK_P);
        passButton.addActionListener(this);
        passButton.setEnabled(false);
        buttonPanel.add(passButton);

        undoButton = new ActionButton(RailsIcon.UNDO);
        undoButton.setToolTipText("Undo last action (Z)");
        undoButton.addActionListener(this);
        undoButton.setEnabled(false);
        buttonPanel.add(undoButton);

        if (round instanceof net.sf.rails.game.specific._1817.StartRound_1817) {
            net.sf.rails.game.specific._1817.StartRound_1817 sr1817 = (net.sf.rails.game.specific._1817.StartRound_1817) round;
            JPanel seedPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
            JLabel seedLabel = new JLabel("  Seed Money: ");
            seedLabel.setFont(new Font("SansSerif", Font.BOLD, currentFontSize));
            Field seedField = new Field(sr1817.getSeedMoneyModel());
            seedField.setFont(new Font("SansSerif", Font.BOLD, currentFontSize));
            seedPanel.add(seedLabel);
            seedPanel.add(seedField);
            buttonPanel.add(seedPanel);
        }

        buttonPanel.setOpaque(true);

        gbc = new GridBagConstraints();

        players = gameUIManager.getRoot().getPlayerManager();

        crossIndex = new int[round.getStartPacket().getNumberOfItems()];

        for (int i = 0; i < round.getNumberOfStartItems(); i++) {
            final StartItem item = round.getStartItem(i);
            crossIndex[item.getIndex()] = i;
        }

        initCells();

        getContentPane().add(statusPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        setLocation(300, 150);
        setSize(275, 325);
        gameUIManager.setMeVisible(this, true);
        requestFocus();

        setupHotkeys();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        final JFrame thisFrame = this;
        final GameUIManager guiMgr = gameUIManager;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (GameUIManager.confirmQuit(thisFrame)) {
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

    // ... (lines of unchanged context code) ...
    private void setupHotkeys() {
        // --- START FIX ---
        // Bind Command/Ctrl + and - to font size adjustment
        InputMap inputMap = statusPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = statusPanel.getActionMap();

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Increase Font (Cmd = and Cmd +)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask), "increaseFont");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask), "increaseFont");
        actionMap.put("increaseFont", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                updateFonts(currentFontSize + 2);
            }
        });

        // Decrease Font (Cmd -)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask), "decreaseFont");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask), "decreaseFont");
        actionMap.put("decreaseFont", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                updateFonts(currentFontSize - 2);
            }
        });

    }

    // We are modifying StartRoundWindow.java

    /**
     * Configures the RailCard with Tooltips and Map Highlighting based on the
     * associated StartItem.
     * Centralized logic ensures that Privates, Minors, and Majors highlight
     * correctly in all games.
     */
    protected void configureMapHighlighting(RailCard card, StartItem si) {
        if (card == null || si == null)
            return;

        // 1. Collect all certificates for highlighting (Primary and Secondary)
        java.util.List<Certificate> certs = new java.util.ArrayList<>();
        if (si.getPrimary() != null)
            certs.add(si.getPrimary());
        if (si.getSecondary() != null)
            certs.add(si.getSecondary());


        // 2. Set Tooltip (based on primary company)
        if (!certs.isEmpty()) {
            Certificate pCert = certs.get(0);
            Company pComp = null;
            if (pCert instanceof PrivateCompany) {
                pComp = (PrivateCompany) pCert;
            } else if (pCert instanceof PublicCertificate) {
                pComp = ((PublicCertificate) pCert).getCompany();
                if (pComp == null && pCert.getParent() instanceof PublicCompany) {
                    pComp = (PublicCompany) pCert.getParent();
                }
            }
            if (pComp != null) {
                card.setCompanyDetailsTooltip(pComp);
            }
        }

        // 3. Add Map Highlight Listeners
        if (gameUIManager != null && gameUIManager.getORUIManager() != null) {

            // A. Attach the StartItem listener (highlights blocked hexes for contained
            // Privates)
            HexHighlightMouseListener.addMouseListener(card, gameUIManager.getORUIManager(), si);

            // B. Attach explicit listeners for all associated companies
            for (Certificate cert : certs) {
                PublicCompany pubComp = null;
                if (cert instanceof PublicCertificate) {
                    pubComp = ((PublicCertificate) cert).getCompany();
                    if (pubComp == null && cert.getParent() instanceof PublicCompany) {
                        pubComp = (PublicCompany) cert.getParent();
                    }
                }

                if (pubComp != null) {
                    // Log the Home Hexes to verify they exist in the model
                    java.util.List<MapHex> homes = pubComp.getHomeHexes();

                    HexHighlightMouseListener.addMouseListener(card, gameUIManager.getORUIManager(), pubComp, true);
                } else if (cert instanceof PrivateCompany) {
                    PrivateCompany priv = (PrivateCompany) cert;

                    HexHighlightMouseListener.addMouseListener(card, gameUIManager.getORUIManager(), priv, true);
                }
            }
        } else {
        }
    }

    protected void clearMapHighlights() {
        if (gameUIManager != null && gameUIManager.getORUIManager() != null) {
            net.sf.rails.ui.swing.hexmap.HexMap map = gameUIManager.getORUIManager().getMap();
            if (map != null) {
                // setOwnerHighlight requires a List, and we must iterate over the map values
                map.setOwnerHighlight(new java.util.ArrayList<net.sf.rails.ui.swing.hexmap.GUIHex>(), null);

                for (net.sf.rails.ui.swing.hexmap.GUIHex guiHex : map.getGuiHexes().values()) {
guiHex.setActiveOwnerHighlight(false, null, true);
                }
            }
        }
    }


    protected void updateMapHighlights() {
if (gameUIManager == null || gameUIManager.getORUIManager() == null)
return;
net.sf.rails.ui.swing.hexmap.HexMap map = gameUIManager.getORUIManager().getMap();
if (map == null)
return;

java.util.List<net.sf.rails.ui.swing.hexmap.GUIHex> hexesToHighlight = new java.util.ArrayList<>();
        java.util.Map<net.sf.rails.ui.swing.hexmap.GUIHex, String> specificLabels = new java.util.HashMap<>();

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null && (cards[i].getState() == RailCard.State.ACTIONABLE
                    || cards[i].getState() == RailCard.State.SELECTED)) {
                StartItem si = round.getStartItem(i);
                
                java.util.List<Certificate> certs = new java.util.ArrayList<>();
                if (si.getPrimary() != null) certs.add(si.getPrimary());
                if (si.getSecondary() != null) certs.add(si.getSecondary());

                for (Certificate cert : certs) {
                    PublicCompany pubComp = null;

                    if (cert instanceof PublicCertificate) {
                        pubComp = ((PublicCertificate) cert).getCompany();
                        if (pubComp == null && cert.getParent() instanceof PublicCompany) {
                            pubComp = (PublicCompany) cert.getParent();
                        }
                    }

                    if (pubComp != null) {
                        for (MapHex hex : pubComp.getHomeHexes()) {
                            net.sf.rails.ui.swing.hexmap.GUIHex guiHex = map.getHex(hex);
                            if (guiHex != null) {
                                hexesToHighlight.add(guiHex);
                                specificLabels.put(guiHex, pubComp.getId());
                            }
                        }
                    } else if (cert instanceof PrivateCompany) {
                        PrivateCompany privComp = (PrivateCompany) cert;
                        if (privComp.getBlockedHexes() != null) {
                            for (MapHex hex : privComp.getBlockedHexes()) {
                                net.sf.rails.ui.swing.hexmap.GUIHex guiHex = map.getHex(hex);
                                if (guiHex != null) {
                                    hexesToHighlight.add(guiHex);
                                    specificLabels.put(guiHex, privComp.getId());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 1. Clear old highlights and register new base highlights generically
        map.setOwnerHighlight(hexesToHighlight, null);
        
        // 2. Overwrite the generic dotted state with the specific solid state (true)
        for (java.util.Map.Entry<net.sf.rails.ui.swing.hexmap.GUIHex, String> entry : specificLabels.entrySet()) {
            entry.getKey().setActiveOwnerHighlight(true, entry.getValue(), true);
        }
        
        map.repaintAll(new Rectangle(map.getSize()));



}



    @Override
    public void updateStatus(boolean myTurn) {
        clearMapHighlights();
      if (gameUIManager != null && gameUIManager.getGameManager() != null) {
            possibleActions = gameUIManager.getGameManager().getPossibleActions();
        }

// 0. Total UI Reset: Reset EVERY card in the array, not just those currently in the round.
        // This ensures items removed from the packet are correctly cleared.
        if (cards != null) {
            for (int i = 0; i < cards.length; i++) {
                if (cards[i] == null) continue;
                cards[i].setState(RailCard.State.PASSIVE);
                cards[i].clearPossibleActions();
                if (cardWrappers[i] != null) {
                    cardWrappers[i].setVisible(false);
                    cardWrappers[i].setBackground(COLOR_AVAILABLE);
                }
            }
        }

        if (playerInventoryPanels != null) {
            for (JPanel panel : playerInventoryPanels) {
                if (panel != null) panel.removeAll();
            }
        }

        for (int i = 0; i < round.getNumberOfStartItems(); i++) {
            StartItem si = round.getStartItem(i);
            StartItemAction buyAction = null;

            // IMPORTANT: VISUAL MOVEMENT LOGIC
            if (si.isSold()) {
                // If sold, hide from Market (Left) and Move to Inventory (Right)

if (cardWrappers[i] != null) {
                    cardWrappers[i].removeAll(); 
                    cardWrappers[i].setBorder(null); 
                    cardWrappers[i].setOpaque(false);
                    cardWrappers[i].setVisible(true); // Keep visible for matrix structure
                }
                if (showBasePrices && basePrice[i] != null) {
                    basePrice[i].setText(""); // Clear text but keep field visible to hold layout
                    basePrice[i].setVisible(true);
                }

 
                // If we have bidding matrix, hide those fields too?
                // Usually better to leave them or clear them.
                
                Player owner = si.getBidder(); // In StartRound, bidder usually becomes owner
                if (owner != null && playerInventoryPanels != null && owner.getIndex() < playerInventoryPanels.length) {
                    cards[i].setState(RailCard.State.PASSIVE);
                    cards[i].clearPossibleActions();
                    cards[i].setVisible(true);
                    
                    // Add to player panel
                    playerInventoryPanels[owner.getIndex()].add(cards[i]);
                    playerInventoryPanels[owner.getIndex()].add(Box.createVerticalStrut(2));
                }
            } else {
                // Not Sold: Ensure it's visible in the Market (Left)
                if (cardWrappers[i] != null) cardWrappers[i].setVisible(true);
                if (showBasePrices && basePrice[i] != null) basePrice[i].setVisible(true);
                
                // Ensure card is physically in the wrapper (it might have been moved previously)
                if (cardWrappers[i] != null && cards[i].getParent() != cardWrappers[i]) {
                    cardWrappers[i].add(cards[i]);
                }
                
                cards[i].clearPossibleActions();
                cards[i].setState(RailCard.State.PASSIVE);
                if (cardWrappers[i] != null) cardWrappers[i].setBackground(COLOR_AVAILABLE);
                
            }
        }

        // Refresh Inventory Panels
        if (playerInventoryPanels != null) {
            for (JPanel panel : playerInventoryPanels) {
                if (panel != null) {
                    panel.add(Box.createVerticalGlue()); // Push all items to the top
                    panel.revalidate();
                    panel.repaint();
                }
            }
        }
        
        // 2. Setup Buttons (Default Disabled)
        dummyButton.setSelected(true);
        if (includeBuying && buyButton != null)
            buyButton.setEnabled(false);
        if (includeBidding != StartRound.Bidding.NO) {
            if (bidButton != null)
                bidButton.setEnabled(false);
            if (bidAmount != null)
                bidAmount.setEnabled(false);
        }
        if (passButton != null)
            passButton.setEnabled(false);
        if (undoButton != null)
            undoButton.setEnabled(true);

        RoundFacade currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof StartRound) || !myTurn || possibleActions == null) {
            return;
        }

        setSRPlayerTurn();

        // 3. Handle Undo Action
        List<GameAction> gameActions = possibleActions.getType(GameAction.class);
        undoButton.setEnabled(false);
        for (GameAction ga : gameActions) {
            if (ga.getMode() == GameAction.Mode.UNDO && undoButton != null) {
                undoButton.setEnabled(true);
                undoButton.setPossibleAction(ga);
                break;
            }
        }

        // 4. Distribute Actions and Apply Prominent Highlighting
        List<StartItemAction> actions = possibleActions.getType(StartItemAction.class);
        if (actions != null) {
            for (StartItemAction action : actions) {
                int j = action.getItemIndex();
                int i = crossIndex[j];
                
// Only highlight and set actions if the item is actually available
                StartItem siCheck = round.getStartItem(i);
                if (cardWrappers[i].isVisible() && !siCheck.isSold()) {
                    cards[i].setPossibleAction(action);
                    
                    if (cardWrappers[i] != null) {
                        cardWrappers[i].setBackground(COLOR_HIGHLIGHT);
                    }

                    if (action instanceof BuyStartItem) {
                        BuyStartItem bsi = (BuyStartItem) action;
                        if (bsi.isSelected() || i == selectedItemIndex) {
                            cards[i].setState(RailCard.State.SELECTED);
                            selectedItemIndex = i;
                            if (cardWrappers[i] != null) {
                                cardWrappers[i].setBackground(Color.YELLOW);
                            }
                            if (buyButton != null && includeBuying) {
                                buyButton.setEnabled(true);
                                buyButton.setPossibleAction(action);
                            }
                        } else {
                            cards[i].setState(RailCard.State.ACTIONABLE);
                        }
                    } else if (action instanceof BidStartItem) {
                        BidStartItem bidAction = (BidStartItem) action;
                        if (bidAction.isSelected() || i == selectedItemIndex) {
                            cards[i].setState(RailCard.State.SELECTED);
                            selectedItemIndex = i;
                            if (bidButton != null) {
                                bidButton.setEnabled(true);
                                bidButton.setPossibleAction(action);
                            }
                            if (bidAmount != null) {
                                bidAmount.setEnabled(true);
                                spinnerModel.setMinimum(bidAction.getMinimumBid());
                                spinnerModel.setValue(bidAction.getMinimumBid());
                            }
                        } else {
                            cards[i].setState(RailCard.State.ACTIONABLE);
                        }
                    }
                }
            }
        }

        // 5. Handle Pass Button
        List<NullAction> passes = possibleActions.getType(NullAction.class);
        if (passes != null && !passes.isEmpty() && passButton != null) {
            passButton.setEnabled(true);
            passButton.setPossibleAction(passes.get(0));
        }
        else if (selectedItemIndex != -1 && cards[selectedItemIndex].getPossibleActions() != null && !cards[selectedItemIndex].getPossibleActions().isEmpty()) {
            PossibleAction act = cards[selectedItemIndex].getPossibleActions().get(0);
            if (act instanceof BidStartItem && ((BidStartItem) act).isSelectForAuction() && passButton != null) {
                passButton.setEnabled(true);
                passButton.setPossibleAction(act);
            }
        }

        if (round instanceof net.sf.rails.game.specific._1817.StartRound_1817) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                net.sf.rails.game.specific._1817.StartRound_1817 sr1817 = (net.sf.rails.game.specific._1817.StartRound_1817) round;
                int currentSeed = sr1817.getSeedMoneyModel().value();
                for (int i = 0; i < round.getNumberOfStartItems(); i++) {
                    if (minBid != null && i < minBid.length && minBid[i] != null) {
                        net.sf.rails.game.StartItem si = round.getStartItem(i);
                        int bid = si.getBid();
                        int min = (si.getBidder() != null) ? bid + 5 : Math.max(5, si.getBasePrice() - currentSeed);
                        minBid[i].setText(String.valueOf(min));
                    }
                }
            });
        }

        // 6. Final UI and Map Refresh
        updateMapHighlights();
        statusPanel.revalidate();
        statusPanel.repaint();
        // --- END FIX ---
    }

// ... (lines of unchanged context code) ...
    @Override
    public void actionPerformed(ActionEvent actor) {
        // --- START FIX ---
        Object source = actor.getSource();
        int clickedIndex = -1;
        
        // Use hierarchy search to find which card (if any) was clicked
        for (int k = 0; k < cards.length; k++) {
            if (cards[k] == null) continue;
            if (source == cards[k] || (source instanceof Component && SwingUtilities.isDescendingFrom((Component)source, cards[k]))) {
                clickedIndex = k;
                break;
            }
        }

        if (clickedIndex != -1) {
            RailCard card = cards[clickedIndex];
            if (card.getState() == RailCard.State.DISABLED) return;

            List<PossibleAction> acts = card.getPossibleActions();
            if (acts == null || acts.isEmpty()) return;

            StartItemAction action = (StartItemAction) acts.get(0);
            SoundManager.notifyOfClickFieldSelection(action);

            if (action instanceof BuyStartItem) {
                if (clickedIndex == selectedItemIndex) {
                    // Second click confirms purchase
                    BuyStartItem bsi = (BuyStartItem) action;
                    if (bsi.hasSharePriceToSet() && requestStartPrice(bsi))
                        return;
                    selectedItemIndex = -1;
                    process(bsi);
                } else {
                    // First click highlights and enables buttons
                    selectedItemIndex = clickedIndex;
                    updateStatus(true);
                }
            } else {
                // For bidding, select and immediately enable the bid interface
                selectedItemIndex = clickedIndex;
                if (action instanceof BidStartItem) {
                    BidStartItem bidAction = (BidStartItem) action;
                    if (bidAmount != null) {
                        bidAmount.setEnabled(true);
                        spinnerModel.setMinimum(bidAction.getMinimumBid());
                        spinnerModel.setValue(bidAction.getMinimumBid());
                    }
                    if (bidButton != null) {
                        bidButton.setEnabled(true);
                        bidButton.setPossibleAction(bidAction);
                    }
                }
                updateStatus(true);
            }
            return;
        }

        // Handle ActionButtons (Buy, Bid, Pass, Undo)
        if (source instanceof ActionButton) {
            List<PossibleAction> actions = ((ActionButton) source).getPossibleActions();
            if (actions != null && !actions.isEmpty()) {
            
                PossibleAction action = actions.get(0);
            
            // Intercept BuyStartItem from the "Buy" button to ensure the price dialog opens
            if (action instanceof BuyStartItem) {
                BuyStartItem bsi = (BuyStartItem) action;
                if (bsi.hasSharePriceToSet() && requestStartPrice(bsi)) {
                    return; // Dialog will handle the process() call
                }
            }
            
else if (action instanceof BidStartItem) {
                BidStartItem bidAction = (BidStartItem) action;
                if (source == passButton) {
                    bidAction.setActualBid(-1);
                    log.info("Player {} passed on bidding.", players.getCurrentPlayer().getId());
                } else if (bidAmount != null && bidAmount.isEnabled()) {
                    try {
                        bidAmount.commitEdit();
                    } catch (java.text.ParseException pe) {
                        log.warn("Invalid bid amount format in spinner", pe);
                    }
                    int committedBid = (Integer) bidAmount.getValue();
                    log.info("Committing bid of {} from UI panel for player {}.", committedBid, players.getCurrentPlayer().getId());
                    bidAction.setActualBid(committedBid);
                }
            }

            selectedItemIndex = -1;
            process(action);
        }
        }
    }


// We are replacing the entire initCells method in StartRoundWindow.java

// --- START FIX ---
    protected void initCells() {
        int ni = round.getNumberOfStartItems();
        int np = players.getNumberOfPlayers();
        int matrixRows = 10; // Enforce exactly 10 rows
        Font cellFont = new Font("SansSerif", Font.BOLD, currentFontSize);

        cards = new RailCard[ni];
        cardWrappers = new JPanel[ni];
        playerInventoryPanels = new JPanel[np];
        
        basePrice = new Field[ni];
        minBid = new Field[ni];
        bidPerPlayer = new Field[ni][np];
        itemStatus = new Field[ni];
        
        itemNameXOffset = new int[numberOfColumns];
        if (showBasePrices) basePriceXOffset = new int[numberOfColumns];
        if (includeBidding == StartRound.Bidding.ON_ITEMS) minBidXOffset = new int[numberOfColumns];
        bidPerPlayerXOffset = new int[numberOfColumns]; 
        
        upperPlayerCaption = new Field[numberOfColumns][np];
        lowerPlayerCaption = new Field[np];
        playerBids = new Field[np];
        playerFree = new Field[np];

        int lastX = -1;

        // 1. Setup Market Headers (Row 0)
        for (int col = 0; col < numberOfColumns; col++) {
            itemNameXOffset[col] = ++lastX;
            addField(new Caption(LocalText.getText("ITEM")), itemNameXOffset[col], 0, 1, 1, WIDE_LEFT + WIDE_RIGHT + WIDE_BOTTOM);

            if (showBasePrices) {
                basePriceXOffset[col] = ++lastX;
                addField(new Caption(LocalText.getText(includeBidding == StartRound.Bidding.ON_ITEMS ? "BASE_PRICE" : "PRICE")), 
                        basePriceXOffset[col], 0, 1, 1, WIDE_BOTTOM);
            }

            if (includeBidding == StartRound.Bidding.ON_ITEMS) {
                minBidXOffset[col] = ++lastX;
                addField(new Caption(LocalText.getText("MINIMUM_BID")), minBidXOffset[col], 0, 1, 1, WIDE_BOTTOM + WIDE_RIGHT);
            }

            if (includeBidding != StartRound.Bidding.NO) {
                bidPerPlayerXOffset[col] = ++lastX;
                for (int j = 0; j < np; j++) {
                    upperPlayerCaption[col][j] = new Field(players.getPlayerByPosition(j).getPlayerNameModel());
                    upperPlayerCaption[col][j].setFont(cellFont);
                    addField(upperPlayerCaption[col][j], lastX, 0, 1, 1, WIDE_BOTTOM);
                    if (j < np - 1) lastX++; 
                }
            }
        }

        // 2. Vertical Separator (Spans headers, matrix, and sponge)
        int separatorX = ++lastX;
        addField(new JSeparator(SwingConstants.VERTICAL), separatorX, 0, 1, matrixRows + 2, WIDE_LEFT + WIDE_RIGHT);

        // 3. Setup Player Inventories (Right Side)
        int playerStartX = ++lastX;
        playerCaptionXOffset = new int[]{playerStartX}; 

        for (int i = 0; i < np; i++) {
            Caption playerHeader = new Caption(players.getPlayerByPosition(i).getName());
            playerHeader.setFont(cellFont);
            addField(playerHeader, playerStartX + i, 0, 1, 1, WIDE_BOTTOM);

            playerInventoryPanels[i] = new JPanel();
            playerInventoryPanels[i].setLayout(new BoxLayout(playerInventoryPanels[i], BoxLayout.Y_AXIS));
            playerInventoryPanels[i].setBackground(Color.WHITE);
            playerInventoryPanels[i].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            gbc.gridx = playerStartX + i;
            gbc.gridy = 1;
            gbc.gridheight = matrixRows + 1; // Span 10 rows + the sponge row
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0; 
            gbc.weighty = 1.0; // Stretches to fill the "gray block"
            statusPanel.add(playerInventoryPanels[i], gbc);
        }

        // 4. Create Market Items (Rows 1 to ni)
        boolean[][] rowOccupied = new boolean[numberOfColumns][matrixRows + 1];
        for (int i = 0; i < ni; i++) {
            final StartItem si = round.getStartItem(i);
            int col = multipleColumns ? si.getColumn() - 1 : 0;
            int row = multipleColumns ? si.getRow() - 1 : i;
            int yPos = row + 1; 
            if (yPos <= matrixRows) rowOccupied[col][yPos] = true;

            cards[i] = new RailCard(si, itemGroup);
            cards[i].addActionListener(this); 
            cards[i].setScale(1.2); 
            configureMapHighlighting(cards[i], si);

            cardWrappers[i] = new JPanel(new GridLayout(1, 1)); 
            cardWrappers[i].setBackground(COLOR_AVAILABLE); 
            cardWrappers[i].setBorder(BorderFactory.createEtchedBorder());
            cardWrappers[i].add(cards[i]);

            final int cardIndex = i;
            cardWrappers[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (cards[cardIndex].getState() != RailCard.State.DISABLED) {
                        actionPerformed(new ActionEvent(cards[cardIndex], ActionEvent.ACTION_PERFORMED, "WrapperClick"));
                    }
                }
            });
            
            gbc.gridx = itemNameXOffset[col];
            gbc.gridy = yPos;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 0.5;
            gbc.weighty = 0.0; 
            gbc.fill = GridBagConstraints.NONE; // Keep highlight tight
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.insets = new Insets(1, 1, 1, 1);
            statusPanel.add(cardWrappers[i], gbc);

            if (showBasePrices) {
                basePrice[i] = new Field(si.getBasePriceModel());
                basePrice[i].setFont(cellFont);
                addField(basePrice[i], basePriceXOffset[col], yPos, 1, 1, 0);
            }
            
            if (includeBidding == StartRound.Bidding.ON_ITEMS) {
                minBid[i] = new Field(round.getMinimumBidModel(i));
                minBid[i].setFont(cellFont);
                addField(minBid[i], minBidXOffset[col], yPos, 1, 1, WIDE_RIGHT);
            }

            if (includeBidding != StartRound.Bidding.NO) {
                for (int j = 0; j < np; j++) {
                    bidPerPlayer[i][j] = new Field(round.getBidModel(i, players.getPlayerByPosition(j)));
                    bidPerPlayer[i][j].setFont(cellFont);
                    addField(bidPerPlayer[i][j], bidPerPlayerXOffset[col] + j, yPos, 1, 1, 0);
                }
            }
            itemStatus[i] = new Field(si.getStatusModel());
        }

        // 5. Fill Matrix Gaps with Spacers (Rows 1 to 10)
        for (int col = 0; col < numberOfColumns; col++) {
            for (int row = 1; row <= matrixRows; row++) {
                if (!rowOccupied[col][row]) {
                    JPanel spacer = new JPanel();
                    spacer.setOpaque(false);
                    spacer.setPreferredSize(new Dimension(10, 40)); 
                    gbc.gridx = itemNameXOffset[col];
                    gbc.gridy = row;
                    gbc.gridwidth = 1;
                    gbc.gridheight = 1;
                    gbc.weightx = 0.0;
                    gbc.weighty = 0.0;
                    gbc.fill = GridBagConstraints.NONE;
                    statusPanel.add(spacer, gbc);
                }
            }
        }

        // 6. The Sponge (Row 11) absorbs all extra height
        int spongeY = matrixRows + 1;
        gbc.gridx = 0;
        gbc.gridy = spongeY;
        gbc.gridwidth = playerStartX;
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.VERTICAL;
        statusPanel.add(Box.createVerticalGlue(), gbc);

        // 7. Footers (Row 12+)
        int footerY = matrixRows + 2;
        addField(new Caption(LocalText.getText("CASH")), playerStartX - 1, footerY + 1, 1, 1, WIDE_RIGHT);

        for (int i = 0; i < np; i++) {
            if (includeBidding != StartRound.Bidding.NO) {
                playerBids[i] = new Field(round.getBlockedCashModel(players.getPlayerByPosition(i)));
                playerBids[i].setFont(cellFont);
                addField(playerBids[i], playerStartX + i, footerY, 1, 1, WIDE_TOP);
            }
            playerFree[i] = new Field(round.getFreeCashModel(players.getPlayerByPosition(i)));
            playerFree[i].setFont(cellFont);
            addField(playerFree[i], playerStartX + i, footerY + 1, 1, 1, 0);
            lowerPlayerCaption[i] = new Field(players.getPlayerByPosition(i).getPlayerNameModel());
            lowerPlayerCaption[i].setFont(cellFont);
            addField(lowerPlayerCaption[i], playerStartX + i, footerY + 2, 1, 1, WIDE_TOP);
        }

        // 8. Rectification Pass: override addField's default 0.5 weighty
        for (Component comp : statusPanel.getComponents()) {
            GridBagConstraints c = gb.getConstraints(comp);
            if (c.gridy != spongeY && !(comp instanceof JPanel && c.gridheight > 1)) {
                c.weighty = 0.0;
                gb.setConstraints(comp, c);
            }
        }

        dummyButton = new ClickField("", "", "", this, itemGroup);
        updateFonts(currentFontSize);
    }

}
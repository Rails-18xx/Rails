package net.sf.rails.ui.swing.gamespecific._1837;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.*;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.ui.swing.StartRoundWindow;
import net.sf.rails.ui.swing.elements.*;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import rails.game.action.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartRoundWindow_1837 extends StartRoundWindow {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(StartRoundWindow_1837.class);
    
    private JPanel[] playerInventoryPanels;

    public StartRoundWindow_1837() {
        super();
    }


    @Override
    public void updateStatus(boolean myTurn) {
        log.info("SRW_1837: updateStatus called. myTurn={}", myTurn);

        super.updateStatus(myTurn);

        for (JPanel panel : playerInventoryPanels) {
            if (panel != null) panel.removeAll();
        }

        for (int i = 0; i < round.getNumberOfStartItems(); i++) {
            StartItem si = round.getStartItem(i);
            RailCard card = cards[i];
            if (card == null) continue;
            
            if (si.isSold()) {
if (cardWrappers[i] != null) {
                    cardWrappers[i].setVisible(true); // Maintain matrix structure
                    cardWrappers[i].removeAll(); // Neutralize content
                    cardWrappers[i].setBorder(null);
                    cardWrappers[i].setOpaque(false);
                }
                if (showBasePrices && basePrice[i] != null) {
                    basePrice[i].setText(""); // Clear text but hold the grid cell
                    basePrice[i].setVisible(true);
                }
                Player owner = si.getBidder();
                if (owner != null) {
                    card.setState(RailCard.State.PASSIVE); 
                    card.clearPossibleActions();
                    card.setVisible(true);
                    playerInventoryPanels[owner.getIndex()].add(card);
                    playerInventoryPanels[owner.getIndex()].add(Box.createVerticalStrut(2));
                }
            } else {
                cardWrappers[i].setVisible(true);

                cardWrappers[i].setOpaque(true);
                cardWrappers[i].setBackground(COLOR_AVAILABLE);
                cardWrappers[i].setBorder(BorderFactory.createEtchedBorder());
                if (showBasePrices && basePrice[i] != null) basePrice[i].setVisible(true);

                if (card.getParent() != cardWrappers[i]) {
                    cardWrappers[i].add(card);
                }
            }
        }
        
        for (JPanel panel : playerInventoryPanels) {
            if (panel != null) {
                panel.add(Box.createVerticalGlue()); // Maintain constant height for bought items
                panel.revalidate();
                panel.repaint();
            }
        }
        statusPanel.revalidate();
        statusPanel.repaint();
        if (gameUIManager != null) {
            gameUIManager.packAndApplySizing(this);
        }
    }

    @Override
    public void setSRPlayerTurn() {
        int playerIndex = players.getCurrentPlayer().getIndex();
        int np = players.getNumberOfPlayers();
        for (int i = 0; i < np; i++) {
            if (upperPlayerCaption != null && upperPlayerCaption[0][i] != null) {
                upperPlayerCaption[0][i].setHighlight(i == playerIndex);
            }
            if (lowerPlayerCaption != null && lowerPlayerCaption[i] != null) {
                lowerPlayerCaption[i].setHighlight(i == playerIndex);
            }
        }
    }
// --- START FIX ---
    private void addField(JComponent comp, int x, int y, int width, int height, int gaps) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0; // Enforce zero weight to prevent items from stretching vertically
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets((gaps & WIDE_TOP) > 0 ? 5 : 2, (gaps & WIDE_LEFT) > 0 ? 5 : 2, 
                                (gaps & WIDE_BOTTOM) > 0 ? 5 : 2, (gaps & WIDE_RIGHT) > 0 ? 5 : 2);
        statusPanel.add(comp, gbc);
    }
// --- END FIX ---




@Override
    protected void initCells() {
        int ni = round.getNumberOfStartItems();
        int np = players.getNumberOfPlayers();
        int matrixRows = 10; // Forced static height for the market
        Font cellFont = new Font("SansSerif", Font.BOLD, currentFontSize);

        cards = new RailCard[ni];
        cardWrappers = new JPanel[ni];
        playerInventoryPanels = new JPanel[np];
        
        basePrice = new Field[ni];
        bidPerPlayer = new Field[ni][np];
        itemStatus = new Field[ni]; 
        
        upperPlayerCaption = new Field[1][np];
        lowerPlayerCaption = new Field[np];
        playerBids = new Field[np];
        playerFree = new Field[np];
        
        itemNameXOffset = new int[numberOfColumns];
        if (showBasePrices) basePriceXOffset = new int[numberOfColumns];

        int lastX = -1;
        
        // --- START FIX ---
        // 1. Setup Header Columns
        for (int col = 0; col < numberOfColumns; col++) {
            itemNameXOffset[col] = ++lastX;
            if (showBasePrices) basePriceXOffset[col] = ++lastX;
            
            addField(new Caption(LocalText.getText("ITEM")), itemNameXOffset[col], 0, 1, 1, WIDE_LEFT + WIDE_RIGHT + WIDE_BOTTOM);
            if (showBasePrices) {
                addField(new Caption(LocalText.getText("PRICE")), basePriceXOffset[col], 0, 1, 1, WIDE_BOTTOM);
            }
        }

        // 2. Vertical Separator between Items and Player Inventories
        int separatorX = ++lastX;
        // Height is dynamic: ni + headers + footers
addField(new JSeparator(SwingConstants.VERTICAL), separatorX, 0, 1, matrixRows + 5, WIDE_LEFT + WIDE_RIGHT);

        // 3. Setup Player Names and Inventory Panels
        int playerStartX = ++lastX;
        playerCaptionXOffset = new int[]{playerStartX};
        
        for (int i = 0; i < np; i++) {
            upperPlayerCaption[0][i] = new Field(players.getPlayerByPosition(i).getPlayerNameModel());
            upperPlayerCaption[0][i].setFont(cellFont);
            addField(upperPlayerCaption[0][i], playerStartX + i, 0, 1, 1, WIDE_BOTTOM);

            playerInventoryPanels[i] = new JPanel();
            playerInventoryPanels[i].setLayout(new BoxLayout(playerInventoryPanels[i], BoxLayout.Y_AXIS));
            playerInventoryPanels[i].setBackground(Color.WHITE);
            playerInventoryPanels[i].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            gbc.gridx = playerStartX + i;
            gbc.gridy = 1;
gbc.gridheight = matrixRows + 1; // Span the 10-row matrix and the sponge row
//             gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0; 
            gbc.weighty = 10.0; 
            statusPanel.add(playerInventoryPanels[i], gbc);
        }

        // 4. Add Items (RailCards)
        for (int i = 0; i < ni; i++) {
            final StartItem si = round.getStartItem(i);
            int col = multipleColumns ? si.getColumn() - 1 : 0;
            int row = multipleColumns ? si.getRow() - 1 : i;
            int yPos = row + 1;

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
            gbc.weighty = 0.0; // CRITICAL: Non-zero weighty prevents collapse
            gbc.fill = GridBagConstraints.BOTH; 
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(1, 1, 1, 1);
            statusPanel.add(cardWrappers[i], gbc);

            if (showBasePrices) {
                basePrice[i] = new Field(si.getBasePriceModel());
                basePrice[i].setFont(cellFont);
                addField(basePrice[i], basePriceXOffset[col], yPos, 1, 1, 0);
            }
            
            itemStatus[i] = new Field(si.getStatusModel());
        }
            // 4b. Add a vertical sponge below row 10 to push all market items to the top
        gbc.gridx = 0;
        gbc.gridy = matrixRows + 1;
        gbc.gridwidth = playerStartX;
        gbc.weighty = 1.0; // This absorbs all extra vertical space
        gbc.fill = GridBagConstraints.VERTICAL;
        statusPanel.add(Box.createVerticalGlue(), gbc);



        
        // 5. Setup Footers (CASH / BIDS)
int footerY = matrixRows + 2; // Position footers below the sponge
//         addField(new Caption(LocalText.getText("CASH")), playerStartX - 1, footerY + 1, 1, 1, WIDE_RIGHT);

        for (int i = 0; i < np; i++) {
            playerBids[i] = new Field(round.getBlockedCashModel(players.getPlayerByPosition(i)));
            playerBids[i].setFont(cellFont);
            addField(playerBids[i], playerStartX + i, footerY, 1, 1, WIDE_TOP);
            
            playerFree[i] = new Field(round.getFreeCashModel(players.getPlayerByPosition(i)));
            playerFree[i].setFont(cellFont);
            addField(playerFree[i], playerStartX + i, footerY + 1, 1, 1, 0);
            
            lowerPlayerCaption[i] = new Field(players.getPlayerByPosition(i).getPlayerNameModel());
            lowerPlayerCaption[i].setFont(cellFont);
            addField(lowerPlayerCaption[i], playerStartX + i, footerY + 2, 1, 1, WIDE_TOP);
        }
// Initialize dummyButton to prevent NPE in super.updateStatus()
        dummyButton = new ClickField("", "", "", this, itemGroup);
        updateFonts(currentFontSize);
    }














}
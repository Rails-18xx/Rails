package net.sf.rails.ui.swing.gamespecific._1817;

import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import net.sf.rails.ui.swing.StartRoundWindow;
import net.sf.rails.ui.swing.elements.Field;
import net.sf.rails.game.specific._1817.StartRound_1817;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.Box;

public class StartRoundWindow_1817 extends StartRoundWindow {

    private static final long serialVersionUID = 1L;
    private Field seedMoneyField;
    private JLabel seedLabel;
    private boolean seedAdded = false;

    public StartRoundWindow_1817() {
        super();
    }

    @Override
public void setSRPlayerTurn() {
// Allow superclass to clear old highlights and set new ones
super.setSRPlayerTurn();

    int playerIndex = players.getCurrentPlayer().getIndex();
    
    for (int i = 0; i < players.getNumberOfPlayers(); i++) {
        if (i == playerIndex) {
            // Apply green background to the active player's upper and lower captions
            for (int j = 0; j < numberOfColumns; j++) {
                if (upperPlayerCaption != null && upperPlayerCaption[j][i] != null) {
                    upperPlayerCaption[j][i].setBackground(java.awt.Color.GREEN);
                    upperPlayerCaption[j][i].setOpaque(true);
                }
            }
            if (lowerPlayerCaption != null && lowerPlayerCaption[i] != null) {
                lowerPlayerCaption[i].setBackground(java.awt.Color.GREEN);
                lowerPlayerCaption[i].setOpaque(true);
            }
        }
    }
}

    @Override
    protected void initCells() {
        super.initCells();

        StartRound_1817 sr1817 = (StartRound_1817) round;

        if (!seedAdded) {

           seedLabel = new JLabel("Seed Money:");
            seedLabel.setFont(new Font("SansSerif", Font.BOLD, currentFontSize));
            
            seedMoneyField = new Field(sr1817.getSeedMoneyModel());
            seedMoneyField.setFont(new Font("SansSerif", Font.BOLD, currentFontSize));
            
            JPanel seedPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            seedPanel.add(seedLabel);
            seedPanel.add(seedMoneyField);
            
            java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 15; // Placed securely below the matrix and footer rows
            gbc.gridwidth = 2;
            gbc.insets = new java.awt.Insets(5, 5, 5, 5);
            gbc.anchor = java.awt.GridBagConstraints.WEST;
            
            statusPanel.add(seedPanel, gbc);

            seedAdded = true;
        } else {
            // Keep font size in sync if initCells is recalled (e.g. zooming)
            seedLabel.setFont(new Font("SansSerif", Font.BOLD, currentFontSize));
            seedMoneyField.setFont(new Font("SansSerif", Font.BOLD, currentFontSize));
        }

        getContentPane().revalidate();
        getContentPane().repaint();
        forceMinimumBids();
    }

    @Override
    public void updateStatus(boolean myTurn) {
        super.updateStatus(myTurn);
        forceMinimumBids();
        
    }

    private void forceMinimumBids() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            StartRound_1817 sr1817 = (StartRound_1817) round;
            int currentSeed = sr1817.getSeedMoneyModel().value();

            for (int i = 0; i < round.getNumberOfStartItems(); i++) {
                if (minBid != null && i < minBid.length && minBid[i] != null) {
                    net.sf.rails.game.StartItem si = round.getStartItem(i);
                    int bid = si.getBid();
                    int min;
                    if (si.getBidder() != null) {
                        min = bid + 5;
                    } else {
                        min = Math.max(5, si.getBasePrice() - currentSeed);
                    }
                    minBid[i].setText(String.valueOf(min));
                }
            }
        });

    }

}
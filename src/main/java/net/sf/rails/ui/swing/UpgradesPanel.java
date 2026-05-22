package net.sf.rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.ui.swing.elements.GUIHexUpgrades;
import net.sf.rails.ui.swing.elements.UpgradeLabel;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.ui.swing.elements.RailsIconButton;
import net.sf.rails.ui.swing.hexmap.*;

public class UpgradesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(UpgradesPanel.class);

    private static final int UPGRADE_TILE_ZOOM_STEP = 10;

    private final ORUIManager orUIManager;
    private final JPanel upgradePanel;
    private final JScrollPane scrollPane;
    private final RailsIconButton confirmButton;
    private final RailsIconButton skipButton;
    private boolean omitButtons;
    private GUIHexUpgrades hexUpgrades;
    
    private RemainingTilesWindow.MiniDock miniDock;
    
    private final int fixedTileHeight;
    private final int fixedTileWidth;

    public UpgradesPanel(ORUIManager orUIManager, boolean omitButtons) {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.orUIManager = orUIManager;
        this.omitButtons = omitButtons;
        this.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color bgColor = UIManager.getColor("Panel.background");

        int baseMetric = (int) Math.round(100 * (2 + GUIGlobals.getFontsScale()) / 3);
        
        this.fixedTileHeight = baseMetric + 15; 
        this.fixedTileWidth = (int) (baseMetric * 0.85); 

        int panelHeight = fixedTileHeight + 10;

        this.setPreferredSize(new Dimension(Short.MAX_VALUE, panelHeight));
        this.setMaximumSize(new Dimension(Short.MAX_VALUE, panelHeight));
        setVisible(true);

        upgradePanel = new JPanel();
        upgradePanel.setOpaque(true);
        upgradePanel.setLayout(new BoxLayout(upgradePanel, BoxLayout.LINE_AXIS));
        upgradePanel.setBackground(bgColor);
        upgradePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        scrollPane = new JScrollPane(upgradePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        scrollPane.setPreferredSize(new Dimension(600, panelHeight));
        scrollPane.setMinimumSize(new Dimension(100, panelHeight));
        scrollPane.getViewport().setBackground(bgColor);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        Action confirmAction = new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                UpgradesPanel.this.orUIManager.confirmUpgrade();
            }
        };
        confirmAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);

        Action skipAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                UpgradesPanel.this.orUIManager.skipUpgrade();
            }
        };
        skipAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
        
        confirmButton = new RailsIconButton(RailsIcon.CONFIRM, confirmAction);
        confirmButton.setEnabled(false);

        skipButton = new RailsIconButton(RailsIcon.SKIP, skipAction);
        skipButton.setEnabled(false);
         
        if (omitButtons) {
            confirmButton.setVisible(false);
            skipButton.setVisible(false);
        } else {
            Dimension buttonDimension = new Dimension(Short.MAX_VALUE, 25);
            confirmButton.setMaximumSize(buttonDimension);
            skipButton.setMaximumSize(buttonDimension);
            confirmButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            skipButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            add(Box.createHorizontalStrut(2)); 
            add(confirmButton);
            add(Box.createHorizontalStrut(2)); 
            add(skipButton);
            add(Box.createHorizontalStrut(5)); 
        }
        
        add(scrollPane);

        add(Box.createHorizontalStrut(5));
        
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setMaximumSize(new Dimension(2, panelHeight - 10));
        add(sep);
        add(Box.createHorizontalStrut(5));

        this.miniDock = new RemainingTilesWindow.MiniDock(orUIManager);
        
        // --- SIZING FIX: Double Wide (480px) ---
        Dimension dockSize = new Dimension(480, panelHeight - 4); 
        miniDock.setPreferredSize(dockSize);
        miniDock.setMaximumSize(dockSize);
        miniDock.setMinimumSize(dockSize);
        
        add(miniDock);

        add(Box.createHorizontalStrut(5)); 
        
        setButtons();
        revalidate();
    }


    private void addLegendItem(JPanel panel, String key, String desc) {
        JLabel lbl = new JLabel("<html><font color='#222222' size='3'><b>[" + key + "]</b></font> " + desc + "</html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11)); 
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT); 
        panel.add(lbl);
        panel.add(Box.createHorizontalStrut(5)); 
    }

    public void setHexUpgrades(GUIHexUpgrades hexUpgrades) {
        this.hexUpgrades = hexUpgrades;
    }
    
    private int getZoomStep() {
        if (orUIManager.getORWindow().isDockingFrameworkEnabled()) {
            return orUIManager.getMap().getZoomStep();
        } else {
            return UPGRADE_TILE_ZOOM_STEP;
        }
    }

    public RailsIconButton[] getButtons() {
        return new RailsIconButton[] { confirmButton, skipButton };
    }
    
    private void setButtons() {
        if (omitButtons) {
            boolean isVisible = confirmButton.isEnabled() || skipButton.isEnabled();
            confirmButton.setVisible(isVisible);
            skipButton.setVisible(isVisible);
        }
    }
    
    private void resetUpgrades(boolean skip) {
        hexUpgrades.setActiveHex(null, 0);
        upgradePanel.removeAll();
        scrollPane.getHorizontalScrollBar().setValue(0);
        scrollPane.repaint();
        confirmButton.setEnabled(false);
        skipButton.setEnabled(skip);
        setButtons();
    }

    public void setInactive() {
        resetUpgrades(false);
    }
    
    public void setActive() {
        resetUpgrades(true);
    }

    public void refreshMiniDock() {
        if (miniDock != null) {
            miniDock.repaint();
        }
    }
    
    public void setSelect(GUIHex hex) {
        hexUpgrades.setActiveHex(hex, getZoomStep());
        showLabels();
        refreshUpgrades();
        HexUpgrade activeUpgrade = hexUpgrades.getActiveUpgrade();
        if (activeUpgrade != null) {
            confirmButton.setEnabled(true);
            orUIManager.orPanel.enableConfirm(true);
        } else {
            confirmButton.setEnabled(false);
            orUIManager.orPanel.enableConfirm(false);
        }
        setButtons();
    }
   
    public void nextSelection() {
        hexUpgrades.nextSelection();
        refreshUpgrades();
    }

    public void nextUpgrade() {
        hexUpgrades.nextUpgrade();
        refreshUpgrades();
    }
    
    public void setActiveUpgrade(HexUpgrade upgrade) {
        hexUpgrades.setUpgrade(upgrade);
        refreshUpgrades();
    }
    
public void refreshUpgrades() {
        upgradePanel.revalidate();
        upgradePanel.repaint();
        UpgradeLabel active = hexUpgrades.getActiveLabel();
        if (active != null) {
            upgradePanel.scrollRectToVisible(active.getBounds());
        }
    }
    
    private void showLabels() {
        upgradePanel.removeAll();
        
        Dimension tightSize = new Dimension(fixedTileWidth, fixedTileHeight);
        
        for (UpgradeLabel label : hexUpgrades.getUpgradeLabels()) {
            final HexUpgrade upgrade = label.getUpgrade();
            if (upgrade.isValid()) {
                label.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        setActiveUpgrade(upgrade);
                    }
                }); 
            } else { 
                if (upgrade instanceof TileHexUpgrade && ((TileHexUpgrade)upgrade).noTileAvailable()) {
                    HexHighlightMouseListener.addMouseListener(label, orUIManager, 
                            ((TileHexUpgrade)upgrade).getUpgrade().getTargetTile(), true);
                }
            }
            
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setToolTipText(null);

            label.setPreferredSize(tightSize);
            label.setMaximumSize(tightSize);
            label.setMinimumSize(tightSize);

            upgradePanel.add(label);
            upgradePanel.add(Box.createHorizontalStrut(1));
        }
        
        upgradePanel.add(Box.createHorizontalGlue());
        
        upgradePanel.revalidate();
        upgradePanel.repaint();
    }
}
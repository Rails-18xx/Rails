package net.sf.rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.rails.ui.swing.elements.GUIHexUpgrades;
import net.sf.rails.ui.swing.elements.UpgradeLabel;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.ui.swing.elements.RailsIconButton;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;


public class UpgradesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int UPGRADE_TILE_ZOOM_STEP = 10;

    private final ORUIManager orUIManager;
    
    // ui elements
    private final JPanel upgradePanel;
    private final JScrollPane scrollPane;
    
    private final RailsIconButton confirmButton;
    private final RailsIconButton skipButton;

    /**
     * If set, done/cancel buttons are not added to the pane. Instead, the
     * visibility property of these buttons are handled such that they are set
     * to visible if they normally would be added to the pane.
     * Required for Docking approach
     */
    private boolean omitButtons;
    
    private GUIHexUpgrades hexUpgrades;
    

    public UpgradesPanel(ORUIManager orUIManager, boolean omitButtons) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        this.orUIManager = orUIManager;
        this.omitButtons = omitButtons;
        //this.setBackground(Color.DARK_GRAY);
        //this.setBorder(border);

        int width = (int) Math.round(110 * (2 + GUIGlobals.getFontsScale()) / 3);
        int height = 200;
        
        this.setPreferredSize(new Dimension(width, height + 50));
        this.setMaximumSize(new Dimension(width, height + 50));
        setVisible(true);

        upgradePanel = new JPanel();

        upgradePanel.setOpaque(true);
        upgradePanel.setLayout(new BoxLayout(upgradePanel, BoxLayout.PAGE_AXIS));
        upgradePanel.setBackground(Color.DARK_GRAY);

        scrollPane = new JScrollPane(upgradePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(width, height));
        scrollPane.setMinimumSize(new Dimension(width, height));
        

        Action confirmAction = new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                UpgradesPanel.this.orUIManager.confirmUpgrade();
            }
        };
        
        confirmAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);

        Action skipAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                UpgradesPanel.this.orUIManager.skipUpgrade();;
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
            confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            skipButton.setMaximumSize(buttonDimension);
            skipButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(confirmButton);
            add(skipButton);
        }
        add(scrollPane);
        
        setButtons();
        
        revalidate();

    }
    
    public void setHexUpgrades(GUIHexUpgrades hexUpgrades) {
        this.hexUpgrades = hexUpgrades;
    }
    
    
    /**
     * @return Default zoom step for conventional panes or, for dockable panes,
     * the zoom step used in the map. Map zoom step can only be used for
     * dockable panes as user-based pane sizing could be necessary when
     * displaying tiles of an arbitrary size
     */
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
            // only set externally managed buttons to visible if at least
            // one of them is enabled
            boolean isVisible =
                    confirmButton.isEnabled() || skipButton.isEnabled();
            confirmButton.setVisible(isVisible);
            skipButton.setVisible(isVisible);
        }
    }
    
    private void resetUpgrades(boolean skip) {
        hexUpgrades.setActiveHex(null, 0);
        upgradePanel.removeAll();
        // set scrollposition to top and show again
        scrollPane.getVerticalScrollBar().setValue(0);
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
    
    public void setSelect(GUIHex hex) {
        hexUpgrades.setActiveHex(hex, getZoomStep());
        showLabels();
        refreshUpgrades();

        HexUpgrade activeUpgrade = hexUpgrades.getActiveUpgrade();
        if (activeUpgrade != null) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
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
    
    private void setActiveUpgrade(HexUpgrade upgrade) {
        hexUpgrades.setUpgrade(upgrade);
        refreshUpgrades();
    }
    
    private void refreshUpgrades() {
        upgradePanel.revalidate();
        upgradePanel.repaint();
        UpgradeLabel active = hexUpgrades.getActiveLabel();
        upgradePanel.scrollRectToVisible(active.getBounds());
    }
    
    private void showLabels() {
        upgradePanel.removeAll();
        for (UpgradeLabel label:hexUpgrades.getUpgradeLabels()) {
            final HexUpgrade upgrade = label.getUpgrade();

            if (upgrade.isValid()) {
                // mouse clicks => activate upgrade
                label.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        setActiveUpgrade(upgrade);
                    }
                }); 
            } else { 
                // invalid TileHexUpgrades == >
                // highlight where tiles of this ID have been laid if no
                // tiles left
                if (upgrade instanceof TileHexUpgrade && ((TileHexUpgrade)upgrade).noTileAvailable()) {
                    HexHighlightMouseListener.addMouseListener(label, orUIManager, 
                            ((TileHexUpgrade)upgrade).getUpgrade().getTargetTile(), true);
                }
            }
            upgradePanel.add(label);
        }
    }
}

package net.sf.rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.sf.rails.ui.swing.elements.GUIHexUpgrades;
import net.sf.rails.ui.swing.elements.UpgradeLabel;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;

public class FloatingUpgradesDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final ORUIManager orUIManager;
    private final JPanel container;
    
    private final JPanel controlPanel;
    private final JButton btnRotLeft;
    private final JButton btnRotRight;
    private final JButton btnConfirm;
    private final JScrollPane scroll;

    private GUIHex currentHex;
    private GUIHexUpgrades currentHexUpgrades;

    public FloatingUpgradesDialog(ORWindow owner, ORUIManager orUIManager) {
        super(owner, "Available Upgrades", false);
        this.orUIManager = orUIManager;
        
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setResizable(false);
        
        setLayout(new BorderLayout(5, 5));
        
        container = new JPanel();
        container.setBackground(UIManager.getColor("Panel.background"));
        
        scroll = new JScrollPane();
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        add(scroll, BorderLayout.CENTER);

        controlPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        btnRotLeft = new JButton("↺");
        btnRotRight = new JButton("↻");
        btnConfirm = new JButton("✓");

        Font btnFont = new Font("SansSerif", Font.BOLD, 16);
        Dimension standardBtnSize = new Dimension(80, 40);

        configureControlAction(btnRotLeft, btnFont, standardBtnSize, UIManager.getColor("Button.background"), UIManager.getColor("Button.foreground"));
        configureControlAction(btnRotRight, btnFont, standardBtnSize, UIManager.getColor("Button.background"), UIManager.getColor("Button.foreground"));
        configureControlAction(btnConfirm, new Font("SansSerif", Font.BOLD, 18), standardBtnSize, new java.awt.Color(30, 144, 255), java.awt.Color.WHITE);

        // --- START FIX --- 
        // Only trigger our manual sweep. Do NOT call the standard nextSelection() as it causes double-rotation
        btnRotLeft.addActionListener(e -> rotateAllDisplayedTiles());
        btnRotRight.addActionListener(e -> rotateAllDisplayedTiles());
        // --- END FIX ---

        btnConfirm.addActionListener(e -> {
            orUIManager.confirmUpgrade();
        });

        controlPanel.add(btnRotLeft);
        controlPanel.add(btnRotRight);
        controlPanel.add(btnConfirm);

        JPanel rightWrapper = new JPanel(new java.awt.GridBagLayout());
        rightWrapper.add(controlPanel);
        add(rightWrapper, BorderLayout.EAST);
        
        
    }

    private void rotateAllDisplayedTiles() {
        if (currentHexUpgrades == null) return;
        boolean changed = false;
        
        for (UpgradeLabel originalLabel : currentHexUpgrades.getUpgradeLabels()) {
            HexUpgrade upgrade = originalLabel.getUpgrade();
            if (upgrade != null && upgrade.isValid() && upgrade instanceof TileHexUpgrade) {
                TileHexUpgrade tileUpgrade = (TileHexUpgrade) upgrade;
                if (tileUpgrade.getRotations() != null) {
                    tileUpgrade.nextSelection();
                    changed = true;
                }
            }
        }
        
        if (changed) {
            // Push the repaint to the map so the newly rotated active tile is drawn correctly
            if (orUIManager.getMap() != null) {
                orUIManager.getMap().repaintTiles(new java.awt.Rectangle(orUIManager.getMap().getSize()));
            }
            if (orUIManager.getUpgradePanel() != null) {
                orUIManager.getUpgradePanel().repaint();
            }
            
            // Rebuild the floating UI to sync images
            showUpgrades(currentHex, currentHexUpgrades, getLocation());
        }
    }

    private void configureControlAction(JButton btn, Font font, Dimension size, java.awt.Color bg, java.awt.Color fg) {
        btn.setFont(font);
        btn.setPreferredSize(size);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
    }

    public void showUpgrades(GUIHex hex, GUIHexUpgrades hexUpgrades, Point screenLocation) {
        this.currentHex = hex;
        this.currentHexUpgrades = hexUpgrades;
        
        container.removeAll();
        
        int validTileCount = 0;
        if (hexUpgrades != null && hexUpgrades.getUpgradeLabels() != null) {
            for (UpgradeLabel originalLabel : hexUpgrades.getUpgradeLabels()) {
                HexUpgrade upgrade = originalLabel.getUpgrade();
                if (upgrade != null && upgrade.isValid()) {
                    validTileCount++;
                }
            }
        }

        if (validTileCount == 0) {
            setVisible(false);
            return;
        }

        // Lock to 3 columns max to prevent stretching
        int columns = 3;
        if (validTileCount < 3) columns = validTileCount;
        
        container.setLayout(new GridLayout(0, columns, 5, 5));
        container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        int currentMapZoom = 10; 
        if (orUIManager != null && orUIManager.getMap() != null) {
            currentMapZoom = orUIManager.getMap().getZoomStep();
        }

        // --- START FIX --- 
        // Hard-lock the precise dimensions used by the original UpgradesPanel
        int calcMetric = (int) Math.round(100 * (2 + net.sf.rails.ui.swing.GUIGlobals.getFontsScale()) / 3);
        Dimension fixedTileSize = new Dimension((int)(calcMetric * 0.85), calcMetric + 15);
        // --- END FIX ---

        if (hexUpgrades != null && hexUpgrades.getUpgradeLabels() != null) {
            for (UpgradeLabel originalLabel : hexUpgrades.getUpgradeLabels()) {
                final HexUpgrade upgrade = originalLabel.getUpgrade();
                
                if (upgrade != null && upgrade.isValid()) {
                    UpgradeLabel floatingLabel = null;
                    try {
                        floatingLabel = new UpgradeLabel(upgrade, currentMapZoom);
                    } catch (Exception e) {
                        try {
                            floatingLabel = originalLabel.getClass().getConstructor(HexUpgrade.class, int.class).newInstance(upgrade, currentMapZoom);
                        } catch (Exception ex) {
                            System.err.println("Failed to instantiate UpgradeLabel");
                        }
                    }

                    if (floatingLabel != null) {
                        floatingLabel.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                if (orUIManager.getUpgradePanel() != null) {
                                    orUIManager.getUpgradePanel().setActiveUpgrade(upgrade);
                                }
                            }
                        });

                        floatingLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                        floatingLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
                        floatingLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
                        floatingLabel.setToolTipText(null);
                        
                        // Enforce the locked dimension on every tile
                        floatingLabel.setPreferredSize(fixedTileSize);
                        floatingLabel.setMaximumSize(fixedTileSize);
                        floatingLabel.setMinimumSize(fixedTileSize);

                        container.add(floatingLabel);
                    }
                }
            }
        }

        // --- START FIX ---
        // Wrap the grid in a BorderLayout.NORTH wrapper to stop the GridLayout from stretching vertically
        JPanel wrapper = new JPanel(new BorderLayout());
       // Wrap the grid in a BorderLayout.NORTH wrapper to stop the GridLayout from stretching vertically
        wrapper.setBackground(UIManager.getColor("Panel.background"));
        wrapper.add(container, BorderLayout.NORTH);
        scroll.setViewportView(wrapper);
        
        // Automatically resize the dialog container to perfectly fit the new tile sizes
        pack();   
        if (screenLocation != null) {
            setLocation(screenLocation.x, screenLocation.y);
        }
        setVisible(true);
    }
}
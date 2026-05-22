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

public class FloatingUpgradesDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final ORUIManager orUIManager;
    private final JPanel container;
    
    private final JPanel controlPanel;
    private final JButton btnRotLeft;
    private final JButton btnRotRight;
    private final JButton btnConfirm;
    
    // --- START FIX ---
    private final JScrollPane scroll;
    // --- END FIX ---

    public FloatingUpgradesDialog(ORWindow owner, ORUIManager orUIManager) {
        super(owner, "Available Upgrades", false);
        this.orUIManager = orUIManager;
        
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setResizable(false);
        
        setLayout(new BorderLayout(5, 0));
        
        // --- START FIX ---
        // Switch container grid assembly dynamically inside showUpgrades instead of FlowLayout
        container = new JPanel();
        container.setBackground(UIManager.getColor("Panel.background"));
        
        scroll = new JScrollPane(container);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        // Allow vertical scrollbar fallback if grid heights cross window threshold bounds
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // --- END FIX ---
        
        add(scroll, BorderLayout.CENTER);

        controlPanel = new JPanel(new GridLayout(3, 1, 0, 4));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 5));

        btnRotLeft = new JButton("↶");
        btnRotRight = new JButton("↷");
        btnConfirm = new JButton("✓");

        Font btnFont = new Font("SansSerif", Font.BOLD, 14);
        btnRotLeft.setFont(btnFont);
        btnRotRight.setFont(btnFont);
        btnConfirm.setFont(new Font("SansSerif", Font.BOLD, 16));

        btnConfirm.setBackground(new java.awt.Color(30, 144, 255));
        btnConfirm.setForeground(java.awt.Color.WHITE);
        btnConfirm.setOpaque(true);
        btnConfirm.setBorderPainted(false);

        btnRotLeft.addActionListener(e -> {
            if (orUIManager.getUpgradePanel() != null) {
                orUIManager.getUpgradePanel().nextSelection(); 
            }
        });

        btnRotRight.addActionListener(e -> {
            if (orUIManager.getUpgradePanel() != null) {
                orUIManager.getUpgradePanel().nextSelection();
            }
        });

        btnConfirm.addActionListener(e -> {
            orUIManager.confirmUpgrade();
        });

        controlPanel.add(btnRotLeft);
        controlPanel.add(btnRotRight);
        controlPanel.add(btnConfirm);

        add(controlPanel, BorderLayout.EAST);
    }

    public void showUpgrades(GUIHex hex, GUIHexUpgrades hexUpgrades, Point screenLocation) {
        container.removeAll();
        
        // Count valid elements first to setup clean square grid boundaries
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

        // --- START FIX ---
        // Dynamically compute square columns and rows based on tile count
        int columns = validTileCount;
        int rows = 1;
        
        if (validTileCount > 2) {
            // Determine square dimensions (e.g., 3 tiles -> 2x2 grid, 5 tiles -> 3x2 grid)
            columns = (int) Math.ceil(Math.sqrt(validTileCount));
            rows = (int) Math.ceil((double) validTileCount / columns);
        }
        
        container.setLayout(new GridLayout(rows, columns, 6, 6));
        container.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        // --- END FIX ---

        int currentMapZoom = 10; 
        if (orUIManager != null && orUIManager.getMap() != null) {
            currentMapZoom = orUIManager.getMap().getZoomStep();
        }

        // Track metrics to accurately compute window dimensions and stop clipping cuts
        int maxTileWidth = 0;
        int maxTileHeight = 0;

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
                            System.err.println("Failed to instantiate UpgradeLabel for floating panel.");
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
                        
                        Dimension mapTileSize = originalLabel.getPreferredSize();
                        if (mapTileSize != null && mapTileSize.width > 0) {
                            floatingLabel.setPreferredSize(mapTileSize);
                            floatingLabel.setMaximumSize(mapTileSize);
                            floatingLabel.setMinimumSize(mapTileSize);
                        } else {
                            int calcMetric = (int) Math.round(100 * (2 + net.sf.rails.ui.swing.GUIGlobals.getFontsScale()) / 3);
                            Dimension dynamicFallback = new Dimension((int)(calcMetric * 0.85), calcMetric + 15);
                            floatingLabel.setPreferredSize(dynamicFallback);
                            floatingLabel.setMaximumSize(dynamicFallback);
                            floatingLabel.setMinimumSize(dynamicFallback);
                        }

                        maxTileWidth = Math.max(maxTileWidth, floatingLabel.getPreferredSize().width);
                        maxTileHeight = Math.max(maxTileHeight, floatingLabel.getPreferredSize().height);

                        container.add(floatingLabel);
                    }
                }
            }
        }

        // --- START FIX ---
        // Dynamically compute exact layout dimensions incorporating margins, pads, gaps, and right control panel
        int containerWidth = (maxTileWidth * columns) + (6 * (columns - 1)) + 12;
        int containerHeight = (maxTileHeight * rows) + (6 * (rows - 1)) + 12;

        // Establish the container and scroll view limits explicitly
        container.setPreferredSize(new Dimension(containerWidth, containerHeight));
        scroll.setPreferredSize(new Dimension(containerWidth, containerHeight));

        pack();

        // Calculate final absolute window bounds including OS frames and the right button sidebar
        int totalWindowWidth = containerWidth + controlPanel.getPreferredSize().width + 25;
        int totalWindowHeight = Math.max(containerHeight + 35, controlPanel.getPreferredSize().height + 40);

        setSize(totalWindowWidth, totalWindowHeight);
        // --- END FIX ---
        
        if (screenLocation != null) {
            setLocation(screenLocation.x, screenLocation.y);
        }
        setVisible(true);
    }
}
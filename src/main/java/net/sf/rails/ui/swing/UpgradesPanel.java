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

    private double scaleMultiplier = GUIGlobals.getFontsScale();

    public void adjustFontScale(double delta) {
        this.scaleMultiplier = Math.max(0.5, Math.min(3.0, this.scaleMultiplier + delta));

        // Recalculate dimensions dynamically using the new local scale multiplier
        int baseMetric = (int) Math.round(100 * (2 + this.scaleMultiplier) / 3);
        int tileHeight = baseMetric + 15;
        int tileWidth = (int) (baseMetric * 0.85);

        // Safely update field reflections via reflection if internal drawing relies on
        // them,
        // or directly push panel dimension overrides.
        int panelHeight = tileHeight + 10;
        this.setPreferredSize(new Dimension(Short.MAX_VALUE, panelHeight));
        this.setMaximumSize(new Dimension(Short.MAX_VALUE, panelHeight));

        // Re-render and enforce layout updates
        showLabels();
        revalidate();
        repaint();
    }

    public UpgradesPanel(ORUIManager orUIManager, boolean omitButtons) {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        this.orUIManager = orUIManager;
        this.omitButtons = omitButtons;
        this.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color bgColor = UIManager.getColor("Panel.background");

        int baseMetric = (int) Math.round(100 * (2 + scaleMultiplier) / 3);

        this.fixedTileHeight = baseMetric + 15;
        this.fixedTileWidth = (int) (baseMetric * 0.85);

        int panelHeight = fixedTileHeight + 10;

        this.setPreferredSize(new Dimension(Short.MAX_VALUE, panelHeight));
        this.setMaximumSize(new Dimension(Short.MAX_VALUE, panelHeight));
        setVisible(true);

        // Fields still required to prevent internal NullPointerExceptions in legacy
        // wrappers
        upgradePanel = new JPanel();
        scrollPane = new JScrollPane(upgradePanel);
        confirmButton = new RailsIconButton(RailsIcon.CONFIRM, null);
        skipButton = new RailsIconButton(RailsIcon.SKIP, null);

        this.miniDock = new RemainingTilesWindow.MiniDock(orUIManager);

        // Force miniDock to consume the entire horizontal width footprint
        miniDock.setPreferredSize(new Dimension(1000, panelHeight - 4));
        miniDock.setMinimumSize(new Dimension(100, panelHeight - 4));
        miniDock.setMaximumSize(new Dimension(Short.MAX_VALUE, panelHeight - 4));

        add(miniDock);

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
        // Safe Stub: Upgrade tiles are hidden. Clear the background panel 
        // to prevent NullPointerExceptions when hexUpgrades is uninitialized.
        if (upgradePanel != null) {
            upgradePanel.removeAll();
            upgradePanel.revalidate();
            upgradePanel.repaint();
        }
    }
}
package net.sf.rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.rails.ui.swing.hexmap.HexUpgrade;

/** JLabel extension to allow attaching the internal hex ID */
public class UpgradeLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    private static final Border BORDER = new EtchedBorder();
    private static final Color DEFAULT_LABEL_BG_COLOUR = new JLabel("").getBackground();
    private static final Color SELECTED_LABEL_BG_COLOUR = new Color(255, 220, 150);

    private final HexUpgrade upgrade;
    private final int zoomStep;

    public UpgradeLabel(HexUpgrade upgrade, int zoomStep) {
        super();
        this.upgrade = upgrade;
        this.zoomStep = zoomStep;
        
        this.setIcon(upgrade.getUpgradeIcon(zoomStep));
        this.setText(upgrade.getUpgradeText());
        this.setToolTipText(upgrade.getUpgradeToolTip());
        
        this.setOpaque(true);
        this.setVisible(true);
        this.setBorder(BORDER);
        this.setBackground(DEFAULT_LABEL_BG_COLOUR);

        this.setMaximumSize(new Dimension(
                Short.MAX_VALUE, (int)this.getPreferredSize().getHeight()));
        this.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        this.setEnabled(upgrade.isValid());
    }
    
    public void setSelected(boolean selected) {
        if (selected) {
            this.setBackground(SELECTED_LABEL_BG_COLOUR);
        } else {
            this.setBackground(DEFAULT_LABEL_BG_COLOUR);
        }
    }
    
    public void updateIcon() {
        this.setIcon(upgrade.getUpgradeIcon(zoomStep));
    }

    public HexUpgrade getUpgrade() {
        return upgrade;
    }

}
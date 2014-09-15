package net.sf.rails.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Set;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import rails.game.action.LayBaseToken;
import rails.game.action.LayBonusToken;
import rails.game.action.LayTile;
import rails.game.action.LayToken;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.TileUpgrade;
import net.sf.rails.ui.swing.elements.ActionLabel;
import net.sf.rails.ui.swing.elements.UpgradeLabel;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.ui.swing.elements.RailsIconButton;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.GUITile;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;
import net.sf.rails.ui.swing.hexmap.TokenHexUpgrade;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade.Validation;


public class UpgradesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int UPGRADE_TILE_ZOOM_STEP = 10;
    private static final Color DEFAULT_LABEL_BG_COLOUR = new JLabel("").getBackground();
    private static final Color SELECTED_LABEL_BG_COLOUR = new Color(255, 220, 150);

    private final ORUIManager orUIManager;
    
    // ui elements
    private final JPanel upgradePanel;
    private final JScrollPane scrollPane;
    private final Border border = new EtchedBorder();
    
    // TODO: Replace this with an action based approach
    private final RailsIconButton confirmButton;
    private final RailsIconButton skipButton;

    /**
     * If set, done/cancel buttons are not added to the pane. Instead, the
     * visibility property of these buttons are handled such that they are set
     * to visible if they normally would be added to the pane.
     * Required for Docking approach
     */
    private boolean omitButtons;

    private Set<HexUpgrade> upgrades = ImmutableSet.of();
    
    // current selected upgrade
    private HexUpgrade activeUpgrade = null;

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
                confirmUpgrade();
            }
        };
        
        confirmAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);

        Action skipAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                skipUpgrade();
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
    
    private void resetUpgrades() {
        upgradePanel.removeAll();
        activeUpgrade = null;
        upgrades = ImmutableSet.of();
        // set scrollposition to top and show again
        scrollPane.getVerticalScrollBar().setValue(0);
        scrollPane.repaint();
    }

    public void setInactive() {
        resetUpgrades();
        confirmButton.setEnabled(false);
        skipButton.setEnabled(false);
        setButtons();
    }
    
    public void setActive() {
        resetUpgrades();
        confirmButton.setEnabled(false);
        skipButton.setEnabled(true);
        setButtons();
    }
    
    public void activateUpgrade(HexUpgrade upgrade) {
        activeUpgrade = upgrade;
    }
    
    public void setSelect(Set<HexUpgrade> upgrades) {
        resetUpgrades();
        this.upgrades = ImmutableSortedSet.copyOf(upgrades);
        showUpgrades();
        confirmButton.setEnabled(false);
        skipButton.setEnabled(true);
        setButtons();
    }

    public void setConfirm() {
        upgradePanel.removeAll();
        showUpgrades();
        confirmButton.setEnabled(true);
        skipButton.setEnabled(true);
        setButtons();
    }
    
    private void confirmUpgrade() {
        if (activeUpgrade instanceof TileHexUpgrade) {
            orUIManager.layTile();
        } else if (activeUpgrade instanceof TokenHexUpgrade) {
            orUIManager.layToken((TokenHexUpgrade)activeUpgrade);
        }
    }

    private void skipUpgrade() {
        orUIManager.skipUpgrade(activeUpgrade);
    }
    
    public void upgradeActivated(HexUpgrade upgrade) {
        if (activeUpgrade == upgrade) {
            orUIManager.upgradeSelectedAgain(upgrade);
        } else {
            activeUpgrade = upgrade;
            orUIManager.upgradeSelected(upgrade);
        }
    }

    private void showUpgrades() {
        
        for (HexUpgrade upgrade:upgrades) {
            JLabel label = null;
            
            if (upgrade instanceof TileHexUpgrade) {
                label = createTileLabel((TileHexUpgrade)upgrade);
            } else if (upgrade instanceof TokenHexUpgrade) {
                label = createTokenLabel((TokenHexUpgrade)upgrade);
            }
            
            if (upgrade == activeUpgrade) {
                label.setBackground(SELECTED_LABEL_BG_COLOUR);
            }
            
            label.setMaximumSize(new Dimension(
                    Short.MAX_VALUE, (int)label.getPreferredSize().getHeight()));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            upgradePanel.add(label);
        }
        scrollPane.revalidate();
        scrollPane.repaint();
    }
    
    private UpgradeLabel createTileLabel(TileHexUpgrade upgrade) {   
        UpgradeLabel label;
        ImageIcon icon = createHexIcon(upgrade);
        if (upgrade.isValid()) {
            // enabled tiles
            label = UpgradeLabel.create(icon, upgrade, null, null);
            final TileHexUpgrade upgrade_final = upgrade;
            label.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    upgradeActivated(upgrade_final);
                }
            });
        } else {
            // not-enabled tiles (currently invalid)
            StringBuilder invalidText = new StringBuilder();
            for (Validation invalid : upgrade.getInvalids()) {
                invalidText.append(invalid.toString() + "<br>");
            }
            label = UpgradeLabel.create(icon, upgrade,
                    LocalText.getText("TILE_INVALID"),
                    invalidText.toString());
            label.setEnabled(false);
            // highlight where tiles of this ID have been laid if no
            // tiles left
            if (upgrade.noTileAvailable()) {
                HexHighlightMouseListener.addMouseListener(label, orUIManager, 
                        upgrade.getUpgrade().getTargetTile(), true);
            }
        }
        label.setOpaque(true);
        return label;
    }

    private ImageIcon createHexIcon(TileHexUpgrade hexUpgrade) {
    
        // target: get a buffered image of the tile
        BufferedImage hexImage = null;

        GUIHex selectedGUIHex = orUIManager.getMap().getHex(hexUpgrade.getHex());
        if (selectedGUIHex != null) {
            // if tile is already selected, choose the current rotation
            HexSide rotation;
            if (hexUpgrade == activeUpgrade) {
                rotation = selectedGUIHex.getProvisionalTileRotation();
            } else { // otherwise in the first valid rotation, returns null if no valid rotation
                rotation = hexUpgrade.getRotations().getNext(HexSide.defaultRotation());
                if (rotation == null) {
                    // fallback if no valid orientation exists:
                    // get the image in the standard orientation
                    rotation = HexSide.defaultRotation();
                }
            }
            TileUpgrade upgrade = hexUpgrade.getUpgrade();
            GUITile tempGUITile =
                    new GUITile(upgrade.getTargetTile(), selectedGUIHex);
            tempGUITile.setRotation(rotation);
            // tile has been rotated to valid orientation
            // get unscaled image for this orientation
            hexImage = tempGUITile.getTileImage(getZoomStep());
        }

        // Cheap n' Easy rescaling.
        ImageIcon hexIcon = new ImageIcon(hexImage);
        hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                Image.SCALE_SMOOTH));
        return hexIcon;
    }
   

    private ActionLabel createTokenLabel(TokenHexUpgrade upgrade) {
        Color fgColour = null;
        Color bgColour = null;
        String text = null;
        String description = null;
        LayToken action = upgrade.getAction();
        if (action instanceof LayBaseToken) {
            PublicCompany comp = ((LayBaseToken) action).getCompany();
            fgColour = comp.getFgColour();
            bgColour = comp.getBgColour();
            text = comp.getId();
            description = "<html>" + text;
            if (action.getSpecialProperty() != null) {
                description +=
                        "<font color=red> ["
                                + action.getSpecialProperty().getOriginalCompany().getId()
                                + "] </font>";
            }
            MapHex hex = upgrade.getHex();
            if (hex.getStops().size() != 1) {
                description += "<br> <font size=-2>";
                description += hex.getConnectionString(upgrade.getSelectedStop().getRelatedStation());
                description += "</font>";
            }
            description += "</html>";
        } else if (action instanceof LayBonusToken) {
            fgColour = Color.BLACK;
            bgColour = Color.WHITE;
            BonusToken token =
                    (BonusToken) action.getSpecialProperty().getToken();
            description = token.getId();
            text = "+" + token.getValue();
        }
        TokenIcon icon = new TokenIcon(25, fgColour, bgColour, text);
        final ActionLabel tokenLabel = new ActionLabel(icon);
        tokenLabel.setText(description);
        tokenLabel.setBackground(DEFAULT_LABEL_BG_COLOUR);
        tokenLabel.setOpaque(true);
        tokenLabel.setVisible(true);
        tokenLabel.setBorder(border);
        final TokenHexUpgrade upgrade_final = upgrade;
        tokenLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                upgradeActivated(upgrade_final);
            }
        });
        tokenLabel.addPossibleAction(action);
        return tokenLabel;
    }
}

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
import java.util.Map;
import java.util.NavigableSet;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

import rails.game.action.LayBaseToken;
import rails.game.action.LayBonusToken;
import rails.game.action.LayToken;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Tile;
import net.sf.rails.ui.swing.elements.ActionLabel;
import net.sf.rails.ui.swing.elements.GUIHexUpgrades;
import net.sf.rails.ui.swing.elements.UpgradeLabel;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.ui.swing.elements.RailsIconButton;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.GUITile;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;
import net.sf.rails.ui.swing.hexmap.HexUpgrade;
import net.sf.rails.ui.swing.hexmap.HexUpgrade.Invalids;
import net.sf.rails.ui.swing.hexmap.TileHexUpgrade;
import net.sf.rails.ui.swing.hexmap.TokenHexUpgrade;


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
    
    private GUIHexUpgrades hexUpgrades;

    private Map<HexUpgrade, JLabel> upgradeLabels;
    

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
        hexUpgrades.setActiveHex(null);
        upgradePanel.removeAll();
        upgradeLabels = null;
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
        hexUpgrades.setActiveHex(hex);
        showUpgrades();

        HexUpgrade activeUpgrade = hexUpgrades.getActiveUpgrade();
        if (activeUpgrade != null) {
            activeUpgrade.firstSelection();
            activeUpgrade.getHex().update();
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
        setButtons();
    }
   
    public void nextSelection() {
        HexUpgrade activeUpgrade = hexUpgrades.getActiveUpgrade();
        if (activeUpgrade.hasSingleSelection()) {
            nextUpgrade();
        } else {
            activeUpgrade.nextSelection();
            activeUpgrade.getHex().update();
        }
    }

    public void nextUpgrade() {
        if (activeUpgrade == null) {
            setActiveUpgrade(upgrades.first());
        } else {
            setActiveUpgrade(upgrades.higher(activeUpgrade));
        }
    }
    
    private void setActiveUpgrade(HexUpgrade upgrade) {
    }
    
    private void showUpgrades() {
        
        ImmutableMap.Builder<HexUpgrade, JLabel> upgradeLabelBuilder = ImmutableMap.builder(); 
        
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
            upgradeLabelBuilder.put(upgrade, label);
            
        }
        upgradeLabels = upgradeLabelBuilder.build();
        
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
                    setActiveUpgrade(upgrade_final);
                }
            });
        } else {
            // not-enabled tiles (currently invalid)
            StringBuilder invalidText = new StringBuilder();
            for (Invalids invalid : upgrade.getInvalids()) {
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

        GUIHex selectedGUIHex = hexUpgrade.getHex();
        if (selectedGUIHex != null) {
            HexSide rotation = hexUpgrade.getCurrentRotation();
            if (rotation == null) {
                // fallback if no valid orientation exists:
                // get the image in the standard orientation
                rotation = HexSide.defaultRotation();
            }
            Tile tile = hexUpgrade.getUpgrade().getTargetTile();
            // get unscaled image for this orientation
            hexImage = GUITile.getTileImage(tile, rotation, getZoomStep());
        }

        // Cheap n' Easy rescaling.
        ImageIcon hexIcon = new ImageIcon(hexImage);
        hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                (int) (hexIcon.getIconWidth() * 0.8),
                (int) (hexIcon.getIconHeight() * 0.8),
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
            MapHex hex = upgrade.getHex().getHex();
            if (upgrade.isValid() && !upgrade.hasSingleSelection()) {
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
                setActiveUpgrade(upgrade_final);
            }
        });
        tokenLabel.addPossibleAction(action);
        return tokenLabel;
    }
}

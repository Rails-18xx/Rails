package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.TileHexUpgrade.Validation;
import net.sf.rails.ui.swing.elements.ActionLabel;
import net.sf.rails.ui.swing.elements.UpgradeLabel;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.ui.swing.elements.RailsIconButton;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.GUITile;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;

import rails.game.action.*;

import com.google.common.collect.Sets;


public class UpgradesPanel extends Box {
    private static final long serialVersionUID = 1L;

    private static final int UPGRADE_TILE_ZOOM_STEP = 10;
    private static final Color DEFAULT_LABEL_BG_COLOUR = new JLabel("").getBackground();
    private static final Color SELECTED_LABEL_BG_COLOUR = new Color(255, 220, 150);
    private static final int DEFAULT_NB_PANEL_ELEMENTS = 15;

    // State class defines methods used in each state of the UpgradesPanel
    private abstract class State {
        abstract void init();
        void showUpgrades() {};
        void addUpgrade(MapUpgrade upgrade) {};
        void done() {};        
        void cancel() {};
    }
    
    public enum States {Inactive, Tile, Token}

    private final ORUIManager orUIManager;
    
    // ui elements
    private final JPanel upgradePanel;
    private final JScrollPane scrollPane;
    private final Border border = new EtchedBorder();
    
    // TODO: Replace this with an action based approach
    private final RailsIconButton doneButton;
    private final RailsIconButton cancelButton;
    
    private Dimension preferredSize;

    /**
     * If set, done/cancel buttons are not added to the pane. Instead, the
     * visibility property of these buttons are handled such that they are set
     * to visible if they normally would be added to the pane.
     * Required for Docking approach
     */
    private boolean omitButtons;

    private final InactiveState inactiveState = new InactiveState();
    private final TileState tileState = new TileState();
    private final TokenState tokenState = new TokenState();
    
    private UpgradesPanel.State state = inactiveState;

    public UpgradesPanel(ORUIManager orUIManager, boolean omitButtons) {
        super(BoxLayout.Y_AXIS);

        this.orUIManager = orUIManager;
        this.omitButtons = omitButtons;

        preferredSize =
                new Dimension(
                        (int) Math.round(110 * (2 + GUIGlobals.getFontsScale()) / 3),
                        200);
        setSize(preferredSize);
        setVisible(true);

        upgradePanel = new JPanel();

        upgradePanel.setOpaque(true);
        upgradePanel.setBackground(Color.DARK_GRAY);
        upgradePanel.setBorder(border);
        upgradePanel.setLayout(new GridLayout(DEFAULT_NB_PANEL_ELEMENTS, 1));

        scrollPane = new JScrollPane(upgradePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setSize(getPreferredSize());

        Action doneAction = new AbstractAction("Done") {
            public void actionPerformed(ActionEvent arg0) {
                state.done();
            }
        };
        
        doneAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);

        Action cancelAction = new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent e) {
                state.cancel();
            }
        };
        cancelAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);

        doneButton = new RailsIconButton(RailsIcon.LAY_TILE);
        doneButton.setAction(doneAction);
        cancelButton = new RailsIconButton(RailsIcon.NO_TILE);
        cancelButton.setAction(cancelAction);
        
        if (omitButtons) {
            doneButton.setVisible(false);
            cancelButton.setVisible(false);
        }
        
        setButtons();

        add(scrollPane);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
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
        return new RailsIconButton[] { doneButton, cancelButton };
    }

    public void setState(States name) {
        switch (name) {
        case Inactive:
            state = inactiveState;
            break;
        case Tile:
            state = tileState;
            break;
        case Token:
            state = tokenState;
            break;
        }
        state.init();
    }
    
    public void addUpgrades(Iterable<? extends MapUpgrade> upgrades) {
        for (MapUpgrade upgrade:upgrades) {
            if (upgrade.isVisible()) {
                state.addUpgrade(upgrade);
            }
        }
    }
    
    public void showUpgrades() {
        upgradePanel.removeAll();
        // reset to the number of elements
        GridLayout panelLayout = (GridLayout) upgradePanel.getLayout();
        panelLayout.setRows(DEFAULT_NB_PANEL_ELEMENTS);
        
        state.showUpgrades();

        setButtons();

        // repaint();
        revalidate();
    }
    
    public void init() {
        upgradePanel.removeAll();
        state.init();
        setButtons();
    }

    // FIXME: How to change the background of the selected token?
//    public void setSelectedToken() {
//        if (tokenLabels.isEmpty()) return;
//        int index = -1;
//        for (ActionLabel tokenLabel : tokenLabels) {
//            tokenLabel.setBackground(++index == selectedTokenndex
//                    ? SELECTED_LABEL_BG_COLOUR : DEFAULT_LABEL_BG_COLOUR);
//        }
//    }

    public void setCancelText(String text) {
        cancelButton.setRailsIcon(RailsIcon.getByConfigKey(text));
    }

    public void setDoneText(String text) {
        doneButton.setRailsIcon(RailsIcon.getByConfigKey(text));
    }

    public void setDoneEnabled(boolean enabled) {
        doneButton.setEnabled(enabled);
    }

    public void setCancelEnabled(boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    private void setButtons() {
        if (omitButtons) {
            // only set externally managed buttons to visible if at least
            // one of them is enabled
            boolean isVisible =
                    doneButton.isEnabled() || cancelButton.isEnabled();
            doneButton.setVisible(isVisible);
            cancelButton.setVisible(isVisible);
        } else {
            upgradePanel.add(doneButton);
            upgradePanel.add(cancelButton);
        }
    }

    private class InactiveState extends State {

        @Override
        void init() {
            UpgradesPanel.this.setDoneEnabled(false);
            UpgradesPanel.this.setCancelEnabled(false);
        }
        
    }
    
    private class TileState extends State {

        private final SortedSet<TileHexUpgrade> tileUpgrades = Sets.newTreeSet();

        @Override
        void init() {
            tileUpgrades.clear();
            setDoneText("LayTile");
            setCancelText("NoTile");
            setDoneEnabled(false);
            setCancelEnabled(true);
        }
        
        @Override
        void showUpgrades() {
            if (tileUpgrades.size() == 0) {
                orUIManager.setMessage(LocalText.getText("NoTiles"));
                return;
            }
            for (TileHexUpgrade upgrade : tileUpgrades) {
                UpgradeLabel label;
                if (upgrade.isValid()) {
                    // enabled tiles
                    label = createHexLabel(upgrade, null, null);
                    final TileHexUpgrade upgrade_final = upgrade;
                    label.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            orUIManager.tileSelected(upgrade_final);
                        }
                    });
                } else {
                    // not-enabled tiles (currently invalid)
                    StringBuilder invalidText = new StringBuilder();
                    for (Validation invalid : upgrade.getInvalids()) {
                        invalidText.append(invalid.toString() + "<br>");
                    }
                    label = createHexLabel(upgrade,
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
                upgradePanel.add(label);
            }
        }

        private UpgradeLabel createHexLabel(TileHexUpgrade hexUpgrade,
                String toolTipHeaderLine, String toolTipBody) {

            // target: get a buffered image of the tile
            BufferedImage hexImage = null;

            GUIHex selectedGUIHex = orUIManager.getMap().getSelectedHex();
            if (selectedGUIHex != null) {
                // if tile is already selected, choose the current rotation
                TileUpgrade upgrade = hexUpgrade.getUpgrade();
                HexSide rotation;
                if (selectedGUIHex.canFixTile()
                    && selectedGUIHex.getProvisionalTile() == upgrade.getTargetTile()) {
                    rotation = selectedGUIHex.getProvisionalTileRotation();
                } else { // otherwise in the first valid rotation, returns null if no valid rotation
                    rotation = hexUpgrade.getRotations().getNext(HexSide.defaultRotation());
                    if (rotation == null) {
                        // fallback if no valid orientation exists:
                        // get the image in the standard orientation
                        rotation = HexSide.defaultRotation();
                    }
                }
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

            return UpgradeLabel.create(hexIcon, hexUpgrade, toolTipHeaderLine,
                            toolTipBody);
        }

        @Override
        void addUpgrade(MapUpgrade upgrade) {
            tileUpgrades.add((TileHexUpgrade)upgrade);
        }

        @Override
        void done() {
            orUIManager.layTile();
        }
        
        @Override
        void cancel() {
            orUIManager.cancelTileUpgrade();
        }

    }
    
    private class TokenState extends State {

        private final SortedSet<TokenStopUpgrade> tokenUpgrades = Sets.newTreeSet();
        private TokenStopUpgrade active;

        @Override
        void init() {
            tokenUpgrades.clear();
            active = null;
            setDoneEnabled(false);
            setCancelEnabled(true);
            setDoneText("LayToken");
            setCancelText("NoToken");
        }

        @Override
        void showUpgrades() {
            if (tokenUpgrades.size() == 0) {
                return;
            }

            Color fgColour = null;
            Color bgColour = null;
            String text = null;
            String description = null;
            for (TokenStopUpgrade upgrade: tokenUpgrades) {
                LayToken action = upgrade.getAction();
                if (action instanceof LayBaseToken) {
                    PublicCompany comp = ((LayBaseToken) action).getCompany();
                    fgColour = comp.getFgColour();
                    bgColour = comp.getBgColour();
                    description = text = comp.getId();
                    if (action.getSpecialProperty() != null) {
                        description +=
                                " ("
                                        + action.getSpecialProperty().getOriginalCompany().getId()
                                        + ")";
                    }
                } else if (action instanceof LayBonusToken) {
                    fgColour = Color.BLACK;
                    bgColour = Color.WHITE;
                    BonusToken token =
                            (BonusToken) action.getSpecialProperty().getToken();
                    description = token.getId();
                    text = "+" + token.getValue();
                }
                TokenIcon icon = new TokenIcon(25, fgColour, bgColour, text);
                ActionLabel tokenLabel = new ActionLabel(icon);
                tokenLabel.setName(description);
                tokenLabel.setText(description);
                tokenLabel.setBackground(DEFAULT_LABEL_BG_COLOUR);
                tokenLabel.setOpaque(true);
                tokenLabel.setVisible(true);
                tokenLabel.setBorder(border);
                if (tokenUpgrades.size() == 1) {
                    activate(upgrade);
                } else {
                    final TokenStopUpgrade upgrade_final = upgrade;
                    tokenLabel.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            activate(upgrade_final);
                        }
                    });
                }
                tokenLabel.addPossibleAction(action);
                upgradePanel.add(tokenLabel);
            }
        }

        @Override
        void addUpgrade(MapUpgrade upgrade) {
            tokenUpgrades.add((TokenStopUpgrade)upgrade);
        }

        @Override
        void done() {
            orUIManager.layToken(active);
        }
        
        @Override
        void cancel() {
            orUIManager.cancelTokenUpgrade();
        }
        
        private void activate(TokenStopUpgrade upgrade) {
            active = upgrade;
            setDoneEnabled(true);
        }
        
    }
}

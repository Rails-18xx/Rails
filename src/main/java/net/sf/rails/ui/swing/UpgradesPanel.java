package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.HexUpgrade.Validation;
import net.sf.rails.game.action.*;
import net.sf.rails.ui.swing.elements.ActionLabel;
import net.sf.rails.ui.swing.elements.HexLabel;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.ui.swing.elements.RailsIconButton;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.GUITile;
import net.sf.rails.ui.swing.hexmap.HexHighlightMouseListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class UpgradesPanel extends Box implements MouseListener, ActionListener {
    private static final long serialVersionUID = 1L;

    private static final int UPGRADE_TILE_ZOOM_STEP = 10;
    private static final Color DEFAULT_LABEL_BG_COLOUR = new JLabel("").getBackground();
    private static final Color SELECTED_LABEL_BG_COLOUR = new Color(255, 220, 150);
    private static final int DEFAULT_NB_PANEL_ELEMENTS = 15;

    private final ORUIManager orUIManager;
    
    // token elements
    private final List<ActionLabel> tokenLabels = Lists.newArrayList();
    private final List<CorrectionTokenLabel> correctionTokenLabels = Lists.newArrayList();
    private final List<LayToken> possibleTokenLays = new ArrayList<LayToken>(3);
    private int selectedTokenndex;
    
    // tile laying
    private final SortedSet<HexUpgrade> tileUpgrades = Sets.newTreeSet();
    private final SortedSet<HexUpgrade> invalidTileUpgrades = Sets.newTreeSet();

    // ui elements
    private final JPanel upgradePanel;
    private final JScrollPane scrollPane;
    private final Border border = new EtchedBorder();
    private final RailsIconButton cancelButton = new RailsIconButton(
            RailsIcon.NO_TILE);
    private final RailsIconButton doneButton =
            new RailsIconButton(RailsIcon.LAY_TILE);

    // dynamic elements
    private Dimension preferredSize;
    private boolean tokenMode = false;
    private boolean correctionTokenMode = false;
    /**
     * If set, done/cancel buttons are not added to the pane. Instead, the
     * visibility property of these buttons are handled such that they are set
     * to visible if they normally would be added to the pane.
     */
    private boolean omitButtons;

    protected static Logger log = LoggerFactory.getLogger(UpgradesPanel.class);

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

        doneButton.setActionCommand("Done");
        doneButton.setMnemonic(KeyEvent.VK_D);
        doneButton.addActionListener(this);
        cancelButton.setActionCommand("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(this);

        if (omitButtons) {
            doneButton.setVisible(false);
            cancelButton.setVisible(false);
        }

        add(scrollPane);
    }

    public void showUpgrades(int localStep) {
        clearPanel();

        // reset to the number of elements
        GridLayout panelLayout = (GridLayout) upgradePanel.getLayout();
        panelLayout.setRows(DEFAULT_NB_PANEL_ELEMENTS);

        if (tokenMode && possibleTokenLays.size() > 0) {

            Color fgColour = null;
            Color bgColour = null;
            String text = null;
            String description = null;
            TokenIcon icon;
            ActionLabel tokenLabel;
            tokenLabels.clear();
            for (LayToken action : possibleTokenLays) {
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
                icon = new TokenIcon(25, fgColour, bgColour, text);
                tokenLabel = new ActionLabel(icon);
                tokenLabel.setName(description);
                tokenLabel.setText(description);
                tokenLabel.setBackground(DEFAULT_LABEL_BG_COLOUR);
                tokenLabel.setOpaque(true);
                tokenLabel.setVisible(true);
                tokenLabel.setBorder(border);
                tokenLabel.addMouseListener(this);
                tokenLabel.addPossibleAction(action);
                tokenLabels.add(tokenLabel);

                upgradePanel.add(tokenLabel);
            }

            setSelectedToken();

        } else {
            if (tileUpgrades.size() == 0 && localStep == ORUIManager.SELECT_TILE) {
                orUIManager.setMessage(LocalText.getText("NoTiles"));
            } else {
                for (HexUpgrade upgrade : tileUpgrades) {
                    HexLabel hexLabel = createHexLabel(upgrade, null, null);
                    hexLabel.addMouseListener(this);
                    upgradePanel.add(hexLabel);
                }
            }
            for (HexUpgrade hexUpgrade : invalidTileUpgrades) {
                TileUpgrade upgrade = hexUpgrade.getUpgrade();
                StringBuilder invalidText = new StringBuilder();
                for (Validation invalid : hexUpgrade.getInvalids()) {
                    invalidText.append(invalid.toString() + "<br>");
                }
                HexLabel hexLabel =
                        createHexLabel(hexUpgrade,
                                LocalText.getText("TILE_INVALID"),
                                invalidText.toString());
                hexLabel.setEnabled(false);
                // highlight where tiles of this ID have been laid if no
                // tiles left
                if (hexUpgrade.getInvalids().contains(
                        Validation.COLOUR_NOT_ALLOWED)) {
                    HexHighlightMouseListener.addMouseListener(hexLabel,
                            orUIManager,
                            upgrade.getTargetTile(), true);
                }
                upgradePanel.add(hexLabel);
            }
        }

        addButtons();

        // repaint();
        revalidate();
    }

    private HexLabel createHexLabel(HexUpgrade hexUpgrade,
            String toolTipHeaderLine, String toolTipBody) {
        BufferedImage hexImage = null;

        // get a buffered image of the tile
        GUIHex selectedGUIHex = orUIManager.getMap().getSelectedHex();
        TileUpgrade upgrade = hexUpgrade.getUpgrade();
        if (selectedGUIHex != null) {
            // if tile is already selected, choose the current rotation
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

        ImageIcon hexIcon = new ImageIcon(hexImage);

        // Cheap n' Easy rescaling.
        hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                Image.SCALE_SMOOTH));

        Tile tile = upgrade.getTargetTile();
        HexLabel hexLabel =
                new HexLabel(hexIcon, hexUpgrade, toolTipHeaderLine,
                        toolTipBody);
        hexLabel.setName(tile.toText());
        hexLabel.setOpaque(true);
        hexLabel.setVisible(true);
        hexLabel.setBorder(border);

        return hexLabel;
    }

    // populate version for corrections
    public void showCorrectionTileUpgrades() {
        // deactivate correctionTokenMode and tokenmode
        correctionTokenMode = false;
        tokenMode = false;

        // activate upgrade panel
        clearPanel();
        GridLayout panelLayout = (GridLayout) upgradePanel.getLayout();

        if (tileUpgrades.size() == 0 && invalidTileUpgrades.size() == 0) {
            // reset to the number of elements
            panelLayout.setRows(DEFAULT_NB_PANEL_ELEMENTS);
            // set to position 0
            scrollPane.getVerticalScrollBar().setValue(0);
        } else {
            // set to the max of available or the default number of elements
            panelLayout.setRows(Math.max(tileUpgrades.size() +
                    invalidTileUpgrades.size() + 2, DEFAULT_NB_PANEL_ELEMENTS));
            for (HexUpgrade u : tileUpgrades) {
                TileUpgrade upgrade = u.getUpgrade();
                Tile tile = upgrade.getTargetTile();
                BufferedImage hexImage = getHexImage(tile.getPictureId());
                ImageIcon hexIcon = new ImageIcon(hexImage);

                // Cheap n' Easy rescaling.
                hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                        (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                        (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                        Image.SCALE_SMOOTH));

                HexLabel hexLabel = new HexLabel(hexIcon, u);
                hexLabel.setName(tile.toText());
                hexLabel.setOpaque(true);
                hexLabel.setVisible(true);
                hexLabel.setBorder(border);
                hexLabel.addMouseListener(this);

                upgradePanel.add(hexLabel);
            }
        }

        addButtons();

        // repaint();
        revalidate();
    }

//    // populate version for corrections
//    public void showCorrectionTokenUpgrades(MapCorrectionAction action) {
//        // activate correctionTokenMode and deactivate standard tokenMode
//        correctionTokenMode = true;
//        tokenMode = false;
//
//        // activate upgrade panel
//        clearPanel();
//        GridLayout panelLayout = (GridLayout) upgradePanel.getLayout();
//        List<? extends Token<?>> tokens = orUIManager.getTokenLays();
//
//        if (tokens == null || tokens.size() == 0) {
//            // reset to the number of elements
//            panelLayout.setRows(DEFAULT_NB_PANEL_ELEMENTS);
//            // set to position 0
//            scrollPane.getVerticalScrollBar().setValue(0);
//        } else {
//            Color fgColour = null;
//            Color bgColour = null;
//            String text = null;
//            String description = null;
//            Tokencon icon;
//            CorrectionTokenLabel tokenLabel;
//            correctionTokenLabels.clear();
//            for (Token<?> token : tokens) {
//                if (token instanceof BaseToken) {
//                    PublicCompany comp = ((BaseToken) token).getParent();
//                    fgColour = comp.getFgColour();
//                    bgColour = comp.getBgColour();
//                    description = text = comp.getId();
//                }
//                icon = new Tokencon(25, fgColour, bgColour, text);
//                tokenLabel = new CorrectionTokenLabel(icon, token);
//                tokenLabel.setName(description);
//                tokenLabel.setText(description);
//                tokenLabel.setBackground(DEFAULT_LABEL_BG_COLOUR);
//                tokenLabel.setOpaque(true);
//                tokenLabel.setVisible(true);
//                tokenLabel.setBorder(border);
//                tokenLabel.addMouseListener(this);
//                tokenLabel.addPossibleAction(action);
//                correctionTokenLabels.add(tokenLabel);
//                upgradePanel.add(tokenLabel);
//            }
//
//        }
//
//        addButtons();
//
//        // repaint();
//        revalidate();
//
//    }

    public void clear() {
        clearPanel();
        addButtons();
        upgradePanel.repaint();
    }

    public void setSelectedTokenndex(int index) {
        log.debug("Selected token index from " + selectedTokenndex + " to "
                  + index);
        selectedTokenndex = index;
    }

    public void setSelectedToken() {
        if (tokenLabels.isEmpty()) return;
        int index = -1;
        for (ActionLabel tokenLabel : tokenLabels) {
            tokenLabel.setBackground(++index == selectedTokenndex
                    ? SELECTED_LABEL_BG_COLOUR : DEFAULT_LABEL_BG_COLOUR);
        }
    }

    // NOTE: NOT USED
    // TODO: Check is this still required
    // private void setSelectedCorrectionToken() {
    // if (correctionTokenLabels == null || correctionTokenLabels.isEmpty())
    // return;
    // int index = -1;
    // for (CorrectionTokenLabel tokenLabel : correctionTokenLabels) {
    // tokenLabel.setBackground(++index == selectedTokenndex
    // ? selectedLabelBgColour : defaultLabelBgColour);
    // }
    // }

    private BufferedImage getHexImage(String tileId) {
        return GameUIManager.getImageLoader().getTile(tileId, getZoomStep());
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

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }

    public void clearTileUpgrades() {
        tileUpgrades.clear();
        invalidTileUpgrades.clear();
    }

    public void addUpgrade(HexUpgrade upgrade, boolean valid) {
        if (valid) {
            tileUpgrades.add(upgrade);
        } else {
            invalidTileUpgrades.add(upgrade);
        }
    }

    public void removeUpgrade(HexUpgrade upgrade) {
        tileUpgrades.remove(upgrade);
    }

    public void setTileMode(boolean tileMode) {
        clearTileUpgrades();
    }

    public void setTokenMode(boolean tokenMode) {
        this.tokenMode = tokenMode;
        clearTileUpgrades();
        possibleTokenLays.clear();
        selectedTokenndex = -1;
    }

    public <T extends LayToken> void setPossibleTokenLays(List<T> actions) {
        possibleTokenLays.clear();
        selectedTokenndex = -1;
        if (actions != null) possibleTokenLays.addAll(actions);
    }

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

    public void actionPerformed(ActionEvent e) {

        Object source = e.getSource();

        if (source == cancelButton) {
            orUIManager.cancelUpgrade();
        } else if (source == doneButton) {
            orUIManager.executeUpgrade();
        }
    }

    public void mouseClicked(MouseEvent e) {

        Object source = e.getSource();
        if (!(source instanceof JLabel)) return;

        if (tokenMode) {
            if (tokenLabels.contains(source)) {
                orUIManager.tokenSelected((LayToken) ((ActionLabel) source).getPossibleActions().get(
                        0));
                setDoneEnabled(true);
            } else {
                orUIManager.tokenSelected(null);
            }
            setSelectedToken();
        } else if (correctionTokenMode) {
            int id = correctionTokenLabels.indexOf(source);
            selectedTokenndex = id;
            log.info("Correction Token index = " + selectedTokenndex
                     + " selected");
        } else {

            HexUpgrade upgrade = ((HexLabel) e.getSource()).getUpgrade();
            orUIManager.tileSelected(upgrade);
        }

    }

    public void mouseEntered(MouseEvent e) {
    
    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void finish() {
        setDoneEnabled(false);
        setCancelEnabled(false);
    }

    private void clearPanel() {
        upgradePanel.removeAll();
        if (omitButtons) {
            doneButton.setVisible(false);
            cancelButton.setVisible(false);
        }
    }

    private void addButtons() {
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

    public RailsIconButton[] getButtons() {
        return new RailsIconButton[] { doneButton, cancelButton };
    }

    /** ActionLabel extension that allows to attach the token */
    private class CorrectionTokenLabel extends ActionLabel {

        private static final long serialVersionUID = 1L;

        // TODO: Was never used
        // private Token token;

        CorrectionTokenLabel(Icon tokenIcon, Token<?> token) {
            super(tokenIcon);
            // this.token = token;
        }

    }
}

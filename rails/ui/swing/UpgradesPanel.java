/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/UpgradesPanel.java,v 1.6 2007/10/27 15:26:34 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import rails.game.*;
import rails.game.action.LayTile;
import rails.ui.swing.hexmap.*;
import rails.util.LocalText;

public class UpgradesPanel extends Box implements MouseListener, ActionListener {
    private ORWindow orWindow;

    private List<TileI> upgrades;

    private JPanel upgradePanel;

    private JScrollPane scrollPane;

    private Dimension preferredSize = new Dimension(100, 200);

    private Border border = new EtchedBorder();

    private final String INIT_CANCEL_TEXT = "NoTile";

    private final String INIT_DONE_TEXT = "LayTile";

    private boolean tokenMode = false;

    private JButton cancelButton = new JButton(
            LocalText.getText(INIT_CANCEL_TEXT));

    private JButton doneButton = new JButton(LocalText.getText(INIT_DONE_TEXT));

    private HexMap hexMap;

    public UpgradesPanel(ORWindow orWindow) {
        super(BoxLayout.Y_AXIS);

        this.orWindow = orWindow;

        setSize(preferredSize);
        setVisible(true);

        upgrades = null;
        upgradePanel = new JPanel();

        upgradePanel.setOpaque(true);
        upgradePanel.setBackground(Color.DARK_GRAY);
        upgradePanel.setBorder(border);
        upgradePanel.setLayout(new GridLayout(15, 1));

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

        add(scrollPane);
        showUpgrades();
    }

    public void repaint() {
        showUpgrades();
    }

    public void populate() {
        if (hexMap == null)
            hexMap = orWindow.getMapPanel().getMap();

        GUIHex uiHex = hexMap.getSelectedHex();
        MapHex hex = uiHex.getHexModel();
        upgrades = new ArrayList<TileI>();
        List<TileI> tiles;
        
        for (LayTile layTile: hexMap.getTileAllowancesForHex(hex)) {
        	tiles = layTile.getTiles();
        	if (tiles == null) {
        		for (TileI tile : uiHex.getCurrentTile().getValidUpgrades(
                    hex,
                    GameManager.getCurrentPhase())) {
        			if (!upgrades.contains(tile)) upgrades.add (tile);
        		}
        		//upgrades.addAll(uiHex.getCurrentTile().getValidUpgrades(
                //    hex,
                //    GameManager.getCurrentPhase()));
        	} else {
        		for (TileI tile : tiles) {
        			if (!upgrades.contains(tile)) upgrades.add (tile);
        		}
        		//upgrades.addAll(tiles);
        	}
        }
    }

    private void showUpgrades() {
        upgradePanel.removeAll();

        if (tokenMode) {
        } else if (upgrades == null) {
        } else if (upgrades.size() == 0) {
            orWindow.setMessage(LocalText.getText("NoTiles"));
        } else {
            Iterator it = upgrades.iterator();

            while (it.hasNext()) {
                TileI tile = (TileI) it.next();
                BufferedImage hexImage = getHexImage(tile.getId());
                ImageIcon hexIcon = new ImageIcon(hexImage);

                // Cheap n' Easy rescaling.
                hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                        (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE),
                        (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE),
                        Image.SCALE_SMOOTH));

                JLabel hexLabel = new JLabel(hexIcon);
                hexLabel.setName(tile.getName());
                hexLabel.setText("" + tile.getId());
                hexLabel.setOpaque(true);
                hexLabel.setVisible(true);
                hexLabel.setBorder(border);
                hexLabel.addMouseListener(this);

                upgradePanel.add(hexLabel);
            }
        }

        upgradePanel.add(doneButton);
        upgradePanel.add(cancelButton);
    }

    private BufferedImage getHexImage(int tileId) {
        return GameUIManager.getImageLoader().getTile(tileId);
    }

    public Dimension getPreferredSize() {
        return preferredSize;
    }

    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }

    public List<TileI> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(List<TileI> upgrades) {
        this.upgrades = upgrades;
    }
    
    public void addUpgrades (List<TileI> upgrades) {
        this.upgrades.addAll(upgrades);
    }

    public void setTileMode(boolean tileMode) {
        setUpgrades(null);
    }

    public void setBaseTokenMode(boolean tokenMode) {
        this.tokenMode = tokenMode;
        setUpgrades(null);
    }

    public void setCancelText(String text) {
        cancelButton.setText(LocalText.getText(text));
    }

    public void setDoneText(String text) {
        doneButton.setText(LocalText.getText(text));
    }

    public void setDoneEnabled(boolean enabled) {
        doneButton.setEnabled(enabled);
    }

    public void setCancelEnabled(boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    public void actionPerformed(ActionEvent e) {

        if (hexMap == null)
            hexMap = orWindow.getMapPanel().getMap();
        Object source = e.getSource();

        if (source == cancelButton) {
            orWindow.processCancel();
        } else if (source == doneButton) {
            if (hexMap.getSelectedHex() != null) {
                orWindow.processDone();
            } else {
                orWindow.processCancel();
            }

        }
        upgrades = null; // ???
        showUpgrades();
    }

    public void mouseClicked(MouseEvent e) {
        if (!(e.getSource() instanceof JLabel))
            return;

        HexMap map = orWindow.getMapPanel().getMap();

        int id = Integer.parseInt(((JLabel) e.getSource()).getText());
        if (map.getSelectedHex().dropTile(id)) {
            /* Lay tile */
            map.repaint(map.getSelectedHex().getBounds());
            orWindow.setSubStep(ORWindow.ROTATE_OR_CONFIRM_TILE);
        } else {
            /* Tile cannot be laid in a valid orientation: refuse it */
            JOptionPane.showMessageDialog(this,
                    "This tile cannot be laid in a valid orientation.");
            upgrades.remove(TileManager.get().getTile(id));
            orWindow.setSubStep(ORWindow.SELECT_TILE);
            showUpgrades();
        }

    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void finish() {
        setDoneEnabled(false);
        setCancelEnabled(false);
    }
}

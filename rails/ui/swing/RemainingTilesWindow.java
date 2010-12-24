/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/RemainingTilesWindow.java,v 1.8 2009/12/15 18:56:11 evos Exp $*/
package rails.ui.swing;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.TileI;
import rails.game.TileManager;
import rails.game.model.ModelObject;
import rails.ui.swing.elements.Field;
import rails.ui.swing.hexmap.GUIHex;
import rails.util.LocalText;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class RemainingTilesWindow extends JFrame implements WindowListener,
        ActionListener {
    private static final long serialVersionUID = 1L;
    private GameUIManager gameUIManager;
    private ORUIManager orUIManager;

    private List<Field> labels = new ArrayList<Field>();
    private List<TileI> shownTiles = new ArrayList<TileI>();

    private final static int COLUMNS = 10;

    protected static Logger log =
            Logger.getLogger(RemainingTilesWindow.class.getPackage().getName());

    public RemainingTilesWindow(ORWindow orWindow) {
        super();

        getContentPane().setLayout(new GridLayout(0, COLUMNS, 5, 5));

        setTitle("Rails: Remaining Tiles");
        setVisible(false);
        setSize(800, 600);
        addWindowListener(this);

        init(orWindow.getGameUIManager());

        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.setLocationRelativeTo(orWindow);
        pack();
        setVisible(true);
    }

    private void init(GameUIManager gameUIManager) {

        TileManager tmgr = gameUIManager.getGameManager().getTileManager();
        TileI tile;
        Field label;
        BufferedImage hexImage;
        ImageIcon hexIcon;
        int picId;

        // Build the grid with tiles in the sequence as
        // these have been defined in Tiles.xml
        List<Integer> tileIds = tmgr.getTileIds();
        log.debug("There are " + tileIds.size() + " tiles known in this game");

        for (int tileId : tileIds) {
            if (tileId <= 0) continue;

            tile = tmgr.getTile(tileId);
            picId = tile.getPictureId();

            hexImage = GameUIManager.getImageLoader().getTile(picId, 10);
            hexIcon = new ImageIcon(hexImage);
            hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                    (int) (hexIcon.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                    (int) (hexIcon.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                    Image.SCALE_SMOOTH));

            label = new Field((ModelObject) tile, hexIcon, Field.CENTER);
            label.setVerticalTextPosition(Field.BOTTOM);
            label.setHorizontalTextPosition(Field.CENTER);
            label.setVisible(true);

            getContentPane().add(label);
            shownTiles.add(tile);
            labels.add(label);

        }

    }

    public void actionPerformed(ActionEvent actor) {

    }

    public ORUIManager getORUIManager() {
        return orUIManager;
    }

    public GameUIManager getGameUIManager() {
        return gameUIManager;
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowClosing(WindowEvent e) {
        StatusWindow.uncheckMenuItemBox(LocalText.getText("MAP"));
        dispose();
    }

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    public void activate() {
        setVisible(true);
        requestFocus();
    }

    /**
     * Round-end settings
     *
     */
    public void finish() {}
}

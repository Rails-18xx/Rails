/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/RemainingTilesWindow.java,v 1.8 2009/12/15 18:56:11 evos Exp $*/
package rails.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.game.Tile;
import rails.game.TileManager;
import rails.ui.swing.elements.Field;
import rails.ui.swing.hexmap.GUIHex;

/**
 * This Window displays the availability of tiles.
 */
public class RemainingTilesWindow extends JFrame implements WindowListener,
        ActionListener {
    private static final long serialVersionUID = 1L;
    private GameUIManager gameUIManager;
    private ORUIManager orUIManager;
    private AlignedWidthPanel tilePanel;
    private JScrollPane slider;

    private List<Field> labels = new ArrayList<Field>();
    private List<Tile> shownTiles = new ArrayList<Tile>();

    protected static Logger log =
            LoggerFactory.getLogger(RemainingTilesWindow.class);

    public RemainingTilesWindow(ORWindow orWindow) {
        super();

        tilePanel = new AlignedWidthPanel();
        slider = new JScrollPane(tilePanel);
        slider.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        slider.setPreferredSize(new Dimension(200,200));
        tilePanel.setParentSlider(slider);

        //use flow layout as it provides for necessary line breaks
        tilePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        init(orWindow.getGameUIManager());

        //setup the JFrame and assign the contents (slider containing tilePane)
        //only for conventional layout as this is a dockable pane for the docking layout
        if (!orWindow.isDockingFrameworkEnabled()) {
            setTitle("Rails: Remaining Tiles");
            setVisible(false);
            setContentPane(slider);
            setSize(800, 600);
            addWindowListener(this);

            this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            this.setLocationRelativeTo(orWindow);

            setVisible(true);
        }
    }

    private void init(GameUIManager gameUIManager) {

        TileManager tmgr = gameUIManager.getRoot().getTileManager();
        Tile tile;
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

            label = new Field(tile, hexIcon, Field.CENTER);
            label.setVerticalTextPosition(Field.BOTTOM);
            label.setHorizontalTextPosition(Field.CENTER);
            label.setVisible(true);

            tilePanel.add(label);
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

    /**
     * @return The scroll pane which holds as child the tile panel
     */
    public JScrollPane getScrollPane() {
        return slider;
    }
    
    /**
     * custom content pane that will align its width with the parent scroll pane
     * needed to ensure only vertical scroll bar is used
     */
    private static class AlignedWidthPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private JScrollPane parentSlider = null;
        @Override
        public Dimension getPreferredSize() {
            //width based on parent slider
            int width = parentSlider.getSize().width
                    - parentSlider.getVerticalScrollBar().getWidth()
                    - 5;
            if (width <= 0) width = 1;
            
            //height based on contained components
            //(no need to take into account width discrepancies since
            // method is invoked several times)
            int height = 1; //minimum height
            for (Component c : this.getComponents()) {
                height = Math.max(height, c.getY() + c.getHeight()); 
            }
            return new Dimension (width , height);
        }
        public void setParentSlider(JScrollPane parentSlider) {
            this.parentSlider = parentSlider;
        }
    }

}

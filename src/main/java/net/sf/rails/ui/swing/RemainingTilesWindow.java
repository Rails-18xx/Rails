package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.Tile;
import net.sf.rails.game.TileManager;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.ui.swing.elements.Field;


/**
 * This Window displays the availability of tiles.
 */

// FIXME: This is a temporary workaround as it does not update the number of tiles
// replace this with a field again
public class RemainingTilesWindow extends JFrame implements WindowListener,
        ActionListener {
    private static final long serialVersionUID = 1L;

    private ORWindow orWindow;

    private final AlignedWidthPanel tilePanel;
    private final JScrollPane slider;

    private final Map<Tile, Field> tileLabels = new HashMap<>();
    private final Map<Tile, Observer> observerMap = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(RemainingTilesWindow.class);

    public RemainingTilesWindow(ORWindow orWindow) {
        super();

        this.orWindow = orWindow;
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

        // Build the grid with tiles in the sequence as
        // these have been defined in Tiles.xml
        Set<Tile> tiles = tmgr.getTiles();
        log.debug("There are {} tiles known in this game", tiles.size());

        for (Tile tile:tiles) {
            if (tile.isFixed()) continue;
            String picId = tile.getPictureId();

            BufferedImage hexImage = ImageLoader.getInstance().getTile(picId, 10);
            ImageIcon hexIcon = new ImageIcon(hexImage);
            hexIcon.setImage(hexIcon.getImage().getScaledInstance(
                    (int) (hexIcon.getIconWidth() * 0.8),
                    (int) (hexIcon.getIconHeight() * 0.8),
                    Image.SCALE_SMOOTH));

//            HexLabel hexLabel = new HexLabel(hexIcon, tile);
//            hexLabel.setVerticalTextPosition(Field.BOTTOM);
//            hexLabel.setHorizontalTextPosition(Field.CENTER);
//            hexLabel.setVisible(true);
//            tilePanel.add(hexLabel);
            Field label = new Field(tile.getCountModel(), hexIcon, Field.CENTER);
            label.setVerticalTextPosition(Field.BOTTOM);
            label.setHorizontalTextPosition(Field.CENTER);
            label.setVisible(true);
            tilePanel.add(label);

            tileLabels.put(tile, label);

            Observer watcher = new Observer() {
                @Override
                public void update(String text) {
                    // TODO could parse out the text, ie [MapHex{uri=/Map/I17}Coordinates{9, 17}]
                    refreshCounts();
                }

                @Override
                public Observable getObservable() {
                    return null;
                }
            };
            tile.getTilesLaid().addObserver(watcher);
            observerMap.put(tile, watcher);
        }
    }

    private void refreshCounts() {
        // refresh our counts
        log.debug("refreshing tile counts");
        for ( Map.Entry<Tile, Field> entry : tileLabels.entrySet() ) {
            entry.getValue().setText(entry.getKey().getCountModel().toText());
        }
    }

    @Override
    public void actionPerformed(ActionEvent actor) {

    }

    @Override
    public void windowActivated(WindowEvent e) {
        refreshCounts();
    }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        orWindow.getGameUIManager().uncheckMenuItemBox(LocalText.getText("MAP"));

        for ( Map.Entry<Tile, Observer> entry : observerMap.entrySet() ) {
            entry.getKey().getTilesLaid().removeObserver(entry.getValue());
        }
        dispose();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
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

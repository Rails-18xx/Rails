/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/RemainingTilesWindotringw.java,v 1.8 2009/12/15 18:56:11 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import rails.common.Config;
import rails.common.LocalText;
import rails.game.TileI;
import rails.game.TileManager;
import rails.game.model.ModelObject;
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
    private List<TileI> shownTiles = new ArrayList<TileI>();

    //The following mapping allows for looking up tiles / their graphical representation
    //based on a tile name
    private Map<Integer,Field> tileID_to_field;
    private Map<Integer,ImageIcon> tileID_to_imageIcon;
    private Map<Integer,ImageIcon> tileID_to_imageIconHighlighted;
    private static final float HIGHLIGHT_RGB_SCALE_FACTOR = 1;
    private static final float HIGHLIGHT_RGB_OFFSET = 50;
    private static final Border TILE_BORDER = new EmptyBorder(6,3,3,3);
    private static final Border TILE_BORDER_HIGHLIGHTED = new CompoundBorder(
            new LineBorder(new Color(255,80,80),3),new EmptyBorder(3,0,0,0));
    private static final Color TILE_BACKGROUND = null;
    private static final Color TILE_BACKGROUND_HIGHLIGHTED = new Color(255,255,255);
    private DeferredTileHighlighter deferredTileHighlighter;
    private static final long HIGHLIGHTING_DEFERRED_BY_MINISECS = 200;
    
    protected static Logger log =
        Logger.getLogger(RemainingTilesWindow.class.getPackage().getName());

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

    /**
     * Synchronized so that no highlighting occurs on a non ready tile list
     * @param gameUIManager
     */
    synchronized private void init(GameUIManager gameUIManager) {

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

        tileID_to_field = new HashMap<Integer,Field>();
        tileID_to_imageIcon = new HashMap<Integer,ImageIcon>();
        tileID_to_imageIconHighlighted = new HashMap<Integer,ImageIcon>();
        deferredTileHighlighter = new DeferredTileHighlighter();
        deferredTileHighlighter.start();

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
            
            //get highlighted version of the tile icon image
            BufferedImage hexImageHighlighted = new BufferedImage(hexImage.getColorModel(), hexImage.copyData(null), hexImage.getColorModel().isAlphaPremultiplied(), null);
            new RescaleOp(HIGHLIGHT_RGB_SCALE_FACTOR, HIGHLIGHT_RGB_OFFSET,null).filter(
                    hexImage,hexImageHighlighted);
            ImageIcon hexIconHighlighted = new ImageIcon(hexImageHighlighted);
            hexIconHighlighted.setImage(hexIconHighlighted.getImage().getScaledInstance(
                    (int) (hexIconHighlighted.getIconWidth() * GUIHex.NORMAL_SCALE * 0.8),
                    (int) (hexIconHighlighted.getIconHeight() * GUIHex.NORMAL_SCALE * 0.8),
                    Image.SCALE_SMOOTH));
            
            label = new Field((ModelObject) tile, hexIcon, Field.CENTER);
            label.setVerticalTextPosition(Field.BOTTOM);
            label.setHorizontalTextPosition(Field.CENTER);
            label.setVisible(true);
            if ("yes".equals(Config.get("map.highlightHexes"))) {
                label.setBackground(TILE_BACKGROUND);
                label.setBorder(TILE_BORDER);
            }

            tilePanel.add(label);
            shownTiles.add(tile);
            labels.add(label);

            //remember tile data for later highlighting
            tileID_to_field.put(tileId, label);
            tileID_to_imageIcon.put(tileId, hexIcon);
            tileID_to_imageIconHighlighted.put(tileId, hexIconHighlighted);
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

    /**
     * Synchronized so that highlighting requests are processed one-by-one
     * @param upgrades Tiles to be highlighted (any tile not listed is not highlighted, even if it was before)
     */
    synchronized public void setHighlightedTiles(List<TileI> upgrades) {
        //ignore if not yet initialized
        if (tileID_to_field == null) return;
        
        List<Integer> requestedTileIDs = new ArrayList<Integer>();
        if (upgrades != null) {
            for (TileI upgrade : upgrades) {
                requestedTileIDs.add(upgrade.getId());
            }
        }

        // schedule the deferred highlighting
        deferredTileHighlighter.setHighlightedTiles(requestedTileIDs);
    }

    /**
     * Performs the highlighting only if no other set of tiles has been chosen
     * to be highlighted in the meantime.
     * 
     * Such deferred highlighting is necessary since otherwise rapid mouse
     * movements over the map lead to slower response times for the map's hex
     * highlighting.
     * 
     * The methods are all synchronized to ensure that new highlight requests
     * are only added if no highlighting is currently performed.
     * 
     * @author Frederick Weld
     *
     */
    private class DeferredTileHighlighter extends Thread {
        private List<Integer> priorHighlightedTileIDs;
        private List<Integer> currentHighlightedTileIDs;
        public DeferredTileHighlighter() {
            priorHighlightedTileIDs = new ArrayList<Integer>();
            currentHighlightedTileIDs = new ArrayList<Integer>();
        }
        @Override
        synchronized public void run() {
            while (true) {
                //wait for next highlighting request
                try {
                    wait();
                } catch (InterruptedException e) {}
                
                //defer highlighting until request has been stable for the specified deferral time
                List<Integer> currentRequest = null;
                while (!currentHighlightedTileIDs.equals(currentRequest)) {
                    currentRequest = currentHighlightedTileIDs;
                    try {
                        wait(HIGHLIGHTING_DEFERRED_BY_MINISECS);
                    } catch (InterruptedException e) {
                        //apparently not thrown even if notified, hence loop check based on request content
                    }
                }
                executeHighlighting();
            }
        }

        synchronized public void setHighlightedTiles(List<Integer> requestedTileIDs) {
            // ensure that actually something has to be done
            if (requestedTileIDs.equals(currentHighlightedTileIDs)) return;

            currentHighlightedTileIDs = requestedTileIDs;

            // schedule the deferred highlighting (meaning wait in run-method is interrupted)
            this.notify();
        }

        synchronized public void executeHighlighting() {
            for (int tileID : priorHighlightedTileIDs) {
                if (!currentHighlightedTileIDs.contains(tileID)) {
                    //remove highlighting
                    Field f = tileID_to_field.get(tileID);
                    f.setIcon(tileID_to_imageIcon.get(tileID));
                    f.setBackground(TILE_BACKGROUND);
                    f.setBorder(TILE_BORDER);
                }
            }
            for (int tileID : currentHighlightedTileIDs) {
                if (!priorHighlightedTileIDs.contains(tileID)) {
                    //add highlighting
                    Field f = tileID_to_field.get(tileID);
                    f.setIcon(tileID_to_imageIconHighlighted.get(tileID));
                    f.setBackground(TILE_BACKGROUND_HIGHLIGHTED);
                    f.setBorder(TILE_BORDER_HIGHLIGHTED);
                }
            }

            priorHighlightedTileIDs = currentHighlightedTileIDs;
        }
        
    }

}

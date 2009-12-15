/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/MapPanel.java,v 1.12 2009/12/15 18:56:11 evos Exp $*/
package rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.MapManager;
import rails.game.action.LayTile;
import rails.game.action.LayToken;
import rails.ui.swing.hexmap.HexMap;

/**
 * MapWindow class displays the Map Window. It's shocking, I know.
 */
public class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private MapManager mmgr;
    private HexMap map;
    private JScrollPane scrollPane;
    private GameUIManager gameUIManager;

    protected static Logger log =
            Logger.getLogger(MapPanel.class.getPackage().getName());

    public MapPanel(GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
        Scale.set(15);
        setLayout(new BorderLayout());

        mmgr = gameUIManager.getGameManager().getMapManager();
        try {
            map =
                    (HexMap) Class.forName(mmgr.getMapUIClassName()).newInstance();
            map.init(gameUIManager.getORUIManager(), mmgr);
        } catch (Exception e) {
            log.fatal("Map class instantiation error:", e);
            e.printStackTrace();
            return;
        }

        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        scrollPane = new JScrollPane(map);

        add(scrollPane, BorderLayout.CENTER);

    	scrollPane.setSize(map.getPreferredSize());

    	setSize(map.getPreferredSize().width, map.getPreferredSize().height);
        setLocation(25, 25);
    }

    public void setAllowedTileLays(List<LayTile> allowedTileLays) {
        map.setAllowedTileLays(allowedTileLays);
    }

    public <T extends LayToken> void setAllowedTokenLays(
            List<T> allowedTokenLays) {
        map.setAllowedTokenLays(allowedTokenLays);
    }

    public void zoomIn() {
    	map.zoomIn();
    }

    public void zoomOut() {
    	map.zoomOut();
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(gameUIManager.getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public HexMap getMap() {
        return map;
    }
}

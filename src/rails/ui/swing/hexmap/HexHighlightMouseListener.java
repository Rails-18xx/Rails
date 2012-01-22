package rails.ui.swing.hexmap;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import rails.common.parser.Config;
import rails.game.MapHex;
import rails.game.PrivateCompany;
import rails.game.PublicCompany;
import rails.game.StartItem;
import rails.game.model.PortfolioModel;
import rails.game.special.LocatedBonus;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialProperty;
import rails.game.special.SpecialRight;
import rails.game.special.SpecialTileLay;
import rails.game.special.SpecialTokenLay;
import rails.ui.swing.ORUIManager;

/**
 * Takes care of highlighting certain hexes in case of mouseover events.
 * @author Frederick Weld
 */
// TODO: The approach to direct calls should be replaced with an appropriate model
public class HexHighlightMouseListener implements MouseListener {

    List<MapHex> hexList;
    List<GUIHex> guiHexList;
    HexMap hexMap;
    ORUIManager orUIManager;
    PortfolioModel portfolio;
    int tileId;
    
    /**
     * lazy creation of the gui hex list for these reasons:
     * - save effort in case the mouse over never happens
     * - hexmap is now more likely to be available than during construction
     * - portfolio is to be evaluated only at mouse over time
     */
    private void initGuiHexList() {
        //only create list if gui hexes are not yet available
        if (!guiHexList.isEmpty()) return;
        
        //initially get hex map if not yet available
        if (hexMap == null) hexMap = orUIManager.getMap();
        
        //create gui hex list based on hex list
        if (hexMap != null) {
            for (MapHex h : hexList) {
                guiHexList.add(hexMap.getHexByName(h.getName()));
            }
        }
    }
    
    /**
     * inefficient but probably ok due to very small size of lists
     */
    private void addToHexListDistinct(List<MapHex> hl) {
        if (hl == null) return;
        for (MapHex h : hl) {
            addToHexListDistinct(h);
        }
    }
    private void addToHexListDistinct(MapHex h) {
        if (h == null) return;
        if (!hexList.contains(h)) hexList.add(h);
    }
    
    private void initPrivateCompanies(Set<PrivateCompany> privList) {
        for (PrivateCompany p : privList) {
            addToHexListDistinct(p.getBlockedHexes());
            for (SpecialProperty sp : p.getSpecialProperties()) {
                if (sp instanceof SpecialTileLay) {
                    addToHexListDistinct(((SpecialTileLay)sp).getLocations());
                }
                if (sp instanceof SpecialTokenLay) {
                    addToHexListDistinct(((SpecialTokenLay)sp).getLocations());
                }
                if (sp instanceof SpecialRight) {
                    addToHexListDistinct(((SpecialRight)sp).getLocations());
                }
                if (sp instanceof LocatedBonus) {
                    addToHexListDistinct(((LocatedBonus)sp).getLocations());
                }
                if (sp instanceof SellBonusToken) {
                    addToHexListDistinct(((SellBonusToken)sp).getLocations());
                }
            }
        }
    }
    
    private void clearHexList () {
        //remove hexes from the list 
        //(as list will be built from the scratch during next mouse entered event)
        hexList.clear();
        guiHexList.clear();
    }

    private void initPortfolioHexList () {
        //build the hex list for the contained private companies
        initPrivateCompanies(portfolio.getPrivateCompanies());
    }
    
    private void initTileIdHexList () {
        //initially get hex map if not yet available
        if (hexMap == null) hexMap = orUIManager.getMap();

        //build the list of hexes the current tiles of which have the given tile ID
        if (hexMap != null) {
            guiHexList = hexMap.getHexesByCurrentTileId(tileId);
        }
    }
    
    /**
     * @param orUIManager The OR UI manager containing the map where the highlighting 
     * should occur
     */
    private HexHighlightMouseListener(ORUIManager orUIManager){
        this.orUIManager = orUIManager;
        hexList = new ArrayList<MapHex>();
        guiHexList = new ArrayList<GUIHex>();
        portfolio = null;
    }
    
    /**
     * @param pf Portfolio which is dynamically evaluated at mouse-even-time for
     * any contained private companies
     */
    public static void addMouseListener(JComponent c,ORUIManager orUIManager,PortfolioModel pf) {
        if (isEnabled(false)) {
            HexHighlightMouseListener l = new HexHighlightMouseListener(orUIManager);
            l.portfolio = pf;
            c.addMouseListener(l);
        }
    }
    
    /**
     * @param tileId ID of the tile the occurrences of which should be highlighted on
     * the map
     * @param enableIrrespectiveOfHighlightConfig If true, the mouse listener is
     * enabled irrespective of the base configuration. Needed since some highlighting
     * should not be disabled by configuration. 
     */
    public static void addMouseListener(JComponent c,ORUIManager orUIManager,int tileId,boolean enableIrrespectiveOfHighlightConfig) {
        if (isEnabled(enableIrrespectiveOfHighlightConfig)) {
            HexHighlightMouseListener l = new HexHighlightMouseListener(orUIManager);
            l.tileId = tileId;
            c.addMouseListener(l);
        }
    }
    
    /**
     * @param p Private company the hexes associated to which are
     * to be highlighted (in case of events)
     * @param enableIrrespectiveOfHighlightConfig If true, the mouse listener is
     * enabled irrespective of the base configuration. Needed since some highlighting
     * should not be disabled by configuration. 
     */
    public static void addMouseListener(JComponent c,ORUIManager orUIManager,PrivateCompany p,boolean enableIrrespectiveOfHighlightConfig) {
        if (isEnabled(enableIrrespectiveOfHighlightConfig)) {
            HexHighlightMouseListener l = new HexHighlightMouseListener(orUIManager);
            Set<PrivateCompany> privList = new HashSet<PrivateCompany>();
            privList.add(p);
            l.initPrivateCompanies(privList);
            c.addMouseListener(l);
        }
    }
    
    /**
     * @param p Public company the hexes associated to it (home, destination, ...) are
     * to be highlighted (in case of events)
     * @param enableIrrespectiveOfHighlightConfig If true, the mouse listener is
     * enabled irrespective of the base configuration. Needed since some highlighting
     * should not be disabled by configuration. 
     */
    public static void addMouseListener(JComponent c,ORUIManager orUIManager,PublicCompany p,boolean enableIrrespectiveOfHighlightConfig) {
        if (isEnabled(enableIrrespectiveOfHighlightConfig)) {
            HexHighlightMouseListener l = new HexHighlightMouseListener(orUIManager);
            l.addToHexListDistinct(p.getHomeHexes());
            l.addToHexListDistinct(p.getDestinationHex());
            c.addMouseListener(l);
        }
    }
    
    /**
     * @param si Start Item which is evaluated for any contained private companies
     */
    public static void addMouseListener(JComponent c,ORUIManager orUIManager,StartItem si) {
        if (isEnabled(false)) {
            HexHighlightMouseListener l = new HexHighlightMouseListener(orUIManager);
            Set<PrivateCompany> privList = new HashSet<PrivateCompany>();
            if (si.getPrimary() instanceof PrivateCompany) {
                privList.add((PrivateCompany)si.getPrimary());
            }
            if (si.getSecondary() instanceof PrivateCompany) {
                privList.add((PrivateCompany)si.getSecondary());
            }
            l.initPrivateCompanies(privList);
            c.addMouseListener(l);
        }
    }
    
    private static boolean isEnabled (boolean enableIrrespectiveOfHighlightConfig) {
        return (enableIrrespectiveOfHighlightConfig 
                || "yes".equals(Config.get("map.highlightHexes")));
    }
    
    public void mouseEntered(MouseEvent e) {
        if (portfolio != null) initPortfolioHexList();
        if (tileId != 0) initTileIdHexList();
        initGuiHexList();
        if (hexMap != null && guiHexList.size() > 0) {
            for (GUIHex guiHex : guiHexList) {
                guiHex.addHighlightRequest();
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        if (hexMap != null && guiHexList.size() > 0) {
            for (GUIHex guiHex : guiHexList) {
                guiHex.removeHighlightRequest();
            }
        }
        if (portfolio != null || tileId != 0) clearHexList();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}

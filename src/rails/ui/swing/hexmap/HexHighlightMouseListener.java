package rails.ui.swing.hexmap;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rails.game.MapHex;
import rails.game.PrivateCompany;
import rails.game.StartItem;
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
 * 
 * TODO: The approach to direct calls should be replaced with an appropriate model
 */
public class HexHighlightMouseListener implements MouseListener {

    List<MapHex> hexList;
    List<GUIHex> guiHexList;
    HexMap hexMap;
    ORUIManager orUIManager;
    
    /**
     * lazy creation of the gui hex list for two reasons:
     * - save effort in case the mouse over never happens
     * - hexmap is now more likely to be available than during construction
     */
    private void initGuiHexList() {
        if (hexMap == null) {
            hexMap = orUIManager.getMap();
            if (hexMap != null) {
                for (MapHex h : hexList) {
                    guiHexList.add(hexMap.getHexByName(h.getName()));
                }
            }
        }
    }

    /**
     * inefficient but probably ok due to very small size of lists
     */
    private void addToHexListOmittingDuplicates(List<MapHex> hl) {
        if (hl == null) return;
        for (MapHex h : hl) {
            if (!hexList.contains(h)) {
                hexList.add(h);
            }
        }
    }
    
    private void initPrivateCompanies(Set<PrivateCompany> privList) {
        for (PrivateCompany p : privList) {
            addToHexListOmittingDuplicates(p.getBlockedHexes());
            for (SpecialProperty sp : p.getSpecialProperties()) {
                if (sp instanceof SpecialTileLay) {
                    addToHexListOmittingDuplicates(((SpecialTileLay)sp).getLocations());
                }
                if (sp instanceof SpecialTokenLay) {
                    addToHexListOmittingDuplicates(((SpecialTokenLay)sp).getLocations());
                }
                if (sp instanceof SpecialRight) {
                    addToHexListOmittingDuplicates(((SpecialRight)sp).getLocations());
                }
                if (sp instanceof LocatedBonus) {
                    addToHexListOmittingDuplicates(((LocatedBonus)sp).getLocations());
                }
                if (sp instanceof SellBonusToken) {
                    addToHexListOmittingDuplicates(((SellBonusToken)sp).getLocations());
                }
            }
        }
    }
    
    private HexHighlightMouseListener(ORUIManager orUIManager){
        this.orUIManager = orUIManager;
        hexList = new ArrayList<MapHex>();
        guiHexList = new ArrayList<GUIHex>();
    }
    
    /**
     * @param orUIManager The OR UI manager containing the map where the highlighting 
     * should occur
     * @param hexList A list of hexes that are to be highlighted (in case of events)
     * @param privList A list of private companies the hexes associated to which are
     * to be highlighted (in case of events)
     */
    public HexHighlightMouseListener(ORUIManager orUIManager,List<MapHex> hexList, Set<PrivateCompany> privList) {
        this(orUIManager);
        addToHexListOmittingDuplicates(hexList);
        initPrivateCompanies(privList);
    }
    
    public HexHighlightMouseListener(ORUIManager orUIManager,PrivateCompany p) {
        this(orUIManager);
        Set<PrivateCompany> privList = new HashSet<PrivateCompany>();
        privList.add(p);
        initPrivateCompanies(privList);
    }
    
    public HexHighlightMouseListener(ORUIManager orUIManager,StartItem si) {
        this(orUIManager);
        Set<PrivateCompany> privList = new HashSet<PrivateCompany>();
        if (si.getPrimary() instanceof PrivateCompany) {
            privList.add((PrivateCompany)si.getPrimary());
        }
        if (si.getSecondary() instanceof PrivateCompany) {
            privList.add((PrivateCompany)si.getSecondary());
        }
        initPrivateCompanies(privList);
    }
    
    public void mouseEntered(MouseEvent e) {
        initGuiHexList();
        if (hexMap != null && guiHexList.size() > 0) {
            for (GUIHex guiHex : guiHexList) {
                guiHex.addHighlightRequest();
            }
            hexMap.repaint();
        }
    }

    public void mouseExited(MouseEvent e) {
        initGuiHexList();
        if (hexMap != null && guiHexList.size() > 0) {
            for (GUIHex guiHex : guiHexList) {
                guiHex.removeHighlightRequest();
            }
            hexMap.repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}

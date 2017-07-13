package net.sf.rails.ui.swing.gamespecific._18AL;

import java.util.List;

import com.google.common.collect.Multimap;

import rails.game.action.LayBonusToken;
import rails.game.action.LayToken;
import rails.game.action.PossibleAction;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Stop;
import net.sf.rails.game.specific._18AL.AssignNamedTrains;
import net.sf.rails.ui.swing.ORUIManager;
import net.sf.rails.ui.swing.hexmap.GUIHex;
import net.sf.rails.ui.swing.hexmap.HexMap;
import net.sf.rails.ui.swing.hexmap.TokenHexUpgrade;


public class ORUIManager_18AL extends ORUIManager {

    
    
    protected boolean processGameSpecificActions(List<PossibleAction> actions) {

        Class<? extends PossibleAction> actionType = actions.get(0).getClass();
        if (actionType == AssignNamedTrains.class) {

            AssignNamedTrains action = (AssignNamedTrains) actions.get(0);
            NameTrainsDialog dialog =
                    new NameTrainsDialog(getORWindow(), action);
            dialog.setVisible(true);

            boolean changed = dialog.hasChanged();
            if (changed) action = dialog.getUpdatedAction();
            dialog.dispose();

            if (changed) orWindow.process(action);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see net.sf.rails.ui.swing.ORUIManager#addLocatedTokenLays(rails.game.action.LayToken)
     */
   @Override
    protected void addLocatedTokenLays(LayToken action) {
  //TODO: Rework for general setup as special property 
        
        if (action instanceof LayBonusToken) { //Special Action from Private Company in 18AL
            PublicCompany company = action.getCompany();
            NetworkGraph graph = networkAdapter.getRouteGraph(company, true);
            
            for (MapHex hex:action.getLocations()) {
                if (graph.getPassableStations().containsKey(hex) )
                {
                    GUIHex guiHex = orWindow.getMapPanel().getMap().getHex(hex);
                    TokenHexUpgrade upgrade = TokenHexUpgrade.create(
                            guiHex, hex.getTokenableStops(action.getCompany()), action);
                    TokenHexUpgrade.validates(upgrade);
                    hexUpgrades.put(guiHex, upgrade);
                    continue;
                }
            }
        }
        else {
        super.addLocatedTokenLays(action);
        }
    }
}

package net.sf.rails.ui.swing.gamespecific._1844;

import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.specific._1844.GameManager_1844;
import net.sf.rails.game.specific._1844.StockRound_1844;
import net.sf.rails.game.specific._1856.CGRFormationRound;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.ui.swing.StatusWindow;
import net.sf.rails.ui.swing.elements.ActionMenuItem;

import java.util.*;

public class StatusWindow_1844 extends StatusWindow {

    private static final long serialVersionUID = 1L;

    public StatusWindow_1844() {
        super();
    }

    @Override
    protected boolean updateSpecialActionMenu() {
        // If we have already specical actions make sure the are shown
        Boolean enabled = super.updateSpecialActionMenu();
        // Check that we are in a StockRound
        RoundFacade currentRound = gameUIManager.getCurrentRound();
        if (!(currentRound instanceof StockRound_1844)) {
            return enabled;
        }
        // Add the actions to buy 1 of the remaining Private Companies
        // Either a Tunnel or a Mountain Railway
        
        //Get all Private Companies
        List<PrivateCompany> AllPrivates = gameUIManager.getAllPrivateCompanies();
        //Check which Privates are already in Player Hands
        //If the private is owned by the bank check if the private is of type Tunnel or Mountain
        //If that is true, put them in a new List AvailablePrivates
        List<PrivateCompany> AvailablePrivates = new ArrayList<PrivateCompany>();
        for (PrivateCompany pc:AllPrivates) {
            if (!pc.isClosed()) {
                // Bank Portfolios are not MoneyOwners!
                if (pc.getOwner() instanceof MoneyOwner) continue;
                if ((pc.getType().getId().equals("Mountain")) || 
                        (pc.getType().getId().equals("Tunnel"))){
                AvailablePrivates.add(pc);
                }
            }
        }
        
        if (AvailablePrivates.size() >0)  {
        
          for(PrivateCompany ap:AvailablePrivates) {   
              ActionMenuItem item = new ActionMenuItem(ap.getId());
              item.addActionListener(this);
              item.setEnabled(false);
        //item.addPossibleAction(sp);
            item.setEnabled(true);
        // specialActionItems.add(item);
            specialMenu.add(item);
          }
    
        enabled=true;
        }
        return enabled;

    }

}


package net.sf.rails.game.specific._1862;

import java.util.List;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartRound;

public class GameManager_1862 extends GameManager {
    
    public GameManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    }

    @Override
    public void nextRound(Round round) {
        if (round instanceof StartRound) {
            if (startRoundNumber.value() == 1) {
                beginStartRound();
            } else {
                startStockRound();
            }            
        }
    }
    
    @Override
    public void startGame() {
        // Randomly pick companies to start 
        // TODO: Fix
        List<PublicCompany> companies = getRoot().getCompanyManager().getAllPublicCompanies();
        int startCount = 8;
        for (PublicCompany c : companies) {
            if (c instanceof PublicCompany_1862) {
                if (startCount > 0) {
                    ((PublicCompany_1862) c).setStartable(true);
                } else {
                    ((PublicCompany_1862) c).setStartable(false);
                }
                startCount--;
            }
        }
        
        super.startGame();
    }



}

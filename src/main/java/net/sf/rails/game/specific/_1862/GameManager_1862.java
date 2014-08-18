package net.sf.rails.game.specific._1862;

import java.util.List;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartPacket;
import net.sf.rails.game.StartRound;


public class GameManager_1862 extends GameManager {
    
    public GameManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    }

    @Override
    protected void beginStartRound() {
        StartPacket startPacket = ((CompanyManager_1862) (getRoot().getCompanyManager())).getStartPacket();
        this.startPacket.set(startPacket);
        
        if (startPacket != null) {
            this.startPacket.set(startPacket);
            createStartRound(startPacket);
        } else {
            startStockRound();
        }
        
        createStartRound();
    }
    
    protected void createStartRound() {
        String parlamentRoundClassName = "net.sf.rails.game.specific._1862.StartRound_1862";  // TODO: Get from some better place?
        StartRound startRound = createRound (StartRound.class, parlamentRoundClassName,    
                "startRound_" + startRoundNumber.value());
        startRoundNumber.add(1);
        startRound.start();
    }
    
    @Override
    public void nextRound(Round round) {
        if (round instanceof StartRound) {
            if (startRoundNumber.value() == 1) {
                beginStartRound();
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
            if (startCount > 0) {
                ((PublicCompany_1862) c).setStartable(true);
            } else {
                ((PublicCompany_1862) c).setStartable(false);
            }
            startCount--;
        }
        
        super.startGame();
    }



}

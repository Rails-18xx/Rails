package net.sf.rails.game.specific._1862;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartPacket;
import net.sf.rails.game.StartRound;


public class GameManager_1862 extends GameManager {

//    private Player playerToStartCGRFRound = null;

    public GameManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    }

    @Override
    protected void beginStartRound() {
        createParlamentRound();
//        startParlamentRound();
//        StartPacket startPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
//        
//        // check if there are still unfinished startPackets
//        if (startPacket != null) {
//            // set this to the current startPacket
//            this.startPacket.set(startPacket);
//            // start a new StartRound
//            createStartRound(startPacket);
//        } else {
//            // otherwise 
//            startStockRound();
//        }
    }
    
    protected void createParlamentRound() {
        String parlamentRoundClassName = "net.sf.rails.game.specific._1862.ParliamentRound";
        StartRound startRound = createRound (StartRound.class, parlamentRoundClassName,    
                "startRound_" + startRoundNumber.value());
        startRoundNumber.add(1);
        startRound.start();
    }

    
//    public void startCGRFormationRound(OperatingRound_1862 or,
//            Player playerToStartCGRFRound) {
//
//        this.playerToStartCGRFRound = playerToStartCGRFRound;
//        interruptedRound = or;
//
//        if (this.playerToStartCGRFRound != null) {
//            // TODO: this id will not work
//            createRound (CGRFormationRound.class, "CGRFormationRound").start (this.playerToStartCGRFRound);
//            this.playerToStartCGRFRound = null;
//        }
//    }
//
//    @Override
//    public void nextRound(Round round) {
//        if (round instanceof CGRFormationRound) {
//            setRound(interruptedRound);
//            ((OperatingRound_1862)interruptedRound).resume(((CGRFormationRound)round).getMergingCompanies());
//        } else {
//            super.nextRound(round);
//        }
//
//    }

}

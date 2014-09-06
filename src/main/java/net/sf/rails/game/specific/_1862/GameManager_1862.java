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

//    public void nextRound(Round round) {
//        if (round instanceof StartRound) {
//            if (((StartRound) round).getStartPacket().areAllSold()) { //This start Round was completed
//                StartPacket nextStartPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
//             if (nextStartPacket == null) {
//                 if (skipFirstStockRound) {
//                     Phase currentPhase =
//                             getRoot().getPhaseManager().getCurrentPhase();
//                     if (currentPhase.getNumberOfOperatingRounds() != numOfORs.value()) {
//                         numOfORs.set(currentPhase.getNumberOfOperatingRounds());
//                     }
//                     log.info("Phase=" + currentPhase.toText() + " ORs=" + numOfORs);
//
//                     // Create a new OperatingRound (never more than one Stock Round)
//                     // OperatingRound.resetRelativeORNumber();
//
//                     relativeORNumber.set(1);
//                     startOperatingRound(true);
//                 } else {
//                     startStockRound();
//                 }
//             } else {
//                 beginStartRound();
//             }
//            }else {
//                 startOperatingRound(runIfStartPacketIsNotCompletelySold());
//             }
//        } else if (round instanceof StockRound) {
//            Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
//            if (currentPhase == null) log.error ("Current Phase is null??", new Exception (""));
//            numOfORs.set(currentPhase.getNumberOfOperatingRounds());
//            log.info("Phase=" + currentPhase.toText() + " ORs=" + numOfORs);
//
//            // Create a new OperatingRound (never more than one Stock Round)
//            // OperatingRound.resetRelativeORNumber();
//            relativeORNumber.set(1);
//            startOperatingRound(true);
//
//        } else if (round instanceof OperatingRound) {
//            if (gameOverPending.value() && !gameEndsAfterSetOfORs) {
//
//                finishGame();
//
//            } else if (relativeORNumber.add(1) <= numOfORs.value()) {
//                // There will be another OR
//                startOperatingRound(true);
//            } else if (getRoot().getCompanyManager().getNextUnfinishedStartPacket() !=null) {
//               beginStartRound();
//            } else {
//                if (gameOverPending.value() && gameEndsAfterSetOfORs) {
//                    finishGame();
//                } else {
//                    ((OperatingRound)round).checkForeignSales();
//                    startStockRound();
//                }
//            }
//        }
//    }
    
    @Override
    public void nextRound(Round round) {
        System.out.println("nextRound");
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

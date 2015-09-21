/**
 * 
 */
package net.sf.rails.game.specific._1837;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Phase;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartPacket;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.NationalFormationRound;
import net.sf.rails.game.specific._1837.OperatingRound_1837;



/**
 * @author martin
 *
 */
public class GameManager_1837 extends GameManager {

    private Round previousRound = null;
    
  
    public GameManager_1837(RailsRoot parent, String id) {
        super(parent, id);
 
    }

    
    public void nextRound(Round round) {
        if (round instanceof StartRound) { 
            if (((StartRound) round).getStartPacket().areAllSold()) { // This start round was "completed"
                // check if there are other StartPackets, otherwise stockRounds start 
                beginStartRound();
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
        }
        else if (round instanceof NationalFormationRound)
        {
            if (interruptedRound != null) {
                setRound(interruptedRound);
                interruptedRound.resume();
                interruptedRound = null;
            } else if (previousRound != null) {
                super.nextRound(previousRound);
                previousRound = null;
            }
        } else {
            Phase phase = getCurrentPhase();
            if ((phase.getId().equals("4E") || phase.getId().equals("5"))
                    && (!NationalFormationRound.nationalIsComplete(((GameManager)this),"Ug"))) {
                previousRound = round;
                startHungaryFormationRound (null);
            } else if ((phase.getId().equals("4"))
                    && (!NationalFormationRound.nationalIsComplete(((GameManager)this),"Sd"))) {
                previousRound = round;
                startSuedBahnFormationRound (null);
            } else if (((phase.getId().equals("4")) || ( phase.getId().equals("4E")) ||
                    (phase.getId().equals("4+1")))
                    && (!NationalFormationRound.nationalIsComplete(((GameManager) this),"KK"))) {
                previousRound = round;
                startKuKFormationRound (null);                
            } else {
                super.nextRound(round);
            }
        }

    }


    
    /* (non-Javadoc)
     * @see net.sf.rails.game.GameManager#runIfStartPacketIsNotCompletelySold()
     */
    @Override
    protected boolean runIfStartPacketIsNotCompletelySold() {
        //After the first Startpacket sold out there will be Operation Rounds
        StartPacket nextStartPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
        if (nextStartPacket.getId() == "Coal Mines") {
            return false;
        }
        else {
            return true;
        }
    }


    public void startHungaryFormationRound(OperatingRound_1837 or) {
        interruptedRound = or;
        String roundName;
        if (interruptedRound == null) {
            // after a round
            roundName = "HungaryFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "HungaryFormationRound_in_" + or.getId();
        }
        this.setNationalToFound("Ug");
        createRound(NationalFormationRound.class, roundName).start();
    }

    public void startSuedBahnFormationRound(OperatingRound_1837 or) {
        interruptedRound = or;
        String roundName;
        if (interruptedRound == null) {
            // after a round
            roundName = "SuedBahnFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "SuedBahnFormationRound_in_" + or.getId();
        }
        this.setNationalToFound("Sd");
        createRound(NationalFormationRound.class, roundName).start();
    }

    public void startKuKFormationRound(OperatingRound_1837 or) {
        interruptedRound = or;
        String roundName;
        if (interruptedRound == null) {
            // after a round
            roundName = "KuKFormationRound_after_" + previousRound.getId();
        } else {
            roundName = "KuKFormationRound_in_" + or.getId();
        }
        this.setNationalToFound("KK");
        createRound(NationalFormationRound.class, roundName).start();
    }

}

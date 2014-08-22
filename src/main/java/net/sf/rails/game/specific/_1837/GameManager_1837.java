/**
 * 
 */
package net.sf.rails.game.specific._1837;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.specific._1837.OperatingRound_1837;



/**
 * @author martin
 *
 */
public class GameManager_1837 extends GameManager {

    private Round previousRound = null;
    private Player KkFormStartingPlayer = null;
    private Player SbFormStartingPlayer = null;
    private Player HuFormStartingPlayer = null;
    
    public final static String K1_ID = "K1";
    public final static String KK_ID = "KK";
    public final static String SU_ID = "SU";
    public final static String HU_ID = "HU";
    public static final String H1_ID = "H1";
    public static final String S1_ID = "S1";
  


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
        else if ((round instanceof HungaryFormationRound) || 
                    (round instanceof SuedBahnFormationRound) ||
                    (round instanceof KuKFormationRound))
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
                    && !HungaryFormationRound.HungaryIsComplete(this)) {
                previousRound = round;
                startHungaryFormationRound (null);
            } else if ((phase.getId().equals("4"))
                    && (!SuedBahnFormationRound.SuedbahnIsComplete(this))) {
                previousRound = round;
                startSuedBahnFormationRound (null);
            } else if (((phase.getId().equals("4")) || ( phase.getId().equals("4E")) ||
                    (phase.getId().equals("4+1")))
                    && !KuKFormationRound.KuKIsComplete(this)) {
                previousRound = round;
                startKuKFormationRound (null);                
            } else {
                super.nextRound(round);
            }
        }

    }


    public void setHungaryFormationStartingPlayer(Player startingPlayer) {
        this.HuFormStartingPlayer = startingPlayer;
    }


    public Player getHungaryFormationStartingPlayer() {
        return this.HuFormStartingPlayer;
    }


    public Player getKuKFormationStartingPlayer() {
        return this.KkFormStartingPlayer;
    }


    public void setKuKFormationStartingPlayer(Player currentPlayer) {
        this.KkFormStartingPlayer=currentPlayer;
    }


    public void setSuedbahnFormationStartingPlayer(Player currentPlayer) {
        this.SbFormStartingPlayer=currentPlayer;
    }


    public Player getSuedbahnFormationStartingPlayer() {
        return this.SbFormStartingPlayer;
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
        createRound(HungaryFormationRound.class, roundName).start();
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
        createRound(SuedBahnFormationRound.class, roundName).start();
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
        createRound(KuKFormationRound.class, roundName).start();
    }

    

}

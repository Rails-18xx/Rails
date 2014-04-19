/**
 * 
 */
package net.sf.rails.game.specific._1837;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartRound;


/**
 * @author martin
 *
 */
public class GameManager_1837 extends GameManager {

    private ParSlotManager_1837 parSlotManager;
    /**
     * 
     */
    public GameManager_1837(RailsRoot parent, String id) {
        super(parent, id);
        parSlotManager = new ParSlotManager_1837(parent, "ParSlotControl");
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
        else {
            super.nextRound(round);
        }
    }
    
    public ParSlotManager_1837 getParSlotManager() {
        return parSlotManager;
    }

}

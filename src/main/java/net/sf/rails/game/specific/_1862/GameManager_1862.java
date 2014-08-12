package net.sf.rails.game.specific._1862;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Round;
import net.sf.rails.game.StartRound;


public class GameManager_1862 extends GameManager {
    
    public GameManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    }

    @Override
    protected void beginStartRound() {
        createParlamentRound();
    }
    
    protected void createParlamentRound() {
        String parlamentRoundClassName = "net.sf.rails.game.specific._1862.ParliamentRound";
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


}

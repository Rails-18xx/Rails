package rails.sound;

import rails.game.GameManagerI;
import rails.game.action.*;

/**
 * Converts processed actions and model updates to triggers for playing sounds.
 *  
 * @author Frederick Weld
 *
 */
public class SoundEventInterpreter {
    private SoundContext context;
    private SoundPlayer player;
    public SoundEventInterpreter (SoundContext context,SoundPlayer player) {
        this.context = context;
        this.player = player;
    }
    public void notifyOfActionProcessing(GameManagerI gm,PossibleAction action) {
        
        /**
         * Interpretation of events for which only sfx is relevant 
         */
        
        if (SoundConfig.isSFXEnabled()) {
            
            //OR actions
            
            if (action instanceof LayTile) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayTile);
                
            } else if (action instanceof LayToken) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_LayToken);
                
            } else if (action instanceof SetDividend) {
                SetDividend sd = (SetDividend)action;
                if (sd.getRevenueAllocation() == SetDividend.PAYOUT) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_Decision_Payout);
                } else if (sd.getRevenueAllocation() == SetDividend.SPLIT) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_Decision_Split);
                } else if (sd.getRevenueAllocation() == SetDividend.WITHHOLD) {
                    player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_Decision_Withhold);
                }  
                
            } else if (action instanceof BuyTrain) {
                String trainName = ((BuyTrain)action).getType().getName();
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_BuyTrain, trainName);
                
            } else if (action instanceof BuyPrivate) {
                player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_BuyPrivate);
                
            }
        }
    }
}

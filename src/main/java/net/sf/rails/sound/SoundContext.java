/**
 * 
 */
package net.sf.rails.sound;

import net.sf.rails.game.*;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.TreasuryShareRound;
import net.sf.rails.game.round.RoundFacade;

/**
 * Takes care of the current context from a music/sfx perspective.
 * Reacts on context changes by triggering changes to the played music/sfx. 
 * 
 * @author Frederick Weld
 *
 */
public class SoundContext {
    private double averageRevenue = 50;
    //to which degree (from 0=none to 1=full) is new revenue considered for determining 
    //the new average revenue value 
    private final static double slidingAverageAdjustmentFactor = 0.2;
    //how much percent of the set revenue sfx is played if the revenue is average
    private final static double averageSetRevenuePlaySoundProportion = 0.4;
    //how much percent of the set revenue sfx is played if the revenue is epsilon;
    private final static double minimumSetRevenuePlaySoundProportion = 0.167;
    
    private RoundFacade currentRound = null;
    private Phase currentPhase = null;
    private String currentBackgroundMusicFileName = null;
    
    private SoundPlayer player;
    
    public SoundContext(SoundPlayer player) {
        this.player = player;
    }
    
    public void notifyOfMusicEnablement(boolean musicEnabled) {
        if (!musicEnabled && player.isBGMPlaying()) {
            player.stopBGM();
        }
        if (musicEnabled && !player.isBGMPlaying()) {
            String musicFileNamePriorToDisable = currentBackgroundMusicFileName;
            
            //try to start BGM based on rounds / phases
            currentBackgroundMusicFileName = null;
            playBackgroundMusic();
            
            //if no BGM could be started, replay the music that was active before disabling
            //BGM music
            if (currentBackgroundMusicFileName == null) {
                currentBackgroundMusicFileName = musicFileNamePriorToDisable;
                player.playBGM(musicFileNamePriorToDisable);
            }
        }
    }

    public void notifyOfSetRevenue(int actualRevenue) {
        //ignore zero revenue
        if (actualRevenue <= 0) return;
        
        double playSoundProportion = minimumSetRevenuePlaySoundProportion
                + ( 1 - minimumSetRevenuePlaySoundProportion )
                * ( averageSetRevenuePlaySoundProportion - minimumSetRevenuePlaySoundProportion )
                * actualRevenue / averageRevenue;
        if (playSoundProportion > 1) playSoundProportion = 1;

        player.playSFXByConfigKey (SoundConfig.KEY_SFX_OR_SetRevenue,
                playSoundProportion);

        averageRevenue = actualRevenue * slidingAverageAdjustmentFactor
                + averageRevenue * (1 - slidingAverageAdjustmentFactor);
    }

    private void playBackgroundMusic() {
        //do nothing if
        // - music is not enabled
        // - phase is not initialized
        if (!SoundConfig.isBGMEnabled() || currentPhase == null) return;
        
        String currentRoundConfigKey = null;
        if (currentRound instanceof StartRound) {
            currentRoundConfigKey = SoundConfig.KEY_BGM_StartRound;
        } else if (currentRound instanceof StockRound) {
            currentRoundConfigKey = SoundConfig.KEY_BGM_StockRound;
        } else if (currentRound instanceof OperatingRound) {
            currentRoundConfigKey = SoundConfig.KEY_BGM_OperatingRound;
        } else if (currentRound instanceof EndOfGameRound) {
            currentRoundConfigKey = SoundConfig.KEY_BGM_EndOfGameRound;
        }
        //only play anything if
        // - round is recognized
        // - new music is to be played
        if (currentRoundConfigKey != null) {
            String currentPhaseName = "";
            if (currentPhase != null) currentPhaseName = currentPhase.getId();
            String newBackgroundMusicFileName = SoundConfig.get(
                    currentRoundConfigKey, currentPhaseName);
            if (!newBackgroundMusicFileName.equals(currentBackgroundMusicFileName)) {
                currentBackgroundMusicFileName = newBackgroundMusicFileName;
                player.playBGM(newBackgroundMusicFileName);
            }
        }
    }
    synchronized public void notifyOfPhase(Phase newPhase) {
        if (newPhase != null && !newPhase.equals(currentPhase)) {
            currentPhase = newPhase;
            playBackgroundMusic();
        }
    }

    synchronized public void notifyOfRound(RoundFacade newRound) {
        if (newRound != null && !newRound.equals(currentRound)) {

            //play stock market opening bell if (non treasury / non share selling)
            //stock round became current round
            //and the round before was not
            if (SoundConfig.isSFXEnabled() 
                    && !(currentRound instanceof StockRound)
                    && newRound instanceof StockRound
                    && !(newRound instanceof TreasuryShareRound)
                    && !(newRound instanceof ShareSellingRound)) {
                player.playSFXByConfigKey(SoundConfig.KEY_SFX_SR_OpeningBell);
            }
            
            currentRound = newRound;

            playBackgroundMusic();
        }
    }
    public void notifyOfGameSetup() {
        currentBackgroundMusicFileName = SoundConfig.get(SoundConfig.KEY_BGM_GameSetup);
        if (SoundConfig.isBGMEnabled()) player.playBGM(currentBackgroundMusicFileName);
    }
}

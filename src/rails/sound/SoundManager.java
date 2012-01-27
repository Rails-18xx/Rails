package rails.sound;

import rails.game.GameManager;
import rails.game.action.PossibleAction;

/**
 * This is a singleton class as there should never be two
 * background musics playing at the same time.
 * 
 * This class is notified of:
 * - processed actions (lay tile,...)
 * - model updates (phase change, presidency change,...)
 * 
 * Based on this information, the appropriate background music or
 * sound effect is played in the background (only mp3 supported). 
 * 
 * @author Frederick Weld
 *
 */
public class SoundManager {
    private SoundPlayer player;
    private SoundContext context;
    private SoundEventInterpreter eventInterpreter;
    private static SoundManager manager;
    
    private SoundManager() {
        player = new SoundPlayer();
        context = new SoundContext();
        eventInterpreter = new SoundEventInterpreter(context,player);
    }
    public static SoundManager getInstance() {
        if (manager == null) manager = new SoundManager();
        return manager;
    }
    public static void init() {
        getInstance().context.notifyOfMusicEnablement(SoundConfig.isMusicEnabled());
    }
    public static void notifyOfActionProcessing(GameManager gm,PossibleAction action) {
        getInstance().eventInterpreter.notifyOfActionProcessing(gm,action);
    }
}

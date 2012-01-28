package rails.sound;

import rails.game.GameManagerI;
import rails.game.action.PossibleAction;
import rails.game.action.SetDividend;

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
        context = new SoundContext(player);
        eventInterpreter = new SoundEventInterpreter(context,player);
    }
    public static SoundManager getInstance() {
        if (manager == null) manager = new SoundManager();
        return manager;
    }
    public static void init() {
        getInstance().context.notifyOfMusicEnablement(SoundConfig.isBGMEnabled());
    }
    public static void notifyOfActionProcessing(GameManagerI gm,PossibleAction action) {
        getInstance().eventInterpreter.notifyOfActionProcessing(gm,action);
    }
    public static void notifyOfSetRevenue(int actualRevenue) {
        if (SoundConfig.isSFXEnabled()) {
            getInstance().context.notifyOfSetRevenue(actualRevenue);
        }
    }
    /**
     * Called when game engine has been instantiated for a specific game
     */
    public static void notifyOfGameInit(GameManagerI gameManager) {
        getInstance().eventInterpreter.notifyOfGameInit(gameManager);
    }
    /**
     * Called when game setup window initially opens
     */
    public static void notifyOfGameSetup() {
        getInstance().eventInterpreter.notifyOfGameSetup();
    }
}

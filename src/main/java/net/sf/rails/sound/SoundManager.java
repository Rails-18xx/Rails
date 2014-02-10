package net.sf.rails.sound;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.ui.swing.ORUIManager;

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
    public static void notifyOfActionProcessing(RailsRoot root,PossibleAction action) {
        getInstance().eventInterpreter.notifyOfActionProcessing(root, action);
    }
    public static void notifyOfSetRevenue(int actualRevenue) {
        if (SoundConfig.isSFXEnabled()) {
            getInstance().context.notifyOfSetRevenue(actualRevenue);
        }
    }
    /**
     * Called when game engine has been instantiated for a specific game
     */
    public static void notifyOfGameInit(RailsRoot root) {
        getInstance().eventInterpreter.notifyOfGameInit(root);
    }
    /**
     * Called when game setup window initially opens
     */
    public static void notifyOfGameSetup() {
        getInstance().context.notifyOfGameSetup();
    }
    public static void notifyOfTimeWarp(boolean timeWarpMode) {
        getInstance().eventInterpreter.notifyOfTimeWarp(timeWarpMode);
    }
    public static void notifyOfORLocalStep(ORUIManager.LocalSteps currentStep) {
        getInstance().eventInterpreter.notifyOfORLocalStep(currentStep);
    }
    public static void notifyOfClickFieldSelection(PossibleAction clickFieldAction) {
        getInstance().eventInterpreter.notifyOfClickFieldSelection(clickFieldAction);
    }
}

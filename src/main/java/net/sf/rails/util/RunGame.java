package net.sf.rails.util;

import net.sf.rails.common.ConfigManager;
import net.sf.rails.common.GuiDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.ui.swing.GameSetupController;
import net.sf.rails.ui.swing.GameSetupWindow;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.SplashWindow;

public class RunGame {

    public static void main(String[] args) {

        // Initialize configuration
        ConfigManager.initConfiguration(false);
        
        int nargs = 0;
        if (args != null && args.length > 0) {
            nargs = args.length;
            System.out.println("Number of args: "+nargs);
            for (String arg : args) {
                System.out.println ("Arg: "+arg);
            }
        }

        if (nargs >= 1) {
            // We have to run loadGame on an AWT EventQueue to make sure
            // that NDC will properly initialize game key so that
            // GameManager instance can be properly queried for. This is a
            // consequence of NDC abuse.
            loadGameOnEventQueue(args);
        } else {
            /* Start the rails.game selector, which will do all the rest. */
            GameSetupController.Builder setupBuilder = GameSetupController.builder();
            setupBuilder.start();
        }
    }

    static void loadGameOnEventQueue(final String[] args)
    {
        try {
            loadGame(args);
        } catch (Exception e) {
            System.err.println("Cannot load game: "+e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    static void loadGame (String[] args) {

        RailsRoot game = null;
        String filepath = args[0];

        SplashWindow splashWindow = new SplashWindow(true,filepath);
        splashWindow.notifyOfStep(SplashWindow.STEP_LOAD_GAME);

        System.out.println("Starting game from saved file "+filepath);
        GameLoader gameLoader = new GameLoader();
        if (gameLoader.createFromFile(filepath)) {
            System.err.println("Loading file "+filepath+" was unsuccessful");
            return;
        }

        GameManager gameManager = gameLoader.getRoot().getGameManager();
        GameUIManager gameUIManager;
        String gameUIManagerClassName = gameManager.getClassName(GuiDef.ClassName.GAME_UI_MANAGER);
        try {
            Class<? extends GameUIManager> gameUIManagerClass =
                Class.forName(gameUIManagerClassName).asSubclass(GameUIManager.class);
            gameUIManager = gameUIManagerClass.newInstance();
            gameUIManager.init(game, true, splashWindow);

            String directory = new java.io.File(filepath).getParent();
            if(directory != null) {
                gameUIManager.setSaveDirectory(directory);
            }

            gameUIManager.startLoadedGame();
            gameUIManager.notifyOfSplashFinalization();
            splashWindow.finalizeGameInit();
            splashWindow = null;

        } catch (Exception e) {
            System.err.println("Cannot instantiate class " + gameUIManagerClassName + ": "+e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

    }
}

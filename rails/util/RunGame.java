package rails.util;

import rails.common.GuiDef;
import rails.game.Game;
import rails.game.GameManagerI;
import rails.ui.swing.GameSetupWindow;
import rails.ui.swing.GameUIManager;

public class RunGame {

    public static void main(String[] args) {

        // Initialize configuration
        Config.setConfigSelection();
        
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
            new GameSetupWindow();
        }
    }

    static void loadGameOnEventQueue(final String[] args)
    {
        try {
            java.awt.EventQueue.invokeAndWait(
                new Runnable()
                {
                    public void run() {
                        loadGame(args);
                    }
                }
            );
        } catch (Exception e) {
            System.err.println("Cannot load game: "+e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    static void loadGame (String[] args) {

        Game game = null;
        String filepath = args[0];
        System.out.println("Starting game from saved file "+filepath);
        if ((game = Game.load(filepath)) == null) {
            System.err.println("Loading file "+filepath+" was unsuccessful");
            return;
        }

        GameManagerI gameManager = game.getGameManager();
        GameUIManager gameUIManager;
        String gameUIManagerClassName = gameManager.getClassName(GuiDef.ClassName.GAME_UI_MANAGER);
        try {
            Class<? extends GameUIManager> gameUIManagerClass =
                Class.forName(gameUIManagerClassName).asSubclass(GameUIManager.class);
            gameUIManager = gameUIManagerClass.newInstance();
            gameUIManager.init(gameManager, true);

            String directory = new java.io.File(filepath).getParent();
            if(directory != null) {
                gameUIManager.setSaveDirectory(directory);
            }

            gameUIManager.startLoadedGame();

        } catch (Exception e) {
            System.err.println("Cannot instantiate class " + gameUIManagerClassName + ": "+e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

    }
}

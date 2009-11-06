package rails.util;

import rails.common.Defs;
import rails.game.Game;
import rails.game.GameManagerI;
import rails.ui.swing.GameSetupWindow;
import rails.ui.swing.GameUIManager;

public class RunGame {
    /** The default properties file name */
    private static String DEFAULT_CONFIG_FILE = "my.properties";

    public static void main(String[] args) {

        /*
         * Check if the property file has been set on the command line. The way
         * to do this is adding an option to the java command: -Dconfigfile=<property-filename>
         */
        String myConfigFile = System.getProperty("configfile");
        System.out.println("Cmdline configfile setting = " + myConfigFile);

        /* If not, use the default configuration file name */
        if (!Util.hasValue(myConfigFile)) {
            myConfigFile = DEFAULT_CONFIG_FILE;
        }

        /*
         * Set the system property that tells log4j to use this file. (Note:
         * this MUST be done before updating Config)
         */
        System.setProperty("log4j.configuration", myConfigFile);
        /* Tell the properties loader to read this file. */
        Config.setConfigFile(myConfigFile);
        System.out.println("Configuration file = " + myConfigFile);

    	int nargs = 0;
    	if (args != null && args.length > 0) {
    		nargs = args.length;
    		System.out.println("Number of args: "+nargs);
    		for (String arg : args) {
    			System.out.println ("Arg: "+arg);
    		}
    	}

    	if (nargs >= 1) {
    		loadGame (args);
    	} else {
    		/* Start the rails.game selector, which will do all the rest. */
    		new GameSetupWindow();
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
    	String gameUIManagerClassName = gameManager.getClassName(Defs.ClassName.GAME_UI_MANAGER);
	    try {
	        Class<? extends GameUIManager> gameUIManagerClass =
	            Class.forName(gameUIManagerClassName).asSubclass(GameUIManager.class);
	        gameUIManager = gameUIManagerClass.newInstance();
	        gameUIManager.init(gameManager);
	        gameUIManager.startLoadedGame();

	    } catch (Exception e) {
	        System.err.println("Cannot instantiate class " + gameUIManagerClassName + ": "+e.getMessage());
	        e.printStackTrace(System.err);
	        System.exit(1);
	    }

    }
}

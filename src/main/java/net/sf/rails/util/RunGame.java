package net.sf.rails.util;

import java.io.File;

import net.sf.rails.common.ConfigManager;
import net.sf.rails.ui.swing.GameSetupController;

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

        // currently we only start one game
        if (nargs >= 1) {
            loadGame(args[0]);
        } else {
            /* Start the rails.game selector, which will do all the rest. */
            GameSetupController.Builder setupBuilder = GameSetupController.builder();
            setupBuilder.start();
        }
    }

    private static void loadGame (String filePath) {
        File gameFile = new File(filePath);
        GameLoader.loadAndStartGame(gameFile);
    }
}

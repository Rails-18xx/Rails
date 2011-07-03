package rails.test;

import rails.common.parser.Config;
import rails.ui.swing.GameSetupWindow;

public class GameTest {

    public static void main(String[] args) {

        // intialize configuration
        Config.setConfigSelection();
        
        int nargs = 0;
        if (args != null && args.length > 0) {
            for (String arg : args) {
                System.out.println ("Arg "+(++nargs)+": "+arg);
            }
        }

        /* Start the rails.game selector, which will do all the rest. */
        new GameSetupWindow();
    }
}

package rails.test;

import rails.ui.swing.GameSetupWindow;
import rails.util.Config;

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

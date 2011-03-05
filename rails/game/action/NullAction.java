/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/NullAction.java,v 1.10 2010/01/31 22:22:29 macfreek Exp $*/
package rails.game.action;

public class NullAction extends PossibleAction {

    public static final int DONE = 0;
    public static final int PASS = 1;
    public static final int SKIP = 2;
    public static final int AUTOPASS = 3;
    public static final int START_GAME = 4; // For use after loading
    public static final int MAX_MODE = 4;

    // optional label that is returned on toString instead of the standard labels defined below
    private String optionalLabel = null;

    // standard labels defined
    private static String[] name = new String[] { "Done", "Pass", "Skip", "Autopass", "StartGame" };

    protected int mode = -1;

    public static final long serialVersionUID = 2L;

    public NullAction(int mode) {
        super();
        if (mode < 0 || mode > MAX_MODE) mode = 0; // For safety
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
    
    /** returns the NullAction itself */
    public NullAction setLabel(String label) {
        this.optionalLabel = label;
        return this;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof NullAction)) return false;
        NullAction a = (NullAction) action;
        return a.mode == mode;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        return equalsAsOption(action);
    }

   @Override
    public String toString() {
        if (optionalLabel != null) return optionalLabel;
        return name[mode];
    }
    
}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/NullAction.java,v 1.8 2010/01/18 18:49:12 evos Exp $*/
package rails.game.action;

public class NullAction extends PossibleAction {

    public static final int DONE = 0;
    public static final int PASS = 1;
    public static final int SKIP = 2;
    public static final int AUTOPASS = 3;
    public static final int MAX_MODE = 3;

    private static String[] name = new String[] { "Done", "Pass", "Skip", "Autopass" };

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

    @Override
	public boolean equals(PossibleAction action) {
        if (!(action instanceof NullAction)) return false;
        NullAction a = (NullAction) action;
        return a.mode == mode;
    }

    @Override
	public String toString() {
        return name[mode];
    }
}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/NullAction.java,v 1.6 2007/12/30 14:25:12 evos Exp $*/
package rails.game.action;

public class NullAction extends PossibleAction {

	public static final int DONE = 0;
	public static final int PASS = 1;
	public static final int SKIP = 2;
	public static final int MAX_MODE = 2;
	
	private String[] name = new String[] 
	      {"Done", "Pass", "Skip"};
	
	protected int mode = -1;
	
    public static final long serialVersionUID = 2L;

    public NullAction (int mode) {
		super();
		if (mode < 0 || mode > MAX_MODE) mode = 0; // For safety
		this.mode =  mode;
	}
	
	public int getMode () {
		return mode;
	}
	
    public boolean equals (PossibleAction action) {
        if (!(action instanceof NullAction)) return false;
        NullAction a = (NullAction) action;
        return a.mode == mode;
    }

    public String toString () {
		return name[mode];
	}
}

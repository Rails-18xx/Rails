package rails.game.action;

public class NullAction extends PossibleAction {

	public static final int DONE = 0;
	public static final int PASS = 1;
	public static final int UNDO = 2;
	public static final int FORCED_UNDO = 3;
	public static final int REDO = 4;
	public static final int CLOSE = 5;
	public static final int SKIP = 6;
	public static final int MAX_MODE = 6;
	
	private String[] name = new String[] 
	      {"Done", "Pass", "Undo", "Undo!", "Redo", "Close", "Skip"};
	
	protected int mode = -1;
	
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

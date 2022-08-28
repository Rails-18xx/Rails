package rails.game.action;

import net.sf.rails.game.RailsRoot;

public class GrowCompany extends PossibleORAction {

    /*--- Server (engine) settings ---*/
    private int newShareUnit;

    /*--- Client (UI) settings ---*/
    // None yet

    public GrowCompany (RailsRoot root, int newShareUnit) {
        super(root);
        this.newShareUnit = newShareUnit;
    }

    public int getNewShareUnit() {
        return newShareUnit;
    }
}

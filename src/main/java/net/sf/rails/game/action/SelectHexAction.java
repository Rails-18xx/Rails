package net.sf.rails.game.action;

import net.sf.rails.game.Player;
import net.sf.rails.game.RailsRoot;
import rails.game.action.PossibleAction;

public class SelectHexAction extends PossibleAction {
    private final String hexId;

    public SelectHexAction(RailsRoot root, String hexId, Player player) {
        super(root);
        // If PossibleAction has a 'setPlayer' or 'player' field, 
        // you may need to use reflection if a setter isn't public.
        // Assuming a protected 'player' field exists in PossibleAction:
        this.player = player; 
        this.hexId = hexId;
    }

    public String getHexId() { return hexId; }
}
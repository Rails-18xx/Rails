package net.sf.rails.game.specific._18Chesapeake;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import rails.game.action.LayTile;

public class OperatingRound_18Chesapeake extends OperatingRound {
    /**
     * Constructed via Configure
     *
     * @param parent
     * @param id
     */
    public OperatingRound_18Chesapeake(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected boolean validateSpecialTileLay(LayTile layTile) {
        return super.validateSpecialTileLay(layTile);
    }
}

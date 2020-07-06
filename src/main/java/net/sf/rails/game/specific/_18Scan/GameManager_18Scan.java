package net.sf.rails.game.specific._18Scan;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;

/**
 * This class is needed because we must have two ORs if all players pass in the Initial Stock Round.
 * Meddling with GameManager resulted in breaking tests of other games, so we need a subclass.
 */
public class GameManager_18Scan extends GameManager {

    public GameManager_18Scan (RailsRoot parent, String id) {
        super(parent, id);
    }

    // A kludge to make sure that the number of "Short OR"s is always 2.
    // The regular GameManager seems broken on the aspect of numbering and counting
    // short ORs. This can be fixed, but then saved test games won't pass because of
    // OR numbering differences.
    //
    // This code relies on the condition that this method is only called at the start
    // of the first of any sequence of short ORs, i.e. when a StartRound precedes a short OR,
    // and that the argument for startOperatingRound in all other cases is true.
    protected boolean runIfStartPacketIsNotCompletelySold() {
        relativeORNumber.set(1);
        numOfORs.set(2);
        return true;
    }

}

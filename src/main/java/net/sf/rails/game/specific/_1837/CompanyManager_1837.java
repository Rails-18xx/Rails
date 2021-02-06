package net.sf.rails.game.specific._1837;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StartPacket;

public class CompanyManager_1837 extends CompanyManager {

    public CompanyManager_1837(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * In 1837, we never return to the sole initial start packet,
     * even if it is not completely sold in v2.
     * See the English version of Lonny's v2.0 rules, Chapter I:
     * "[A player] may either start an auction (...) or pass."
     * "If the Start Packet is not completely sold out when the First
     * Stock Round [= StartRound] ends [because all players pass],
     * place any unsold items in a group with the two secondary (non-director)
     * certificates of Ug minors #1 and #3."
     * "Any remaining Start Packet items are sold at face value [in the next
     * Stock Round, i.e., no more bidding]."
     *
     * @return Always null, because we never do a second start round.
     */
    @Override
    public StartPacket getNextUnfinishedStartPacket() {
        return null;
    }

}

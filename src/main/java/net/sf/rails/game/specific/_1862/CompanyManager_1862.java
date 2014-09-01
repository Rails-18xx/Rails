package net.sf.rails.game.specific._1862;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartPacket;

public class CompanyManager_1862 extends CompanyManager {

    static int startNumber = 1;

    public CompanyManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    } 

    public StartPacket getNextUnfinishedStartPacket() {
        StartPacket originalPacket = startPackets.get(0);
        StartPacket newPacket =
                StartPacket.create(originalPacket.getParent(),
                        originalPacket.getId() + (startNumber++),
                        originalPacket.getRoundClassName());

        int index = 0;
        for (PublicCompany c : getAllPublicCompanies()) {
            if (((PublicCompany_1862) c).isStartable()) {
                StartItem si =
                        StartItem.create(newPacket, c.getLongName(), "Public",
                                0, index++, true);
                si.init(gameManager);
                si.setMinimumBid(0);
                newPacket.addItem(si);
            }
        }

        return newPacket;
    }
}

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

//    <StartPacket name="sellPrivates" roundClass="net.sf.rails.game.specific._1862.StartRound_1862">
//    <Bidding initial="0" minimum="0" increment="5"/>
//    <Item name="E&amp;H" type="DummyPrivate" basePrice="0" />
//</StartPacket>

    
    public StartPacket getNextUnfinishedStartPacket() {
        StartPacket newPacket =
                StartPacket.create(this,
                        "StartPacket" + (startNumber++),
                        "net.sf.rails.game.specific._1862.StartRound_1862"); // TODO: Clean this up

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

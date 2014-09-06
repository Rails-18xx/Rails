package net.sf.rails.game.specific._1862;

import java.util.List;

import net.sf.rails.game.CompanyManager;
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
        
        StartPacket newPacket = StartPacket.create(originalPacket.getParent(), originalPacket.getId() + (startNumber++), 
                originalPacket.getRoundClassName());
        
        List<StartItem> items = originalPacket.getItems();
        
        for (StartItem item : items) {
            PublicCompany_1862 company = (PublicCompany_1862) getPublicCompany(item.getName());
            if (company.isStartable()) {
                item.setSecondary(company.getPresidentsShare());
                newPacket.addItem(item);
            }
        }
        
        return newPacket;
    }
}

package net.sf.rails.game.specific._1862;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StartPacket;

public class CompanyManager_1862 extends CompanyManager {

    public CompanyManager_1862(RailsRoot parent, String id) {
        super(parent, id);
    }
    
    public StartPacket getStartPacket() {
        StartPacket_1862 startPacket = StartPacket_1862.create(this, "parliamentCompanies", 
                "net.sf.rails.game.StartRound_1830");
        
        int index = 0;
        for (PublicCompany c : getAllPublicCompanies()) {
            if (((PublicCompany_1862) c).isStartable()) {
                startPacket.addCompany(c.getLongName(), index++, gameManager);
            }
        }
        
        return startPacket;        
    }
    
}

package net.sf.rails.game.specific._1862;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.StartPacket;

public class StartPacket_1862 extends StartPacket {

    private StartPacket_1862(RailsItem parent, String id, String roundClassName) {
        super(parent, id, roundClassName);
    }
    
    public static StartPacket_1862 create(RailsItem parent, String id, String roundClassName) {
        return new StartPacket_1862(parent, id, roundClassName);
    }

    public void addCompany(String name, int index, GameManager gameManager) {
        StartItem_1862 item = StartItem_1862.create(this, name, "Public", 0, index, true); 
        item.init(gameManager);
        // TODO: Get "Public" from someplace better
        items.add(item);
    }
     

    

}

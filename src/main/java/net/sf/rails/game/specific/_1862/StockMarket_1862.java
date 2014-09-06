package net.sf.rails.game.specific._1862;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StockMarket;
import net.sf.rails.game.StockSpace;

public class StockMarket_1862 extends StockMarket {

    public StockMarket_1862(RailsRoot parent, String id) {
        super(parent, id);
    }
    
    public void finishConfiguration (RailsRoot root) {
        super.finishConfiguration(root);
        // TODO: Temporary
        startSpaces.clear();
        startSpaces.add(StockSpace.create(this, "54", 54, null));
        startSpaces.add(StockSpace.create(this, "58", 58, null));
        startSpaces.add(StockSpace.create(this, "62", 62, null));
        startSpaces.add(StockSpace.create(this, "68", 68, null));
        startSpaces.add(StockSpace.create(this, "74", 74, null));
        startSpaces.add(StockSpace.create(this, "82", 82, null));
        startSpaces.add(StockSpace.create(this, "90", 90, null));
        startSpaces.add(StockSpace.create(this, "100", 100, null));
    }


}

package net.sf.rails.game.model;

import net.sf.rails.game.financial.StockMarket;

public class StockMarketModel extends RailsModel {

    public static final String ID = "StockMarketModel";
    
    private StockMarketModel(StockMarket parent, String id) {
        super(parent, id);
    }

    public static StockMarketModel create (StockMarket parent) {
        return new StockMarketModel(parent, ID);
    }
    
}

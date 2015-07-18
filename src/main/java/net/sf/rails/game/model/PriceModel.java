package net.sf.rails.game.model;

import java.awt.Color;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Bank;
import net.sf.rails.game.StockSpace;
import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.GenericState;

public final class PriceModel extends ColorModel {

    // FIXME: Remove duplication of company and parent
    private final PublicCompany company;

    private final GenericState<StockSpace> stockPrice = GenericState.create(this, "stockPrice");
    
    private PriceModel(PublicCompany parent, String id) {
        super(parent, id);
        company = parent;
    }

    public static PriceModel create(PublicCompany parent, String id){
        return new PriceModel(parent, id);
    }
    
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)super.getParent();
    }
    
    public void setPrice(StockSpace price) {
        stockPrice.set(price);
    }

    public StockSpace getPrice() {
        return stockPrice.value();
    }

    public PublicCompany getCompany() {
        return company;
    }

    @Override
    public Color getBackground() {
        if (stockPrice.value() != null) {
            return stockPrice.value().getColour();
        } else {
            return null;
        }
    }

    @Override
    public Color getForeground() {
        return null;
    }

    @Override
    public String toText() {
        if (stockPrice.value() != null) {
            return Bank.format(getParent(), stockPrice.value().getPrice()) + " ("
                   + stockPrice.value().getId() + ")";
        }
        return "";
    }

}

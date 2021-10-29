package net.sf.rails.game.model;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;

import java.awt.*;

public final class PriceModel extends ColorModel {

    // FIXME: Remove duplication of company and parent
    private final PublicCompany company;

    /** Share price as represented by a field on the stock market (StockSpace) */
    private final GenericState<StockSpace> stockPrice = new GenericState<>(this, "stockPrice");

    /** Share price as represented by an integer value.
     * Used in 1837 for fixed minor buy cost (face price) in a stock round. */
    private final IntegerState facePrice = IntegerState.create (this, "facePrice");

    private boolean showCoordinates = true;

    private PriceModel(PublicCompany parent, String id, boolean showCoordinates) {
        super(parent, id);
        company = parent;
        this.showCoordinates = showCoordinates;
    }

    public static PriceModel create(PublicCompany parent, String id, boolean showCoordinates) {
        return new PriceModel(parent, id, showCoordinates);
    }

    @Override
    public PublicCompany getParent() {
        return (PublicCompany) super.getParent();
    }

    public void setPrice(StockSpace price) {
        stockPrice.set(price);
    }

    public void setPrice (int price) { facePrice.set(price); }

    public StockSpace getPrice() {
        return stockPrice.value();
    }

    public int getPriceAsInt() { return facePrice.value(); }

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
        if (getPriceAsInt() == 0) {
            return null;
        } else {
            return Color.GRAY;
        }
    }

    @Override
    public String toText() {
        String text = "";
        if (stockPrice.value() != null) {
            text = Bank.format(getParent(), stockPrice.value().getPrice());
            if (showCoordinates) {
                text += " (" + stockPrice.value().getId() + ")";
            }
        } else if (facePrice.value() > 0) {
            text = Bank.format(getParent(), facePrice.value());
        }
        return text;
    }

}

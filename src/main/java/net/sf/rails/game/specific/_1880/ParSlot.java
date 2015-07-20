package net.sf.rails.game.specific._1880;

import java.awt.Color;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.StringState;

/**
 * A ParSlot allows one public company to start at a certain par price
 */

public class ParSlot extends RailsAbstractItem {

    private final int price;
    private final int index;
    private final GenericState<PublicCompany> company = GenericState.create(this, "company");
    private final StringState lastTrain = StringState.create(this, "lastTrain");
    private final ColorModel companyColors;
    
    protected ParSlot(ParSlotManager parent, String id, int price, int index) {
        super(parent, id);
        this.price = price;
        this.index = index;
        
        companyColors = new ColorModel(this, "companyColors") {
            @Override
            public Color getBackground() {
                if (company.value() != null) {
                    return company.value().getBgColour();
                } else {
                    return null;
                }
            }

            @Override
            public Color getForeground() {
                if (company.value() != null) {
                    return company.value().getFgColour();
                } else {
                    return null;
                }
            }
        };
        company.addModel(companyColors);
    }
    
    public int getPrice() {
        return price;
    }
    
    public int getIndex() {
        return index;
    }
    
    public GenericState<PublicCompany> getCompany() {
        return company;
    }
    
    public ColorModel getCompanyColors() {
        return companyColors;
     }
    
    public boolean isEmpty() {
        return company.value() == null;
    }
    
    public StringState getLastTrain() {
        return lastTrain;
    }

    void setCompany(PublicCompany company) {
        this.company.set(company);
    }

}

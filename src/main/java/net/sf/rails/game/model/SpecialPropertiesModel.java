package net.sf.rails.game.model;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bonus;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.special.LocatedBonus;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Change;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.SetChange;
import net.sf.rails.game.state.PortfolioSet;
import net.sf.rails.game.state.Triggerable;

public class SpecialPropertiesModel extends RailsModel implements Triggerable {

    public final static String ID = "SpecialPropertiesModel";

    private final PortfolioSet<SpecialProperty> specialProperties;

    private SpecialPropertiesModel(RailsOwner parent) {
        super(parent, ID);
        // specialProperties have the Owner as parent directly
        specialProperties = PortfolioSet.create(parent, "specialProperties", SpecialProperty.class);
        // so make this model updating
        specialProperties.addModel(this);
        // and add it as triggerable
        specialProperties.addTrigger(this);
    }
    
    public static SpecialPropertiesModel create(RailsOwner parent) {
        return new SpecialPropertiesModel(parent);
    }
    
    @Override
    public RailsOwner getParent() {
        return (RailsOwner)super.getParent();
    }

    PortfolioSet<SpecialProperty> getPortfolio() {
        return specialProperties;
    }
    
    // triggerable interface
    
    public void triggered(Observable observable, Change change) {
        
        // checks if the specialproperty moved into the portfolio carries a LocatedBonus

        if (!(change instanceof SetChange)) {
            return;
        }
        
        @SuppressWarnings("rawtypes")
        SetChange sChange = (SetChange)change;
        if (!sChange.isAddToSet()) return;

        SpecialProperty property = (SpecialProperty)sChange.getElement();
        if (getParent() instanceof PublicCompany && property instanceof LocatedBonus) {
            PublicCompany company = (PublicCompany)getParent();
            LocatedBonus locBonus = (LocatedBonus)property;
            Bonus bonus = new Bonus(company, locBonus.getId(), locBonus.getValue(),
                    locBonus.getLocations());
            company.addBonus(bonus);
            ReportBuffer.add(this, LocalText.getText("AcquiresBonus",
                    getParent().getId(),
                    locBonus.getName(),
                    Bank.format(company, locBonus.getValue()),
                    locBonus.getLocationNameString()));
        }
    }

}

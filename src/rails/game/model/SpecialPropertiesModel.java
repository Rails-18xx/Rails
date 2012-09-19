package rails.game.model;

import rails.common.LocalText;
import rails.game.Bonus;
import rails.game.Currency;
import rails.game.PublicCompany;
import rails.game.ReportBuffer;
import rails.game.state.Change;
import rails.game.state.Model;
import rails.game.state.Observable;
import rails.game.state.Owner;
import rails.game.state.PortfolioChange;
import rails.game.state.PortfolioSet;
import rails.game.state.Triggerable;
import rails.game.special.LocatedBonus;
import rails.game.special.SpecialProperty;

public class SpecialPropertiesModel extends Model implements Triggerable {

    public final static String ID = "SpecialPropertiesModel";

    private final PortfolioSet<SpecialProperty> specialProperties;

    private SpecialPropertiesModel(Owner parent) {
        super(parent, ID);
        // specialProperties have the Owner as parent directly
        specialProperties = PortfolioSet.create(parent, "specialProperties", SpecialProperty.class);
        // so make this model updating
        specialProperties.addModel(this);
        // and add it as triggerable
        specialProperties.addTrigger(this);
    }
    
    public static SpecialPropertiesModel create(Owner parent) {
        return new SpecialPropertiesModel(parent);
    }
    
    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }

    PortfolioSet<SpecialProperty> getPortfolio() {
        return specialProperties;
    }
    
    // triggerable interface
    
    public void triggered(Observable observable, Change change) {
        
        // checks if the specialproperty moved into the portfolio carries a LocatedBonus
        @SuppressWarnings("rawtypes")
        PortfolioChange pchange = (PortfolioChange)change;
        if (!pchange.isIntoPortfolio()) return;

        SpecialProperty property = (SpecialProperty)pchange.getItem();
        if (getParent() instanceof PublicCompany && property instanceof LocatedBonus) {
            PublicCompany company = (PublicCompany)getParent();
            LocatedBonus locBonus = (LocatedBonus)property;
            Bonus bonus = new Bonus(company, locBonus.getId(), locBonus.getValue(),
                    locBonus.getLocations());
            company.addBonus(bonus);
            ReportBuffer.add(LocalText.getText("AcquiresBonus",
                    getParent().getId(),
                    locBonus.getName(),
                    Currency.format(company, locBonus.getValue()),
                    locBonus.getLocationNameString()));
        }
    }

}

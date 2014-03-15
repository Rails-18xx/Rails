package rails.game.specific._1880;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartItem;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;

/**
 * 
 * Rails 2.0: Updated equals and toString methods
 */

public class SetupNewPublicDetails_1880 extends StartItemAction {

    private static final long serialVersionUID = 1L;

    transient protected PublicCompany company;
    protected String companyName;
    private int price = 0;
    private int shares = 0;
    private int parSlotIndex = 0;
    private String buildRightsString = "";

    private SetupNewPublicDetails_1880(StartItem startItem,
            PublicCompany company) {
        super(startItem);
        this.company = company;
        this.companyName = company.getId();
    }

    public SetupNewPublicDetails_1880(StartItem startItem,
            PublicCompany company, int price, int shares) {
        this(startItem, company);
        this.price = price;
        this.shares = shares;
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
    }

    public PublicCompany getCompany() {
        return company;
    }

    public int getPrice() {
        return price;
    }

    public int getShares() {
        return shares;
    }

    public int getParSlotIndex() {
        return parSlotIndex;
    }

    public void setParSlotIndex(int parSlotIndex) {
        this.parSlotIndex = parSlotIndex;
    }

    public String getBuildRightsString() {
        return buildRightsString;
    }

    public void setBuildRightsString(String buildRightsString) {
        this.buildRightsString = buildRightsString;
    }

    public Object getCompanyName() {
        return companyName;
    }
    
    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAsOption(pa)) return false; 

        // check further attributes
        SetupNewPublicDetails_1880 action = (SetupNewPublicDetails_1880)pa; 
        return Objects.equal(this.company, action.company)
                && Objects.equal(this.price, action.price)
                && Objects.equal(this.shares, action.shares)
        ;
    }

    @Override
    public boolean equalsAsAction(PossibleAction pa) { // TODO
        // first check if equal as option
        if (!this.equalsAsOption(pa)) return false;
        
        // check further attributes
        SetupNewPublicDetails_1880 action = (SetupNewPublicDetails_1880)pa; 
        return Objects.equal(this.parSlotIndex, action.parSlotIndex)
                && Objects.equal(this.buildRightsString, action.buildRightsString)
        ;
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                .addToString("company", company)
                .addToString("price", price)
                .addToString("shares", shares)
                .addToStringOnlyActed("parSlotIndex", parSlotIndex)
                .addToStringOnlyActed("buildRightsString", buildRightsString)
                .toString()
        ;
    }



}

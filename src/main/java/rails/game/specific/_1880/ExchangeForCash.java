package rails.game.specific._1880;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.TrainType;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class ExchangeForCash extends PossibleAction {
    private static final Map<String, Integer> CASH_VALUE_MAP = 
            ImmutableMap.of("2+2", 40, "3", 70, "3+3", 100);

    private static final Map<String, Boolean> CHOICE_MAP = 
            ImmutableMap.of("2+2", true, "3", true, "3+3", false );
           
    private static final long serialVersionUID = 1L;
    private transient Owner owner;
    private String ownerName;
    private int value = 0;
    private transient boolean ownerHasChoice;
    // TODO: What is the function of exchangeCompany?
    private boolean exchangeCompany = false;
    
    private ExchangeForCash(PrivateCompany company, int value, boolean ownerHasChoice) {
        super(null); // not defined by an activity yet
        this.owner = company.getOwner();
        this.ownerName = owner.getId();
        this.value = value;
        this.ownerHasChoice = ownerHasChoice;
    }

    public static ExchangeForCash getAction(PrivateCompany company, TrainType soldOutTrainType) {
        ExchangeForCash action = null;
        String trainName = soldOutTrainType.getName();
        if (CASH_VALUE_MAP.containsKey(trainName) == true) {
            action = new ExchangeForCash(company, CASH_VALUE_MAP.get(trainName), CHOICE_MAP.get(trainName));
        }
        return action;
    }
    

    public String getOwnerName() {
        return ownerName;
    }

    public int getCashValue() {
        return value;
    }
    
    public boolean getOwnerHasChoice() {
        return ownerHasChoice;
    }

    public boolean getExchangeCompany() {
        return exchangeCompany;
    }

    public void setExchangeCompany(boolean exchangeCompany) {
        this.exchangeCompany = exchangeCompany;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        ExchangeForCash action = (ExchangeForCash)pa; 
        boolean options = 
                Objects.equal(this.owner, action.owner)
                && Objects.equal(this.value, action.value)
             // not stored:                && Objects.equal(this.ownerHasChoice, action.ownerHasChoice)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.exchangeCompany, action.exchangeCompany)
        ;
    }

    @Override
    public String toString() {
        return super.toString() 
                + RailsObjects.stringHelper(this)
                .addToString("owner", owner)
                .addToString("value", value)
                .addToStringOnlyActed("exchangeCompany", exchangeCompany)
        ;
    }
    
    // deserialize to assign owner
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        if (Util.hasValue(ownerName)) {
            owner = getRoot().getPlayerManager().getPlayerByName(ownerName);
        } 
    }
}

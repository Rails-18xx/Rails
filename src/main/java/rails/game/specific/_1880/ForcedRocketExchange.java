package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import net.sf.rails.game.Train;
import net.sf.rails.game.specific._1880.PublicCompany_1880;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public class ForcedRocketExchange extends PossibleORAction {
    private static final long serialVersionUID = 1L;
    
    // FIXME: Do not use strings to indicate companies
    private transient List<String> companiesWithSpace = new ArrayList<String>();
    private transient Map<String, List<Train>> companiesWithNoSpace = new HashMap<String, List<Train>>();

    private String companyToReceiveTrain;
    private String trainToReplace;
        
    public ForcedRocketExchange() {
        companyToReceiveTrain = null;
        trainToReplace = null;
    }

    public void addCompanyWithSpace(PublicCompany_1880 company) {
        companiesWithSpace.add(company.getId());
    }    
    
    public List<String> getCompaniesWithSpace() {
        return companiesWithSpace;
    }
    
    public boolean hasCompaniesWithSpace() {
        return (companiesWithSpace.isEmpty() == false);
    }

    public void addCompanyWithNoSpace(PublicCompany_1880 company) {
        List<Train> trains = Lists.newArrayList(company.getPortfolioModel().getUniqueTrains());
        companiesWithNoSpace.put(company.getId(), trains);
    }

    public Map<String, List<Train>> getCompaniesWithNoSpace() {
        return companiesWithNoSpace;
    }
    
    public String getCompanyToReceiveTrain() {
        return companyToReceiveTrain;
    }

    public void setCompanyToReceiveTrain(String companyToReceiveTrain) {
        this.companyToReceiveTrain = companyToReceiveTrain;
    }
    
    public String getTrainToReplace() {
        return trainToReplace;
    }
    
    public void setTrainToReplace(String trainToReplace) {
        this.trainToReplace = trainToReplace;        
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // no asOption attributes
        if (asOption) return true;
        
        // check asAction attributes
        ForcedRocketExchange action = (ForcedRocketExchange)pa; 
        return Objects.equal(this.companyToReceiveTrain, action.companyToReceiveTrain)
                && Objects.equal(this.trainToReplace, action.trainToReplace)
        ;
    }

    @Override
    public String toString() {
        // TODO: does not print the static values, as they do not get serialized
        return super.toString() 
                + RailsObjects.stringHelper(this)
                .addToStringOnlyActed("companyToReceiveTrain", companyToReceiveTrain)
                .addToStringOnlyActed("trainToReplace", trainToReplace)
                .toString()
        ;
    }
    
}

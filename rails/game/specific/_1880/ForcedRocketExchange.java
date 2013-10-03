package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.game.TrainI;
import rails.game.TrainType;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;

/**
 * @author Michael Alexander
 * 
 */
public class ForcedRocketExchange extends PossibleORAction {
    
    private static final long serialVersionUID = 1L;
    private transient List<String> companiesWithSpace = new ArrayList<String>();
    private transient Map<String, List<TrainI>> companiesWithNoSpace = new HashMap<String, List<TrainI>>();

    private String companyToReceiveTrain;
    private String trainToReplace;
        
    public ForcedRocketExchange() {
        companyToReceiveTrain = null;
        trainToReplace = null;
    }

    public void addCompanyWithSpace(PublicCompany_1880 company) {
        companiesWithSpace.add(company.getName());
    }    
    
    public List<String> getCompaniesWithSpace() {
        return companiesWithSpace;
    }
    
    public boolean hasCompaniesWithSpace() {
        return (companiesWithSpace.isEmpty() == false);
    }

    public void addCompanyWithNoSpace(PublicCompany_1880 company) {
        List<TrainI> trains = company.getPortfolio().getUniqueTrains();
        companiesWithNoSpace.put(company.getName(), trains);
    }

    public Map<String, List<TrainI>> getCompaniesWithNoSpace() {
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

    
    public String toString() {
        return("ForcedRocketExchange");
    }


    @Override
    public boolean equalsAsOption(PossibleAction pa) {
//        if (pa instanceof ForcedRocketExchange) {
//            if ((((ForcedRocketExchange) pa).getInvestor() == company) &&
//                    (((ForcedRocketExchange) pa).getTreasuryToLinkedCompany() == treasuryToLinkedCompany) &&
//                    (((ForcedRocketExchange) pa).getReplaceToken() == replaceToken)) {
//                return true;
//            }
//        }
        return true;
    }

    @Override
    public boolean equalsAsAction(PossibleAction pa) {
//        if (pa instanceof ForcedRocketExchange) {
//            if ((((ForcedRocketExchange) pa).getInvestor() == company) &&
//                    (((ForcedRocketExchange) pa).getTreasuryToLinkedCompany() == treasuryToLinkedCompany) &&
//                    (((ForcedRocketExchange) pa).getReplaceToken() == replaceToken)) {
//                return true;
//            }
//        }
        return true;
    }




}

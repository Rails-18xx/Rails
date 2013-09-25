package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;

/**
 * @author Michael Alexander
 * 
 */
public class ForcedRocketExchange extends PossibleORAction {
    
    private static final long serialVersionUID = 1L;
    private List<String> companiesWithSpace = new ArrayList<String>();

    private String companyToReceiveTrain;
        
    public ForcedRocketExchange() {
        companyToReceiveTrain = "";
    }

    public void addCompanyWithSpace(PublicCompany_1880 company) {
        companiesWithSpace.add(company.getName());
    }    
    
    public List<String> getCompaniesWithSpace() {
        return companiesWithSpace;
    }

    public void addCompanyWithNoSpace(PublicCompany_1880 company) {
        // TODO Auto-generated method stub
    }

    public String getCompanyToReceiveTrain() {
        return companyToReceiveTrain;
    }

    public void setCompanyToReceiveTrain(String companyToReceiveTrain) {
        this.companyToReceiveTrain = companyToReceiveTrain;
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

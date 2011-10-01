package rails.game.specific._1825;

import java.util.*;

import rails.game.*;

public class OperatingRound_1825 extends OperatingRound {

    public OperatingRound_1825(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public List<PublicCompany> setOperatingCompanies() {
        Map<Integer, PublicCompany> operatingCompanies = new TreeMap<Integer, PublicCompany>();
        int space;
        int key;
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            PublicCompany_1825 companycasted = (PublicCompany_1825)company;
            if (!canCompanyOperateThisRound(companycasted)) continue;    
            if (!canCompanyOperateThisRound(company)) continue;
            // Key must put companies in reverse operating order, because sort
            // is ascending.
            space = companycasted.getIPOPrice();
            //Corps operate in descending IPO price
            //Corps with the same IPO price operate in the order they were floated
            //IPO price will inherently be in the right order
            //subtracting the formation order index will put it at the right point to operate
            //This wouldn't work if there are lots of corps at the same price
            //there are not too many corps in each banding for this to be an issue in 1825 even with all 3 units
            key = 1000000 - (space - companycasted.getFormationOrderIndex());
            operatingCompanies.put(new Integer(key), companycasted);
            }
        return new ArrayList<PublicCompany>(operatingCompanies.values());
     }

    @Override
    public List<PublicCompany> setOperatingCompanies(List<PublicCompany> oldOperatingCompanies,
            PublicCompany lastOperatingCompany) {
        return setOperatingCompanies();
    }
}
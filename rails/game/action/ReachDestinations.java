package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.CompanyManagerI;
import rails.game.Game;
import rails.game.PublicCompanyI;

/** This class is needed until we have a means to determine reaching
 * destinations automatically.
 * @author Erik
 *
 */
public class ReachDestinations extends PossibleORAction {

    // Server-side settings
    transient protected List<PublicCompanyI> possibleCompanies;
    protected String possibleCompanyNames = "";

    // Client-side settings
    transient protected List<PublicCompanyI> reachedCompanies;
    protected String reachedCompanyNames = "";

    public static final long serialVersionUID = 1L;
    
    public ReachDestinations (List<PublicCompanyI> companies) {
        possibleCompanies = companies;
        StringBuffer b = new StringBuffer(); 
        for (PublicCompanyI company : companies) {
            if (b.length() > 0) b.append(",");
            b.append (company.getName());
        }
        possibleCompanyNames = b.toString();
    }
    
    /** Required for deserialization */
    public ReachDestinations() {}

    public List<PublicCompanyI> getPossibleCompanies() {
        return possibleCompanies;
    }
    
    public String getPossibleCompanyNames () {
        return possibleCompanyNames;
    }
    
    public void addReachedCompany (PublicCompanyI company) {
        if (reachedCompanies == null) 
            reachedCompanies = new ArrayList<PublicCompanyI>();
        reachedCompanies.add (company);
        if (reachedCompanyNames.length() > 0) {
            reachedCompanyNames += ",";
        }
        reachedCompanyNames += company.getName();
    }

    public List<PublicCompanyI> getReachedCompanies() {
        return reachedCompanies;
    }
    
   

    @Override
    public boolean equals(PossibleAction pa) {
        if (!(pa instanceof ReachDestinations)) return false;
        ReachDestinations rd = (ReachDestinations) pa;
        return possibleCompanyNames.equals(rd.getPossibleCompanyNames());
    }

    @Override
    public String toString() {
        StringBuffer text = new StringBuffer();
        text.append("ReachDestinations: ").append(possibleCompanyNames);
        if (reachedCompanyNames.length() > 0) {
            text.append(" reached="+reachedCompanyNames);
        }
        return text.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        CompanyManagerI cmgr = Game.getCompanyManager();

        possibleCompanies = new ArrayList<PublicCompanyI>();
        for (String cname : possibleCompanyNames.split(",")) {
            possibleCompanies.add(cmgr.getCompanyByName(cname));
        }
        reachedCompanies = new ArrayList<PublicCompanyI>();
        for (String cname : reachedCompanyNames.split(",")) {
            reachedCompanies.add(cmgr.getCompanyByName(cname));
        }
    }
}

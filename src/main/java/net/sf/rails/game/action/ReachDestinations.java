package net.sf.rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.Util;


/** This class is needed until we have a means to determine reaching
 * destinations automatically.
 * @author Erik
 *
 */
public class ReachDestinations extends PossibleORAction {

    // Server-side settings
    transient protected List<PublicCompany> possibleCompanies;
    protected String possibleCompanyNames = "";

    // Client-side settings
    transient protected List<PublicCompany> reachedCompanies;
    protected String reachedCompanyNames = "";

    public static final long serialVersionUID = 1L;

    public ReachDestinations (List<PublicCompany> companies) {
        possibleCompanies = companies;
        StringBuffer b = new StringBuffer();
        for (PublicCompany company : companies) {
            if (b.length() > 0) b.append(",");
            b.append (company.getId());
        }
        possibleCompanyNames = b.toString();
    }

    /** Required for deserialization */
    public ReachDestinations() {}

    public List<PublicCompany> getPossibleCompanies() {
        return possibleCompanies;
    }

    public String getPossibleCompanyNames () {
        return possibleCompanyNames;
    }

    public void addReachedCompany (PublicCompany company) {
        if (reachedCompanies == null)
            reachedCompanies = new ArrayList<PublicCompany>();
        reachedCompanies.add (company);
        if (reachedCompanyNames.length() > 0) {
            reachedCompanyNames += ",";
        }
        reachedCompanyNames += company.getId();
    }

    public List<PublicCompany> getReachedCompanies() {
        return reachedCompanies;
    }

    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        if (!(pa instanceof ReachDestinations)) return false;
        ReachDestinations rd = (ReachDestinations) pa;
        return possibleCompanyNames.equals(rd.getPossibleCompanyNames());
    }

     @Override
    public boolean equalsAsAction(PossibleAction pa) {
        if (!(pa instanceof ReachDestinations)) return false;
        ReachDestinations rd = (ReachDestinations) pa;
        return possibleCompanyNames.equals(rd.possibleCompanyNames)
                && reachedCompanyNames.equals(rd.reachedCompanyNames);
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

        CompanyManager cmgr = getCompanyManager();

        possibleCompanies = new ArrayList<PublicCompany>();
        if (Util.hasValue(possibleCompanyNames)) {
            for (String cname : possibleCompanyNames.split(",")) {
                if (!"".equals(cname)) {
                    possibleCompanies.add(cmgr.getPublicCompany(cname));
                }
            }
        }
        reachedCompanies = new ArrayList<PublicCompany>();
        if (Util.hasValue(reachedCompanyNames)) {
            for (String cname : reachedCompanyNames.split(",")) {
                if (!"".equals(cname)) {
                    reachedCompanies.add(cmgr.getPublicCompany(cname));
                }
            }
        }
    }
}

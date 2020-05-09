package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Objects;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;


/** This class is needed until we have a means to determine reaching
 * destinations automatically.
 *
 * Rails 2.0: updated equals and toString methods
 */
public class ReachDestinations extends PossibleORAction {

    // Server-side settings
    transient protected List<PublicCompany> possibleCompanies;
    protected String possibleCompanyNames;

    // Client-side settings
    transient protected List<PublicCompany> reachedCompanies;
    protected String reachedCompanyNames = "";

    public static final long serialVersionUID = 1L;

    public ReachDestinations (RailsRoot root, @NotNull List<PublicCompany> possibleCompanies) {
        super(root);
        this.possibleCompanies = possibleCompanies;
        StringBuilder b = new StringBuilder();
        for (PublicCompany company : possibleCompanies) {
            if (b.length() > 0) b.append(",");
            b.append (company.getId());
        }
        possibleCompanyNames = b.toString();
    }

    public List<PublicCompany> getPossibleCompanies() {
        return possibleCompanies;
    }

    public String getPossibleCompanyNames () {
        return possibleCompanyNames;
    }

    public void addReachedCompany (PublicCompany company) {
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
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        ReachDestinations action = (ReachDestinations)pa;
        boolean options = Objects.equal(this.possibleCompanies, action.possibleCompanies);

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options && Objects.equal(this.reachedCompanies, action.reachedCompanies);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("possibleCompanies", possibleCompanies)
                    .addToStringOnlyActed("reachedCompanies", reachedCompanies)
                    .toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        CompanyManager cmgr = getCompanyManager();

        possibleCompanies = new ArrayList<>();
        if (Util.hasValue(possibleCompanyNames)) {
            for (String cname : possibleCompanyNames.split(",")) {
                if ( StringUtils.isNotBlank(cname) ) {
                    possibleCompanies.add(cmgr.getPublicCompany(cname));
                }
            }
        }

        if (Util.hasValue(reachedCompanyNames)) {
            reachedCompanies = new ArrayList<>();
            for (String cname : reachedCompanyNames.split(",")) {
                if ( StringUtils.isNotBlank(cname) ) {
                    reachedCompanies.add(cmgr.getPublicCompany(cname));
                }
            }
        }
    }
}

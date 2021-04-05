/**
 *
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.rails.util.GameLoader;
import org.jetbrains.annotations.NotNull;

import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

import com.google.common.base.Objects;

/**
 * @author martin
 *
 */
public class FoldIntoNational extends PossibleAction {

    // Server settings
    protected transient List<Company> foldableCompanies;
    protected String foldableCompanyNames;

    // Client settings
    protected transient List<Company> foldedCompanies = null;
    protected String foldedCompanyNames = null;

    public static final long serialVersionUID = 1L;

    public FoldIntoNational(RailsRoot root, @NotNull List<Company> foldableCompanies) {
        super(root); // not defined by an activity yet
        this.foldableCompanies = foldableCompanies;
        foldableCompanyNames = Util.joinNames(foldableCompanies, ",");
    }

    public FoldIntoNational(Company company) {
        this (company.getRoot(), Arrays.asList(company));
    }

    public List<Company> getFoldedCompanies() {
        return foldedCompanies;
    }


    public void setFoldedCompanies(List<Company> foldedCompanies) {
        this.foldedCompanies = foldedCompanies;
        foldedCompanyNames = Util.joinNames(foldedCompanies, ",");
    }

    public List<Company> getFoldableCompanies() {
        return foldableCompanies;
    }


    public String getFoldableCompanyNames() {
        return foldableCompanyNames;
    }


    public String getFoldedCompanyNames() {
        return foldedCompanyNames;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        FoldIntoNational action = (FoldIntoNational)pa;
        boolean options =
                Objects.equal(this.foldableCompanies, action.foldableCompanies) ||
                    // additional conditions required as there is no sorting defined in old save files
                    this.foldableCompanies != null && action.foldableCompanies != null
                    && this.foldableCompanies.containsAll(action.foldableCompanies)
                    && action.foldableCompanies.containsAll(this.foldableCompanies)
        ;

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
                && Objects.equal(this.foldedCompanies, action.foldedCompanies) ||
                // additional conditions required as there is no sorting defined in old save files
                this.foldedCompanies != null && action.foldedCompanies != null
                && this.foldedCompanies.containsAll(action.foldedCompanies)
                && action.foldedCompanies.containsAll(this.foldedCompanies)
        ;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("foldableCompanies", foldableCompanies)
                    .addToStringOnlyActed("foldedCompanies", foldedCompanies)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (in instanceof GameLoader.RailsObjectInputStream) {
            Company company;
            CompanyManager cmgr = getCompanyManager();

            foldableCompanies = new ArrayList<>();
            if (foldableCompanyNames != null) {
                for (String name : foldableCompanyNames.split(",")) {
                    company = cmgr.getPublicCompany(name);
                    if (company == null) company = cmgr.getPrivateCompany(name);
                    if (company != null) foldableCompanies.add(company);
                }
            }
            if (Util.hasValue(foldedCompanyNames)) {
                foldedCompanies = new ArrayList<>();
                for (String name : foldedCompanyNames.split(",")) {
                    company = cmgr.getPublicCompany(name);
                    if (company == null) company = cmgr.getPrivateCompany(name);
                    if (company != null) foldedCompanies.add(company);
                }
            }
        }
    }

    public void applyRailsRoot(RailsRoot root) {
        super.applyRailsRoot(root);

        Company company;
        CompanyManager cmgr = getCompanyManager();

        foldableCompanies = new ArrayList<>();
        if (foldableCompanyNames != null) {
            for (String name : foldableCompanyNames.split(",")) {
                company = cmgr.getPublicCompany(name);
                if (company == null) company = cmgr.getPrivateCompany(name);
                if (company != null) foldableCompanies.add(company);
            }
        }
        if (Util.hasValue(foldedCompanyNames)) {
            foldedCompanies = new ArrayList<>();
            for (String name : foldedCompanyNames.split(",")) {
                company = cmgr.getPublicCompany(name);
                if (company == null) company = cmgr.getPrivateCompany(name);
                if (company != null) foldedCompanies.add(company);
            }
        }
    }

}

package rails.game.specific._1835;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rails.game.*;
import rails.game.action.PossibleAction;
import rails.util.Util;

public class FoldIntoPrussian extends PossibleAction {

    // Server settings
    protected transient List<CompanyI> foldableCompanies = null;
    protected String foldableCompanyNames = null;

    // Client settings
    protected transient List<CompanyI> foldedCompanies = null;
    protected String foldedCompanyNames = null;

    public static final long serialVersionUID = 1L;

    public FoldIntoPrussian(List<CompanyI> companies) {
        this.foldableCompanies = companies;
        foldableCompanyNames = Company.joinNamesWithDelimiter (foldableCompanies, ",");
    }

    public FoldIntoPrussian(CompanyI company) {
        this (Arrays.asList(new CompanyI[] {company}));
    }

    public List<CompanyI> getFoldedCompanies() {
        return foldedCompanies;
    }


    public void setFoldedCompanies(List<CompanyI> foldedCompanies) {
        this.foldedCompanies = foldedCompanies;
        foldedCompanyNames = Company.joinNamesWithDelimiter (foldedCompanies, ",");
    }


    public List<CompanyI> getFoldableCompanies() {
        return foldableCompanies;
    }


    public String getFoldableCompanyNames() {
        return foldableCompanyNames;
    }


    public String getFoldedCompanyNames() {
        return foldedCompanyNames;
    }


    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("Fold into Prussian:");
        if (foldableCompanyNames != null) {
            text.append(" foldable=").append(foldableCompanyNames);
        }
        if (foldedCompanyNames != null) {
            text.append(" folded=").append(foldedCompanyNames);
        }
        return text.toString();
    }
    
    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof FoldIntoPrussian)) return false;
        FoldIntoPrussian a = (FoldIntoPrussian) action;
        return a.foldableCompanyNames.equals(foldableCompanyNames);
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof FoldIntoPrussian)) return false;
        FoldIntoPrussian a = (FoldIntoPrussian) action;
        return a.equalsAsOption(this) && a.foldedCompanyNames.equals(foldedCompanyNames);
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        
        CompanyI company;

        in.defaultReadObject();

        CompanyManagerI cmgr = getCompanyManager();
        if (foldableCompanyNames != null) {
            foldableCompanies = new ArrayList<CompanyI>();
            for (String name : foldableCompanyNames.split(",")) {
                foldableCompanies.add(cmgr.getPublicCompany(name));
            }
        }
        if (Util.hasValue(foldedCompanyNames)) {
            foldedCompanies = new ArrayList<CompanyI>();
            for (String name : foldedCompanyNames.split(",")) {
                company = cmgr.getPublicCompany(name);
                if (company == null) company = cmgr.getPrivateCompany(name);
                if (company != null) foldedCompanies.add(company);
            }
        }
    }

}

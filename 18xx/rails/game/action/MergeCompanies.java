/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/MergeCompanies.java,v 1.3 2008/06/04 19:00:29 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class MergeCompanies extends PossibleAction {

    // Server-side settings
    transient protected PublicCompanyI mergingCompany;
    protected String mergingCompanyName;
    transient protected List<PublicCompanyI> targetCompanies;
    protected String targetCompanyNames;

    // Client-side settings
    transient protected PublicCompanyI selectedTargetCompany = null;
    protected String selectedTargetCompanyName = null;
    protected boolean replaceToken = false;

    public static final long serialVersionUID = 1L;

    /**
     * Common constructor.
     */
    public MergeCompanies(PublicCompanyI mergingCompany,
            List<PublicCompanyI> targetCompanies) {
        this.mergingCompany = mergingCompany;
        this.mergingCompanyName = mergingCompany.getName();
        this.targetCompanies = targetCompanies;
        StringBuffer b = new StringBuffer();
        for (PublicCompanyI target : targetCompanies) {
            if (b.length() > 0) b.append(",");
            if (target == null) {
                b.append("null");
            } else {
                b.append(target.getName());
            }
        }
        targetCompanyNames = b.toString();
    }

    /** Required for deserialization */
    public MergeCompanies() {}

    public PublicCompanyI getMergingCompany() {
        return mergingCompany;
    }

    public List<PublicCompanyI> getTargetCompanies() {
        return targetCompanies;
    }

    public PublicCompanyI getSelectedTargetCompany() {
        return selectedTargetCompany;
    }

    public void setSelectedTargetCompany(PublicCompanyI selectedTargetCompany) {
        this.selectedTargetCompany = selectedTargetCompany;
        if (selectedTargetCompany != null)
            selectedTargetCompanyName = selectedTargetCompany.getName();
    }

    public void setReplaceToken(boolean value) {
        replaceToken = value;
    }

    public boolean getReplaceToken() {
        return replaceToken;
    }

    @Override
    public boolean equals(PossibleAction action) {
        if (!(action instanceof MergeCompanies)) return false;
        MergeCompanies a = (MergeCompanies) action;
        return a.mergingCompanyName.equals(mergingCompanyName)
               && a.targetCompanyNames.equals(targetCompanyNames);
    }

    @Override
    public String toString() {
        StringBuffer text = new StringBuffer();
        text.append("MergeCompanies: ").append(mergingCompanyName).append(
                " targets=").append(targetCompanyNames);
        if (selectedTargetCompanyName != null) {
            text.append(" selectedTarget=").append(selectedTargetCompanyName).append(
                    " replaceToken=").append(replaceToken);
        }
        return text.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        CompanyManagerI cmgr = Game.getCompanyManager();

        mergingCompany = cmgr.getCompanyByName(mergingCompanyName);

        targetCompanies = new ArrayList<PublicCompanyI>();
        for (String name : targetCompanyNames.split(",")) {
            if (name.equals("null")) {
                targetCompanies.add(null);
            } else {
                targetCompanies.add(cmgr.getCompanyByName(name));
            }
        }

        if (selectedTargetCompanyName != null
            && !selectedTargetCompanyName.equals("null")) {
            selectedTargetCompany =
                    cmgr.getCompanyByName(selectedTargetCompanyName);
        }
    }
}

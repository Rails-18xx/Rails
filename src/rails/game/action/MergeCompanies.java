/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/MergeCompanies.java,v 1.9 2010/04/15 19:49:50 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rails.game.*;
import rails.game.action.PossibleAction;

/**
 * @author Erik Vos
 */
public class MergeCompanies extends PossibleAction {

    // Server-side settings
    transient protected PublicCompany mergingCompany;
    protected String mergingCompanyName;
    transient protected List<PublicCompany> targetCompanies;
    protected String targetCompanyNames;
    protected List<Boolean> canReplaceToken;

    // Client-side settings
    transient protected PublicCompany selectedTargetCompany = null;
    protected String selectedTargetCompanyName = null;
    protected boolean replaceToken = false;

    public static final long serialVersionUID = 1L;

    /**
     * Common constructor.
     */
    public MergeCompanies(PublicCompany mergingCompany,
            List<PublicCompany> targetCompanies) {
        this.mergingCompany = mergingCompany;
        this.mergingCompanyName = mergingCompany.getId();
        this.targetCompanies = targetCompanies;
        StringBuffer b = new StringBuffer();
        canReplaceToken = new ArrayList<Boolean>(targetCompanies.size());
        for (PublicCompany target : targetCompanies) {
            if (b.length() > 0) b.append(",");
            if (target == null) {
                b.append("null");
                canReplaceToken.add(false);
            } else {
                b.append(target.getId());
                MapHex hex = mergingCompany.getHomeHexes().get(0);
                canReplaceToken.add(target.getNumberOfFreeBaseTokens() > 0
                    && (!hex.hasTokenOfCompany(target)
                        || hex.getCurrentTile().allowsMultipleBasesOfOneCompany()
                            && hex.getCityOfBaseToken(mergingCompany)
                                != hex.getCityOfBaseToken(target)));
            }
        }
        targetCompanyNames = b.toString();
    }
    
    public MergeCompanies(PublicCompany mergingCompany,
            PublicCompany targetCompany) {
        this (mergingCompany, Arrays.asList(new PublicCompany[] {targetCompany}));
    }

    /** Required for deserialization */
    public MergeCompanies() {}

    public PublicCompany getMergingCompany() {
        return mergingCompany;
    }

    public List<PublicCompany> getTargetCompanies() {
        return targetCompanies;
    }

    public boolean canReplaceToken(int index) {
        return canReplaceToken.get(index);
    }

    public PublicCompany getSelectedTargetCompany() {
        return selectedTargetCompany;
    }

    public void setSelectedTargetCompany(PublicCompany selectedTargetCompany) {
        this.selectedTargetCompany = selectedTargetCompany;
        if (selectedTargetCompany != null)
            selectedTargetCompanyName = selectedTargetCompany.getId();
    }

    public void setReplaceToken(boolean value) {
        replaceToken = value;
    }

    public boolean getReplaceToken() {
        return replaceToken;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof MergeCompanies)) return false;
        MergeCompanies a = (MergeCompanies) action;
        return a.mergingCompanyName.equals(mergingCompanyName)
               && a.targetCompanyNames.equals(targetCompanyNames);
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof MergeCompanies)) return false;
        MergeCompanies a = (MergeCompanies) action;
        return a.mergingCompanyName.equals(mergingCompanyName)
               && a.selectedTargetCompanyName.equals(selectedTargetCompanyName)
               && a.replaceToken == replaceToken;
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

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        //in.defaultReadObject();
        // Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();
        mergingCompanyName = (String) fields.get("mergingCompanyName", mergingCompanyName);
        targetCompanyNames = (String) fields.get("targetCompanyNames", targetCompanyNames);
        canReplaceToken = (List<Boolean>) fields.get("canReplaceToken", canReplaceToken);
        selectedTargetCompanyName = (String) fields.get("selectedTargetCompanyName", selectedTargetCompanyName);
        replaceToken = fields.get("replaceToken", replaceToken);

        CompanyManager cmgr = getCompanyManager();

        mergingCompany = cmgr.getPublicCompany(mergingCompanyName);

        targetCompanies = new ArrayList<PublicCompany>();
        for (String name : targetCompanyNames.split(",")) {
            if (name.equals("null")) {
                targetCompanies.add(null);
            } else {
                targetCompanies.add(cmgr.getPublicCompany(name));
            }
        }

        if (selectedTargetCompanyName != null
            && !selectedTargetCompanyName.equals("null")) {
            selectedTargetCompany =
                    cmgr.getPublicCompany(selectedTargetCompanyName);
        }
    }
}

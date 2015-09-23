package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Objects;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;

/**
 * 
 * Rails 2.0: Updated equals and toString methods
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
            List<PublicCompany> targetCompanies, boolean forced) {
        super(null); // not defined by an activity yet
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
                            && hex.getStopOfBaseToken(mergingCompany)
                                != hex.getStopOfBaseToken(target)));
            }
        }
        targetCompanyNames = b.toString();
    }
    
    public MergeCompanies(PublicCompany mergingCompany,
            PublicCompany targetCompany, boolean forced) {
        this (mergingCompany, Arrays.asList(new PublicCompany[] {targetCompany}), forced);
    }

    /** Required for deserialization */
    public MergeCompanies() {
        super(null); // not defined by an activity yet
    }

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
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        MergeCompanies action = (MergeCompanies)pa; 
        boolean options = Objects.equal(this.mergingCompany, action.mergingCompany)
                && Objects.equal(this.targetCompanies, action.targetCompanies)
                && Objects.equal(this.canReplaceToken, action.canReplaceToken)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.selectedTargetCompany, action.selectedTargetCompany)
                && Objects.equal(this.replaceToken, action.replaceToken)
        ;
    }
    
    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("mergingCompany", mergingCompany)
                    .addToString("targetCompanies", targetCompanies)
                    .addToString("canReplaceToken", canReplaceToken)
                    .addToStringOnlyActed("selectedTargetCompany", selectedTargetCompany)
                    .addToStringOnlyActed("replaceToken", replaceToken)
                    .toString()
        ;
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

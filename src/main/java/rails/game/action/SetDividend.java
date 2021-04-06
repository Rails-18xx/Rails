package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;

/**
 * Action class that comprises the earnings setting and distribution steps. In
 * the current versions, the earnings must always be calculated and entered by
 * the user. In a later version, the earnings may have been calculated by the
 * back-end; in that case, the user can only select the earnings distribution
 * method.
 *
 * Rails 2.0: Updated equals and toString methods
 */
public class SetDividend extends PossibleORAction implements Cloneable {

    public static final int UNKNOWN = -1;
    public static final int WITHHOLD = 0;
    public static final int SPLIT = 1;
    public static final int PAYOUT = 2;
    public static final int NO_TRAIN = 3;
    public static final int NUM_OPTIONS = 4;

    /** Allocation name keys in the resource bundle */
    protected static final String[] allocationNameKeys =
            new String[] { "WITHHOLD", "SPLIT", "PAYOUT", "NO_TRAIN" };

    /*--- Server-side settings ---*/
    /**
     * The revenue as proposed by the back-end. Currently this is always the
     * previous revenue. In the future, this could be the calculated revenue.
     */
    protected int presetRevenue;

    /**
     * Is the user allowed to set the revenue? Currently, this will aways be
     * true, except if the company has no trains (the revenue is then 0). In the
     * future, it will only be true if the user has some influence on it (e.g.,
     * in 1844, the user may opt for less that maximum revenue is some cases).
     */
    private boolean mayUserSetRevenue = true;

    /**
     * The revenue allocations that the user may select from. If only one value
     * is provided, the user has no option (e.g. minor companies always split in
     * most games).
     */
    private int[] allowedRevenueAllocations;

    /** Cash that should be minimally raised as revenue
     * (for instance, to pay loan interest as in 1856).
     * If actual revenue is below this value, the dividend will be zero,
     * and no dividend allocation should be requested.
     * */
    protected int requiredCash = 0;

    /*--- Client-side settings ---*/

    /** The revenue as set (or accepted, or just seen) by the user. */
    protected int actualRevenue = 0;

    /** The revenue destination selected by the user (if he has a choice at all). */
    protected int revenueAllocation;

    /**The
     * The direct revenue for the company treasury (not as dividend) as proposed by the back-end. Currently this is always the
     * previous revenue. In the future, this could be the calculated revenue.
     */
    private int presetCompanyTreasuryRevenue = 0;
    private int actualCompanyTreasuryRevenue = 0;

    public static final long serialVersionUID = 1L;

    public SetDividend(RailsRoot root, int presetRevenue, boolean mayUserSetRevenue,
            int[] allowedAllocations) {
        this (root, presetRevenue, 0, mayUserSetRevenue, allowedAllocations, 0);
    }

    public SetDividend(RailsRoot root, int presetRevenue, boolean mayUserSetRevenue,
                int[] allowedAllocations, int requiredCash) {
        this (root, presetRevenue, 0, mayUserSetRevenue, allowedAllocations, requiredCash);
    }

    public SetDividend(RailsRoot root, int presetRevenue, int presetCompanyTreasuryRevenue, boolean mayUserSetRevenue,
            int[] allowedAllocations, int requiredCash) {
        super(root);
        this.presetRevenue = presetRevenue;
        this.presetCompanyTreasuryRevenue = presetCompanyTreasuryRevenue;
        this.setMayUserSetRevenue(mayUserSetRevenue);
        this.setAllowedRevenueAllocations(allowedAllocations.clone());
        this.requiredCash = requiredCash;
        if (getAllowedRevenueAllocations().length == 1) {
            revenueAllocation = getAllowedRevenueAllocations()[0];
        } else {
            revenueAllocation = UNKNOWN;
        }
    }


    /** Clone an instance (used by clone) */
    protected SetDividend(SetDividend action) {
        this(action.getRoot(), action.presetRevenue,
                action.presetCompanyTreasuryRevenue,
                action.getMayUserSetRevenue(),
                action.getAllowedRevenueAllocations(),
                action.requiredCash);
    }

    public void setPresetRevenue(int presetRevenue) {
        this.presetRevenue = presetRevenue;
    }

    public int getPresetRevenue() {
        return presetRevenue;
    }

    public void setActualRevenue(int revenue) {
        actualRevenue = revenue;
    }

    public int getActualRevenue() {
        return actualRevenue;
    }

    public void setActualCompanyTreasuryRevenue(
            int actualCompanyTreasuryRevenue) {
        this.actualCompanyTreasuryRevenue = actualCompanyTreasuryRevenue;
    }

    public int getActualCompanyTreasuryRevenue() {
                    return actualCompanyTreasuryRevenue;
    }

    public int getPresetCompanyTreasuryRevenue() {
        return presetCompanyTreasuryRevenue;
    }

    public int[] getAllowedAllocations() {
        return getAllowedRevenueAllocations();
    }

    public boolean isAllocationAllowed(int allocationType) {
        for (int at : getAllowedRevenueAllocations()) {
            if (at == allocationType) return true;
        }
        return false;
    }

    public int getRequiredCash() {
        return requiredCash;
    }

    public void setRevenueAllocation(int allocation) {
        revenueAllocation = allocation;
    }

    public int getRevenueAllocation() {
        return revenueAllocation;
    }

    public static String getAllocationNameKey(int allocationType) {
        if (allocationType >= 0 && allocationType < NUM_OPTIONS) {
            return allocationNameKeys[allocationType];
        } else {
            return "<invalid allocation type: " + allocationType + ">";
        }
    }

    @Override
    public Object clone() {
        SetDividend result = new SetDividend(this);
        result.setActualRevenue(actualRevenue);
        result.setActualCompanyTreasuryRevenue(actualCompanyTreasuryRevenue);
        result.setRevenueAllocation(revenueAllocation);
        return result;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        SetDividend action = (SetDividend)pa;
        boolean options =
             Objects.equal(this.presetRevenue, action.presetRevenue)
                && Objects.equal(this.presetCompanyTreasuryRevenue, action.presetCompanyTreasuryRevenue)
                && Objects.equal(this.getMayUserSetRevenue(), action.getMayUserSetRevenue())
                     // Temporarily disabled
                //&& Arrays.equals(this.getAllowedRevenueAllocations(), action.getAllowedRevenueAllocations())
                && Objects.equal(this.requiredCash, action.requiredCash)
        ;

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        // EV 2020/07/18: Disabled. In 18Scan the revenue and allocation
        // may sometimes be changed afterwards (e.g. minors pay out even if no route)

        return options
                && Objects.equal(this.actualRevenue, action.actualRevenue)
                && Objects.equal(this.actualCompanyTreasuryRevenue, action.actualCompanyTreasuryRevenue)
                && Objects.equal(this.revenueAllocation, action.revenueAllocation);

        //return true;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("presetRevenue", presetRevenue)
                    .addToString("presetTreasuryBonusRevenue",presetCompanyTreasuryRevenue)
                    .addToString("mayUserSetRevenue", getMayUserSetRevenue())
                    .addToString("allowedRevenueAllocations", getAllowedAllocationsAsString())
                    .addToString("requiredCash", requiredCash)
                    .addToStringOnlyActed("actualRevenue", actualRevenue)
                    .addToStringOnlyActed("actualCompanyTreasuryRevenue", actualCompanyTreasuryRevenue)
                    .addToStringOnlyActed("revenueAllocation", getAllocationNameKey(revenueAllocation))
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Custom deserialization for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();
        presetRevenue = fields.get("presetRevenue", presetRevenue);
        presetCompanyTreasuryRevenue = fields.get("presetCompanyTreasuryRevenue", presetCompanyTreasuryRevenue);
        setMayUserSetRevenue(fields.get("mayUserSetRevenue", getMayUserSetRevenue()));
        setAllowedRevenueAllocations((int[]) fields.get("allowedRevenueAllocations", getAllowedRevenueAllocations()));
        requiredCash = fields.get("requiredCash", 0);
        actualRevenue = fields.get("actualRevenue", actualRevenue);
        actualCompanyTreasuryRevenue = fields.get("actualCompanyTreasuryRevenue", actualCompanyTreasuryRevenue);
        revenueAllocation = fields.get("revenueAllocation", revenueAllocation);

        if (Util.hasValue(companyName)) {
            company = getCompanyManager().getPublicCompany(companyName);
        }
    }

    public boolean getMayUserSetRevenue() {
        return mayUserSetRevenue;
    }

    public void setMayUserSetRevenue(boolean mayUserSetRevenue) {
        this.mayUserSetRevenue = mayUserSetRevenue;
    }

    public int[] getAllowedRevenueAllocations() {
        return allowedRevenueAllocations;
    }

    public void setAllowedRevenueAllocations(
            int[] allowedRevenueAllocations) {
        this.allowedRevenueAllocations = allowedRevenueAllocations;
    }

    public String getAllowedAllocationsAsString () {
        if (allowedRevenueAllocations == null || allowedRevenueAllocations.length == 0) return "";
        String[] s = new String[allowedRevenueAllocations.length];
        for (int i=0; i < allowedRevenueAllocations.length; i++) {
            s[i] = getAllocationNameKey(allowedRevenueAllocations[i]);
        }
        return Util.join (s, ";");
    }

}

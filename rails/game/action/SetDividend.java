package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import rails.game.Bank;
import rails.util.Util;

/**
 * Action class that comprises the earnings setting and distribution steps. In
 * the current versions, the earnings must always be calculated and entered by
 * the user. In a later version, the earnings may have been calculated by the
 * back-end; in that case, the user can only select the earnings distribution
 * method.
 *
 * @author Erik Vos
 *
 */
public class SetDividend extends PossibleORAction implements Cloneable {

    public static final int UNKNOWN = -1;
    public static final int WITHHOLD = 0;
    public static final int SPLIT = 1;
    public static final int PAYOUT = 2;
    public static final int NO_TRAIN = 3;
    public static final int NUM_OPTIONS = 4;

    /** Allocation name keys in the resource bundle */
    public static final String[] allocationNameKeys =
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
    protected boolean mayUserSetRevenue;

    /**
     * The revenue allocations that the user may select from. If only one value
     * is provided, the user has no option (e.g. minor companies always split in
     * most games).
     */
    protected int[] allowedRevenueAllocations;

    /** Cash that should be minimally raised as revenue
     * (for instance, to pay loan interest as in 1856).
     * If actual revenue is below this value, the dividend will be zero,
     * and no dividend allocation should be requested.
     * */
    protected int requiredCash = 0;

    /*--- Client-side settings ---*/

    /** The revenue as set (or accepted, or just seen) by the user. */
    protected int actualRevenue;

    /** The revenue destination selected by the user (if he has a choice at all). */
    protected int revenueAllocation;

    public static final long serialVersionUID = 1L;

    public SetDividend(int presetRevenue, boolean mayUserSetRevenue,
            int[] allowedAllocations) {
        this (presetRevenue, mayUserSetRevenue, allowedAllocations, 0);
    }

    public SetDividend(int presetRevenue, boolean mayUserSetRevenue,
                int[] allowedAllocations, int requiredCash) {
        super();
        this.presetRevenue = presetRevenue;
        this.mayUserSetRevenue = mayUserSetRevenue;
        this.allowedRevenueAllocations = allowedAllocations.clone();
        this.requiredCash = requiredCash;
        if (allowedRevenueAllocations.length == 1) {
            revenueAllocation = allowedRevenueAllocations[0];
        } else {
            revenueAllocation = UNKNOWN;
        }
    }

    /** Clone an instance (used by clone) */
    protected SetDividend(SetDividend action) {
        this(action.presetRevenue, action.mayUserSetRevenue,
                action.allowedRevenueAllocations,
                action.requiredCash);
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

    public int[] getAllowedAllocations() {
        return allowedRevenueAllocations;
    }

    public boolean isAllocationAllowed(int allocationType) {
        for (int at : allowedRevenueAllocations) {
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
        result.setRevenueAllocation(revenueAllocation);
        return result;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof SetDividend)) return false;
        SetDividend a = (SetDividend) action;
        return a.company == company
               && a.presetRevenue == presetRevenue
               && a.mayUserSetRevenue == mayUserSetRevenue
               && a.requiredCash == requiredCash
               && Arrays.equals(a.allowedRevenueAllocations,
                       allowedRevenueAllocations);
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof SetDividend)) return false;
        SetDividend a = (SetDividend) action;
        return a.company == company
               && a.actualRevenue == actualRevenue
               && a.revenueAllocation == revenueAllocation;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append(getClass().getSimpleName()).append(": ").append(company.getName());
        if (mayUserSetRevenue) {
            b.append(", settable, previous=").append(Bank.format(presetRevenue));
            if (actualRevenue > 0) {
                b.append(", new=").append(Bank.format(actualRevenue));
            }
        } else {
            b.append(", fixed, calculated=").append(Bank.format(presetRevenue));
        }
        b.append(", allowed=");
        for (int i : allowedRevenueAllocations) {
            b.append(allocationNameKeys[i]).append(",");
        }
        if (revenueAllocation >= 0) {
            b.append(" chosen=").append(allocationNameKeys[revenueAllocation]);
        }
        if (requiredCash > 0) {
            b.append(" requiredCash="+requiredCash);
        }

        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        // Custom deserialization for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();
        presetRevenue = fields.get("presetRevenue", presetRevenue);
        mayUserSetRevenue = fields.get("mayUserSetRevenue", mayUserSetRevenue);
        allowedRevenueAllocations = (int[]) fields.get("allowedRevenueAllocations", allowedRevenueAllocations);
        requiredCash = fields.get("requiredCash", 0);
        actualRevenue = fields.get("actualRevenue", actualRevenue);
        revenueAllocation = fields.get("revenueAllocation", revenueAllocation);

        if (Util.hasValue(companyName)) {
            company = getCompanyManager().getPublicCompany(companyName);
        }
    }

}

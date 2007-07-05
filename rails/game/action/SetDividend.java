package rails.game.action;

import java.util.Arrays;

import rails.game.Bank;

/** 
 * Action class that comprises the earnings setting and 
 * distribution steps. In the current versions, the earnings
 * must always be calculated and entered by the user.
 * In a later version, the earnings may have been calculated
 * by the back-end; in that case, the user can only
 * select the earnings distribution method.
 * @author Erik Vos
 *
 */
public class SetDividend extends PossibleORAction implements Cloneable {
    
    public static final int UNKNOWN = -1;
    public static final int WITHHOLD = 0;
    public static final int SPLIT = 1;
    public static final int PAYOUT = 2;
    public static final int NUM_OPTIONS = 3;
    
    /** Allocation name keys in the resource bundle */
    public static final String[] allocationNameKeys
        = new String[] {"WITHHOLD", "SPLIT", "PAYOUT"};

    /** The revenue as proposed by the back-end.
     * Currently this is always the previous revenue.
     * In the future, this could be the calculated revenue.
     */
    int presetRevenue;
    
    /** Is the user allowed to set the revenue?
     * Currently, this will aways be true, except if
     * the company has no trains (the revenue is then 0).
     * In the future, it will only be true if the user has
     * some influence on it (e.g., in 1844, the user may
     * opt for less that maximum revenue is some cases). 
     */
    boolean mayUserSetRevenue;
    
    /** The revenue as set (or accepted, or just seen) by the user. */
    int actualRevenue;
    
    /** 
     * The revenue allocations that the user may select from.
     * If only one value is provided, the user has no option
     * (e.g. minor companies always split in most games). 
     */ 
    int[] allowedRevenueAllocations;
    
    /** The revenue destination selected by the user (if he has a choice at all). */
    int revenueAllocation;
    

    public SetDividend(int presetRevenue,
            boolean mayUserSetRevenue, int[] allowedAllocations) {
        super();
        this.presetRevenue = presetRevenue;
        this.mayUserSetRevenue = mayUserSetRevenue;
        this.allowedRevenueAllocations = (int[])allowedAllocations.clone();
        if (allowedRevenueAllocations.length == 1) {
            revenueAllocation = allowedRevenueAllocations[0];
        } else {
            revenueAllocation = UNKNOWN;
        }
    }
    
    /** Clone an instance (used by clone) */
    private SetDividend (SetDividend action) {
        this (action.presetRevenue, 
                action.mayUserSetRevenue, action.allowedRevenueAllocations);
    }
    
    public int getPresetRevenue() {
        return presetRevenue;
    }
    
    public void setActualRevenue(int revenue) {
        actualRevenue = revenue;
    }
    
    public int getActualRevenue () {
        return actualRevenue;
    }
    
    public int[] getAllowedAllocations() {
        return allowedRevenueAllocations;
    }
    
    public boolean isAllocationAllowed (int allocationType) {
        for (int at : allowedRevenueAllocations) {
            if (at == allocationType) return true;
        }
        return false;
    }
    
    public void setRevenueAllocation(int allocation) {
        revenueAllocation = allocation;
    }
    
    public int getRevenueAllocation() {
        return revenueAllocation;
    }
    
    public static String getAllocationNameKey (int allocationType) {
        if (allocationType >= 0 && allocationType < NUM_OPTIONS) {
            return allocationNameKeys[allocationType];
        } else {
            return "<invalid allocation type: "+allocationType+">";
        }
    }
    
    public Object clone() {
        
        SetDividend result = new SetDividend (this);
        result.setActualRevenue(actualRevenue);
        result.setRevenueAllocation(revenueAllocation);
        return result;
    }
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof SetDividend)) return false;
        SetDividend a = (SetDividend) action;
        return a.company == company
            && a.presetRevenue == presetRevenue
            && a.mayUserSetRevenue == mayUserSetRevenue
            && Arrays.equals(a.allowedRevenueAllocations, allowedRevenueAllocations);
    }
    
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("SetDividend: ").append(company.getName());
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
            b.append (allocationNameKeys[i]).append(",");
        }
        if (revenueAllocation >= 0) {
            b.append(" chosen=").append(allocationNameKeys[revenueAllocation]);
        }
  
        return b.toString();
    }

}

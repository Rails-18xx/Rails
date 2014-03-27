package net.sf.rails.algorithms;

public interface RevenueCalculatorModifier {

    /**
     * Allows to replace the usual calculation process (evaluate all trains simultaneously)
     * If several dynamic modifier have their own method, their prediction values are added up.  
     * @return optimal value
     */
    public int calculateRevenue(RevenueAdapter revenueAdpater);

    
    
}

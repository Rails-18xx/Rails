package net.sf.rails.algorithms;


/**
 * Classes that change properties of the revenue calculation
 * before the actual calculation starts implement a the static modifier.
 *  
 * They have to register themselves to the RevenueManager via the GameManager instance.
 * @author freystef
 *
 */


public interface RevenueStaticModifier{
    
    /** method called after the setup of the revenueAdapter, but before the actual calculation
     * Allows to call methods of the revenue adapter to change the content
     * @param revenueAdapter
     * @return if true a pretty print text is required
     */
    public boolean modifyCalculator(RevenueAdapter revenueAdapter);

    /** 
     * Allows to append additional text
     * Only called if modifyCalculator returned true
     * @return String output for display in Rails */
    public String prettyPrint(RevenueAdapter revenueAdapter);
    
}

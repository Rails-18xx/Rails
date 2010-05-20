package rails.algorithms;
/**
 * Classes that change properties of the revenue calculation
 * after the actual calculation started implement the dynamic modifier.
 *  
 * They have to register themselves to the RevenueManager via the GameManager instance.
 * @author freystef
 *
 */

public interface RevenueDynamicModifier {

    /** after the setup of the revenueAdapter, but before the actual calculation
     * if return is false => deactivate */
    public boolean prepareModifier(RevenueAdapter revenueAdapter);
    
    /** returns the value used for prediction */
    public int predictionValue(RevenueAdapter revenueAdapter);
    
    /** returns the value used for evaluation */
    public int evaluationValue(RevenueAdapter revenueAdapter);
    
    /** returns the prettyPrintName */
    public String prettyPrint(RevenueAdapter revenueAdapter);
    
}

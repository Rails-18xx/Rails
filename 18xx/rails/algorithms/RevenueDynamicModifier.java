package rails.algorithms;

import java.util.List;

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
    public int predictionValue();
    
    /** returns the value used for evaluation (at the runs supplied) */
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns);
    
    /** allows to adjust the run list of the optimal train run output */
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns);
    
    /** returns the results as pretty prints */
    public String prettyPrint(RevenueAdapter adapter);
    
}

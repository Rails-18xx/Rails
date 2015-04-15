package net.sf.rails.algorithms;

import java.util.List;

/**
 * A dynamic modifier allows to change revenue calculation
 * during the revenue calculation
 * 
 * For any modfication that only change the setup (e.g. adding bonuses, change train attributes)
 * the simpler {@link RevenueStaticModifier} is preferred.
 *  
 * They have to register themselves to the RevenueManager via the GameManager instance.
 *
 * Caveats:
 * The interaction between several dynamic modifiers can be complicated.
 * The are called in the order of the definition in game.xml.
 */

public interface RevenueDynamicModifier {

    /** method called after the setup of the revenueAdapter, but before the actual calculation
     * @return true => active, false => deactivate */
    public boolean prepareModifier(RevenueAdapter revenueAdapter);
    
    /** 
     * Allows to change the value for the prediction
     * If several dynamic modifiers are active simultaneously, their prediction values are added up.  
     * @param runs Current run of the revenue calculator
     * @return value used to change the prediction
     */
    public int predictionValue(List<RevenueTrainRun> runs);
    
    /** 
     * Allows to change the value for the supplied runs from the revenue calculator
     * @param runs Current run of the revenue calculator
     * @param optimalRuns true => after optimization, false => during optimization
     * @return value used to change the run results
     */
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns);
    
    /** 
     * Allows to adjust the run list of the optimal train run output 
     * @param optimalRuns Optimized run from the revenue calculator
     * */
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns);

    /** 
     * Allows to append additional text
     * @return String output for display in Rails */
    public String prettyPrint(RevenueAdapter revenueAdapter);
    
}

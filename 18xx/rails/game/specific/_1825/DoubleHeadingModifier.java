package rails.game.specific._1825;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rails.algorithms.NetworkTrain;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueDynamicModifier;
import rails.algorithms.RevenueTrainRun;
/**
 * 1825 modifiers:
 * Trains have to start and end in a major station
 * Allows two 2-trains to run as a 3-train (double heading)
 */
public class DoubleHeadingModifier implements RevenueDynamicModifier {

    private final static String TRAIN_2_NAME = "2";
    private final static String DUALHEAD_NAME = "2&2";
   
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        int nbTrain2 = 0;
        for (NetworkTrain train:revenueAdapter.getTrains()) {
            // checks name of traintype
            if (train.getRailsTrainType().getName().equals(TRAIN_2_NAME)) {
                nbTrain2 ++;
            }
        }
        
        // add dualhead 3 train for each of a pair of 2-trains
        boolean hasDualHead = false;
        while (nbTrain2 >= 2) {
            NetworkTrain dualHead = new NetworkTrain(3, 0, false, 1, 1, DUALHEAD_NAME, null);
            revenueAdapter.addTrain(dualHead);
            hasDualHead = true;
            nbTrain2 -= 2;
        }
        
        return hasDualHead;
    }

    /** 
     * the prediction value itself is zero, as the add value stems from the train above
     */
    public int predictionValue() {
        return 0;
    }

    /**
     * returns the runs of the of the double heading trains 
     */
    private List<RevenueTrainRun> identifyDoubleHeadingTrains(List<RevenueTrainRun> runs) {
        // find and sort the train2Revenues
        List<RevenueTrainRun> train2Runs = new ArrayList<RevenueTrainRun>();
        for (RevenueTrainRun run:runs) {
            if (run.getTrain().getTrainName().equals(TRAIN_2_NAME)) {
                train2Runs.add(run);
            }
        }
        Collections.sort(train2Runs);
        
        // keep index on train2Runs
        int index2Runs = 0;
        // find DualHeads and remove two 2-train revenues
        for (RevenueTrainRun run:runs) {
            // only if train has non zero value
            if (run.getTrain().getTrainName().equals(DUALHEAD_NAME) && run.getRunValue() !=0) {
                // two trains get removed
                index2Runs += 2;
            }
        }
        return train2Runs.subList(0, index2Runs);
    }
    
    
    /**
     * - checks if runs start and end at major stations
     * - allows doubleheading
     */
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {


        if (optimalRuns) return 0; // optimalRuns are adjusted 
        
        // count the adjustments
        int changeRevenues = 0;
        for (RevenueTrainRun run:identifyDoubleHeadingTrains(runs)) {
            changeRevenues -= run.getRunValue();
        }
        return changeRevenues;
    }

    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // remove the double heading runs from the revenue list
        optimalRuns.removeAll(identifyDoubleHeadingTrains(optimalRuns));

        // remove double heading trains that do not generate value
        List<RevenueTrainRun> removeDoubleHeading = new ArrayList<RevenueTrainRun>();
        for (RevenueTrainRun run:optimalRuns) {
            if (run.getTrain().getTrainName().equals(DUALHEAD_NAME) && run.getRunValue() == 0) {
                removeDoubleHeading.add(run);
            }
        }
        optimalRuns.removeAll(removeDoubleHeading);
    }

    public String prettyPrint(RevenueAdapter adapter) {
        // nothing to print
        return null;
    }

}

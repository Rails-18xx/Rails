package net.sf.rails.game.specific._1825;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.rails.algorithms.NetworkTrain;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.common.LocalText;

/**
 * Double heading modifier
 * Allows two trains to run as a longer train (double heading)
 */
public class DoubleHeadingModifier implements RevenueDynamicModifier {

    private final static String TRAIN_SINGLE = "2";
    private final static String DOUBLEHEAD_NAME = "2&2";
    private final static String TRAIN_DOUBLE = "3";
   
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        int nbTrain2 = 0;
        for (NetworkTrain train:revenueAdapter.getTrains()) {
            // checks name of train
            if (train.getTrainName().equals(TRAIN_SINGLE)) {
                nbTrain2 ++;
            }
        }
        
        // add dualhead 3 train for each of a pair of 2-trains
        boolean hasDualHead = false;
        while (nbTrain2 >= 2) {
            NetworkTrain dualHead = NetworkTrain.createFromString(TRAIN_DOUBLE);
            dualHead.setTrainName(DOUBLEHEAD_NAME);
            revenueAdapter.addTrain(dualHead);
            hasDualHead = true;
            nbTrain2 -= 2;
        }
        
        return hasDualHead;
    }

    /** 
     * the prediction value itself is zero, as the add value stems from the train above
     */
    public int predictionValue(List<RevenueTrainRun> runs) {
        return 0;
    }

    /**
     * returns the runs of the of the double heading trains 
     */
    private List<RevenueTrainRun> identifyDoubleHeadingTrains(List<RevenueTrainRun> runs) {
        // find and sort the train2Revenues
        List<RevenueTrainRun> train2Runs = new ArrayList<RevenueTrainRun>();
        for (RevenueTrainRun run:runs) {
            if (run.getTrain().getTrainName().equals(TRAIN_SINGLE)) {
                train2Runs.add(run);
            }
        }
        Collections.sort(train2Runs);
        
        // keep index on train2Runs
        int index2Runs = 0;
        // find DualHeads and remove two 2-train revenues
        for (RevenueTrainRun run:runs) {
            // only if train has non zero value
            if (run.getTrain().getTrainName().equals(DOUBLEHEAD_NAME) && run.getRunValue() !=0) {
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
            if (run.getTrain().getTrainName().equals(DOUBLEHEAD_NAME) && run.getRunValue() == 0) {
                removeDoubleHeading.add(run);
            }
        }
        optimalRuns.removeAll(removeDoubleHeading);
    }

    public boolean providesOwnCalculateRevenue() {
        // does not
        return false;
    }

    public int calculateRevenue(RevenueAdapter revenueAdpater) {
        // zero does no change
        return 0;
    }

    public String prettyPrint(RevenueAdapter adapter) {
        return LocalText.getText("DoubleHeadingModifier1825", DOUBLEHEAD_NAME, TRAIN_SINGLE, TRAIN_DOUBLE);
    }

}

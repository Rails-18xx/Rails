package net.sf.rails.game.specific._1825;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;


public class TerminateAtMajorModifier implements RevenueDynamicModifier {

    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        // always active
        return true;
    }

    public int predictionValue() {
        // cannot be predicted
        return 0;
    }

    private List<RevenueTrainRun> identifyInvalidRuns(List<RevenueTrainRun> runs) {
        // check if runs end and start at major stations
        List<RevenueTrainRun> invalidRuns = new ArrayList<RevenueTrainRun>();
        for (RevenueTrainRun run:runs) {
            if (!run.hasAValidRun()) continue;
            if (!run.getFirstVertex().isMajor() || !run.getLastVertex().isMajor()) {
                invalidRuns.add(run);
            }
        }
        return invalidRuns;
    }
    
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        // optimal runs is already adjusted
        if (optimalRuns) return 0;
        // otherwise check invalid runs
        int changeRevenues = 0;
        for (RevenueTrainRun run:identifyInvalidRuns(runs)) {
            changeRevenues -= run.getRunValue();
        }
        return changeRevenues;
    }

    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // set invalid runs to be empty
        for (RevenueTrainRun run:identifyInvalidRuns(optimalRuns)) {
            run.getRunVertices().clear();
        }
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
        // nothing to do
        return null;
    }

}

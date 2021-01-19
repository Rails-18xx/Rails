package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.game.Access;
import net.sf.rails.game.Station;
import net.sf.rails.game.Stop;
import net.sf.rails.util.Util;


public class RunToCoalMineModifier implements RevenueDynamicModifier {

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        // always active
        return true;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        // cannot be predicted
        return 0;
    }

    private List<RevenueTrainRun> identifyInvalidRuns(List<RevenueTrainRun> runs) {
        List<RevenueTrainRun> invalidRuns = new ArrayList<>();
        for (RevenueTrainRun run:runs) {
            if (!run.hasAValidRun()) continue;
            String trainCategory = run.getTrain().getRailsTrain().getCategory();
            if (!Util.hasValue(trainCategory)) continue;

            // check if runs do not start or end at a coal mine
            // for a train category that is not allowed to do so
            // (this part of the modifier is not specific for 1837)
            Station firstStation = run.getFirstVertex().getStop().getRelatedStation();
            boolean firstStationIsMine = firstStation.getType() == Stop.Type.MINE;
            Access firstStationAccess = firstStation.getAccess();
            Station lastStation = run.getLastVertex().getStop().getRelatedStation();
            boolean lastStationIsMine = lastStation.getType() == Stop.Type.MINE;
            Access lastStationAccess = lastStation.getAccess();

            if (firstStationIsMine && !(firstStationAccess == null
                        || firstStationAccess.getRunToTrainCategories().contains(trainCategory))) {
                invalidRuns.add(run);
                continue;
            }
            if (lastStationIsMine && !(lastStationAccess == null
                    || lastStationAccess.getRunToTrainCategories().contains(trainCategory))) {
                invalidRuns.add(run);
                continue;
            }
            // Coal train runs must include just one mine
            // (note: this makes the mutexId check redundant)
            // "goods" may be 1837-specific
            if (trainCategory.equalsIgnoreCase("goods")
                    && firstStationIsMine == lastStationIsMine) {
                invalidRuns.add(run);
            }
        }
        return invalidRuns;
    }

    @Override
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

    @Override
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

    @Override
    public String prettyPrint(RevenueAdapter adapter) {
        // nothing to do
        return null;
    }

}

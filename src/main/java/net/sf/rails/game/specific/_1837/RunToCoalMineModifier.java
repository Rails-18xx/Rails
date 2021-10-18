package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueCalculatorModifier;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.game.Access;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Station;
import net.sf.rails.game.Stop;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This modifier enforces the rules for accessing mines:
 * - goods trains <i>must</i> run from or to exactly <i>one</i> mine,
 * - passenger trains <b>may not</b> run from or to any mine.
 */
public class RunToCoalMineModifier
        implements RevenueDynamicModifier, RevenueCalculatorModifier {

    private static final Logger log = LoggerFactory.getLogger(RunToCoalMineModifier.class);

    private int directRevenueFromMines;
    //private boolean evaluateMine;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        directRevenueFromMines = 0;
        return true;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        // cannot be predicted
        return 0;
    }

    private List<RevenueTrainRun> identifyInvalidRuns(List<RevenueTrainRun> runs) {

        int totalMineRevenue = 0;

        List<RevenueTrainRun> invalidRuns = new ArrayList<>();
        int i = 0;
        log.debug ("--------------------------------------------------");
        for (RevenueTrainRun run:runs) {
            log.debug ("Run {}: {}", ++i,
                    run.prettyPrint(true)
                            .replaceAll("\\n+", "")
                            .replaceAll("\\s+", " "));
            if (!run.hasAValidRun()) {
                log.debug ("Invalid run");
                continue;
            }
            String trainCategory = run.getTrain().getRailsTrain().getCategory();
            if (!Util.hasValue(trainCategory)) {
                log.debug("No category");
                continue;
            }

            // check if runs do not start or end at a coal mine
            // for a train category that is not allowed to do so
            // (this part of the modifier is not specific for 1837)
            Stop firstStop = run.getFirstVertex().getStop();
            Station firstStation = firstStop.getRelatedStation();
            boolean firstStationIsMine = firstStation.getType() == Stop.Type.MINE;
            Access firstStationAccess = firstStation.getAccess();
            Stop lastStop = run.getLastVertex().getStop();
            Station lastStation = lastStop.getRelatedStation();
            boolean lastStationIsMine = lastStation.getType() == Stop.Type.MINE;
            Access lastStationAccess = lastStation.getAccess();

            if (firstStationIsMine && !(firstStationAccess == null
                        || firstStationAccess.getRunToTrainCategories().contains(trainCategory))) {
                invalidRuns.add(run);
                log.debug("Invalid first stop: access={} or wrong category");
                continue;
            }
            if (lastStationIsMine && !(lastStationAccess == null
                    || lastStationAccess.getRunToTrainCategories().contains(trainCategory))) {
                invalidRuns.add(run);
                log.debug("Invalid last stop: access={} or wrong category");
                continue;
            }
            // Coal train runs must include just one mine
            // (note: this makes the mutexId check redundant)
            // "goods" may be 1837-specific
            if (trainCategory.equalsIgnoreCase("goods")) {
                if (firstStationIsMine == lastStationIsMine) {
                    log.debug("Invalid, GT mines: {}, {}",firstStationIsMine,lastStationIsMine);
                    invalidRuns.add(run);
                } else {
                    // Save the revenue from the mine(s), which in 1837
                    // becomes 'direct revenue' into the company treasury.
                    Stop mine = (firstStationIsMine ? firstStop : lastStop);
                    Phase phase = run.getTrain().getRailsTrain().getRoot().getPhaseManager().getCurrentPhase();
                    int mineRevenue = mine.getParent().getCurrentValueForPhase(phase);
                    totalMineRevenue += mineRevenue;
                }
            }

        }
        //evaluateMine = false;
        // Maximize the mine revenue (not sure if this is optimal).
        directRevenueFromMines = Math.max (directRevenueFromMines, totalMineRevenue);
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

    /**
     * Here used to separately report the revenue from mines.
     * The engine will later subtract that from the total revenue.
     * @param revenueAdapter
     * @return The revenue gathered by the mine(s)
     */
    @Override
    public int calculateRevenue(RevenueAdapter revenueAdapter) {
        // EV: it appears that this method is never called,
        // so I could not use to report just the mines revenue.
        // It was also intended for a different purpose, as it seems.
        // It has been replaced with a new method, see getSpecialRevenue().
        return 0;
    }

    // The above method is never called, do it another way.
    // This method has been added to the RevenueCalculatorModifier.
    public int getSpecialRevenue () {
        return directRevenueFromMines;
    }

    @Override
    public String prettyPrint(RevenueAdapter adapter) {
        // nothing to do
        return null;
    }

}

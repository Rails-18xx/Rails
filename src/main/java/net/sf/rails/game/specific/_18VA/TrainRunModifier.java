package net.sf.rails.game.specific._18VA;

import net.sf.rails.algorithms.*;
import net.sf.rails.game.*;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This modifier enforces the rules for accessing mines:
 * - goods trains <i>must</i> run from or to exactly <i>one</i> mine,
 * - passenger trains <b>may not</b> run from or to any mine.
 */
public class TrainRunModifier
        implements RevenueDynamicModifier, RevenueCalculatorModifier {

    private static final Logger log = LoggerFactory.getLogger(TrainRunModifier.class);

    private int cmdValue;
    private int cmdDirectValue;
    private int portExtraValue;
    private int offMapExtraValue;
    private PublicCompany company;
    private Phase phase;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        company = revenueAdapter.getCompany();
        phase = revenueAdapter.getPhase();
        cmdDirectValue = 0;
        return true;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        // cannot be predicted
        return 0;
    }

    private List<RevenueTrainRun> identifyInvalidRuns(List<RevenueTrainRun> runs) {

        cmdValue = 0;
        //cmdDirectValue = 0;
        portExtraValue = 0;
        offMapExtraValue = 0;

        List<RevenueTrainRun> invalidRuns = new ArrayList<>();
        int i = 0;
        boolean directValueReset = true;
        log.debug ("--------------------------------------------------");
        for (RevenueTrainRun run:runs) {

            // NOTE: most of these checks are specific for 18VA
            log.debug("Run {}: {}", ++i,
                    run.prettyPrint(true)
                            .replaceAll("\\n+", "")
                            .replaceAll("\\s+", " "));
            String trainCategory = run.getTrain().getRailsTrain().getCategory();
            if (!Util.hasValue(trainCategory)) {
                invalidRuns.add(run);
                log.debug("No category");
                continue;
            }

            // A passenger run must have at least 2 major stops, and a goods run 1.
            // (for passenger trains this only works because in 18VA
            // there are no passenger town stops, only ports)
            int majors =
                    NetworkVertex.numberOfVertexType(run.getUniqueVertices(),
                            NetworkVertex.VertexType.STATION, NetworkVertex.StationType.MAJOR);
            boolean isGoods = trainCategory.equalsIgnoreCase(GameDef_18VA.GOODS);
            int requiredMajors = isGoods ? 1 : 2;
            String categoryName = isGoods ? "Goods" : "Passenger";

            if (majors < requiredMajors) {
                invalidRuns.add(run);
                log.debug("{} run has {} major stops, but required is at least {}",
                        categoryName, majors, requiredMajors);
                continue;
            }

            List<NetworkVertex> vertices = run.getRunVertices();

            Stop firstStop = run.getFirstVertex().getStop();
            Station firstStation = firstStop.getRelatedStation();
            Stop.Type firstStationType = firstStation.getType();
            Access firstStationAccess = firstStop.getAccess();

            Stop lastStop = run.getLastVertex().getStop();
            Station lastStation = lastStop.getRelatedStation();
            Stop.Type lastStationType = lastStation.getType();
            Access lastStationAccess = lastStop.getAccess();

            // Check if a visited port is accessible
            // (NOTE: this only works with the 18VA map!)
            boolean portReached = false;
            NetworkVertex portCityVertex = null;
            if (firstStationType == Stop.Type.PORT || lastStationType == Stop.Type.PORT) {
                List<NetworkVertex> portVertices = new ArrayList<>(vertices);
                if (lastStationType == Stop.Type.PORT) Collections.reverse(portVertices);
                for (NetworkVertex vertex : portVertices) {
                    log.debug("Checking {}", vertex.getIdentifier());
                    if (!vertex.isSide()
                            && vertex.getStop().getRelatedStation().getType() == Stop.Type.CITY) {
                        // This must be the city where the port belongs to
                        log.debug("Found city {} for port {}", vertex.getStop(), vertices.get(0));
                        portReached = vertex.getStop().hasTokenOf(company);
                        portCityVertex = vertex;
                        break;
                    }
                }
                if (!portReached) {
                    invalidRuns.add(run);
                    log.debug("Port not reached");
                    continue;
                } else if (isGoods) {
                    // Calculate extra port value
                    portExtraValue += portVertices.get(0).getValue() + portCityVertex.getValue();
                }
            }

            // Calculate CMD income
            if (firstStationType == Stop.Type.MINE || lastStationType == Stop.Type.MINE) {
                if (directValueReset) {
                    cmdDirectValue = 0;
                    log.debug(">>>>> DirRev set to 0");
                    directValueReset = false;
                }
                Stop cmdStop = (firstStationType == Stop.Type.MINE ? firstStop : lastStop);
                int trainLevel = run.getTrain().getRailsTrain().getMajorStops();
                int baseValue = trainLevel * cmdStop.getHex().getCurrentValueForPhase(phase); // 20
                cmdValue += baseValue;
                if (isGoods && cmdStop.hasTokenOf(company)) {
                    // Calculate CMD direct value (i.e. what goes into treasury)
                    cmdDirectValue += baseValue;
                }
            }

            // Calculate extra OffMap value
            if (firstStationType == Stop.Type.OFFMAP || lastStationType == Stop.Type.OFFMAP
                    && !phase.getId().equalsIgnoreCase("4D")) {
                Stop offMapStop = (firstStationType == Stop.Type.OFFMAP ? firstStop : lastStop);
                if (offMapStop.hasTokenOf(company)) {
                    // Calculate offmap extra value
                    offMapExtraValue += offMapStop.getHex().getCurrentValueForPhase(phase);
                }
            }
        }
        log.debug("After invalid runs: cmdValue={} directValue={}", cmdValue, cmdDirectValue);

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
        log.debug("Eval: inv={} port={} cmd={} direct={} off={}",
                changeRevenues, portExtraValue, cmdValue, cmdDirectValue, offMapExtraValue);
        return changeRevenues + portExtraValue + cmdValue + offMapExtraValue;
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
        log.debug("calculateRevenue called!");
        return 0;
    }

    // The above method is never called, do it another way.
    // This method has been added to the RevenueCalculatorModifier.
    public int getSpecialRevenue () {
        return cmdDirectValue;
    }

    @Override
    public String prettyPrint(RevenueAdapter adapter) {
        // nothing to do
        return null;
    }

}

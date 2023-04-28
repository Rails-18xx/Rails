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

    private PublicCompany company;
    private int cmdDirectValue;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        company = revenueAdapter.getCompany();
        cmdDirectValue = 0;
        return true;
    }

    public int predictionValue(List<RevenueTrainRun> runs) {

        return 0;
    }

    // FIXME The value calculations should use RevenueManager_18VA.
    private List<RevenueTrainRun> identifyInvalidRuns(List<RevenueTrainRun> runs) {

        List<NetworkVertex> vertices;
        List<RevenueTrainRun> invalidRuns = new ArrayList<>();
        int i = 0;
        boolean directValueReset = true;
        log.debug ("--------------------------------------------------");
        for (RevenueTrainRun run:runs) {

            NetworkTrain train = run.getTrain();
            Train t = train.getRailsTrain();
            int value = run.getRunValue();
            vertices = run.getRunVertices();
            int majors =
                    NetworkVertex.numberOfVertexType(run.getUniqueVertices(),
                            NetworkVertex.VertexType.STATION, NetworkVertex.StationType.MAJOR);
            int minors =
                    NetworkVertex.numberOfVertexType(run.getUniqueVertices(),
                            NetworkVertex.VertexType.STATION, NetworkVertex.StationType.MINOR);
            log.debug(">>> Run {} {}={} {} M={} m={}", ++i, t, value, vertices, majors, minors);
            if (vertices.size() < 2) {
                invalidRuns.add(run);
                log.debug("Skipped: no run");
                continue;
            }

            String trainCategory = run.getTrain().getRailsTrain().getCategory();
            if (!Util.hasValue(trainCategory)) {
                invalidRuns.add(run);
                log.debug("No category");
                continue;
            }

            // A passenger run must have at least 2 major stops, and a goods run 1.
            // (for passenger trains this only works because in 18VA
            // there are no passenger town stops, only ports)
            boolean isGoods = trainCategory.equalsIgnoreCase(GameDef_18VA.GOODS);
            int requiredMajors = isGoods ? 1 : 2;
            String categoryName = isGoods ? "Goods" : "Passenger";

            if (majors < requiredMajors) {
                invalidRuns.add(run);
                log.debug("Skipped: {} run has {} majors, required >={}",
                        categoryName, majors, requiredMajors);
                continue;
            }

            NetworkVertex firstVertex = run.getFirstVertex();
            Stop firstStop = firstVertex.getStop();
            Station firstStation = firstStop.getRelatedStation();
            Stop.Type firstStationType = firstStation.getType();
            Access firstStationAccess = firstStop.getAccess();

            NetworkVertex lastVertex = run.getLastVertex();
            Stop lastStop = lastVertex.getStop();
            Station lastStation = lastStop.getRelatedStation();
            Stop.Type lastStationType = lastStation.getType();
            Access lastStationAccess = lastStop.getAccess();

            // Check if a visited port is accessible
            // (NOTE: this only works with the 18VA map!)
            if (firstStationType == Stop.Type.PORT || lastStationType == Stop.Type.PORT) {
                boolean portReached = false;
                NetworkVertex port = firstStationType == Stop.Type.PORT ? firstVertex : lastVertex;
                NetworkVertex city = getCityOfPort(port, run);
                if (city != null && city.getStop().hasTokenOf(company) && majors >= 2) {
                    // An 1G-train cannot reach a port, which does not count as a separate station
                    portReached = true;
                }
                if (!portReached) {
                    invalidRuns.add(run);
                    log.debug("Skipped: port not reached");
                    continue;
                }
            }

            // Calculate CMD income
            if (firstStationType == Stop.Type.MINE || lastStationType == Stop.Type.MINE) {
                if (directValueReset) {
                    if (!isGoods) {
                        invalidRuns.add(run);
                        log.debug("Skipped: {} wrong category to CMD", trainCategory);
                        continue;
                    }
                    cmdDirectValue = 0;
                    directValueReset = false;
                }
                Stop cmdStop = (firstStationType == Stop.Type.MINE ? firstStop : lastStop);
                int trainLevel = run.getTrain().getRailsTrain().getMajorStops();
                int baseValue = trainLevel * 20;
                if (isGoods && cmdStop.hasTokenOf(company)) {
                    // Calculate CMD direct value (i.e. what goes into treasury)
                    cmdDirectValue += baseValue;
                }
            }

            // Temporary fixture to keep passenger trains off towns (i.e. mines)
            if (!isGoods && (firstStationType == Stop.Type.TOWN || lastStationType == Stop.Type.TOWN)) {
                invalidRuns.add(run);
                log.debug("Skipped: {} wrong category to mine (town)", trainCategory);
                continue;
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
        return 0;
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
        return "";
    }


    // Should if possible be merged with the similar method in RevenueManager_18VA.
    // That one uses stops, this one vertices. Not sure what is better.
    private NetworkVertex getCityOfPort (NetworkVertex port, RevenueTrainRun run) {
        if (port.getStop().getType() != Stop.Type.PORT) {
            log.debug ("Error: {} is not a Port!", port);
            return null;
        }
        List<NetworkVertex> portVertices = new ArrayList<>(run.getRunVertices());

        if (run.getLastVertex() == port) {
            Collections.reverse(portVertices);
        }
        for (NetworkVertex vertex : portVertices) {
            if (!vertex.isSide()
                    && vertex.getStop().getType() == Stop.Type.CITY) {
                // This must be the city where the port belongs to
                return vertex;
            }
        }
        return null;
    }
}

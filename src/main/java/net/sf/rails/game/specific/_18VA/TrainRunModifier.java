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

        int predictionValue = 0;
        for (RevenueTrainRun run : runs) {
            Train train = run.getTrain().getRailsTrain();
            boolean isGoods = train.getCategory().equalsIgnoreCase("goods");
            int majors = 0;
            int value = 0;
            for (NetworkVertex v : run.getRunVertices()) {
                switch (v.getStop().getRelatedStation().getType()) {
                    case PORT:
                        NetworkVertex port = v;
                        NetworkVertex city = getCityOfPort(v, run);
                        //value += port.getValue();
                        if (isGoods) {
                            value += port.getValue() + city.getValue();
                        }
                        break;
                    case MINE: // CMD
                        if (isGoods) {
                            //int factor = v.getStop().hasTokenOf(company) ? 2 : 1;
                            if (v.getStop().hasTokenOf(company)) {
                                value += 20 * train.getMajorStops();
                            }
                        }
                        break;
                    case OFFMAP:
                        //int factor = v.getStop().hasTokenOf(company) ? 2 : 1;
                        if (v.getStop().hasTokenOf(company)) {
                            value += v.getValue();
                        }
                        majors++;
                        break;
                    case TOWN: // mine
                        //if (isGoods) value += v.getValue();
                        break;
                    case CITY:
                        majors++;
                    default:
                        //value += v.getValue();
                }
            }
            if (majors >= (isGoods ? 1 : 2)) { // Otherwise no valid run
                log.debug("Prediction for {} {} is {}",
                        run.getTrain().getRailsTrain(), run.getRunVertices(), value);

                predictionValue += value;
            } else {
                log.debug ("Prediction for {} {} is 0 - not a valid run",
                        run.getTrain().getRailsTrain(), run.getRunVertices());
            }
        }
        log.debug ("Total extra prediction={}", predictionValue);
        return predictionValue;
    }

    private List<RevenueTrainRun> identifyInvalidRuns(List<RevenueTrainRun> runs) {

        cmdValue = 0;
        //cmdDirectValue = 0;
        portExtraValue = 0;
        offMapExtraValue = 0;

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
            /*
            log.debug(">>> Run {} {}: {}", i, t,
                    run.prettyPrint(true)
                            .replaceAll("\\n+", "")
                            .replaceAll("\\s+", " "));*/
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
                NetworkVertex city = getCityOfPort (port, run);
                if (city != null && city.getStop().hasTokenOf(company) && majors >= 2) {
                    // An 1G-train cannot reach a port, which does not count as a separate station
                    portReached = true;
                }
                if (!portReached) {
                    invalidRuns.add(run);
                    log.debug("Skipped: port not reached");
                    continue;
                } else if (isGoods && port != null && city != null) {
                    // Calculate extra port value
                    portExtraValue += port.getValue() + city.getValue();
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
                    log.debug(">>>>> DirRev set to 0");
                    directValueReset = false;
                }
                Stop cmdStop = (firstStationType == Stop.Type.MINE ? firstStop : lastStop);
                int trainLevel = run.getTrain().getRailsTrain().getMajorStops();
                //int baseValue = trainLevel * cmdStop.getHex().getCurrentValueForPhase(phase); // 20
                int baseValue = trainLevel * 20;
                cmdValue += baseValue;
                if (isGoods && cmdStop.hasTokenOf(company)) {
                    // Calculate CMD direct value (i.e. what goes into treasury)
                    cmdDirectValue += baseValue;
                }
            }

            // Temporary fixture to keep passenger trains off towns (i.e. mines)
            if (!isGoods && (firstStationType == Stop.Type.TOWN || lastStationType == Stop.Type.TOWN)) {
                invalidRuns.add (run);
                log.debug("Skipped: {} wrong category to mine (town)", trainCategory);
                continue;
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
        log.debug("After run validation: port={} cmd={} cmdDirect={} offmap={}",
                portExtraValue, cmdValue, cmdDirectValue, offMapExtraValue);

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
        // Note: total revenue must include direct revenue, which will be subtracted later
        return changeRevenues + portExtraValue + cmdValue + cmdDirectValue + offMapExtraValue;
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
        StringBuilder b = new StringBuilder("");
        if (portExtraValue != 0) b.append("Port bonus = ").append(portExtraValue);
        if (cmdValue != 0) b.append(b.length() > 0 ? ",  " : "")
                .append("CMD value = ").append(cmdValue);
        if (cmdDirectValue != 0) b.append(b.length() > 0 ? ",  " : "")
                .append("CMD treasury income = ").append(cmdDirectValue);
        if (offMapExtraValue != 0) b.append(b.length() > 0 ? ",  " : "")
                .append("OffMap bonus = ").append(offMapExtraValue);
        return b.length() > 0 ? b.toString() : null;
    }

    private NetworkVertex getCityOfPort (NetworkVertex port, RevenueTrainRun run) {
        if (port.getStop().getRelatedStation().getType() != Stop.Type.PORT) {
            log.debug ("Error: {} is not a Port!", port);
            return null;
        }
        List<NetworkVertex> portVertices = new ArrayList<>(run.getRunVertices());
        NetworkVertex city = null;
        if (run.getLastVertex() == port) {
            Collections.reverse(portVertices);
        }
        for (NetworkVertex vertex : portVertices) {
            if (!vertex.isSide()
                    && vertex.getStop().getRelatedStation().getType() == Stop.Type.CITY) {
                // This must be the city where the port belongs to
                city = vertex;
                log.debug("Found city {} for port {}", city.getStop(), port.getStop());
                break;
            }
        }
        return city;
    }
}

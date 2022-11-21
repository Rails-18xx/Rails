package net.sf.rails.algorithms;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RevenueCalculator {

    protected final int nbVertexes;
    protected final int nbTrains;
    protected final int nbEdges;
    protected final int nbBonuses;

    // static vertex data
    protected final int[][] vertexValueByTrain; // dimensions: vertexId, trainId
    protected final boolean[] vertexMajor;
    protected final boolean[] vertexMinor;
    protected final boolean[] vertexSink;
    protected final int[] vertexNbNeighbors;
    protected final int[] vertexNbVisitSets;
    protected final int[] vertexNbBonusSets;

    protected final int[][] vertexNeighbors;
    protected final int[][] vertexEdges;
    protected final int[][] vertexVisitSets; // vertex belongs to a visit set, dimension: nbVertex x maxVertexSets
    protected final int[][] vertexBonusSets; // vertex belongs to a bonus set, dimension: nbVertex x nbBonuses

    // start vertexes
    protected int[] startVertexes;

    // static edge data
    protected final boolean[] edgeGreedy;
    protected final int[] edgeDistance;

    // static train data
    protected final int[] trainMaxMajors;
    protected final int[] trainMaxMinors;
    protected final int[] trainMaxBonuses;
    protected final boolean[] trainIgnoreMinors;
    protected final boolean[] trainIsH; // true => train is H-train
    protected final boolean[] trainIsE; // true => train is Express-train

    // dynamic train data
    protected final int[] trainCurrentValue;
    protected final int[] trainMajors;
    protected final int[] trainMinors;
    protected final int[] trainBonuses; // counts the number of bonuses received
    protected final boolean[][] trainVisited;
    protected final int[][] trainStack; // store either vertices or edges
    protected final int[] trainStackPos;
    protected final boolean [] trainBottomActive;
    protected final int [] trainStartEdge;
    protected final int[] trainDistance; // keeps track of distance travelled (for H-trains)

    int specialRevenue;
    int currentBestSpecRev;

    // static bonus data
    protected final int [] bonusValue;
    protected final boolean [][] bonusActiveForTrain; // dimensions: bonus x train
    protected final int [] bonusRequiresVertices;

    // dynamic bonus data
    protected final int [][] bonusTrainVertices;

    // run settings
    protected int startTrainSet;
    protected int finalTrainSet;
    protected int startTrain;
    protected int finalTrain;
    protected boolean useRevenuePrediction;

    // current best run results
    protected int currentBestValue;
    protected final int [][] currentBestRun;

    // prediction data
    protected int[] maxCumulatedTrainRevenues;
    protected int[][] maxMajorRevenues; // dimensions trainId x nb vertex;
    protected int[][] maxMinorRevenues; // dimensions trainId x nb vertex;
    protected int[][] maxBonusRevenues; // dimensions trainId x nb bonuses

    // statistic data
    protected int countVisits;
    protected int countEdges;
    protected int nbEdgesTravelled;
    protected int nbEvaluations;
    protected int nbPredictions;

    // revenue Adapter
    protected RevenueAdapter revenueAdapter;

    // activate dynamic revenue modifiers
    protected boolean callDynamicModifiers;

    // termination results
    protected enum Terminated {
        WITH_EVALUATION,
        WITHOUT_EVALUATION,
        NOT_YET
    }

    private static final Logger log = LoggerFactory.getLogger(RevenueCalculator.class);


    public RevenueCalculator (RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges,
            int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses) {

        log.debug("RC defined: nbVertexes = {}, nbEdges = {}, maxNeighbors = {}, maxVertexSets = {}, maxEdgeSets = {}, nbTrains = {}, nbBonuses = {}", nbVertexes, nbEdges, maxNeighbors, maxVertexSets, maxEdgeSets, nbTrains, nbBonuses);

        this.revenueAdapter = revenueAdapter;
        this.nbVertexes = nbVertexes;
        this.nbEdges = nbEdges;
        this.nbTrains = nbTrains;
        this.nbBonuses = nbBonuses;

        // initialize all required variables
        vertexValueByTrain = new int[nbVertexes][nbTrains];
        vertexMajor = new boolean[nbVertexes];
        vertexMinor = new boolean[nbVertexes];
        vertexSink = new boolean[nbVertexes];
        vertexNbNeighbors = new int[nbVertexes];
        vertexNbVisitSets = new int[nbVertexes];
        vertexNbBonusSets = new int[nbVertexes];
        vertexNeighbors = new int[nbVertexes][maxNeighbors];
        vertexEdges = new int[nbVertexes][maxNeighbors];
        vertexVisitSets = new int[nbVertexes][maxVertexSets];
        vertexBonusSets = new int[nbVertexes][nbBonuses];

        edgeGreedy = new boolean[nbEdges];
        edgeDistance = new int[nbEdges];

        trainMaxMajors = new int[nbTrains];
        trainMaxMinors = new int[nbTrains];
        trainMaxBonuses = new int[nbTrains]; // only required for revenue prediction
        trainIgnoreMinors = new boolean[nbTrains];
        trainIsH = new boolean[nbTrains];
        trainIsE = new boolean[nbTrains];

        trainCurrentValue = new int[nbTrains];
        trainMajors = new int[nbTrains];
        trainMinors = new int[nbTrains];
        trainBonuses = new int[nbTrains];
        trainVisited = new boolean[nbTrains][nbVertexes];
        // increase necessary due to buttom train
        trainStack = new int[nbTrains][nbVertexes + 1];
        trainStackPos = new int[nbTrains];
        trainBottomActive = new boolean[nbTrains];
        trainStartEdge = new int[nbTrains];
        trainDistance = new int[nbTrains];
        maxCumulatedTrainRevenues = new int[nbTrains];

        bonusValue = new int[nbBonuses];
        bonusRequiresVertices = new int[nbBonuses];
        bonusActiveForTrain = new boolean[nbBonuses][nbTrains];
        bonusTrainVertices = new int[nbBonuses][nbTrains];

        currentBestRun = new int[nbTrains][nbVertexes + 1];

        useRevenuePrediction = false;

        callDynamicModifiers = false;
    }

    final void setVertex(int id, boolean major, boolean minor, boolean sink) {
        vertexMajor[id] = major;
        vertexMinor[id] = minor;
        vertexSink[id] = sink;
        // default neighbors && visit and bonus sets
        vertexNbNeighbors[id] = 0;
        vertexNbVisitSets[id] = 0;
        vertexNbBonusSets[id] = 0;
    }

    final void setVertexValue(int vertexId, int trainId, int value) {
        vertexValueByTrain[vertexId][trainId] = value;
    }

    final void setVertexNeighbors(int id, int[] neighbors, int[] edges) {
        // copy neighbors
        for (int j=0; j < neighbors.length; j++) {
                vertexNeighbors[id][j] = neighbors[j];
                vertexEdges[id][j] = edges[j];
        }
        vertexNbNeighbors[id] = neighbors.length;

    }

    final void setStartVertexes(int[] startVertexes) {
        this.startVertexes = startVertexes;
    }


    void setEdge(int edgeId, boolean greedy, int distance) {
        edgeGreedy[edgeId] = greedy;
        edgeDistance[edgeId] = distance;
        // default travel sets
//        edgeNbTravelSets[edgeId] = 0;
    }

    final void setTrain(int id, int majors, int minors, boolean ignoreMinors, boolean isHTrain, boolean isETrain) {
        trainMaxMajors[id] = majors;
        trainMaxMinors[id] = minors;
        trainMaxBonuses[id] = 0;
        trainIgnoreMinors[id] = ignoreMinors;
        trainIsH[id] = isHTrain;
        trainIsE[id] = isETrain;
    }

    final void setVisitSet(int[] vertices) {
        for (int j=0; j < vertices.length; j++) {
            int vertexId = vertices[j];
            for (int k=0; k < vertices.length; k++) {
                if (k == j) continue;
                vertexVisitSets[vertexId][vertexNbVisitSets[vertexId]++] = vertices[k];
            }
        }
    }


    final void setBonus(int id, int value, int[] vertices, boolean[] bonusForTrain) {
        log.debug("RC: define bonus value = {}, vertices = {}, bonusForTrain = {}", value, Arrays.toString(vertices), Arrays.toString(bonusForTrain));

        bonusValue[id] = value;
        bonusRequiresVertices[id] = vertices.length;
        for ( int vertexId : vertices ) {
            vertexBonusSets[vertexId][vertexNbBonusSets[vertexId]++] = id;
        }
        bonusActiveForTrain[id] = bonusForTrain;
    }

    final void setDynamicModifiers(boolean activate) {
        callDynamicModifiers = activate;
    }

    final int[][] getOptimalRun() {
        log.debug("RC: currentBestRun = {}", Arrays.deepToString(currentBestRun));
        return currentBestRun;
    }

    final int[][] getCurrentRun() {
        int[][] currentRun = new int[nbTrains][nbVertexes+1];
        for (int j = startTrainSet; j <= finalTrainSet; j++) {
            for (int v = 0; v < nbVertexes + 1; v++) {
                if (v < trainStackPos[j]) {
                    currentRun[j][v] = trainStack[j][v];
                } else {
                    currentRun[j][v] = -1; // terminator
                    break;
                }
            }
        }
        return currentRun;
    }

    final int getNumberOfEvaluations() {
        return nbEvaluations;
    }

    final String getStatistics() {
        StringBuilder statistics = new StringBuilder();
        statistics.append(nbEvaluations).append(" evaluations");
        if (useRevenuePrediction)
            statistics.append(", ").append(nbPredictions).append(" predictions");
        statistics.append(" and ").append(nbEdgesTravelled).append(" edges travelled.");
        return statistics.toString();
    }

    private void notifyRevenueAdapter(final int revenue, final int specialRevenue, final boolean finalResult) {
        String modifier;
        if (finalResult)
            modifier = "final";
        else
            modifier = "new best";
        log.debug("Report {} result of {} after {}", modifier, revenue, getStatistics());
        revenueAdapter.notifyRevenueListener(revenue, specialRevenue, finalResult);
    }

    private int[] bestRevenues(final int[] values, final int length) {
        int[] bestRevenues = new int[length + 1];
        Arrays.sort(values);
        int cumulatedRevenues = 0;
        for (int j=1; j <= length ; j++) {
            cumulatedRevenues += values[values.length - j];
            bestRevenues[j] = cumulatedRevenues;
        }
        log.debug("Values={} length={} Best Revenues = {}",
                values, length, Arrays.toString(bestRevenues));
        return bestRevenues;
    }

    private void initRevenueValues(final int startTrain, final int finalTrain){

      // intialize values
        maxMajorRevenues = new int[nbTrains][nbVertexes];
        maxMinorRevenues = new int[nbTrains][nbVertexes];
        maxBonusRevenues = new int[nbTrains][nbVertexes + nbBonuses];
        for (int t=startTrain; t <= finalTrain; t++) {
            int[] majorValues = new int[nbVertexes];
            int[] minorValues = new int[nbVertexes];
            int[] bonusValues = new int[nbVertexes + nbBonuses];
            int major = 0, minor = 0, bonus = 0;
            // scan vertices for values
            for (int v=0; v < nbVertexes; v++) {
                if (vertexValueByTrain[v][t] == 0) continue;
                if (vertexMajor[v]) {
                    majorValues[major++] = vertexValueByTrain[v][t];
                } else if (vertexMinor[v]) {
                    minorValues[minor++] = vertexValueByTrain[v][t];
                } else { // define it as bonus
                    bonusValues[bonus++] = vertexValueByTrain[v][t];
                }
            }
            // add the (complex) bonuses
            for (int b=0; b < nbBonuses; b++) {
                if (bonusValue[b] <= 0 || !bonusActiveForTrain[b][t]) continue;
                bonusValues[bonus++] = bonusValue[b];
            }
            trainMaxBonuses[t] = bonus;

            maxMajorRevenues[t] = bestRevenues(majorValues, trainMaxMajors[t]);
            maxMinorRevenues[t] = bestRevenues(minorValues, trainMaxMinors[t]);
            maxBonusRevenues[t] = bestRevenues(bonusValues, trainMaxBonuses[t]);
            // initially the cumulated train revenues are the individual run revenues
            int trainRevenues = maxMajorRevenues[t][trainMaxMajors[t]] + maxMinorRevenues[t][trainMaxMinors[t]]
                     + maxBonusRevenues[t][trainMaxBonuses[t]];
            maxCumulatedTrainRevenues[t] = trainRevenues;
        }
        log.debug("maxMajorRevenues = {}", Arrays.deepToString(maxMajorRevenues));
        log.debug("maxMinorRevenues = {}", Arrays.deepToString(maxMinorRevenues));
        log.debug("maxBonusRevenues = {}", Arrays.deepToString(maxBonusRevenues));
        log.debug("maxCumulatedTrainRevenues = {}", Arrays.toString(maxCumulatedTrainRevenues));
    }

    final void initRuns(final int startTrain, final int finalTrain) {
        log.debug("RC: init train index from {} to {}", startTrain, finalTrain);
        if (startTrain > finalTrain) return;

        this.startTrainSet = startTrain;
        this.finalTrainSet = finalTrain;

        // initialize all trains and currentValues
        for (int i = startTrain; i < finalTrain; i++) {
            currentBestRun[i][0] = -1;
        }
        currentBestValue = 0;

    }
    final void executePredictions(final int startTrain, final int finalTrain) {

        useRevenuePrediction = true;

        if (startTrain > finalTrain) return;

        initRevenueValues(startTrain, finalTrain);

        if (startTrain == finalTrain) {
            return;
        }

        // start prediction runs
        nbEvaluations = 0; nbPredictions = 0; nbEdgesTravelled = 0;

        log.debug("RC: start individual prediction Runs");
        int[] maxSingleTrainRevenues = new int[nbTrains];
        for (int j = startTrain; j <= finalTrain; j++) {
            this.startTrain = j;
            this.finalTrain = j;
            currentBestValue = 0;
            runTrain(j);
            log.debug("RC: Best prediction run of train number {} value = {} after {}", j, currentBestValue, getStatistics());
            maxSingleTrainRevenues[j] = currentBestValue;
        }
        int cumulatedRevenues = 0;
        for (int j = finalTrain; j >= startTrain; j--) {
            cumulatedRevenues +=  maxSingleTrainRevenues[j];
            maxCumulatedTrainRevenues[j] = cumulatedRevenues;
        }
        log.debug("maxCumulatedTrainRevenues = {}", Arrays.toString(maxCumulatedTrainRevenues));

        if (startTrain == finalTrain - 1) return;

        log.debug("RC: start combined prediction runs");
        this.finalTrain = finalTrain;
        for (int j=finalTrain - 1; j > startTrain; j--) {
            this.startTrain = j;
//            currentBestValue = 0;
            runTrain(j);
            log.debug("RC: Best prediction run until train nb. {} value = {} after {}", j, currentBestValue, getStatistics());
            maxCumulatedTrainRevenues[j] = currentBestValue;
            maxCumulatedTrainRevenues[j-1] = currentBestValue  + maxSingleTrainRevenues[j-1];
            log.debug("maxCumulatedTrainRevenues = {}", Arrays.toString(maxCumulatedTrainRevenues));
        }
    }

    final int calculateRevenue(final int startTrain, final int finalTrain) {
        log.debug("RC: calculateRevenue trains from {} to {}", startTrain, finalTrain);

        this.startTrain = startTrain;
        this.finalTrain = finalTrain;

        runTrain(startTrain);

        // inform revenue listener via adapter
        notifyRevenueAdapter(currentBestValue, currentBestSpecRev, true);

        return currentBestValue;
    }

    protected abstract void runTrain(final int trainId);

    protected abstract void runBottom(final int trainId);


    // next vertex is either:
    // protected void nextVertex(int trainId, int vertexId, boolean previousGreedy);
    // protected void nextVertex(int trainId, int vertexId);

    protected final boolean encounterVertex(final int trainId, final int vertexId, final boolean arrive) {

        log.debug("RC: EncounterVertex, trainId = {} vertexId = {} arrive = {}", trainId, vertexId, arrive);

        // set visit to true if arriving, otherwise you leave
        trainVisited[trainId][vertexId] = arrive;

        boolean stationVertex = false;
        if (arrive) {
            trainCurrentValue[trainId] += vertexValueByTrain[vertexId][trainId];
            log.debug("Added {}", vertexValueByTrain[vertexId][trainId]);
            if (vertexMajor[vertexId]) {
                trainMajors[trainId]--;
                stationVertex = true;
            } else if (vertexMinor[vertexId]) {
                trainMinors[trainId]--;
                stationVertex = !trainIgnoreMinors[trainId];
            }
            countVisits++;
        } else {
            trainCurrentValue[trainId] -= vertexValueByTrain[vertexId][trainId];
            log.debug("Subtracted {}", vertexValueByTrain[vertexId][trainId]);
            if (vertexMajor[vertexId]) {
                trainMajors[trainId]++;
                stationVertex = true;
            } else if (vertexMinor[vertexId]) {
                trainMinors[trainId]++;
                stationVertex = !trainIgnoreMinors[trainId];
            }
            countVisits--;
        }

        // check vertex sets
        for (int j=0; j < vertexNbVisitSets[vertexId]; j++) {
            trainVisited[trainId][vertexVisitSets[vertexId][j]] = arrive;
            log.debug("RC: visited = {} for vertex {} due to block rule", arrive, vertexVisitSets[vertexId][j]);
        }

        // check bonus sets
        for (int j=0; j < vertexNbBonusSets[vertexId]; j++) {
            int bonusId = vertexBonusSets[vertexId][j];
            if (!bonusActiveForTrain[bonusId][trainId]) continue;
            if (arrive) {
                bonusTrainVertices[bonusId][trainId]--;
                log.debug("RC: Decreased bonus {} to {}", bonusId, bonusTrainVertices[bonusId][trainId]);
                if (bonusTrainVertices[bonusId][trainId] == 0) {
                   trainCurrentValue[trainId] += bonusValue[bonusId];
                   if (bonusValue[bonusId] > 0) trainBonuses[trainId]--;
                    log.debug("RC: Added bonus {} with value {}", bonusId, bonusValue[bonusId]);
                }
            } else {
                if (bonusTrainVertices[bonusId][trainId] == 0) {
                    trainCurrentValue[trainId] -= bonusValue[bonusId];
                    if (bonusValue[bonusId] > 0) trainBonuses[trainId]++;
                    log.debug("RC: Removed bonus {} with value {}", bonusId, bonusValue[bonusId]);
                 }
                bonusTrainVertices[bonusId][trainId]++;
                log.debug("RC: Increases bonus {} to {}", bonusId, bonusTrainVertices[bonusId][trainId]);
            }
        }

        log.debug("RC: stop={} station={} visits={}", vertexId, stationVertex, countVisits);
        return stationVertex;
    }

    // travel edge is either:
//      protected boolean travelEdge(int trainId, int edgeId, boolean previousGreedy);
//      protected boolean travelEdge(int trainId, int edgeId);

    protected abstract void returnEdge(final int trainId, final int edgeId);

    protected Terminated trainTerminated(final int trainId) {
        Terminated terminated = Terminated.NOT_YET;
        if (trainIgnoreMinors[trainId]) {
            // express trains
            if (trainMajors[trainId] == 0)
                terminated = Terminated.WITH_EVALUATION;
        } else { // default and plus trains
            if (trainMajors[trainId] < 0){
                terminated = Terminated.WITHOUT_EVALUATION;
            } else if (trainMajors[trainId] + trainMinors[trainId] == 0)
                terminated = Terminated.WITH_EVALUATION;
        }
        if (terminated != Terminated.NOT_YET ) {
            log.debug("RC: Train {} has terminated: majors = {} minors = {}", trainId, trainMajors[trainId], trainMinors[trainId]);
        }
        return terminated;
    }

    protected final void finalizeVertex(final int trainId, final int vertexId) {
        log.debug("RC: Finalize Vertex id {} for train {}", vertexId, trainId);

        if (trainId == finalTrain) {
            evaluateResults();
        } else {
            runTrain(trainId + 1);
        }
    }

    protected final void evaluateResults() {
        // sum to total value
        int totalValue = 0;
        for (int j = startTrain; j <= finalTrain; j++) {
          totalValue += trainCurrentValue[j];
//            check for two stations requirement not necessary if stationVertex approach works
//            if (trainIgnoreMinors[j]) {
//                if (trainMaxMajors[j] - trainMajors[j] >= 2)
//                    totalValue += trainCurrentValue[j];
//            } else {
//                if (trainMaxMajors[j] + trainMaxMinors[j] - trainMajors[j] - trainMinors[j] >= 2)
//                    totalValue += trainCurrentValue[j];
//            }
        }

        if (callDynamicModifiers) {
            totalValue += revenueAdapter.dynamicEvaluation();
            specialRevenue = revenueAdapter.getSpecialRevenue();
        }

        nbEvaluations++;
        log.debug("RC: current total value {}", totalValue);

        // compare to current best result
        if (totalValue > currentBestValue) {
            currentBestValue = totalValue;
            currentBestSpecRev = specialRevenue;
            // exceed thus deep copy of vertex stack
            for (int j = startTrainSet; j <= finalTrainSet; j++) {
                for (int v = 0; v < nbVertexes + 1; v++) {
                    if (v < trainStackPos[j]) {
                        currentBestRun[j][v] = trainStack[j][v];
                    } else {
                        currentBestRun[j][v] = -1; // terminator
                        break;
                    }
                }
            }
            log.debug("RC: Found better run with {}", totalValue);
            // inform revenue listener via adapter
            // special revenue only to be reported with the final result
            notifyRevenueAdapter(currentBestValue, currentBestSpecRev, false);
        }
    }

    // predict revenues and returns true if best value can still be exceeded
    protected final boolean predictRevenues(final int trainId){
        // the potential revenues of the future trains
        int totalValue = 0;
        if (trainId < finalTrain)
             totalValue = maxCumulatedTrainRevenues[trainId + 1];

        // predict the current train
        int trainValue = trainCurrentValue[trainId];
        if (trainIgnoreMinors[trainId]) {
            // express train
            trainValue += maxMajorRevenues[trainId][trainMajors[trainId]];
        } else {
            if (trainMinors[trainId] > 0){
                trainValue += maxMajorRevenues[trainId][trainMajors[trainId]];
                trainValue += maxMinorRevenues[trainId][trainMinors[trainId]];
            } else { // <= 0
                int trainStations = trainMajors[trainId] + trainMinors[trainId];
                // trainStations can be zero or negative (for H trains)
                if (trainStations > 0) {
                    trainValue += maxMajorRevenues[trainId][trainStations];
                }
            }
        }
        // add potential bonuses
        if (trainBonuses[trainId] != 0) {
            trainValue += maxBonusRevenues[trainId][trainBonuses[trainId]];
        }
        log.debug("RC: Current train has predicted  value of {}", trainValue);

        // maximum value for the trainId including future trains
        totalValue = Math.min(totalValue + trainValue, maxCumulatedTrainRevenues[trainId]);

        // and add the past trains: current realized values
        for (int j = startTrain; j < trainId; j++) {
          totalValue += trainCurrentValue[j];
//        check for two stations requirement not necessary if stationVertex approach works
//            if (trainIgnoreMinors[j]) {
//                if (trainMaxMajors[j] - trainMajors[j] >= 2)
//                    totalValue += trainCurrentValue[j];
//            } else {
//                if (trainMaxMajors[j] + trainMaxMinors[j] - trainMajors[j] - trainMinors[j] >= 2)
//                    totalValue += trainCurrentValue[j];
//            }
        }

        if (callDynamicModifiers) totalValue += revenueAdapter.dynamicPrediction();

        nbPredictions++;

        boolean terminate = (totalValue <= currentBestValue);
        if (terminate) log.debug("Run terminated due to predicted value of {}", totalValue);

        return terminate;
    }


    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("vertexValuesByTrain:").append(Arrays.deepToString(vertexValueByTrain)).append("\n");
        buffer.append("vertexMajor:").append(Arrays.toString(vertexMajor)).append("\n");
        buffer.append("vertexMinor:").append(Arrays.toString(vertexMinor)).append("\n");
        buffer.append("vertexNeighbors:").append(Arrays.deepToString(vertexNeighbors)).append("\n");
        buffer.append("vertexEdges:").append(Arrays.deepToString(vertexEdges)).append("\n");
        buffer.append("vertexVisitSets:").append(Arrays.deepToString(vertexVisitSets)).append("\n");
        buffer.append("vertexBonusSets:").append(Arrays.deepToString(vertexBonusSets)).append("\n");
        buffer.append("vertexNbVisitSets:").append(Arrays.toString(vertexNbVisitSets)).append("\n");
        buffer.append("vertexNbBonusSets:").append(Arrays.toString(vertexNbBonusSets)).append("\n");
        buffer.append("edgeGreedy:").append(Arrays.toString(edgeGreedy)).append("\n");
        buffer.append("edgeDistance:").append(Arrays.toString(edgeDistance)).append("\n");
//        buffer.append("edgeTravelSets:" + Arrays.deepToString(edgeTravelSets) + "\n");
//        buffer.append("egdeNbTravelSets:" + Arrays.toString(edgeNbTravelSets) + "\n");
        buffer.append("startVertexes:").append(Arrays.toString(startVertexes)).append("\n");
        buffer.append("trainMaxMajors:").append(Arrays.toString(trainMaxMajors)).append("\n");
        buffer.append("trainMaxMinors:").append(Arrays.toString(trainMaxMinors)).append("\n");
        buffer.append("trainIgnoreMinors:").append(Arrays.toString(trainIgnoreMinors)).append("\n");

        return buffer.toString();
    }

}

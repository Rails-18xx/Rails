package rails.algorithms;

import java.util.Arrays;

import org.apache.log4j.Logger;

final class RevenueCalculator {
    
    private final int nbVertexes;
    private final int nbTrains;
    private final int nbEdges;
    private final int nbBonuses;
    
    // static vertex data
    private final int[][] vertexValueByTrain; // dimensions: vertexId, trainId
    private final boolean[] vertexMajor;
    private final boolean[] vertexMinor;
    private final boolean[] vertexSink;
    private final int[] vertexNbNeighbors;
    private final int[] vertexNbVisitSets;
    private final int[] vertexNbBonusSets;
    
    private final int[][] vertexNeighbors;
    private final int[][] vertexEdges;
    private final int[][] vertexVisitSets; // vertex belongs to a visit set, dimension: nbVertex x maxBlocks
    private final int[][] vertexBonusSets; // vertex belongs to a bonus set, dimension: nbVertex x nbBonuses

    // start vertexes
    private int[] startVertexes;
    
    // static edge data
    private final boolean[] edgeGreedy;
    private final int[] edgeDistance;
    
    // dynamic edge data
    private final boolean[] edgeUsed;
    
    // static train data
    private final int[] trainMaxMajors;
    private final int[] trainMaxMinors;
    private final int[] trainMaxBonuses;
    private final boolean[] trainIgnoreMinors;
    
    // dynamic train data
    private final int[] trainCurrentValue;
    private final int[] trainMajors;
    private final int[] trainMinors;
    private final int[] trainBonuses; // counts the number of bonuses received
    private final boolean[][] trainVisited;
    private final int[][] trainVertexStack;
//    private final int[][] trainEdgeStack;
    private final int[] trainStackPos;
    private final int [] trainBottomPos;
    private final int [] trainStartEdge;
    
    // static bonus data
    private final int [] bonusValue;
    private final boolean [][] bonusActiveForTrain; // dimensions: bonus x train
    private final int [] bonusRequiresVertices;
    
    // dynamic bonus data
    private final int [][] bonusTrainVertices;
    
    // run settings
    private int startTrainSet;
    private int finalTrainSet;
    private int startTrain;
    private int finalTrain;
    private boolean useRevenuePrediction;
    
    // current best run results
    private int currentBestValue;
    private final int [][] currentBestRun;
    
    // prediction data
    private int[] maxCumulatedTrainRevenues;
    private int[][] maxMajorRevenues; // dimensions trainId x nb vertex; 
    private int[][] maxMinorRevenues; // dimensions trainId x nb vertex;
    private int[][] maxBonusRevenues; // dimensions trainId x nb bonuses

    // statistic data
    private int countVisits;
    private int countEdges;
    private int nbEdgesTravelled;
    private int nbEvaluations;
    private int nbPredictions;

    // revenue Adapter
    private RevenueAdapter revenueAdapter;
    
    // termination results
    private static enum Terminated {
        WithEvaluation,
        WithoutEvaluation,
        NotYet
        
    }
    
    protected static Logger log =
        Logger.getLogger(RevenueCalculator.class.getPackage().getName());

    
    public RevenueCalculator (RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges, 
            int maxNeighbors, int maxVertexSets, int nbTrains, int nbBonuses) {
        
        log.info("RC defined: nbVertexes = " + nbVertexes + ", nbEdges = " + nbEdges + ", maxNeighbors = " + maxNeighbors +
                ", maxVertexSets = " + maxVertexSets + ", nbTrains = " + nbTrains + ", nbBonuses = " + nbBonuses );

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
        edgeUsed = new boolean[nbEdges];
        
        trainMaxMajors = new int[nbTrains];
        trainMaxMinors = new int[nbTrains];
        trainMaxBonuses = new int[nbTrains]; // only required for revenue prediction
        trainIgnoreMinors = new boolean[nbTrains];
        
        trainCurrentValue = new int[nbTrains];
        trainMajors = new int[nbTrains];
        trainMinors = new int[nbTrains];
        trainBonuses = new int[nbTrains];
        trainVisited = new boolean[nbTrains][nbVertexes];
        trainVertexStack = new int[nbTrains][nbVertexes + 1];
        // increase necessary due to buttom train
//        trainEdgeStack = new int[nbTrains][nbVertexes + 1];
        trainStackPos = new int[nbTrains];
        trainBottomPos = new int[nbTrains];
        trainStartEdge = new int[nbTrains];
        
        bonusValue = new int[nbBonuses];
        bonusRequiresVertices = new int[nbBonuses];
        bonusActiveForTrain = new boolean[nbBonuses][nbTrains];
        bonusTrainVertices = new int[nbBonuses][nbTrains];
        
        currentBestRun = new int[nbTrains][nbVertexes + 1];
        
        useRevenuePrediction = false;
    }

    void setVertex(int id, boolean major, boolean minor, boolean sink) {
        vertexMajor[id] = major;
        vertexMinor[id] = minor;
        vertexSink[id] = sink;
        // default neighbors && visit and bonus sets
        vertexNbNeighbors[id] = 0;
        vertexNbVisitSets[id] = 0;
        vertexNbBonusSets[id] = 0;
    }
    
    void setVertexValue(int vertexId, int trainId, int value) {
        vertexValueByTrain[vertexId][trainId] = value;
    }

    void setVertexNeighbors(int id, int[] neighbors, int[] edges) {
        // copy neighbors
        for (int j=0; j < neighbors.length; j++) {
                vertexNeighbors[id][j] = neighbors[j];
                vertexEdges[id][j] = edges[j];
        }
        vertexNbNeighbors[id] = neighbors.length;
        
    }
   
    void setStartVertexes(int[] startVertexes) {
        this.startVertexes = startVertexes;
    }
    
    
    void setEdge(int edgeId, boolean greedy, int distance) {
        edgeGreedy[edgeId] = greedy;
        edgeDistance[edgeId] = distance;
    }
    
    void setTrain(int id, int majors, int minors, boolean ignoreMinors) {
        trainMaxMajors[id] = majors;
        trainMaxMinors[id] = minors;
        trainMaxBonuses[id] = 0;
        trainIgnoreMinors[id] = ignoreMinors;
    }
    
    void setVisitSet(int[] vertices) {
        for (int j=0; j < vertices.length; j++) {
            int vertexId = vertices[j];
            for (int k=0; k < vertices.length; k++) {
                if (k == j) continue;
                vertexVisitSets[vertexId][vertexNbVisitSets[vertexId]++] = vertices[k];
            }
        }
    }
    
    void setBonus(int id, int value, int[] vertices, boolean[] bonusForTrain) {
        log.info("RC: define bonus value = " + value + ", vertices = " + Arrays.toString(vertices) +
                ", bonusForTrain = " + Arrays.toString(bonusForTrain));
        
        bonusValue[id] = value;
        bonusRequiresVertices[id] = vertices.length;
        for (int j=0; j < vertices.length; j++) {
            int vertexId = vertices[j];
            vertexBonusSets[vertexId][vertexNbBonusSets[vertexId]++] = id;
        }
        bonusActiveForTrain[id] = bonusForTrain;
    }
    
    int[][] getOptimalRun() {
        log.info("RC: currentBestRun = " + Arrays.deepToString(currentBestRun));
        return currentBestRun;
    }
    
    int getNumberOfEvaluations() {
        return nbEvaluations;
    }
    
    String getStatistics() {
        StringBuffer statistics = new StringBuffer();
        statistics.append(nbEvaluations + " evaluations");
        if (useRevenuePrediction)
            statistics.append(", " + nbPredictions + " predictions");
        statistics.append(" and " + nbEdgesTravelled + " edges travelled.");
        return statistics.toString();
    }
    
    private void notifyRevenueAdapter(final int revenue, final boolean finalResult) {
        String modifier;
        if (finalResult)
            modifier = "final";
        else
            modifier = "new best";
        log.info("Report " + modifier + " result of " +  revenue + " after " + getStatistics());
        revenueAdapter.notifyRevenueListener(revenue, finalResult);
    }

    private int[] bestRevenues(int[] values, int length) {
        int[] bestRevenues = new int[length + 1];
        Arrays.sort(values);
        int cumulatedRevenues = 0;
        for (int j=1; j <= length ; j++) {
            cumulatedRevenues += values[values.length - j];
            bestRevenues[j] = cumulatedRevenues;
        }
        log.debug("Best Revenues = " + Arrays.toString(bestRevenues));
        return bestRevenues;
    }
    
    private void initRevenueValues(int startTrain, int finalTrain){
        
      // intialize values
        maxMajorRevenues = new int[nbTrains][nbVertexes];
        maxMinorRevenues = new int[nbTrains][nbVertexes];
        maxBonusRevenues = new int[nbTrains][nbVertexes + nbBonuses];
        int cumulatedRevenues = 0;
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
            cumulatedRevenues += maxMajorRevenues[t][trainMaxMajors[t]] + maxMinorRevenues[t][trainMaxMinors[t]] 
                     + maxBonusRevenues[t][trainMaxBonuses[t]];
            maxCumulatedTrainRevenues[t] = cumulatedRevenues;
        }
        log.info("maxMajorRevenues = " + Arrays.deepToString(maxMajorRevenues));
        log.info("maxMinorRevenues = " + Arrays.deepToString(maxMinorRevenues));
        log.info("maxBonusRevenues = " + Arrays.deepToString(maxBonusRevenues));
        log.info("maxCumulatedTrainRevenues = " + Arrays.toString(maxCumulatedTrainRevenues));
    }
    
    void initialPredictionRuns(int startTrain, int finalTrain) {
        
        if (startTrain > finalTrain) return;

        this.startTrainSet = startTrain;
        this.finalTrainSet = finalTrain;
        useRevenuePrediction = true;
        this.maxCumulatedTrainRevenues = new int[nbTrains];
        for (int i=0; i < nbTrains; i++) {
            currentBestRun[i][0] = -1;
        }

        initRevenueValues(startTrain, finalTrain);
        
        if (startTrain == finalTrain) return;
        
        // start prediction runs
        nbEvaluations = 0; nbPredictions = 0; nbEdgesTravelled = 0;

        log.info("RC: start individual prediction Runs");
        int[] maxSingleTrainRevenues = new int[nbTrains]; 
        for (int j = startTrain; j <= finalTrain; j++) {
            this.startTrain = j;
            this.finalTrain = j;
            currentBestValue = 0;
            runTrain(j);
            log.info("RC: Best prediction run of train number " + j + " value = " + currentBestValue + 
                " after " + getStatistics());
            maxSingleTrainRevenues[j] = currentBestValue;
        }
        int cumulatedRevenues = 0;
        for (int j = finalTrain; j >= startTrain; j--) {
            cumulatedRevenues +=  maxSingleTrainRevenues[j];
            maxCumulatedTrainRevenues[j] = cumulatedRevenues;
        }
        log.info("maxCumulatedTrainRevenues = " + Arrays.toString(maxCumulatedTrainRevenues));
        
        if (startTrain == finalTrain - 1) return;

        log.info("RC: start combined prediction runs");
        this.finalTrain = finalTrain;
        for (int j=finalTrain - 1; j > startTrain; j--) {
            this.startTrain = j;
//            currentBestValue = 0;
            runTrain(j);
            log.info("RC: Best prediction run until train nb. " + j + " value = " + currentBestValue +
                " after " + getStatistics());
            maxCumulatedTrainRevenues[j] = currentBestValue;
            maxCumulatedTrainRevenues[j-1] = currentBestValue  + maxSingleTrainRevenues[j-1];
            log.info("maxCumulatedTrainRevenues = " + Arrays.toString(maxCumulatedTrainRevenues));
        }
    }
    
    int calculateRevenue(int startTrain, int finalTrain) {
        log.info("RC: calculateRevenue trains from " + startTrain + " to " + finalTrain);

        if (!useRevenuePrediction) {
            for (int i=0; i < nbTrains; i++) {
                currentBestRun[i][0] = -1;
            }
        }
        
        this.startTrainSet = startTrain;
        this.finalTrainSet = finalTrain;

        this.startTrain = startTrain;
        this.finalTrainSet = finalTrain;
       
        runTrain(startTrain);

        // inform revenue listener via adapter
        notifyRevenueAdapter(currentBestValue, true);

        return currentBestValue;
    }
   
    private void runTrain(int trainId) {
        log.debug("RC: runTrain " + trainId);
        
        // initialize value
        trainCurrentValue[trainId] = 0;

        // initialize train lengths
        trainMajors[trainId] = trainMaxMajors[trainId];
        trainMinors[trainId] = trainMaxMinors[trainId];
        trainBonuses[trainId] = trainMaxBonuses[trainId];
        
        // initialize the positions
        trainStackPos[trainId] = 0;
        trainBottomPos[trainId] = 0;
        
        // initialize bonuses
        for (int b=0; b < nbBonuses; b++) {
            bonusTrainVertices[b][trainId] = bonusRequiresVertices[b];
        }
        
        // check if the revenue is enough
        if (useRevenuePrediction && predictRevenues(trainId))
            return;

        // try all startVertexes
        for (int i=0; i < startVertexes.length; i++) {
            int vertexId = startVertexes[i];
            log.debug("RC: Using startVertex nr. " + i + " for train " + trainId);
            boolean stationVertex = encounterVertex(trainId, vertexId, true);
            if (stationVertex) {
                // train cannot terminate at start vertex
                if (useRevenuePrediction && predictRevenues(trainId)) {
                    // cannot beat current best value => leave immediately
                    encounterVertex(trainId, vertexId, false);
                    // but keep them on the visited vertex list to avoid route duplication
                    trainVisited[trainId][vertexId] = true;
                    log.debug("RC: finished startVertex " + vertexId + " for train " +trainId);
                    continue;
                }
            }

            // then try all edges of it
            // for startVertices the sink property is ignored
            for (int j = 0; j < vertexNbNeighbors[vertexId]; j++) {
                int edgeId = vertexEdges[vertexId][j];
                if (edgeUsed[edgeId]) continue; 
                log.debug("RC: Testing Neighbor Nr. " + j + " of startVertex");
                int neighborId = vertexNeighbors[vertexId][j];
                if (trainVisited[trainId][neighborId]) {
                    log.debug("RC: Hex already visited");
                    continue;
                }
                if (travelEdge(trainId, edgeId, true)) {
                    trainStartEdge[trainId] = j; // store start edge
                    nextVertex(trainId, neighborId, edgeGreedy[edgeId]);
                    returnEdge(edgeId);
                }
            }

            // no more edges to find
            encounterVertex(trainId, vertexId, false);
            // keep them on the visited vertex list to avoid route duplication
            trainVisited[trainId][vertexId] = true;
            log.debug("RC: finished startVertex " + vertexId + " for train " +trainId);
        }

        // finished all tries
        for (int i=0; i < startVertexes.length; i++) {
            // remove all of them from the visited vertex list
            trainVisited[trainId][startVertexes[i]] = false;
        }

        // allow that the train does not run at all
        finalizeVertex(trainId, -1);
        
        log.debug("RC: finishTrain " + trainId);
    }
    
    private void runBottom(int trainId) {
        log.debug("RC: runBottom " +trainId);
        
        // use startvertex, check if it is a sink
        int vertexId = trainVertexStack[trainId][0];
        if (vertexSink[vertexId]) {
            log.debug("RC: startvertex is sink, finished bottom of " + trainId);
            return;
        }
                
        // push to stack
        trainBottomPos[trainId] = trainStackPos[trainId]; // store the stack position where bottom starts
        trainVertexStack[trainId][trainStackPos[trainId]++] = vertexId;
        log.debug("RC: Restart at bottom at stack position " + trainBottomPos[trainId]);
        
        for (int j = trainStartEdge[trainId] + 1; j < vertexNbNeighbors[vertexId]; j++) {
            int edgeId = vertexEdges[vertexId][j]; 
            if (edgeUsed[edgeId]) continue; 
            int neighborId = vertexNeighbors[vertexId][j];
            log.debug("RC: Testing Neighbor Nr. " + j + " of bottomVertex is " + neighborId);
            if (trainVisited[trainId][neighborId]) {
                log.debug(" RC: Hex already visited");
                continue;
            }
            if (travelEdge(trainId, edgeId, true)) {
                nextVertex(trainId, neighborId, edgeGreedy[edgeId]);
                returnEdge(edgeId);
            }
        }
        
        trainStackPos[trainId]--; // pull from stack
        trainBottomPos[trainId] = 0;
        log.debug("RC: finished bottom of " + trainId);
    }

    /**
     * arrives at an unvisited vertex
     */
    private void nextVertex(int trainId, int vertexId, boolean previousGreedy) {

        // 1. encounterVertex adds value and returns true if value vertex
        Terminated trainTerminated = Terminated.NotYet; 
        boolean stationVertex = encounterVertex(trainId, vertexId, true);
        if (stationVertex) {
            // check usual train termination
            trainTerminated = trainTerminated(trainId);
            if (trainTerminated == Terminated.WithoutEvaluation ||
                    useRevenuePrediction && predictRevenues(trainId)) {
                // cannot beat current best value => leave immediately
                encounterVertex(trainId, vertexId, false);
//                returnEdge(trainId);
                return;
            }
        }
        
        // 2a. visit neighbors, if train has not terminated and vertex is not a sink
        if (trainTerminated == Terminated.NotYet) {
            if (!vertexSink[vertexId]) {
                for (int j = 0; j < vertexNbNeighbors[vertexId]; j++) {
                    int edgeId = vertexEdges[vertexId][j];
                    if (edgeUsed[edgeId]) continue;
                    int neighborId = vertexNeighbors[vertexId][j];
                    log.debug("RC: Testing Neighbor Nr. " + j + " of " + vertexId + " is " + neighborId);
                    if (trainVisited[trainId][neighborId]) {
                        log.debug("RC: Hex already visited");
                        continue;
                    }
                    if (travelEdge(trainId, edgeId, previousGreedy)) {
                        nextVertex(trainId, neighborId, edgeGreedy[edgeId]);
                        returnEdge(edgeId);
                    }
                }
            }
            // 2b. restart at startVertex for bottom part
            if (stationVertex && trainBottomPos[trainId] == 0){
                runBottom(trainId);
            }
        }
        
        // 3. no more edges to visit from here => evaluate or start new train
        if (stationVertex)
            finalizeVertex(trainId, vertexId);
        
        // 4. then leave that vertex
        encounterVertex(trainId, vertexId, false);
//        returnEdge(trainId);
    }
    
    private boolean encounterVertex(int trainId, int vertexId, boolean arrive) {

        log.debug("RC: EncounterVertex, trainId = " + trainId + " vertexId = " + vertexId + " arrive = " + arrive);
        
        // set visit to true if arriving, otherwise you leave
        trainVisited[trainId][vertexId] = arrive;

        boolean stationVertex = false;
        if (arrive) {
            trainCurrentValue[trainId] += vertexValueByTrain[vertexId][trainId];
            if (vertexMajor[vertexId]) {
                trainMajors[trainId]--;
                stationVertex = true;
            } else if (vertexMinor[vertexId]) {
                trainMinors[trainId]--;
                stationVertex = !trainIgnoreMinors[trainId];
            }
            trainVertexStack[trainId][trainStackPos[trainId]++] = vertexId; // push to stack
            countVisits++;
        } else {
            trainCurrentValue[trainId] -= vertexValueByTrain[vertexId][trainId];
            if (vertexMajor[vertexId]) {
                trainMajors[trainId]++;
                stationVertex = true;
            } else if (vertexMinor[vertexId]) {
                trainMinors[trainId]++;
                stationVertex = !trainIgnoreMinors[trainId];
            }
            trainStackPos[trainId]--; // pull from stack
            countVisits--;
        }   
        
        // check vertex sets
        for (int j=0; j < vertexNbVisitSets[vertexId]; j++) {
            trainVisited[trainId][vertexVisitSets[vertexId][j]] = arrive;
            log.debug("RC: visited = " + arrive + " for vertex " + vertexVisitSets[vertexId][j] + " due to block rule");
        }
        
        // check bonus sets
        for (int j=0; j < vertexNbBonusSets[vertexId]; j++) {
            int bonusId = vertexBonusSets[vertexId][j];
            if (!bonusActiveForTrain[bonusId][trainId]) continue;
            if (arrive) { 
                bonusTrainVertices[bonusId][trainId]--;
                log.debug("RC: Decreased bonus " + bonusId + " to " + bonusTrainVertices[bonusId][trainId]);
                if (bonusTrainVertices[bonusId][trainId] == 0) {
                   trainCurrentValue[trainId] += bonusValue[bonusId];
                   if (bonusValue[bonusId] > 0) trainBonuses[trainId]--; 
                   log.debug("RC: Added bonus " + bonusId + " with value " + bonusValue[bonusId]);
                }
            } else {
                if (bonusTrainVertices[bonusId][trainId] == 0) {
                    trainCurrentValue[trainId] -= bonusValue[bonusId];
                    if (bonusValue[bonusId] > 0) trainBonuses[trainId]++; 
                    log.debug("RC: Removed bonus " + bonusId + " with value " + bonusValue[bonusId]);
                 }
                bonusTrainVertices[bonusId][trainId]++;
                log.debug("RC: Increases bonus " + bonusId + " to " + bonusTrainVertices[bonusId][trainId]);
            }
        }
        
        log.debug("RC: stationVertex = " + stationVertex);
        log.debug("RC: Count Visits = " + countVisits);
        return stationVertex;
    }
    
    private boolean travelEdge(int trainId, int edgeId, boolean previousGreedy) {
        if (previousGreedy || edgeGreedy[edgeId]) {
            log.debug("RC: Travel edge id " + edgeId);
            edgeUsed[edgeId] = true;
//            trainEdgeStack[trainId][trainStackPos[trainId]] = edgeId;
            countEdges++; nbEdgesTravelled++;
            log.debug("RC: Count Edges = " + countEdges);
            return true;
        } else {
            log.debug("RC: Cannot travel edge id " + edgeId + ", because of greedy rule");
            return false;
        }
    }

    private void returnEdge(int edgeId) {
        if (edgeUsed[edgeId]) {
            edgeUsed[edgeId] = false;
            countEdges--;
            log.debug("RC: Cleared edge id " + edgeId);
            log.debug("RC: Count Edges = " + countEdges);
        } else {
            log.debug("RC: Error return edge id used: " + edgeId);
        }
    }
    
    private Terminated trainTerminated(int trainId) {
        Terminated terminated = Terminated.NotYet;
        if (trainIgnoreMinors[trainId]) {
            // express trains
            if (trainMajors[trainId] == 0)
                terminated = Terminated.WithEvaluation;
        } else { // default and plus trains
            if (trainMajors[trainId] < 0){
                terminated = Terminated.WithoutEvaluation;
            } else if (trainMajors[trainId] + trainMinors[trainId] == 0)
                terminated = Terminated.WithEvaluation;
        }
        if (terminated != Terminated.NotYet) {
            log.debug ("RC: Train " + trainId + " has terminated: " +
            		"majors = " + trainMajors[trainId] + " minors = " + trainMinors[trainId]);
        }
        return terminated;
    }
    
    private void finalizeVertex(int trainId, int vertexId) {
        log.debug("RC: Finalize Vertex id " + vertexId + " for train " + trainId);
        
        if (trainId == finalTrain) {
            evaluateResults();
        } else {
            runTrain(trainId + 1);
        }
    }

    private void evaluateResults() {
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
        
        nbEvaluations++;
        log.debug("RC: current total value " + totalValue);
        
        // compare to current best result
        if (totalValue > currentBestValue) {
            currentBestValue = totalValue;
            // exceed thus deep copy of vertex stack
            for (int j = startTrainSet; j <= finalTrainSet; j++) {
                for (int v = 0; v < nbVertexes + 1; v++) {
                    if (v < trainStackPos[j]) {
                        currentBestRun[j][v] = trainVertexStack[j][v];
                    } else {
                        currentBestRun[j][v] = -1; // terminator
                        break;
                    }
                }
            }
            log.info("RC: Found better run with " + totalValue);
            // inform revenue listener via adapter
            notifyRevenueAdapter(currentBestValue, false);
        }
    }
    
    // predict revenues and returns true if best value can still be exceeded
    private boolean predictRevenues(int trainId){
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
            // default and plus trains
            if (trainMinors[trainId] > 0){
                trainValue += maxMajorRevenues[trainId][trainMajors[trainId]];
                trainValue += maxMinorRevenues[trainId][trainMinors[trainId]];
            } else { // <= 0
                trainValue += maxMajorRevenues[trainId][trainMajors[trainId] + trainMinors[trainId]]; 
            }
        }
        // add potential bonuses
        if (trainBonuses[trainId] != 0) {
            trainValue += maxBonusRevenues[trainId][trainBonuses[trainId]];
        }
        log.debug("RC: Current train has predicted  value of " + trainValue);

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

        nbPredictions++;
        
        boolean terminate = (totalValue <= currentBestValue);
        if (terminate) log.debug("Run terminated due to predicted value of " +  totalValue);

        return terminate;
    }
    
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("vertexValuesByTrain:" + Arrays.deepToString(vertexValueByTrain) + "\n");
        buffer.append("vertexMajor:" + Arrays.toString(vertexMajor) + "\n");
        buffer.append("vertexMinor:" + Arrays.toString(vertexMinor) + "\n");
        buffer.append("vertexNeighbors:" + Arrays.deepToString(vertexNeighbors) + "\n");
        buffer.append("vertexEdges:" + Arrays.deepToString(vertexEdges) + "\n");
        buffer.append("vertexVisitSets:" + Arrays.deepToString(vertexVisitSets) + "\n");
        buffer.append("vertexBonusSets:" + Arrays.deepToString(vertexBonusSets) + "\n");
        buffer.append("vertexNbVisitSets:" + Arrays.toString(vertexNbVisitSets) + "\n");
        buffer.append("vertexNbBonusSets:" + Arrays.toString(vertexNbBonusSets) + "\n");
        buffer.append("edgeGreedy:" + Arrays.toString(edgeGreedy) + "\n");
        buffer.append("edgeDistance:" + Arrays.toString(edgeDistance) + "\n");
        buffer.append("startVertexes:" + Arrays.toString(startVertexes) + "\n"); 
        buffer.append("trainMaxMajors:" + Arrays.toString(trainMaxMajors) + "\n"); 
        buffer.append("trainMaxMinors:" + Arrays.toString(trainMaxMinors) + "\n"); 
        buffer.append("trainIgnoreMinors:" + Arrays.toString(trainIgnoreMinors) + "\n"); 
      
        return buffer.toString();
    }
    
}

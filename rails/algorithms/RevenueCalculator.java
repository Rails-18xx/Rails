package rails.algorithms;

import java.util.Arrays;

import org.apache.log4j.Logger;

final class RevenueCalculator {
    
    private final int nbVertexes;
    private final int maxNeighbors;
    private final int nbTrains;
    
    // static vertex data
    private final int[] vertexValue;
    private final boolean[] vertexCity;
    private final boolean[] vertexTown;
    private final int[][] vertexNeighbors;

    // start vertexes
    private int[] startVertexes;
    
    // static edge data
    private final boolean[][] edgeGreedy;
    private final int[][] edgeDistance;
    
    // dynamic edge data
    private final boolean[][] edgeUsed;
    
    // static train data
    private final int[] trainMaxCities;
    private final int[] trainMaxTowns;
    private final boolean[] trainIgnoreTowns;
    private final int[] trainMultiplyCities;
    private final int[] trainMultiplyTowns;
    
    // dynamic train data
    private final int[] trainCurrentValue;
    private final int[] trainCities;
    private final int[] trainTowns;
    private final boolean[][] trainVisited;
    private final int[][] trainVertexStack;
    private final int[] trainStackPos;
    private final int [] trainBottomPos;
    private final int [] trainStartEdge;
    
    // dynamic run data
    private int finalTrain;
    private boolean useRevenuePrediction;
    
    private int currentBestValue;
    private final int [][] currentBestRun;
    
    private int countVisits;
    private int countEdges;
    private int nbEdges;
    private int nbEvaluations;
    private int nbPredictions;

    // prediction data
    private int[] maxCityRevenues;
    private int[] maxTownRevenues;
    private int[] maxTrainRevenues;
    
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

    
    public RevenueCalculator (RevenueAdapter revenueAdapter, int nbVertexes, int maxNeighbors, int nbTrains) {
        
        this.revenueAdapter = revenueAdapter;
        this.nbVertexes = nbVertexes;
        this.maxNeighbors = maxNeighbors;
        this.nbTrains = nbTrains;
        
        log.debug("RC defined: nbVertexes = " + nbVertexes + ", maxNeighbors = " + maxNeighbors + ", nbTrains = " + nbTrains);
        
        // initialize all required variables
        vertexValue = new int[nbVertexes];
        vertexCity = new boolean[nbVertexes];
        vertexTown = new boolean[nbVertexes];
        vertexNeighbors = new int[nbVertexes][maxNeighbors];
        
        edgeGreedy = new boolean[nbVertexes][nbVertexes];
        edgeDistance = new int[nbVertexes][nbVertexes];
        edgeUsed = new boolean[nbVertexes][nbVertexes];
        
        trainCities = new int[nbTrains];
        trainTowns = new int[nbTrains];
        trainIgnoreTowns = new boolean[nbTrains];
        trainMultiplyCities = new int[nbTrains];
        trainMultiplyTowns = new int[nbTrains];
        
        trainCurrentValue = new int[nbTrains];
        trainMaxCities = new int[nbTrains];
        trainMaxTowns = new int[nbTrains];
        trainVisited = new boolean[nbTrains][nbVertexes];
        trainVertexStack = new int[nbTrains][nbVertexes];
        trainStackPos = new int[nbTrains];
        trainBottomPos = new int[nbTrains];
        trainStartEdge = new int[nbTrains];
        
        currentBestRun = new int[nbTrains][nbVertexes];
        
        useRevenuePrediction = false;
        
    }

    void setVertex(int id, int value, boolean city, boolean town, int[]neighbors) {
        vertexValue[id] = value;
        vertexCity[id] = city;
        vertexTown[id] = town;
        vertexNeighbors[id] = neighbors;
    }
    
    void setStartVertexes(int[] startVertexes) {
        this.startVertexes = startVertexes;
    }
    
    void setEdge(int vertexLo, int vertexHi, boolean greedy, int distance) {
        edgeGreedy[vertexLo][vertexHi] = greedy;
        edgeDistance[vertexLo][vertexHi] = distance;
    }
    
    void setTrain(int id, int cities, int towns, boolean ingoreTowns, int multiplyCities, int multiplyTowns) {
        trainMaxCities[id] = cities;
        trainMaxTowns[id] = towns;
        trainIgnoreTowns[id] = ingoreTowns;
        trainMultiplyCities[id] = multiplyCities;
        trainMultiplyTowns[id] = multiplyTowns;
    }
    
    void setPredictionData(int[] maxCityRevenues, int[] maxTownRevenues) {
        this.maxCityRevenues = maxCityRevenues;
        this.maxTownRevenues = maxTownRevenues;
        useRevenuePrediction = true;
    }
    
    int[][] getOptimalRun() {
        return currentBestRun;
    }
    
    int getNumberOfEvaluations() {
        return nbEvaluations;
    }
    
    private void notifyRevenueAdapter(final int revenue, final boolean finalResult) {
        String modifier;
        if (finalResult)
            modifier = "final";
        else
            modifier = "new best";
        StringBuffer statistics = new StringBuffer();
        statistics.append(nbEvaluations + " evaluations");
        if (useRevenuePrediction)
            statistics.append(", " + nbPredictions + " predictions");
        statistics.append(" and " + nbEdges + " edges travelled.");
        log.info("Report " + modifier + " result of " +  revenue + " after " + statistics.toString());
        revenueAdapter.notifyRevenueListener(revenue, finalResult);
    }
    
    int calculateRevenue(int startTrain, int finalTrain) {
        log.info("RC: calculateRevenue trains from " + startTrain + " to " + finalTrain);
        
        nbEvaluations = 0; nbPredictions = 0; nbEdges = 0;

        // initialize maximum train revenues 
        if (useRevenuePrediction) {
            maxTrainRevenues = new int[nbTrains];
            for (int j=0; j < nbTrains; j++) {
                maxTrainRevenues[j] = maxCityRevenues[trainMaxCities[j]] * trainMultiplyCities[j] + 
                maxTownRevenues[trainMaxTowns[j]] * trainMultiplyTowns[j];
            }
        }
        
        this.finalTrain = finalTrain;
        runTrain(startTrain);

        // inform revenue listener via adapter
        notifyRevenueAdapter(currentBestValue, true);

        return currentBestValue;
    }
    
    private void runTrain(int trainId) {
        log.debug("RC: runTrain " + trainId);
        
        // initialize the positions
        trainStackPos[trainId] = 0;
        trainBottomPos[trainId] = 0;

        // try all startVertexes
        for (int i=0; i < startVertexes.length; i++) {
            int vertexId = startVertexes[i];
            log.debug("RC: Using startVertex nr. " + i + " for train " + trainId);

            // encounterVertex adds value and returns true if value vertex
            Terminated trainTerminated = Terminated.NotYet; 
            boolean valueStation = encounterVertex(trainId, vertexId, true);
            if (valueStation) {
                // check usual train termination
                trainTerminated = trainTerminated(trainId);
                if (trainTerminated == Terminated.WithoutEvaluation || 
//                        trainTerminated == Terminated.NotYet && 
                        useRevenuePrediction && predictRevenues(trainId)) {
                    // cannot beat current best value => leave immediately
                    encounterVertex(trainId, vertexId, false);
                    log.debug("RC: finished startVertex " + vertexId + " for train " +trainId);
                    continue;
                }
            }
            
            // and all edges of it
            if (trainTerminated == Terminated.NotYet) {
                for (int j = 0; j < maxNeighbors; j++) {
                    int neighborId = vertexNeighbors[vertexId][j];
                    log.debug("RC: Testing Neighbor Nr. " + j + " of startVertex is " + neighborId);
                    if (neighborId == -1) break; // no more neighbors
                    if (travelEdge(vertexId, neighborId, true)) {
                        trainStartEdge[trainId] = j; // store edge
                        nextVertex(trainId, neighborId, vertexId);
                    }
                }
            }

            // no more edges to find
            finalizeVertex(trainId, vertexId, valueStation);
            encounterVertex(trainId, vertexId, false);
            log.debug("RC: finished startVertex " + vertexId + " for train " +trainId);
        }
        log.debug("RC: finishTrain " + trainId);
    }
    
    private void runBottom(int trainId) {
        log.debug("RC: runBottom " +trainId);
        
        trainBottomPos[trainId] = trainStackPos[trainId]; // store the stack position where bottom starts
        log.debug("RC: Restart at bottom at stack position " + trainBottomPos[trainId]);
        
        // use startvertex
        int vertexId = trainVertexStack[trainId][0];
        trainVertexStack[trainId][trainStackPos[trainId]++] = vertexId; // push to stack
        
        for (int j = trainStartEdge[trainId] + 1; j < maxNeighbors; j++) {
            int neighborId = vertexNeighbors[vertexId][j];
            log.debug("RC: Testing Neighbor Nr. " + j + " of bottomVertex is " + neighborId);
            if (neighborId == -1) break; // no more neighbors
            if (trainVisited[trainId][neighborId]) {
                log.debug(" RC: Hex already visited");
                continue;
            }
            if (travelEdge(vertexId, neighborId, true)) {
                nextVertex(trainId, neighborId, vertexId);
            }
        }
        // no more edges to find
//        finalizeVertex(trainId);
        
        trainStackPos[trainId]--; // pull from stack
        trainBottomPos[trainId] = 0;
        log.debug("RC: finished bottom of " + trainId);
    }

    /**
     * arrives at an unvisited vertex
     */
    private void nextVertex(int trainId, int vertexId, int previousId) {

        // 1. encounterVertex adds value and returns true if value vertex
        Terminated trainTerminated = Terminated.NotYet; 
        boolean valueStation = encounterVertex(trainId, vertexId, true);
        if (valueStation) {
            // check usual train termination
            trainTerminated = trainTerminated(trainId);
            if (trainTerminated == Terminated.WithoutEvaluation ||
//                    trainTerminated == Terminated.NotYet &&
                    useRevenuePrediction && predictRevenues(trainId)) {
                // cannot beat current best value => leave immediately
                encounterVertex(trainId, vertexId, false);
                returnEdge(trainId);
                return;
            }
        }
        
        // 2a. visit neighbors, if train has not terminated
        if (trainTerminated == Terminated.NotYet) {
            for (int j = 0; j < maxNeighbors; j++) {
                int neighborId = vertexNeighbors[vertexId][j];
                log.debug("RC: Testing Neighbor Nr. " + j + " of " + vertexId + " is " + neighborId);
                if (neighborId == -1) break; // no more neighbors
                if (trainVisited[trainId][neighborId]) {
                    log.debug("RC: Hex already visited");
                    continue;
                }
                if (travelEdge(vertexId, neighborId, edgeGreedy[previousId][vertexId])) {
                    nextVertex(trainId, neighborId, vertexId);
                }
            }
            // 2b. restart at startVertex for bottom part
            if (valueStation && trainBottomPos[trainId] == 0 && (vertexCity[vertexId] || vertexTown[vertexId])){
                runBottom(trainId);
            }
        }
        
        // 3. no more edges to visit from here => evaluate or start new train
        finalizeVertex(trainId, vertexId, valueStation);
        
        // 4. then leave that vertex
        encounterVertex(trainId, vertexId, false);
        returnEdge(trainId);
    }
    
    private boolean encounterVertex(int trainId, int vertexId, boolean arrive) {

        log.debug("RC: EncounterVertex, trainId = " + trainId + " vertexId = " + vertexId + " arrive = " + arrive);
        
        // set visit to true if arriving, otherwise you leave
        trainVisited[trainId][vertexId] = arrive;
        
        boolean valueVertex = false;
        if (arrive) {
            if (vertexCity[vertexId]) {
                trainCities[trainId]++;
                trainCurrentValue[trainId] += vertexValue[vertexId] * trainMultiplyCities[trainId];
                valueVertex = true;
            } else if (vertexTown[vertexId] && !trainIgnoreTowns[trainId]) {
                trainTowns[trainId]++;
                trainCurrentValue[trainId] += vertexValue[vertexId] * trainMultiplyTowns[trainId];
                valueVertex = true;
            }
            trainVertexStack[trainId][trainStackPos[trainId]++] = vertexId; // push to stack
            countVisits++;
        } else {
            if (vertexCity[vertexId]) {
                trainCities[trainId]--;
                trainCurrentValue[trainId] -= vertexValue[vertexId] * trainMultiplyCities[trainId];
                valueVertex = true;
            } else if (vertexTown[vertexId] && !trainIgnoreTowns[trainId] ) {
                trainTowns[trainId]--;
                trainCurrentValue[trainId] -= vertexValue[vertexId] * trainMultiplyTowns[trainId];
                valueVertex = true;
            }
            trainStackPos[trainId]--; // pull from stack
            countVisits--;
        }
        log.debug("RC: Count Visits = " + countVisits);
        return valueVertex;
    }
    
    private boolean travelEdge(int startVertex, int endVertex, boolean previousGreedy) {
        if (edgeUsed[startVertex][endVertex]) {
            log.debug("RC: Edge from " + startVertex + " to " + endVertex + " already used" );
            return false;
        } else if (previousGreedy || edgeGreedy[startVertex][endVertex]) {
            log.debug("RC: Travel edge from " + startVertex + " to " + endVertex );
            edgeUsed[startVertex][endVertex] = true;
            edgeUsed[endVertex][startVertex] = true;
            countEdges++; nbEdges++;
            log.debug("RC: Count Edges = " + countEdges);
            return true;
        } else {
            log.debug("RC: Cannot travel from " + startVertex + " to " + endVertex + ", because of greedy rule");
            return false;
        }
    }

    private void returnEdge(int trainId) {
        int stackPos = trainStackPos[trainId];
        log.debug("RC: Tries to clear edge at stack position " + stackPos + " of train " + trainId);
        
        if (stackPos == 0) {
            log.debug("RC: Position zero has not to be cleared");
            return;
        }
        
        if (stackPos == trainBottomPos[trainId]) {
            log.debug("RC: Replace start Vertex for bottom position");
        }
        
        int startVertex = trainVertexStack[trainId][stackPos];
        int endVertex = trainVertexStack[trainId][stackPos - 1];
        
        if (edgeUsed[startVertex][endVertex]) {
            edgeUsed[startVertex][endVertex] = false;
            edgeUsed[endVertex][startVertex] = false;
            countEdges--;
            log.debug("RC: Cleared edge from " + startVertex + " to " + endVertex);
            log.debug("RC: Count Edges = " + countEdges);
        } else {
            log.debug ("RC: Error return edge not used: " + startVertex + " to " + endVertex);
        }
    }
    
    private Terminated trainTerminated(int trainId) {
        Terminated terminated = Terminated.NotYet;
        if (trainIgnoreTowns[trainId]) {
            // express trains
            if (trainCities[trainId] == trainMaxCities[trainId])
                terminated = Terminated.WithEvaluation;
        } else if (trainTowns[trainId] == 0) {
            // default train
            if (trainCities[trainId] + trainTowns[trainId] == trainMaxCities[trainId])
                terminated = Terminated.WithEvaluation;
        } else {
            // plus trains
            if (trainCities[trainId] > trainMaxCities[trainId]){
                terminated = Terminated.WithoutEvaluation;
            } else if (trainCities[trainId] + trainTowns[trainId] == 
                trainMaxCities[trainId] + trainMaxTowns[trainId] )
                terminated = Terminated.WithEvaluation;
        }
        if (terminated != Terminated.NotYet) {
            log.debug ("RC: Train " + trainId + " has terminated: " +
            		"cities = " + trainCities[trainId] + " towns = " + trainTowns[trainId] +
            		"maxCities = " + trainMaxCities[trainId] + "maxTowns = " + trainMaxTowns[trainId]);
        }
        return terminated;
    }
    
    private void finalizeVertex(int trainId, int vertexId, boolean evaluate) {
        log.debug("RC: No more edges found at " + vertexId + " for train " + trainId);
        
        if (!vertexCity[vertexId] && !vertexTown[vertexId]) return;
        
        if (trainId == finalTrain) {
            if (evaluate) evaluateResults();
        } else {
            runTrain(trainId + 1);
        }
    }

    private void evaluateResults() {
        // sum to total value
        int totalValue = 0;
        for (int j = 0; j <= finalTrain; j++) {
            if (trainCities[j] + trainTowns[j] <= 1) {
                log.debug("RC: Train " + j + " has no value / does not have 2+ stations");
            } else {
                totalValue += trainCurrentValue[j];
                log.debug("RC: Train " + j + " has value of " + trainCurrentValue);
            }
        }
        nbEvaluations++;
        log.debug("RC: current total value " + totalValue);
        
        // compare to current best result
        if (totalValue > currentBestValue) {
            currentBestValue = totalValue;
            // exceed thus deep copy of vertex stack
            for (int j = 0; j <= finalTrain; j++)
                for (int v = 0; v < nbVertexes; v++)
                    if (v < trainStackPos[j])
                        currentBestRun[j][v] = trainVertexStack[j][v];
                    else {
                        currentBestRun[j][v] = -1; // terminator
                        break;
                    }
            log.info("RC: Found better run with " + totalValue);
            // inform revenue listener via adapter
            notifyRevenueAdapter(currentBestValue, false);
        }
    }
    
    // predict revenues and returns true if best value can still be exceeded
    private boolean predictRevenues(int trainId){
        int totalValue = 0;
        for (int j = 0; j <= finalTrain; j++) {
            int trainValue;
            if (j < trainId) { // train has run already => use realized values
                trainValue =  trainCurrentValue[j];
            } else if (j > trainId) { // train is in the future => use maximum values
                trainValue =  maxTrainRevenues[j];
            } else { // the current train
                if (trainIgnoreTowns[j]) {
                    // express train
                    trainValue = trainCurrentValue[j] + 
                    maxCityRevenues[trainMaxCities[j] - trainCities[j] ] * trainMultiplyCities[j];
                } else if (trainMaxTowns[j] == 0) {
                    // default train
                    trainValue = trainCurrentValue[j] + 
                    maxCityRevenues[trainMaxCities[j] - trainCities[j] - trainTowns[j] ] * trainMultiplyCities[j];
                } else {
                    // plus trains (or capped default trains)
                    int townDiff = trainMaxTowns[j] - trainTowns[j];
                    if (townDiff > 0) {
                        trainValue = trainCurrentValue[j] + 
                        maxCityRevenues[trainMaxCities[j] - trainCities[j]] * trainMultiplyCities[j] +
                        maxTownRevenues[trainMaxTowns[j] - trainTowns[j]] * trainMultiplyTowns[j];
                    } else if (townDiff == 0) {
                        trainValue = trainCurrentValue[j] + 
                        maxCityRevenues[trainMaxCities[j] - trainCities[j]] * trainMultiplyCities[j];
                    } else { // negative townDiff, thus too many towns already visited
                        trainValue = trainCurrentValue[j] + 
                        maxCityRevenues[trainMaxCities[j] - trainCities[j] + townDiff] * trainMultiplyCities[j];
                    }
                }
                trainValue = Math.min(trainValue, maxTrainRevenues[j]); 
            }
            log.debug("RC: Train " + j + " has value of " + trainValue);
            totalValue += trainValue;
        }

        nbPredictions++;
        
        boolean terminate = (totalValue <= currentBestValue);
        if (terminate) log.debug("Run terminated due to predicted value of " +  totalValue);

        return terminate;
    }
    
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("vertexValues:" + Arrays.toString(vertexValue) + "\n");
        buffer.append("vertexCity:" + Arrays.toString(vertexCity) + "\n");
        buffer.append("vertexTown:" + Arrays.toString(vertexTown) + "\n");
        buffer.append("vertexEdges:" + Arrays.deepToString(vertexNeighbors) + "\n");
//        buffer.append("edgeGreedy:" + Arrays.deepToString(edgeGreedy));
//        buffer.append("edgeDistance:" + Arrays.deepToString(edgeDistance));
        buffer.append("startVertexes:" + Arrays.toString(startVertexes) + "\n"); 
        buffer.append("trainMaxCities:" + Arrays.toString(trainMaxCities) + "\n"); 
        buffer.append("trainMaxTowns:" + Arrays.toString(trainMaxTowns) + "\n"); 
        buffer.append("trainIgnoreTowns:" + Arrays.toString(trainIgnoreTowns) + "\n"); 
        buffer.append("maxCityRevenues:" + Arrays.toString(maxCityRevenues) + "\n"); 
        buffer.append("maxTownRevenues:" + Arrays.toString(maxTownRevenues) + "\n"); 
      
        return buffer.toString();
    }
    
}

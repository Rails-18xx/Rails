package rails.algorithms;

import java.util.Arrays;

import org.apache.log4j.Logger;

final class RevenueCalculator {
    
    private final int nbVertexes;
    private final int maxNeighbors;
    private final int nbTrains;
    
    // static vertex data
    private final int[][] vertexValueByTrain; // dimensions: vertexId, trainId
    private final boolean[] vertexMajor;
    private final boolean[] vertexMinor;
    private final int[][] vertexNeighbors;

    // start vertexes
    private int[] startVertexes;
    
    // static edge data
    private final boolean[][] edgeGreedy;
    private final int[][] edgeDistance;
    
    // dynamic edge data
    private final boolean[][] edgeUsed;
    
    // static train data
    private final int[] trainMaxMajors;
    private final int[] trainMaxMinors;
    private final boolean[] trainIgnoreMinors;
    
    // dynamic train data
    private final int[] trainCurrentValue;
    private final int[] trainMajors;
    private final int[] trainMinors;
    private final boolean[][] trainVisited;
    private final int[][] trainVertexStack;
    private final int[] trainStackPos;
    private final int [] trainBottomPos;
    private final int [] trainStartEdge;
    
    // run settings
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
    private int nbEdges;
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

    
    public RevenueCalculator (RevenueAdapter revenueAdapter, int nbVertexes, int maxNeighbors, int nbTrains) {
        
        this.revenueAdapter = revenueAdapter;
        this.nbVertexes = nbVertexes;
        this.maxNeighbors = maxNeighbors;
        this.nbTrains = nbTrains;
        
        log.debug("RC defined: nbVertexes = " + nbVertexes + ", maxNeighbors = " + maxNeighbors + ", nbTrains = " + nbTrains);
        
        // initialize all required variables
        vertexValueByTrain = new int[nbVertexes][nbTrains];
        vertexMajor = new boolean[nbVertexes];
        vertexMinor = new boolean[nbVertexes];
        vertexNeighbors = new int[nbVertexes][maxNeighbors];
        
        edgeGreedy = new boolean[nbVertexes][nbVertexes];
        edgeDistance = new int[nbVertexes][nbVertexes];
        edgeUsed = new boolean[nbVertexes][nbVertexes];
        
        trainMaxMajors = new int[nbTrains];
        trainMaxMinors = new int[nbTrains];
        trainIgnoreMinors = new boolean[nbTrains];
        
        trainCurrentValue = new int[nbTrains];
        trainMajors = new int[nbTrains];
        trainMinors = new int[nbTrains];
        trainVisited = new boolean[nbTrains][nbVertexes];
        trainVertexStack = new int[nbTrains][nbVertexes];
        trainStackPos = new int[nbTrains];
        trainBottomPos = new int[nbTrains];
        trainStartEdge = new int[nbTrains];
        
        currentBestRun = new int[nbTrains][nbVertexes];
        
        useRevenuePrediction = false;
    }

    void setVertex(int id, int value, boolean major, boolean minor, int[] neighbors) {
        for (int j=0; j < nbTrains; j++) {
            vertexValueByTrain[id][j] = value;
        }
        vertexMajor[id] = major;
        vertexMinor[id] = minor;
        vertexNeighbors[id] = neighbors;
    }
    
    void setStartVertexes(int[] startVertexes) {
        this.startVertexes = startVertexes;
    }
    
    void setEdge(int vertexLo, int vertexHi, boolean greedy, int distance) {
        edgeGreedy[vertexLo][vertexHi] = greedy;
        edgeDistance[vertexLo][vertexHi] = distance;
    }
    
    void setTrain(int id, int majors, int minors, boolean ignoreMinors, int multiplyMajors, int multiplyMinors) {
        trainMaxMajors[id] = majors;
        trainMaxMinors[id] = minors;
        trainIgnoreMinors[id] = ignoreMinors;
        
        for (int j=0; j < nbVertexes; j++) {
            if (vertexMajor[id]) {
                vertexValueByTrain[j][id] = vertexValueByTrain[j][id] * multiplyMajors;
            }
            if (vertexMinor[id]) {
                vertexValueByTrain[j][id] = vertexValueByTrain[j][id] * multiplyMinors;
            }
        }
    }
    
//    void setPredictionData(int[] maxMajorRevenues, int[] maxMinorRevenues) {
//    }
    
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

    private int[] bestRevenues(int[] values, int length) {
        int[] bestRevenues = new int[length + 1];
        Arrays.sort(values);
        int cumulatedRevenues = 0;
        for (int j=1; j <= length ; j++) {
            cumulatedRevenues += values[values.length - j];
            bestRevenues[j] = cumulatedRevenues;
        }
        log.info(Arrays.toString(bestRevenues));
        return bestRevenues;
    }
    
    private void initRevenueValues(int startTrain, int finalTrain){
        
      // intialize values
        maxMajorRevenues = new int[nbTrains][nbVertexes];
        maxMinorRevenues = new int[nbTrains][nbVertexes];
        for (int t=startTrain; t <= finalTrain; t++) {
            int[] majorValues = new int[nbVertexes];
            int[] minorValues = new int[nbVertexes];
            int[] bonusValues = new int[nbVertexes];
            int major = 0, minor = 0, bonus = 0;
            for (int v=0; v < nbVertexes; v++) {
                if (vertexValueByTrain[v][t] == 0) continue;
                if (vertexMajor[v])
                    majorValues[major++] = vertexValueByTrain[v][t];
                else if (vertexMinor[v])
                    minorValues[minor++] = vertexValueByTrain[v][t];
                else
                    bonusValues[bonus++] = vertexValueByTrain[v][t];
            }
            maxMajorRevenues[t] = bestRevenues(majorValues, trainMaxMajors[t]);
            maxMinorRevenues[t] = bestRevenues(minorValues, trainMaxMinors[t]);
            maxCumulatedTrainRevenues[t] = maxMajorRevenues[t][trainMaxMajors[t]] + maxMinorRevenues[t][trainMaxMinors[t]];
        }
    }
    
    void initialPredictionRuns(int startTrain, int finalTrain) {
        
        if (startTrain > finalTrain) return;
       
        useRevenuePrediction = true;
        this.maxCumulatedTrainRevenues = new int[nbTrains];
         initRevenueValues(startTrain, finalTrain);
        
        if (startTrain == finalTrain) return;
        
        // start prediction runs
        nbEvaluations = 0; nbPredictions = 0; nbEdges = 0;

        log.info("RC: start individual prediction Runs");
        int cumulatedRevenues = 0;
        int[] maxSingleTrainRevenues = new int[nbTrains]; 
        for (int j=finalTrain; j >= startTrain; j--) {
            this.startTrain = j;
            this.finalTrain = j;
            currentBestValue = 0;
            runTrain(j);
            log.info("RC: Best prediction run of train number " + j + " value = " + currentBestValue);
            maxSingleTrainRevenues[j] = currentBestValue;
            cumulatedRevenues +=  currentBestValue;
            maxCumulatedTrainRevenues[j] = cumulatedRevenues;
        }
        
        if (startTrain == finalTrain-1) return;

        log.info("RC: start combined prediction runs");
        this.finalTrain = finalTrain;
        for (int j=finalTrain - 1; j > startTrain; j--) {
            this.startTrain = j;
            currentBestValue = 0;
            runTrain(j);
            log.info("RC: Best prediction run until train nb. " + j + " value = " + currentBestValue);
            maxCumulatedTrainRevenues[j] = currentBestValue;
            maxCumulatedTrainRevenues[j-1] = currentBestValue  + maxSingleTrainRevenues[j-1];
        }
    }
    
    int calculateRevenue(int startTrain, int finalTrain) {
        log.info("RC: calculateRevenue trains from " + startTrain + " to " + finalTrain);
        
        nbEvaluations = 0; nbPredictions = 0; nbEdges = 0;

        this.startTrain = startTrain;
        this.finalTrain = finalTrain;
        
        runTrain(startTrain);

        // inform revenue listener via adapter
        notifyRevenueAdapter(currentBestValue, true);

        return currentBestValue;
    }
   
    private void runTrain(int trainId) {
        log.debug("RC: runTrain " + trainId);
        
        // initialize lengths
        trainMajors[trainId] = trainMaxMajors[trainId];
        trainMinors[trainId] = trainMaxMinors[trainId];
        
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
            finalizeVertex(trainId, vertexId);
            encounterVertex(trainId, vertexId, false);
            // keep them on the visited vertex list to avoid route duplication
            trainVisited[trainId][vertexId] = true;
            log.debug("RC: finished startVertex " + vertexId + " for train " +trainId);
        }

        for (int i=0; i < startVertexes.length; i++) {
            // remove all of them from the visited vertex list
            trainVisited[trainId][startVertexes[i]] = false;
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
            if (valueStation && trainBottomPos[trainId] == 0){
                runBottom(trainId);
            }
        }
        
        // 3. no more edges to visit from here => evaluate or start new train
        if (valueStation)
            finalizeVertex(trainId, vertexId);
        
        // 4. then leave that vertex
        encounterVertex(trainId, vertexId, false);
        returnEdge(trainId);
    }
    
    private boolean encounterVertex(int trainId, int vertexId, boolean arrive) {

        log.debug("RC: EncounterVertex, trainId = " + trainId + " vertexId = " + vertexId + " arrive = " + arrive);
        
        // set visit to true if arriving, otherwise you leave
        trainVisited[trainId][vertexId] = arrive;

        boolean valueVertex;
        if (arrive) {
            if (vertexValueByTrain[vertexId][trainId] == 0) {
                valueVertex = false; 
            } else {
                valueVertex = true;
                trainCurrentValue[trainId] += vertexValueByTrain[vertexId][trainId];
            }
            if (vertexMajor[vertexId])
                trainMajors[trainId]--;
            if (vertexMinor[vertexId]) {
                trainMinors[trainId]--;
            }
            trainVertexStack[trainId][trainStackPos[trainId]++] = vertexId; // push to stack
            countVisits++;
        } else {
            if (vertexValueByTrain[vertexId][trainId] == 0) {
                valueVertex = false; 
            } else {
                valueVertex = true;
                trainCurrentValue[trainId] -= vertexValueByTrain[vertexId][trainId];
            }
            if (vertexMajor[vertexId])
                trainMajors[trainId]++;
            if (vertexMinor[vertexId])
                trainMinors[trainId]++;
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
        log.debug("RC: No more edges found at " + vertexId + " for train " + trainId);
        
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
            if (trainIgnoreMinors[j]) { 
                if (trainMaxMajors[j] - trainMajors[j] >= 2)
                    totalValue += trainCurrentValue[j];
            } else {
                if (trainMaxMajors[j] + trainMaxMinors[j] - trainMajors[j] - trainMinors[j] >= 2)
                    totalValue += trainCurrentValue[j];
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
        log.debug("RC: Current train has predicted  value of " + trainValue);

        // maximum value for the trainId including future trains
        totalValue = Math.min(totalValue + trainValue, maxCumulatedTrainRevenues[trainId]); 
        
        // and add the past trains: current realized values
        for (int j = startTrain; j < trainId; j++) {
            if (trainIgnoreMinors[j]) { 
                if (trainMaxMajors[j] - trainMajors[j] >= 2)
                    totalValue += trainCurrentValue[j];
            } else {
                if (trainMaxMajors[j] + trainMaxMinors[j] - trainMajors[j] - trainMinors[j] >= 2)
                    totalValue += trainCurrentValue[j];
            }
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
        buffer.append("vertexEdges:" + Arrays.deepToString(vertexNeighbors) + "\n");
//        buffer.append("edgeGreedy:" + Arrays.deepToString(edgeGreedy));
//        buffer.append("edgeDistance:" + Arrays.deepToString(edgeDistance));
        buffer.append("startVertexes:" + Arrays.toString(startVertexes) + "\n"); 
        buffer.append("trainMaxMajors:" + Arrays.toString(trainMaxMajors) + "\n"); 
        buffer.append("trainMaxMinors:" + Arrays.toString(trainMaxMinors) + "\n"); 
        buffer.append("trainIgnoreMinors:" + Arrays.toString(trainIgnoreMinors) + "\n"); 
      
        return buffer.toString();
    }
    
}

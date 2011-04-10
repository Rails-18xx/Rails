package rails.algorithms;


final class RevenueCalculatorMulti extends RevenueCalculator {

    protected final int[] edgeNbTravelSets;
    protected final int[][] edgeTravelSets; // edge belongs to a travel set, dimension: nbEdges x nbTravelSets

    // dynamic edge data
    private final int[] edgeUsed;
    
    // dynamic train data
    private final int[] startVertexActive;
    
      
    public RevenueCalculatorMulti (RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges, 
            int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses) {
    
        // maxEdgeSet set to zero
        super(revenueAdapter, nbVertexes, nbEdges, 
                maxNeighbors, maxVertexSets, maxEdgeSets, nbTrains, nbBonuses);

        edgeNbTravelSets = new int[nbEdges];
        edgeTravelSets = new int[nbEdges][maxEdgeSets];
        
        // edge used is integer here
        edgeUsed = new int[nbEdges];
        
        startVertexActive = new int[nbTrains];

    }
    
    @Override
    void setEdge(int edgeId, boolean greedy, int distance) {
        super.setEdge(edgeId, greedy, distance);
        // default number for travel sets
        edgeNbTravelSets[edgeId] = 0;
    }
    
    // define edgeTravelSets
    void setTravelSet(int edgeId, int[] edges) {
        for (int j=0; j < edges.length; j++) {
            edgeTravelSets[edgeId][edgeNbTravelSets[edgeId]++] = edges[j];
        }
    }
    
    @Override
    protected final void runTrain(final int trainId) {
        log.debug("RC: runTrain " + trainId);
        
        // initialize value
        trainCurrentValue[trainId] = 0;

        // initialize train lengths
        trainMajors[trainId] = trainMaxMajors[trainId];
        trainMinors[trainId] = trainMaxMinors[trainId];
        trainBonuses[trainId] = trainMaxBonuses[trainId];
        
        // initialize the positions
        trainStackPos[trainId] = 0;
        trainBottomActive[trainId] = false;
        
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
            startVertexActive[trainId] = vertexId;
            // for startVertices the sink property is ignored
            for (int j = 0; j < vertexNbNeighbors[vertexId]; j++) {
                int edgeId = vertexEdges[vertexId][j];
                if (edgeUsed[edgeId] != 0) continue; 
                log.debug("RC: Testing Neighbor Nr. " + j + " of startVertex");
                int neighborId = vertexNeighbors[vertexId][j];
                if (trainVisited[trainId][neighborId]) {
                    log.debug("RC: Hex already visited");
                    continue;
                }
                travelEdge(trainId, edgeId);
                trainStartEdge[trainId] = j; // store start edge
                nextVertex(trainId, neighborId);
                returnEdge(edgeId);
                trainStackPos[trainId]--; // pull from stack
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

    @Override
    final protected void runBottom(final int trainId) {
        log.debug("RC: runBottom " + trainId);
        
        // use startvertex, check if it is a sink
        int vertexId = startVertexActive[trainId];
        if (vertexSink[vertexId]) {
            log.debug("RC: startvertex is sink, finished bottom of " + trainId);
            return;
        }
                
        // push to stack
        trainBottomActive[trainId] = true; 
        log.debug("RC: Restart at bottom at stack position " + trainStackPos[trainId]);
//        trainStack[trainId][trainStackPos[trainId]++] = vertexId;
        
        for (int j = trainStartEdge[trainId] + 1; j < vertexNbNeighbors[vertexId]; j++) {
            int edgeId = vertexEdges[vertexId][j]; 
            if (edgeUsed[edgeId] != 0) continue; 
            int neighborId = vertexNeighbors[vertexId][j];
            log.debug("RC: Testing Neighbor Nr. " + j + " of bottomVertex is " + neighborId);
            if (trainVisited[trainId][neighborId]) {
                log.debug(" RC: Hex already visited");
                continue;
            }
            travelEdge(trainId, edgeId);
            nextVertex(trainId, neighborId);
            returnEdge(edgeId);
            trainStackPos[trainId]--; // pull from stack
        }
        
//        trainStackPos[trainId]--; // pull from stack
        trainBottomActive[trainId] = false;
        log.debug("RC: finished bottom of " + trainId);


    }

    private final void nextVertex(final int trainId, final int vertexId) {

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
                return;
            }
        }
        
        // 2a. visit neighbors, if train has not terminated and vertex is not a sink
        if (trainTerminated == Terminated.NotYet) {
            if (!vertexSink[vertexId]) {
                for (int j = 0; j < vertexNbNeighbors[vertexId]; j++) {
                    int edgeId = vertexEdges[vertexId][j];
                    if (edgeUsed[edgeId] != 0) continue; 
                    int neighborId = vertexNeighbors[vertexId][j];
                    log.debug("RC: Testing Neighbor Nr. " + j + " of " + vertexId + " is " + neighborId);
                    if (trainVisited[trainId][neighborId]) {
                        log.debug("RC: Hex already visited");
                        continue;
                    }
                    travelEdge(trainId, edgeId);
                    nextVertex(trainId, neighborId);
                    returnEdge(edgeId);
                    trainStackPos[trainId]--; // pull from stack
                }
            }
            // 2b. restart at startVertex for bottom part
            if (stationVertex && !trainBottomActive[trainId]){
                runBottom(trainId);
            }
        }
        
        // 3. no more edges to visit from here => evaluate or start new train
        if (stationVertex)
            finalizeVertex(trainId, vertexId);
        
        // 4. then leave that vertex
        encounterVertex(trainId, vertexId, false);
    }

    private final void travelEdge(final int trainId, final int edgeId) {
        log.debug("RC: Travel edge id " + edgeId);
        edgeUsed[edgeId]++;
        trainStack[trainId][trainStackPos[trainId]++] = edgeId; // push to stack
        countEdges++; nbEdgesTravelled++;
        log.debug("RC: Count Edges = " + countEdges);

        // check edge sets
        for (int j=0; j < edgeNbTravelSets[edgeId]; j++) {
            edgeUsed[edgeTravelSets[edgeId][j]]++;
            log.debug("RC: travelled edge " + edgeTravelSets[edgeId][j]  + " due to edge set.");
        }
    }
    
    
    @Override
    protected void returnEdge(int edgeId) {
          if (edgeUsed[edgeId] != 0) {
              edgeUsed[edgeId]--;
              countEdges--;
              log.debug("RC: Cleared edge id " + edgeId);
              log.debug("RC: Count Edges = " + countEdges);
     
              // check edge sets
              for (int j=0; j < edgeNbTravelSets[edgeId]; j++) {
                  edgeUsed[edgeTravelSets[edgeId][j]]--;
                  log.debug("RC: Cleared edge " + edgeTravelSets[edgeId][j]  + " due to edge set.");
              }
          } else {
              log.debug("RC: Error return edge id used: " + edgeId);
          }

    }

}

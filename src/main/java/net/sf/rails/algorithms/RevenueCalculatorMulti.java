package net.sf.rails.algorithms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.specific._1856.OperatingRound_1856;

class RevenueCalculatorMulti extends RevenueCalculator {
    private static final Logger log = LoggerFactory.getLogger(RevenueCalculatorMulti.class);


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
    final void setEdge(int edgeId, boolean greedy, int distance) {
        super.setEdge(edgeId, greedy, distance);
        // default number for travel sets
        edgeNbTravelSets[edgeId] = 0;
    }

    // define edgeTravelSets
    final void setTravelSet(int edgeId, int[] edges) {
        for (int j=0; j < edges.length; j++) {
            edgeTravelSets[edgeId][edgeNbTravelSets[edgeId]++] = edges[j];
        }
    }

    @Override
    protected void runTrain(final int trainId) {
        log.debug("RCM: runTrain {}", trainId);

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
            log.debug("RCM: Using startVertex nr. {} for train {}", i, trainId);
            boolean stationVertex = encounterVertex(trainId, vertexId, true);
            if (stationVertex) {
                // train cannot terminate at start vertex
                if (useRevenuePrediction && predictRevenues(trainId)) {
                    // cannot beat current best value => leave immediately
                    encounterVertex(trainId, vertexId, false);
                    // but keep them on the visited vertex list to avoid route duplication
                    trainVisited[trainId][vertexId] = true;
                    log.debug("RCM: finished startVertex {} for train {}", vertexId, trainId);
                    continue;
                }
            }

            // then try all edges of it
            startVertexActive[trainId] = vertexId;
            // for startVertices the sink property is ignored
            for (int j = 0; j < vertexNbNeighbors[vertexId]; j++) {
                int edgeId = vertexEdges[vertexId][j];
                if (edgeUsed[edgeId] != 0) continue;
                log.debug("RCM: Testing Neighbor Nr. {} of startVertex", j);
                int neighborId = vertexNeighbors[vertexId][j];
                if (trainVisited[trainId][neighborId]) {
                    log.debug("RCM: Hex already visited");
                    continue;
                }
                travelEdge(trainId, edgeId);
                trainStartEdge[trainId] = j; // store start edge
                nextVertex(trainId, neighborId);
                returnEdge(trainId, edgeId);
                trainStackPos[trainId]--; // pull from stack
            }

            // no more edges to find
            encounterVertex(trainId, vertexId, false);
            // keep them on the visited vertex list to avoid route duplication
            trainVisited[trainId][vertexId] = true;
            log.debug("RCM: finished startVertex {} for train {}", vertexId, trainId);
        }

        // finished all tries
        for ( int startVertex : startVertexes ) {
            // remove all of them from the visited vertex list
            trainVisited[trainId][startVertex] = false;
        }

        // allow that the train does not run at all
        finalizeVertex(trainId, -1);

        log.debug("RCM: finishTrain {}", trainId);

    }

    @Override
    final protected void runBottom(final int trainId) {
        log.debug("RCM: runBottom {}", trainId);

        // use startvertex, check if it is a sink
        int vertexId = startVertexActive[trainId];
        if (vertexSink[vertexId]) {
            log.debug("RCM: startvertex is sink, finished bottom of {}", trainId);
            return;
        }

        // push to stack
        trainBottomActive[trainId] = true;
        log.debug("RCM: Restart at bottom at stack position {}", trainStackPos[trainId]);
//        trainStack[trainId][trainStackPos[trainId]++] = vertexId;

        for (int j = trainStartEdge[trainId] + 1; j < vertexNbNeighbors[vertexId]; j++) {
            int edgeId = vertexEdges[vertexId][j];
            if (edgeUsed[edgeId] != 0) continue;
            int neighborId = vertexNeighbors[vertexId][j];
            log.debug("RCM: Testing Neighbor Nr. {} of bottomVertex is {}", j, neighborId);
            if (trainVisited[trainId][neighborId]) {
                log.debug(" RCM: Hex already visited");
                continue;
            }
            travelEdge(trainId, edgeId);
            nextVertex(trainId, neighborId);
            returnEdge(trainId, edgeId);
            trainStackPos[trainId]--; // pull from stack
        }

//        trainStackPos[trainId]--; // pull from stack
        trainBottomActive[trainId] = false;
        log.debug("RCM: finished bottom of {}", trainId);


    }

    private void nextVertex(final int trainId, final int vertexId) {

        // 1. encounterVertex adds value and returns true if value vertex
        Terminated trainTerminated = Terminated.NOT_YET;
        boolean stationVertex = encounterVertex(trainId, vertexId, true);
        if (stationVertex) {
            // check usual train termination
            trainTerminated = trainTerminated(trainId);
            if (trainTerminated == Terminated.WITHOUT_EVALUATION ||
                    useRevenuePrediction && predictRevenues(trainId)) {
                // cannot beat current best value => leave immediately
                encounterVertex(trainId, vertexId, false);
                return;
            }
        }

        // 2a. visit neighbors, if train has not terminated and vertex is not a sink
        if (trainTerminated == Terminated.NOT_YET ) {
            if (!vertexSink[vertexId]) {
                for (int j = 0; j < vertexNbNeighbors[vertexId]; j++) {
                    int edgeId = vertexEdges[vertexId][j];
                    if (edgeUsed[edgeId] != 0) continue;
                    int neighborId = vertexNeighbors[vertexId][j];
                    log.debug("RCM: Testing Neighbor Nr. {} of {} is {}", j, vertexId, neighborId);
                    if (trainVisited[trainId][neighborId]) {
                        log.debug("RCM: Hex already visited");
                        continue;
                    }
                    travelEdge(trainId, edgeId);
                    nextVertex(trainId, neighborId);
                    returnEdge(trainId, edgeId);
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

    protected void travelEdge(final int trainId, final int edgeId) {
        log.debug("RCM: Travel edge id {}", edgeId);
        edgeUsed[edgeId]++;
        trainStack[trainId][trainStackPos[trainId]++] = edgeId; // push to stack
        countEdges++; nbEdgesTravelled++;
        log.debug("RCM: Count Edges = {}", countEdges);

        // check edge sets
        for (int j=0; j < edgeNbTravelSets[edgeId]; j++) {
            edgeUsed[edgeTravelSets[edgeId][j]]++;
            log.debug("RCM: travelled edge {} due to edge set.", edgeTravelSets[edgeId][j]);
        }
    }


    @Override
    protected void returnEdge(final int trainId, final int edgeId) {
          if (edgeUsed[edgeId] != 0) {
              edgeUsed[edgeId]--;
              countEdges--;
              log.debug("RCM: Cleared edge id {}", edgeId);
              log.debug("RCM: Count Edges = {}", countEdges);

              // check edge sets
              for (int j=0; j < edgeNbTravelSets[edgeId]; j++) {
                  edgeUsed[edgeTravelSets[edgeId][j]]--;
                  log.debug("RCM: Cleared edge {} due to edge set.", edgeTravelSets[edgeId][j]);
              }
          } else {
              log.debug("RCM: Error return edge id used: {}", edgeId);
          }

    }

}

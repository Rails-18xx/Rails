using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public sealed class RevenueCalculatorSimple : RevenueCalculator
    {
        // dynamic edge data
        private bool[] edgeUsed;

        public RevenueCalculatorSimple(RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges,
                int maxNeighbors, int maxVertexSets, int nbTrains, int nbBonuses) :
                base(revenueAdapter, nbVertexes, nbEdges, maxNeighbors, maxVertexSets, 0, nbTrains, nbBonuses)// maxEdgeSet set to zero
        {
            // edge used is bool here
            edgeUsed = new bool[nbEdges];
        }

        override protected void RunTrain(int trainId)
        {
            log.Debug("RC: runTrain " + trainId);

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
            for (int b = 0; b < nbBonuses; b++)
            {
                bonusTrainVertices[b, trainId] = bonusRequiresVertices[b];
            }

            // check if the revenue is enough
            if (useRevenuePrediction && PredictRevenues(trainId))
                return;

            // try all startVertexes
            for (int i = 0; i < startVertexes.Length; i++)
            {
                int vertexId = startVertexes[i];
                log.Debug("RC: Using startVertex nr. " + i + " for train " + trainId);
                bool stationVertex = EncounterVertex(trainId, vertexId, true);
                trainStack[trainId, trainStackPos[trainId]++] = vertexId; // push to stack
                if (stationVertex)
                {
                    // train cannot terminate at start vertex
                    if (useRevenuePrediction && PredictRevenues(trainId))
                    {
                        // cannot beat current best value => leave immediately
                        EncounterVertex(trainId, vertexId, false);
                        trainStackPos[trainId]--; // pull from stack
                                                  // but keep them on the visited vertex list to avoid route duplication
                        trainVisited[trainId, vertexId] = true;
                        log.Debug("RC: finished startVertex " + vertexId + " for train " + trainId);
                        continue;
                    }
                }

                // then try all edges of it
                // for startVertices the sink property is ignored
                for (int j = 0; j < vertexNbNeighbors[vertexId]; j++)
                {
                    int edgeId = vertexEdges[vertexId, j];
                    if (edgeUsed[edgeId]) continue;
                    log.Debug("RC: Testing Neighbor Nr. " + j + " of startVertex");
                    int neighborId = vertexNeighbors[vertexId, j];
                    if (trainVisited[trainId, neighborId])
                    {
                        log.Debug("RC: Hex already visited");
                        continue;
                    }
                    if (TravelEdge(trainId, edgeId, true))
                    {
                        trainStartEdge[trainId] = j; // store start edge
                        NextVertex(trainId, neighborId, edgeGreedy[edgeId]);
                        ReturnEdge(trainId, edgeId);
                    }
                }

                // no more edges to find
                EncounterVertex(trainId, vertexId, false);
                trainStackPos[trainId]--; // pull from stack
                                          // keep them on the visited vertex list to avoid route duplication
                trainVisited[trainId, vertexId] = true;
                log.Debug("RC: finished startVertex " + vertexId + " for train " + trainId);
            }

            // finished all tries
            for (int i = 0; i < startVertexes.Length; i++)
            {
                // remove all of them from the visited vertex list
                trainVisited[trainId, startVertexes[i]] = false;
            }

            // allow that the train does not run at all
            FinalizeVertex(trainId, -1);

            log.Debug("RC: finishTrain " + trainId);
        }

        override protected void RunBottom(int trainId)
        {
            log.Debug("RC: runBottom " + trainId);

            // use startvertex, check if it is a sink
            int vertexId = trainStack[trainId, 0];
            if (vertexSink[vertexId])
            {
                log.Debug("RC: startvertex is sink, finished bottom of " + trainId);
                return;
            }

            trainBottomActive[trainId] = true;
            // push to stack
            log.Debug("RC: Restart at bottom at stack position " + trainStackPos[trainId]);
            trainStack[trainId, trainStackPos[trainId]++] = vertexId;

            for (int j = trainStartEdge[trainId] + 1; j < vertexNbNeighbors[vertexId]; j++)
            {
                int edgeId = vertexEdges[vertexId, j];
                if (edgeUsed[edgeId]) continue;
                int neighborId = vertexNeighbors[vertexId, j];
                log.Debug("RC: Testing Neighbor Nr. " + j + " of bottomVertex is " + neighborId);
                if (trainVisited[trainId, neighborId])
                {
                    log.Debug(" RC: Hex already visited");
                    continue;
                }
                if (TravelEdge(trainId, edgeId, true))
                {
                    NextVertex(trainId, neighborId, edgeGreedy[edgeId]);
                    ReturnEdge(trainId, edgeId);
                }
            }

            trainStackPos[trainId]--; // pull from stack
            trainBottomActive[trainId] = false;
            log.Debug("RC: finished bottom of " + trainId);
        }

        private void NextVertex(int trainId, int vertexId, bool previousGreedy)
        {
            // 1. encounterVertex adds value and returns true if value vertex
            Terminated trainTerminated = Terminated.NotYet;
            bool stationVertex = EncounterVertex(trainId, vertexId, true);
            trainStack[trainId, trainStackPos[trainId]++] = vertexId; // push to stack
            if (stationVertex)
            {
                // check usual train termination
                trainTerminated = TrainTerminated(trainId);
                if (trainTerminated == Terminated.WithoutEvaluation ||
                        useRevenuePrediction && PredictRevenues(trainId))
                {
                    // cannot beat current best value => leave immediately
                    EncounterVertex(trainId, vertexId, false);
                    trainStackPos[trainId]--; // pull from stack
                    return;
                }
            }

            // 2a. visit neighbors, if train has not terminated and vertex is not a sink
            if (trainTerminated == Terminated.NotYet)
            {
                if (!vertexSink[vertexId])
                {
                    for (int j = 0; j < vertexNbNeighbors[vertexId]; j++)
                    {
                        int edgeId = vertexEdges[vertexId, j];
                        if (edgeUsed[edgeId]) continue;
                        int neighborId = vertexNeighbors[vertexId, j];
                        log.Debug("RC: Testing Neighbor Nr. " + j + " of " + vertexId + " is " + neighborId);
                        if (trainVisited[trainId, neighborId])
                        {
                            log.Debug("RC: Hex already visited");
                            continue;
                        }
                        if (TravelEdge(trainId, edgeId, previousGreedy))
                        {
                            NextVertex(trainId, neighborId, edgeGreedy[edgeId]);
                            ReturnEdge(trainId, edgeId);
                        }
                    }
                }
                // 2b. restart at startVertex for bottom part
                if (stationVertex && !trainBottomActive[trainId])
                {
                    RunBottom(trainId);
                }
            }

            // 3. no more edges to visit from here => evaluate or start new train
            if (stationVertex)
                FinalizeVertex(trainId, vertexId);

            // 4. then leave that vertex
            EncounterVertex(trainId, vertexId, false);
            trainStackPos[trainId]--; // pull from stack
        }

        private bool TravelEdge(int trainId, int edgeId, bool previousGreedy)
        {
            if (previousGreedy || edgeGreedy[edgeId])
            {
                log.Debug("RC: Travel edge id " + edgeId);
                edgeUsed[edgeId] = true;
                //           edgeUsed[edgeId]++;
                // trainEdgeStack[trainId][trainStackPos[trainId]] = edgeId;
                countEdges++; nbEdgesTravelled++;
                log.Debug("RC: Count Edges = " + countEdges);

                // check edge sets
                //           for (int j=0; j < edgeNbTravelSets[edgeId]; j++) {
                //               edgeUsed[edgeTravelSets[edgeId][j]]++;
                //               log.debug("RC: traveled edge " + edgeTravelSets[edgeId][j]  + " due to edge set.");
                //           }

                return true;
            }
            else
            {
                log.Debug("RC: Cannot travel edge id " + edgeId + ", because of greedy rule");
                return false;
            }
        }

        override protected void ReturnEdge(int trainId, int edgeId)
        {
            if (edgeUsed[edgeId])
            {
                edgeUsed[edgeId] = false;
                countEdges--;
                log.Debug("RC: Cleared edge id " + edgeId);
                log.Debug("RC: Count Edges = " + countEdges);
            }
            else
            {
                log.Debug("RC: Error return edge id used: " + edgeId);
            }
        }
    }
}

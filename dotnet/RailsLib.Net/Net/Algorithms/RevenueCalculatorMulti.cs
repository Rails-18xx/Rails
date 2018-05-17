using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public class RevenueCalculatorMulti : RevenueCalculator
    {
        protected int[] edgeNbTravelSets;
        protected int[,] edgeTravelSets; // edge belongs to a travel set, dimension: nbEdges x nbTravelSets

        // dynamic edge data
        private int[] edgeUsed;

        // dynamic train data
        private int[] startVertexActive;


        public RevenueCalculatorMulti(RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges,
                int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses) :
            base(revenueAdapter, nbVertexes, nbEdges,
                    maxNeighbors, maxVertexSets, maxEdgeSets, nbTrains, nbBonuses)// maxEdgeSet set to zero
        {
            edgeNbTravelSets = new int[nbEdges];
            edgeTravelSets = new int[nbEdges, maxEdgeSets];

            // edge used is integer here
            edgeUsed = new int[nbEdges];

            startVertexActive = new int[nbTrains];
        }

        override public void SetEdge(int edgeId, bool greedy, int distance)
        {
            base.SetEdge(edgeId, greedy, distance);
            // default number for travel sets
            edgeNbTravelSets[edgeId] = 0;
        }

        // define edgeTravelSets
        public void SetTravelSet(int edgeId, int[] edges)
        {
            for (int j = 0; j < edges.Length; j++)
            {
                edgeTravelSets[edgeId, edgeNbTravelSets[edgeId]++] = edges[j];
            }
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
                if (stationVertex)
                {
                    // train cannot terminate at start vertex
                    if (useRevenuePrediction && PredictRevenues(trainId))
                    {
                        // cannot beat current best value => leave immediately
                        EncounterVertex(trainId, vertexId, false);
                        // but keep them on the visited vertex list to avoid route duplication
                        trainVisited[trainId, vertexId] = true;
                        log.Debug("RC: finished startVertex " + vertexId + " for train " + trainId);
                        continue;
                    }
                }

                // then try all edges of it
                startVertexActive[trainId] = vertexId;
                // for startVertices the sink property is ignored
                for (int j = 0; j < vertexNbNeighbors[vertexId]; j++)
                {
                    int edgeId = vertexEdges[vertexId, j];
                    if (edgeUsed[edgeId] != 0) continue;
                    log.Debug("RC: Testing Neighbor Nr. " + j + " of startVertex");
                    int neighborId = vertexNeighbors[vertexId, j];
                    if (trainVisited[trainId, neighborId])
                    {
                        log.Debug("RC: Hex already visited");
                        continue;
                    }
                    TravelEdge(trainId, edgeId);
                    trainStartEdge[trainId] = j; // store start edge
                    NextVertex(trainId, neighborId);
                    ReturnEdge(trainId, edgeId);
                    trainStackPos[trainId]--; // pull from stack
                }

                // no more edges to find
                EncounterVertex(trainId, vertexId, false);
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
            int vertexId = startVertexActive[trainId];
            if (vertexSink[vertexId])
            {
                log.Debug("RC: startvertex is sink, finished bottom of " + trainId);
                return;
            }

            // push to stack
            trainBottomActive[trainId] = true;
            log.Debug("RC: Restart at bottom at stack position " + trainStackPos[trainId]);
            //        trainStack[trainId][trainStackPos[trainId]++] = vertexId;

            for (int j = trainStartEdge[trainId] + 1; j < vertexNbNeighbors[vertexId]; j++)
            {
                int edgeId = vertexEdges[vertexId, j];
                if (edgeUsed[edgeId] != 0) continue;
                int neighborId = vertexNeighbors[vertexId, j];
                log.Debug("RC: Testing Neighbor Nr. " + j + " of bottomVertex is " + neighborId);
                if (trainVisited[trainId, neighborId])
                {
                    log.Debug(" RC: Hex already visited");
                    continue;
                }
                TravelEdge(trainId, edgeId);
                NextVertex(trainId, neighborId);
                ReturnEdge(trainId, edgeId);
                trainStackPos[trainId]--; // pull from stack
            }

            //        trainStackPos[trainId]--; // pull from stack
            trainBottomActive[trainId] = false;
            log.Debug("RC: finished bottom of " + trainId);
        }

        private void NextVertex(int trainId, int vertexId)
        {

            // 1. encounterVertex adds value and returns true if value vertex
            Terminated trainTerminated = Terminated.NotYet;
            bool stationVertex = EncounterVertex(trainId, vertexId, true);
            if (stationVertex)
            {
                // check usual train termination
                trainTerminated = TrainTerminated(trainId);
                if (trainTerminated == Terminated.WithoutEvaluation ||
                        useRevenuePrediction && PredictRevenues(trainId))
                {
                    // cannot beat current best value => leave immediately
                    EncounterVertex(trainId, vertexId, false);
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
                        if (edgeUsed[edgeId] != 0) continue;
                        int neighborId = vertexNeighbors[vertexId, j];
                        log.Debug("RC: Testing Neighbor Nr. " + j + " of " + vertexId + " is " + neighborId);
                        if (trainVisited[trainId, neighborId])
                        {
                            log.Debug("RC: Hex already visited");
                            continue;
                        }
                        TravelEdge(trainId, edgeId);
                        NextVertex(trainId, neighborId);
                        ReturnEdge(trainId, edgeId);
                        trainStackPos[trainId]--; // pull from stack
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
        }

        virtual protected void TravelEdge(int trainId, int edgeId)
        {
            log.Debug("RC: Travel edge id " + edgeId);
            edgeUsed[edgeId]++;
            trainStack[trainId, trainStackPos[trainId]++] = edgeId; // push to stack
            countEdges++; nbEdgesTravelled++;
            log.Debug("RC: Count Edges = " + countEdges);

            // check edge sets
            for (int j = 0; j < edgeNbTravelSets[edgeId]; j++)
            {
                edgeUsed[edgeTravelSets[edgeId, j]]++;
                log.Debug("RC: traveled edge " + edgeTravelSets[edgeId, j] + " due to edge set.");
            }
        }

        override protected void ReturnEdge(int trainId, int edgeId)
        {
            if (edgeUsed[edgeId] != 0)
            {
                edgeUsed[edgeId]--;
                countEdges--;
                log.Debug("RC: Cleared edge id " + edgeId);
                log.Debug("RC: Count Edges = " + countEdges);

                // check edge sets
                for (int j = 0; j < edgeNbTravelSets[edgeId]; j++)
                {
                    edgeUsed[edgeTravelSets[edgeId, j]]--;
                    log.Debug("RC: Cleared edge " + edgeTravelSets[edgeId, j] + " due to edge set.");
                }
            }
            else
            {
                log.Debug("RC: Error return edge id used: " + edgeId);
            }

        }
    }
}

using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public class RevenueCalculatorMultiHex : RevenueCalculatorMulti
    {
        public RevenueCalculatorMultiHex(RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges,
                int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses) :
            base(revenueAdapter, nbVertexes, nbEdges,
                    maxNeighbors, maxVertexSets, maxEdgeSets, nbTrains, nbBonuses)
        {
        }

        override protected void RunTrain(int trainId)
        {
            // init train distance
            if (trainIsH[trainId])
            {
                trainDistance[trainId] = trainMaxMajors[trainId];
            }
            base.RunTrain(trainId);
        }

        override protected void TravelEdge(int trainId, int edgeId)
        {
            base.TravelEdge(trainId, edgeId);
            trainDistance[trainId] -= edgeDistance[edgeId];
        }

        override protected void ReturnEdge(int trainId, int edgeId)
        {
            base.ReturnEdge(trainId, edgeId);
            trainDistance[trainId] += edgeDistance[edgeId];
        }

        override protected Terminated TrainTerminated(int trainId)
        {
            if (!trainIsH[trainId])
            { // other trains use standard termination method
                return base.TrainTerminated(trainId);
            }
            else
            {
                // H-train can always travel a zero distance to a next node
                // thus always check until negative distance
                if (trainDistance[trainId] < 0)
                {
                    log.Debug("RC: H-Train " + trainId + " has terminated: " +
                            "distance = " + trainDistance[trainId]);
                    return Terminated.WithoutEvaluation;
                }
                else
                {
                    return Terminated.NotYet;
                }
            }
        }
    }
}

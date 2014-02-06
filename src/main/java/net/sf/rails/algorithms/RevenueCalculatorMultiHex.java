package net.sf.rails.algorithms;

class RevenueCalculatorMultiHex extends RevenueCalculatorMulti {

    public RevenueCalculatorMultiHex (RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges, 
            int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses) {

        super(revenueAdapter, nbVertexes, nbEdges, 
                maxNeighbors, maxVertexSets, maxEdgeSets, nbTrains, nbBonuses);

    }

    @Override
    protected void runTrain(int trainId) {
        // init train distance
        if (trainIsH[trainId]) {
            trainDistance[trainId] = trainMaxMajors[trainId];
        }
        super.runTrain(trainId);
    }

    @Override
    protected void travelEdge(int trainId, int edgeId) {
        super.travelEdge(trainId, edgeId);
        trainDistance[trainId] -= edgeDistance[edgeId];
    }

    @Override
    protected void returnEdge(int trainId, int edgeId) {
        super.returnEdge(trainId, edgeId);
        trainDistance[trainId] += edgeDistance[edgeId];
    }

    @Override
    protected Terminated trainTerminated(int trainId) {
        if (!trainIsH[trainId]) { // other trains use standard termination method
            return super.trainTerminated(trainId);
        } else {
            // H-train can always travel a zero distance to a next node
            // thus always check until negative distance
            if (trainDistance[trainId] < 0){
                log.debug ("RC: H-Train " + trainId + " has terminated: " +
                        "distance = " + trainDistance[trainId]);
                return Terminated.WithoutEvaluation;
            } else {
                return Terminated.NotYet;
            }
        }
    }
  
}

package rails.algorithms;

final class RevenueCalculatorMultiHex extends RevenueCalculatorMulti {

    public RevenueCalculatorMultiHex (RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges, 
            int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses) {
    
        super(revenueAdapter, nbVertexes, nbEdges, 
                maxNeighbors, maxVertexSets, maxEdgeSets, nbTrains, nbBonuses);

    }

    @Override
    protected final void runTrain(final int trainId) {
        // init train distance
        if (trainIsH[trainId]) {
            trainDistance[trainId] = trainMaxMajors[trainId];
        }
        super.runTrain(trainId);
    }
    
    @Override
    protected final void travelEdge(final int trainId, final int edgeId) {
        super.travelEdge(trainId, edgeId);
        trainDistance[trainId] -= edgeDistance[edgeId];
    }
    
    @Override
    protected final void returnEdge(final int trainId, final int edgeId) {
        super.returnEdge(trainId, edgeId);
        trainDistance[trainId] += edgeDistance[edgeId];
    }
   
    @Override
    protected final Terminated trainTerminated(final int trainId) {
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

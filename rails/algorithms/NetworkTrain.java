package rails.algorithms;

import rails.game.TrainI;
import rails.game.TrainTypeI;

public final class NetworkTrain {

    private int majors;
    private int minors;
    private final boolean ignoreMinors;
    private final int multiplyMajors;
    private final int multiplyMinors;
    private final String trainName;
    private final TrainTypeI railsTrainType;
    
    NetworkTrain(int majors, int minors, boolean ignoreMinors,
            int multiplyMajors, int multiplyMinors, String trainName,
            TrainTypeI trainType) {
        this.majors = majors;
        this.minors = minors;
        this.ignoreMinors = ignoreMinors;
        this.multiplyMajors = multiplyMajors;
        this.multiplyMinors = multiplyMinors;
        this.trainName = trainName;
        this.railsTrainType = trainType;
    }

    static NetworkTrain createFromRailsTrain(TrainI railsTrain){
        int cities = railsTrain.getMajorStops();
        int towns = railsTrain.getMinorStops();
        boolean townsCostNothing = (railsTrain.getTownCountIndicator() == 0);
        int multiplyCities = railsTrain.getCityScoreFactor();
        int multiplyTowns = railsTrain.getTownScoreFactor();
        String trainName = railsTrain.getName();
        TrainTypeI trainType = railsTrain.getType();

        if (cities == 0 && towns == 0) {
            return null;// protection against pullman
        } else {
            return new NetworkTrain(cities, towns, townsCostNothing, multiplyCities, multiplyTowns,
                trainName, trainType); 
        }
    }
    
    void addToRevenueCalculator(RevenueCalculator rc, int trainId) {
        rc.setTrain(trainId, majors, minors, ignoreMinors);
    }

    int getMajors(){
        return majors;
    }
    
    void setMajors(int majors){
        this.majors = majors;
    }
    
    int getMinors() {
        return minors;
    }
    
    void setMinors(int minors){
        this.minors = minors;
    }
    
    int getMultiplyMajors() {
        return multiplyMajors;
    }
    
    int getMultiplyMinors() {
        return multiplyMinors;
    }
    
    boolean ignoresMinors() {
        return ignoreMinors;
    }
    
    TrainTypeI getRailsTrainType() {
        return railsTrainType;
    }
    
    public String toString() {
        return trainName;
    }
    
}

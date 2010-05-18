package rails.algorithms;

import org.apache.log4j.Logger;

import rails.game.TrainI;
import rails.game.TrainTypeI;

public final class NetworkTrain {

    protected static Logger log =
        Logger.getLogger(NetworkTrain.class.getPackage().getName());

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
        log.info("Created NetworkTrain " + this.toString() + " / " + this.attributes());
    }

    static NetworkTrain createFromRailsTrain(TrainI railsTrain){
        int majors = railsTrain.getMajorStops();
        int minors = railsTrain.getMinorStops();
        if (railsTrain.getTownCountIndicator() == 0) {
            minors = 999;
        }
        int multiplyMajors = railsTrain.getCityScoreFactor();
        int multiplyMinors = railsTrain.getTownScoreFactor();
        boolean ignoreMinors = false;
        if (multiplyMinors == 0){
            ignoreMinors = true;
        }
        String trainName = railsTrain.getName();
        TrainTypeI trainType = railsTrain.getType();

        if (majors == -1) {
            return null;// protection against pullman
        } else {
            return new NetworkTrain(majors, minors, ignoreMinors, multiplyMajors, multiplyMinors,
                trainName, trainType); 
        }
    }
    
    static NetworkTrain createFromString(String trainString) {
        String t = trainString.trim();
        int cities = 0; int towns = 0; boolean ignoreTowns = false; int multiplyCities = 1; int multiplyTowns = 1;
        if (t.equals("D")) {
            log.info("RA: found Diesel train");
            cities = 99;
        } else if (t.contains("+")) {
            log.info("RA: found Plus train");
            cities = Integer.parseInt(t.split("\\+")[0]); // + train
            towns = Integer.parseInt(t.split("\\+")[1]);
        } else if (t.contains("E")) {
            log.info("RA: found Express train");
            cities = Integer.parseInt(t.replace("E", ""));
            ignoreTowns = true;
            multiplyTowns = 0;
        } else if (t.contains("D")) {
            log.info("RA: found Double Express train");
            cities = Integer.parseInt(t.replace("D",  ""));
            ignoreTowns = true;
            multiplyCities = 2;
            multiplyTowns = 0;
        } else { 
            log.info("RA: found Default train");
            cities = Integer.parseInt(t);
        }
        NetworkTrain train = new NetworkTrain(cities, towns, ignoreTowns, multiplyCities, multiplyTowns, t, null); 
        return train;
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

    public String attributes() {
       StringBuffer attributes = new StringBuffer();
       attributes.append("majors = " + majors);
       attributes.append(", minors = " + minors);
       attributes.append(", ignoreMinors = " + ignoreMinors);
       attributes.append(", mulitplyMajors = " + multiplyMajors);
       attributes.append(", mulitplyMinors = " + multiplyMinors);
       return attributes.toString(); 
    }
    
    public String toString() {
        return trainName;
    }
    
}

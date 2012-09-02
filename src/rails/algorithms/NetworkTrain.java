package rails.algorithms;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.Train;
import rails.game.TrainType;

public final class NetworkTrain implements Comparable<NetworkTrain>{

    protected static Logger log =
        LoggerFactory.getLogger(NetworkTrain.class);

    private int majors;
    private int minors;
    private final boolean ignoreMinors;
    private final int multiplyMajors;
    private final int multiplyMinors;
    private final boolean isHTrain;
    private String trainName;
    private final Train railsTrain;
    
    
    private NetworkTrain(int majors, int minors, boolean ignoreMinors,
            int multiplyMajors, int multiplyMinors, boolean isHTrain, String trainName, Train train) {
        this.majors = majors;
        this.minors = minors;
        this.ignoreMinors = ignoreMinors;
        this.multiplyMajors = multiplyMajors;
        this.multiplyMinors = multiplyMinors;
        this.isHTrain = isHTrain;
        this.trainName = trainName;
        this.railsTrain = train;
        log.info("Created NetworkTrain " + this.toString() + " / " + this.attributes());
    }

    static NetworkTrain createFromRailsTrain(Train railsTrain){
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
        boolean isHTrain = railsTrain.isHTrain();
        String trainName = railsTrain.getId();

        return new NetworkTrain(majors, minors, ignoreMinors, multiplyMajors, multiplyMinors,
                isHTrain, trainName, railsTrain); 
    }
    
    public static NetworkTrain createFromString(String trainString) {
        String t = trainString.trim();
        int cities = 0; int towns = 0;
        boolean ignoreTowns = false; int multiplyCities = 1; int multiplyTowns = 1;
        boolean isHTrain = false;
        if (t.equals("D")) {
            log.info("RA: found Diesel train");
            cities = 99;
        } else if (t.equals("TGV")) {
            log.info("RA: found TGV  train");
            cities = 3;
            ignoreTowns = true;
            multiplyCities = 2;
            multiplyTowns = 0;
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
        } else if (t.contains("H")) {
            log.info("RA: found Hex train");
            cities = Integer.parseInt(t.replace("H",  ""));
            isHTrain = true;
        } else { 
            log.info("RA: found Default train");
            cities = Integer.parseInt(t);
        }
        NetworkTrain train = new NetworkTrain(cities, towns, ignoreTowns, multiplyCities, 
                multiplyTowns, isHTrain, t, null); 
        return train;
    }
    
    void addToRevenueCalculator(RevenueCalculator rc, int trainId) {
        rc.setTrain(trainId, majors, minors, ignoreMinors, isHTrain);
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
    
    boolean isHTrain() {
        return isHTrain;
    }
    
    public void setTrainName(String name) {
        trainName = name;
    }
    
    public String getTrainName() {
        return trainName;
    }
    
    public Train getRailsTrain() {
        return railsTrain;
    }
    
    public TrainType getRailsTrainType() {
        if (railsTrain == null) return null;
        
        return railsTrain.getType();
    }
    

    public String attributes() {
       StringBuffer attributes = new StringBuffer();
       attributes.append("majors = " + majors);
       attributes.append(", minors = " + minors);
       attributes.append(", ignoreMinors = " + ignoreMinors);
       attributes.append(", mulitplyMajors = " + multiplyMajors);
       attributes.append(", mulitplyMinors = " + multiplyMinors);
       attributes.append(", isHTrain = " + isHTrain);
       return attributes.toString(); 
    }
    
    public String toString() {
        return trainName;
    }
    
    
    /**
     * Comperator on trains as defined by train domination
     * 
     * A train dominates:
     * it has to be longer in either majors and minors
     * and at least equally long in both
     * 
     * Furthermore the dominating train has at least the same multiples as the shorter
     */

    public int compareTo(NetworkTrain other) {

        // Check if A is the longer train first
        boolean longerA = this.majors > other.majors && this.minors >= other.minors || this.majors == other.majors && this.minors > other.minors;        
        
        if (longerA) {
            // then check the multiples
            if (this.multiplyMajors >= other.multiplyMajors && this.multiplyMinors >= other.multiplyMinors) {
                return 1;
            } else {
                return 0;
            }
        } else {
            // otherwise B might B longer
            boolean longerB = this.majors < other.majors && this.minors <= other.minors || this.majors == other.majors && this.minors < other.minors;
            if (longerB) {
                // then check the multiples
                if (this.multiplyMajors <= other.multiplyMajors && this.multiplyMinors <= other.multiplyMinors) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                // none is longer
                return 0;
            }
        }
    }

}

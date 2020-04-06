package net.sf.rails.algorithms;


import net.sf.rails.game.Train;
import net.sf.rails.game.TrainType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class NetworkTrain implements Comparable<NetworkTrain>{

    private static final Logger log = LoggerFactory.getLogger(NetworkTrain.class);

    private int majors;
    private int minors;
    private final boolean ignoreMinors;
    private final int multiplyMajors;
    private final int multiplyMinors;
    private final boolean isHTrain;
    private final boolean isETrain;
    private String trainName;
    private final Train railsTrain;


    private NetworkTrain(int majors, int minors, boolean ignoreMinors,
            int multiplyMajors, int multiplyMinors, boolean isHTrain, boolean isETrain, String trainName, Train train) {
        this.majors = majors;
        this.minors = minors;
        this.ignoreMinors = ignoreMinors;
        this.multiplyMajors = multiplyMajors;
        this.multiplyMinors = multiplyMinors;
        this.isHTrain = isHTrain;
        this.isETrain = isETrain;
        this.trainName = trainName;
        this.railsTrain = train;
        log.debug("Created NetworkTrain {} / {}", this.toString(), this.attributes());
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
        boolean isETrain = railsTrain.isETrain();
        String trainName = railsTrain.toText();

        return new NetworkTrain(majors, minors, ignoreMinors, multiplyMajors, multiplyMinors,
                isHTrain, isETrain, trainName, railsTrain);
    }

    public static NetworkTrain createFromString(String trainString) {
        String t = trainString.trim();
        int cities = 0; int towns = 0;
        boolean ignoreTowns = false; int multiplyCities = 1; int multiplyTowns = 1;
        boolean isHTrain = false;
        boolean isETrain = false;
        if (t.equals("D")) {
            log.debug("RA: found Diesel train");
            cities = 99;
        } else if (t.equals("TGV")) {
            log.debug("RA: found TGV  train");
            cities = 3;
            ignoreTowns = true;
            multiplyCities = 2;
            multiplyTowns = 0;
        } else if (t.contains("+")) {
            log.debug("RA: found Plus train");
            cities = Integer.parseInt(t.split("\\+")[0]); // + train
            towns = Integer.parseInt(t.split("\\+")[1]);
        } else if (t.contains("E")) {
            log.debug("RA: found Express train");
            //cities = Integer.parseInt(t.replace("E", ""));
            ignoreTowns = true;
            isETrain = true;
            multiplyTowns = 0;
            cities = 99; //for now in 1880, specific implementation in ExpressTrainModifier
        } else if (t.contains("D")) {
            log.debug("RA: found Double Express train");
            cities = Integer.parseInt(t.replace("D",  ""));
            ignoreTowns = true;
            isETrain = true;
            multiplyCities = 2;
            multiplyTowns = 0;
        } else if (t.contains("H")) {
            log.debug("RA: found Hex train");
            cities = Integer.parseInt(t.replace("H",  ""));
            isHTrain = true;
        } else {
            log.debug("RA: found Default train");
            cities = Integer.parseInt(t);
        }
        NetworkTrain train = new NetworkTrain(cities, towns, ignoreTowns, multiplyCities,
                multiplyTowns, isHTrain, isETrain, t, null);
        return train;
    }

    void addToRevenueCalculator(RevenueCalculator rc, int trainId) {
        rc.setTrain(trainId, majors, minors, ignoreMinors, isHTrain, isETrain);
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

    public boolean isETrain() {
        return isETrain;
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
       StringBuilder attributes = new StringBuilder();
       attributes.append("majors = ").append(majors);
       attributes.append(", minors = ").append(minors);
       attributes.append(", ignoreMinors = ").append(ignoreMinors);
       attributes.append(", mulitplyMajors = ").append(multiplyMajors);
       attributes.append(", mulitplyMinors = ").append(multiplyMinors);
       attributes.append(", isHTrain = ").append(isHTrain);
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

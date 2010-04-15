package rails.algorithms;

public final class NetworkTrain {

    private int cities;
    private int towns;
    private final boolean townsCostNothing;
    private final int multiplyCities;
    private final int multiplyTowns;
    private final String trainName;
    
    NetworkTrain(int cities, int towns, boolean townsCostNothing,
            int multiplyCities, int multiplyTowns, String trainName) {
        this.cities = cities;
        this.towns = towns;
        this.townsCostNothing = townsCostNothing;
        this.multiplyCities = multiplyCities;
        this.multiplyTowns = multiplyTowns;
        this.trainName = trainName;
    }

    void addToRevenueCalculator(RevenueCalculator rc, int trainId) {
        rc.setTrain(trainId, cities, towns, townsCostNothing, multiplyCities, multiplyTowns);
    }

    int getCities(){
        return cities;
    }
    
    void setCities(int cities){
        this.cities = cities;
    }
    
    int getTowns() {
        return towns;
    }
    
    void setTowns(int towns){
        this.towns = towns;
    }
    
    int calculateRevenue(int[] cityValues, int[] townValues) {
        return cityValues[cities] * multiplyCities + townValues[towns] * multiplyTowns; 
    }
    
    public String toString() {
        return trainName;
    }
    
}

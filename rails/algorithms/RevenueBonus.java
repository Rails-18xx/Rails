package rails.algorithms;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import rails.game.PhaseI;
import rails.game.TrainTypeI;

public final class RevenueBonus {

    protected static Logger log =
        Logger.getLogger(RevenueBonus.class.getPackage().getName());
    
    // bonus values
    private final int value;
    
    // bonus name, also identifies mutually exclusive bonuses
    private final String name;
    
    // internal attributes
    private List<NetworkVertex> vertices;
    private List<TrainTypeI> trainTypes;
    private List<PhaseI> phases;
    
    public RevenueBonus(int value, String name) {
        this.value = value;
        this.name = name;
     
        vertices = new ArrayList<NetworkVertex>();
        trainTypes = new ArrayList<TrainTypeI>();
        phases = new ArrayList<PhaseI>();
    }
    
    public void addVertex(NetworkVertex vertex) {
        vertices.add(vertex);
    }

    public void addTrainType(TrainTypeI trainType) {
        trainTypes.add(trainType);
    }
    
    public void addPhase(PhaseI phase) {
        phases.add(phase);
    }
    
    public int getValue() {
        return value;
    }
    
    public String getName() {
        return name;
    }
    
    public List<NetworkVertex> getVertices() {
        return vertices;
    }
    
    public List<TrainTypeI> getTrainTypes() {
        return trainTypes;
    }

    public List<PhaseI> getPhases() {
        return phases;
    }
    
    public boolean isSimpleBonus() {
        return (vertices.size() == 1);
    }
    
    public boolean addToRevenueCalculator(RevenueCalculator rc, int bonusId, List<NetworkVertex> allVertices, List<NetworkTrain> trains, PhaseI phase) {
        if (isSimpleBonus() || !phases.isEmpty() && !phases.contains(phase)) return false;
        // only non-simple bonuses and checks phase condition
        
        int[] verticesArray = new int[vertices.size()];
        for (int j=0; j < vertices.size(); j++) {
            if (!allVertices.contains(vertices.get(j))) return false; // if vertex is not on graph, do not add bonus
            verticesArray[j] = allVertices.indexOf(vertices.get(j));
        }
        
        boolean[] trainsArray = new boolean[trains.size()];
        for (int j=0; j < trains.size(); j++) {
            trainsArray[j] = checkConditions(trains.get(j).getRailsTrainType(), phase);
        }
        
        log.info("Add revenueBonus to RC, id = " + bonusId + ", bonus = " + this);
        
        rc.setBonus(bonusId, value, verticesArray, trainsArray);
        
        return true;
    }
    
    public boolean checkSimpleBonus(NetworkVertex vertex, TrainTypeI trainType, PhaseI phase) {
        return (isSimpleBonus() && vertices.contains(vertex) && checkConditions(trainType, phase));
    }
    
    public boolean checkComplexBonus(List<NetworkVertex> visitVertices, TrainTypeI trainType, PhaseI phase) {
        boolean result = !isSimpleBonus() && checkConditions(trainType, phase);
        if (result) {
            for (NetworkVertex vertex:vertices) {
                if (!visitVertices.contains(vertex)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
    public boolean checkConditions(TrainTypeI trainType, PhaseI phase) {
        boolean result = true;

        // check trainTypes
        if (!trainTypes.isEmpty()) {
            if (trainType == null) {
                result = false;
            } else {
                result = result && trainTypes.contains(trainType); 
            }
        }
        
        // check phase
        if (!phases.isEmpty()) {
            if (phase == null) {
                result = false;
            } else {
                result = result && phases.contains(phase);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("RevenueBonus");
        if (name == null) 
            s.append (" unnamed");
        else
            s.append(" name = " + name);
        s.append(", value " + value);
        s.append(", vertices = " + vertices);
        s.append(", trainTypes = " + trainTypes);
        s.append(", phases = " + phases);
        return s.toString();
    }
  
    
    public static int getNumberNonSimpleBonuses(List<RevenueBonus> bonuses) {
        int number = 0;
        for (RevenueBonus bonus:bonuses) {
            if (!bonus.isSimpleBonus()) number++;
        }
        return number;
    }
    
}

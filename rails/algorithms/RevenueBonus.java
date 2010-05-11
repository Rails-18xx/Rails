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
    
    // internal attributes
    private List<NetworkVertex> vertices;
    private List<TrainTypeI> trainTypes;
    private List<PhaseI> phases;
    
    public RevenueBonus(int value) {
        this.value = value;
     
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
    
    public boolean checkSimpleBonus(NetworkVertex vertex, TrainTypeI trainType, PhaseI phase) {
        return (isSimpleBonus() && vertices.contains(vertex) && checkConditions(trainType, phase));
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
        s.append("RevenueBonus with value " + value);
        s.append(", vertices = " + vertices);
        s.append(", trainTypes = " + trainTypes);
        s.append(", phases = " + phases);
        return s.toString();
    }
    
}

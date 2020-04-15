package net.sf.rails.algorithms;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import net.sf.rails.game.Phase;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RevenueBonus {

    private static final Logger log = LoggerFactory.getLogger(RevenueBonus.class);

    // bonus values
    private final int value;

    // bonus name, also identifies mutually exclusive bonuses
    private final String name;

    // internal attributes
    private List<NetworkVertex> vertices;
    private List<TrainType> trainTypes;
    private List<Train> trains;
    private List<Phase> phases;

    public RevenueBonus(int value, String name) {
        this.value = value;
        this.name = name;

        vertices = new ArrayList<>();
        trainTypes = new ArrayList<>();
        trains = new ArrayList<>();
        phases = new ArrayList<>();
    }

    public void addVertex(NetworkVertex vertex) {
        vertices.add(vertex);
    }

    public void addVertices(Collection<NetworkVertex> vertices) {
        this.vertices.addAll(vertices);
    }

    public void addTrainType(TrainType trainType) {
        trainTypes.add(trainType);
    }

    public void addTrain(Train train) {
        trains.add(train);
    }

    public void addPhase(Phase phase) {
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

    public List<TrainType> getTrainTypes() {
        return trainTypes;
    }

    public List<Train> getTrains() {
        return trains;
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public boolean isSimpleBonus() {
        return (vertices.size() == 1);
    }

    public boolean addToRevenueCalculator(RevenueCalculator rc, int bonusId, List<NetworkVertex> allVertices, List<NetworkTrain> trains, Phase phase) {
        if (isSimpleBonus() || !phases.isEmpty() && !phases.contains(phase)) return false;
        // only non-simple bonuses and checks phase condition

        int[] verticesArray = new int[vertices.size()];
        for (int j=0; j < vertices.size(); j++) {
            if (!allVertices.contains(vertices.get(j))) return false; // if vertex is not on graph, do not add bonus
            verticesArray[j] = allVertices.indexOf(vertices.get(j));
        }

        boolean[] trainsArray = new boolean[trains.size()];
        for (int j=0; j < trains.size(); j++) {
            trainsArray[j] = checkConditions(trains.get(j).getRailsTrain(), phase);
        }

        log.debug("Add revenueBonus to RC, id = {}, bonus = {}", bonusId, this);

        rc.setBonus(bonusId, value, verticesArray, trainsArray);

        return true;
    }

    public boolean checkSimpleBonus(NetworkVertex vertex, Train train, Phase phase) {
        return (isSimpleBonus() && vertices.contains(vertex) && checkConditions(train, phase));
    }

    public boolean checkComplexBonus(List<NetworkVertex> visitVertices, Train train, Phase phase) {
        boolean result = !isSimpleBonus() && checkConditions(train, phase);
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

    public boolean checkConditions(Train train, Phase phase) {
        boolean result = true;

        // check train
        if (!trains.isEmpty()) {
            if (train == null) {
                result = false;
            } else {
                result = result && trains.contains(train);
            }
        }

        // check trainTypes
        if (!trainTypes.isEmpty()) {
            if (train == null) {
                result = false;
            } else {
                result = result && trainTypes.contains(train.getType());
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
        StringBuilder s = new StringBuilder();
        s.append("RevenueBonus");
        if (name == null)
            s.append (" unnamed");
        else
            s.append(" name = ").append(name);
        s.append(", value ").append(value);
        s.append(", vertices = ").append(vertices);
        s.append(", trainTypes = ").append(trainTypes);
        s.append(", phases = ").append(phases);
        return s.toString();
    }

    public static Map<String, Integer> combineBonuses(Collection<RevenueBonus> bonuses){
        Map<String, Integer> combined = new HashMap<String, Integer>();
        for (RevenueBonus bonus:bonuses) {
            String name = bonus.getName();
            if (combined.containsKey(name)) {
                combined.put(name, combined.get(name) + bonus.getValue());
            } else {
                combined.put(name, bonus.getValue());
            }
         }
        return combined;
    }

    public static int getNumberNonSimpleBonuses(List<RevenueBonus> bonuses) {
        int number = 0;
        for (RevenueBonus bonus:bonuses) {
            if (!bonus.isSimpleBonus()) number++;
        }
        return number;
    }

}

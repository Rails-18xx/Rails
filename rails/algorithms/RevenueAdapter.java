package rails.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;

import rails.game.PhaseI;
import rails.game.PublicCompanyI;
import rails.game.TrainI;

public class RevenueAdapter {

    private Graph<NetworkVertex, NetworkEdge> graph;
    
    private RevenueCalculator rc;
    private int maxNeighbors;
    
    private List<NetworkVertex> vertexes;
    private List<NetworkEdge> edges;
    private List<NetworkVertex> startVertexes;
    private List<NetworkTrain> trains;
    
    public RevenueAdapter(Graph<NetworkVertex, NetworkEdge> graph){
        this.graph = graph;
        
        this.vertexes = new ArrayList<NetworkVertex>(graph.vertexSet());
        this.edges = new ArrayList<NetworkEdge>(graph.edgeSet());
        this.trains = new ArrayList<NetworkTrain>();
        this.startVertexes = new ArrayList<NetworkVertex>();
    }
    
    public void initRevenueCalculator(){
        if (rc == null) {
            // define the maximum number of vertexes
            maxNeighbors = 0;
            for (NetworkVertex vertex:vertexes)
                if (!vertex.isHQ())
                    maxNeighbors = Math.max(maxNeighbors,
                        graph.edgesOf(vertex).size());
            this.rc = new RevenueCalculator(vertexes.size(), ++maxNeighbors, trains.size()); // increase maxEdges to allow for cutoff
        }
    }
    
    public void refreshRevenueCalculator() {
        rc = null;
    }
    
    
    private int[] revenueList(List<NetworkVertex> vertexes, int maxLength) {
        Collections.sort(vertexes, new NetworkVertex.ValueOrder());
        
        int[] revenue = new int[maxLength + 1];
        revenue[0] = 0;
        for (int j=1; j <= maxLength; j++) {
            revenue[j] = revenue[j-1] + vertexes.get(j-1).getValue();
        }
        
        return revenue;
    }
    
    public void activateRevenuePrediction() {
        
        // separate vertexes
        List<NetworkVertex> cities = new ArrayList<NetworkVertex>();
        List<NetworkVertex> towns = new ArrayList<NetworkVertex>();
        for (NetworkVertex vertex: vertexes) {
            if (vertex.isCityType()) cities.add(vertex);
            if (vertex.isTownType()) towns.add(vertex);
        }
        
        // check train lengths
        int maxCityLength = 0, maxTownLength = 0;
        for (NetworkTrain train: trains) {
            maxCityLength = Math.max(maxCityLength, train.getCities());
            maxTownLength = Math.max(maxTownLength, train.getTowns());
        }
        
        // get max revenue results
        int[] maxCityRevenues = revenueList(cities, maxCityLength);
        int[] maxTownRevenues = revenueList(towns, maxTownLength);

        // set revenue results in revenue calculator
        rc.setPredictionData(maxCityRevenues, maxTownRevenues);
    }
    
    
    public void populateRevenueCalculator(PublicCompanyI company, PhaseI phase){
        if (rc == null) initRevenueCalculator();

        // set vertexes

        // Define ordering on vertexes by value
        NetworkVertex.setPhaseForAll(vertexes, phase);
        Collections.sort(vertexes, new NetworkVertex.ValueOrder());
        
        for (int id=0; id < vertexes.size(); id++){ 
            NetworkVertex v = vertexes.get(id);
            if (v.isHQ()) {
            // HQ is not added to list, but used to assign startVertexes
                startVertexes.addAll(Graphs.neighborListOf(graph, v));
            } else {
                // prepare values
                int value = v.getValue();
                boolean city = v.isCityType();
                boolean town = v.isTownType();
                int j = 0, e[] = new int[maxNeighbors];
                if (v.canCompanyRunThrough(company)) {
                    for (NetworkVertex n:Graphs.neighborListOf(graph, v)){
                        if (!n.isHQ()) {
                            e[j++] = vertexes.lastIndexOf(n);
                        }
                    }
                }
                // sort by value order
                Arrays.sort(e, 0, j);
                e[j] = -1; // stop 
                rc.setVertex(id, value, city, town, e);
            }
        }

        // set startVertexes
        int[] sv = new int[startVertexes.size()];
        for (int j=0; j < startVertexes.size(); j++) {
            sv[j] = vertexes.lastIndexOf(startVertexes.get(j));
        }
        Arrays.sort(sv); // sort by value order 
        rc.setStartVertexes(sv);

        // set edges
        for (int id=0; id < edges.size(); id++) {
            // prepare values
            NetworkEdge e = edges.get(id);
            int vA = vertexes.lastIndexOf(e.getSource());
            int vB = vertexes.lastIndexOf(e.getTarget());
            boolean greedy = e.isGreedy();
            int distance = e.getDistance();
            rc.setEdge(vA, vB,
                    greedy, distance);
            rc.setEdge(vB, vA,
                    greedy, distance);
        }
        
        // set trains
        for (int id=0; id < trains.size(); id++) {
            NetworkTrain train = trains.get(id);
            train.addToRevenueCalculator(rc, id);
        }
    }

    public void addDefaultTrain(int cities) {
        String trainName = Integer.valueOf(cities).toString();
        NetworkTrain train =new NetworkTrain(cities, 0, false, 1, 1, trainName); 
        trains.add(train);
    }
    
    public void addTrain(TrainI railsTrain){
        int cities = railsTrain.getMajorStops();
        int towns = railsTrain.getMinorStops();
        boolean townsCostNothing = (railsTrain.getTownCountIndicator() == 0);
        int multiplyCities = railsTrain.getCityScoreFactor();
        int multiplyTowns = railsTrain.getTownScoreFactor();
        String trainName = railsTrain.getName();
        
        if (cities > 0 || towns > 0) { // protection against pullman
            NetworkTrain networkTrain = new NetworkTrain(cities, towns, townsCostNothing, multiplyCities, multiplyTowns, trainName); 
            trains.add(networkTrain);
        }
    }
    
    public void addTrainByString(String trainString) {
        String t = trainString.trim();
        
        int cities = 0; int towns = 0; boolean townsCostNothing = false; int multiplyCities = 1; int multiplyTowns = 1;
        if (t.equals("D")) {
            cities = 99; // diesel
        } else if (t.contains("+")) {
            cities = Integer.parseInt(t.split("\\+")[0]); // + train
            towns = Integer.parseInt(t.split("\\+")[1]);
        } else if (t.contains("E")) {
            // express train
            cities = Integer.parseInt(t.replace("E", ""));
            townsCostNothing = true;
            multiplyTowns = 0;
        } else if (t.contains("D")) {
            // double (express) train
            cities = Integer.parseInt(t.replace("D", ""));
            townsCostNothing = true;
            multiplyCities = 2;
            multiplyTowns = 0;
        } else { 
            // default train
            cities = Integer.parseInt(t);
        }
        NetworkTrain networkTrain = new NetworkTrain(cities, towns, townsCostNothing, multiplyCities, multiplyTowns, t); 
        trains.add(networkTrain);
    }
    
    public void addStartVertex(NetworkVertex v) {
        startVertexes.add(v);
    }
    
    public Map<NetworkTrain, List<NetworkVertex>> getOptimalRun() {
        int[][] optimalRunRaw = rc.getOptimalRun();
        Map<NetworkTrain, List<NetworkVertex>> optimalRun = new HashMap<NetworkTrain, List<NetworkVertex>>(); 
        for (int j=0; j < optimalRunRaw.length; j++) {
            List<NetworkVertex> runList = new ArrayList<NetworkVertex>();
            for (int v=0; v < optimalRunRaw[j].length; v++) {
                int vertexId = optimalRunRaw[j][v];
                if (vertexId == -1) break;
                runList.add(vertexes.get(vertexId));
            }
            optimalRun.put(trains.get(j), runList);
        }
        return optimalRun;
    }
    
    public int calculateRevenue() {
        return calculateRevenue(0, trains.size() - 1);
    }
    
    public int calculateRevenue(int startTrain, int finalTrain) {
        if (startTrain < 0 || finalTrain >= trains.size() || startTrain > finalTrain) return -1;
        return rc.calculateRevenue(startTrain, finalTrain);
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("RevenueCalculator:" + rc);
        buffer.append("Vertexes:" + vertexes);
        buffer.append("Edges:" + edges);
        return buffer.toString();
    }
    
}

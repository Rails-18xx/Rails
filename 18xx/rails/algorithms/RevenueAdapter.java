package rails.algorithms;

import java.awt.EventQueue;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.PhaseI;
import rails.game.PublicCompanyI;
import rails.game.TrainI;
import rails.game.TrainTypeI;
import rails.ui.swing.hexmap.HexMap;


public class RevenueAdapter implements Runnable {

    protected static Logger log =
        Logger.getLogger(RevenueAdapter.class.getPackage().getName());
    
    private GameManagerI gameManager;
    private NetworkGraphBuilder graphBuilder;
    private PublicCompanyI company;
    private PhaseI phase;
    private SimpleGraph<NetworkVertex, NetworkEdge> graph;
    
    private RevenueCalculator rc;
    private int maxNeighbors;
    private int maxBlocks;
    
    private List<NetworkVertex> vertexes;
    private List<NetworkEdge> edges;
    private List<NetworkVertex> startVertexes;
    private List<NetworkTrain> trains;
    private Map<NetworkVertex, List<NetworkVertex>> vertexVisitSets;
    private List<RevenueBonus> revenueBonuses;
    
    // revenue listener
    private RevenueListener revenueListener;
    
    public RevenueAdapter(GameManagerI gameManager, NetworkGraphBuilder graphBuilder, PublicCompanyI company){
        this.gameManager = gameManager;
        this.graphBuilder = graphBuilder;
        
        this.graph = NetworkGraphBuilder.optimizeGraph(graphBuilder.getRailRoadGraph(company));
        this.vertexes = new ArrayList<NetworkVertex>(graph.vertexSet());
        this.edges = new ArrayList<NetworkEdge>(graph.edgeSet());
        this.trains = new ArrayList<NetworkTrain>();
        this.startVertexes = new ArrayList<NetworkVertex>();
        this.vertexVisitSets = new HashMap<NetworkVertex, List<NetworkVertex>>();
        this.revenueBonuses = new ArrayList<RevenueBonus>();
    }
    
    private void defineRevenueBonuses() {
        // check each vertex hex for a potential revenue bonus
        for (NetworkVertex vertex:vertexes) {
            MapHex hex = vertex.getHex();
            if (hex == null) continue;
            
            List<RevenueBonusTemplate> bonuses = new ArrayList<RevenueBonusTemplate>();
            List<RevenueBonusTemplate> hexBonuses = hex.getRevenueBonuses();
            if (hexBonuses != null) bonuses.addAll(hexBonuses);
//            List<RevenueBonusTemplate> tileBonuses = hex.getCurrentTile().getRevenueBonuses();
//            if (tileBonuses != null) bonuses.addAll(hexBonuses);

            if (bonuses == null) continue;
            for (RevenueBonusTemplate bonus:bonuses) {
                revenueBonuses.add(bonus.toRevenueBonus(hex, gameManager, graphBuilder));
            }
        }
        log.info("RevenueBonuses = " + revenueBonuses);
    }
    
    private int defineVertexVisitSets() {
        // define map of all locationNames 
        Map<String, List<NetworkVertex>> locations = new HashMap<String, List<NetworkVertex>>();
        for (NetworkVertex vertex:vertexes) {
            String ln = vertex.getCityName();
            if (ln == null) continue;
            if (locations.containsKey(ln)) {
                locations.get(ln).add(vertex);
            } else {
                List<NetworkVertex> v = new ArrayList<NetworkVertex>();
                v.add(vertex);
                locations.put(ln, v);
            }
        }
        log.info("Locations = " + locations);
        // transfer those locationNames to vertexBlocks
        int maxBlocks = 0;
        for (List<NetworkVertex> location:locations.values()) {
            if (location.size() >= 2) {
                for (NetworkVertex vertex:location){
                    if (vertexVisitSets.containsKey(vertex)) {
                        for (NetworkVertex v:location) {
                            if (v != vertex && !vertexVisitSets.get(vertex).contains(v)) {
                                vertexVisitSets.get(vertex).add(v);
                            }
                        }
                    } else {
                        List<NetworkVertex> blocks = new ArrayList<NetworkVertex>();
                        for (NetworkVertex v:location) {
                            if (v != vertex) {
                                blocks.add(v);
                            }
                        }
                        vertexVisitSets.put(vertex, blocks);
                    }
                }
                maxBlocks = Math.max(maxBlocks, location.size());
            }
        }
        log.info("RA: Block of " + vertexVisitSets + ", maxBlocks = " + maxBlocks);
        return maxBlocks;
    }
        
    public void initRevenueCalculator(){
        if (rc == null) {
            // define blocks and return those
            maxBlocks = defineVertexVisitSets();
            
            // define revenueBonuses
            defineRevenueBonuses();
            
            // define the maximum number of vertexes
            maxNeighbors = 0;
            for (NetworkVertex vertex:vertexes)
                if (!vertex.isHQ())
                    maxNeighbors = Math.max(maxNeighbors,
                        graph.edgesOf(vertex).size());
            log.info("maxNeighbors = " + maxNeighbors);
            this.rc = new RevenueCalculator(this, vertexes.size(), edges.size(), maxNeighbors, maxBlocks, trains.size()); 
        }
    }
    
    public void refreshRevenueCalculator() {
        rc = null;
        // refresh startVertexes
        this.startVertexes = new ArrayList<NetworkVertex>();
        // refresh revenueBonuses
        this.revenueBonuses = new ArrayList<RevenueBonus>();
    }
    
    private void prepareRevenuePrediction(boolean activatePrediction) {
        
        // separate vertexes
        List<NetworkVertex> cities = new ArrayList<NetworkVertex>();
        List<NetworkVertex> towns = new ArrayList<NetworkVertex>();
        for (NetworkVertex vertex: vertexes) {
            if (vertex.isMajor()) cities.add(vertex);
            if (vertex.isMinor()) towns.add(vertex);
        }
        
        int maxCities = cities.size();
        int maxTowns = towns.size();
        
        // check train lengths
        int maxCityLength = 0, maxTownLength = 0;
        for (NetworkTrain train: trains) {
            int trainTowns = train.getMinors();
            if (train.getMajors() > maxCities) {
                trainTowns = trainTowns+ train.getMajors() - maxCities;
                train.setMajors(maxCities);
            }
            train.setMinors(Math.min(trainTowns, maxTowns));

            maxCityLength = Math.max(maxCityLength, train.getMajors());
            maxTownLength = Math.max(maxTownLength, train.getMinors());
        }
        
    }
    
    public void populateRevenueCalculator(PhaseI phase, boolean activatePrediction){
        // store phase
        this.phase = phase;
        
        if (rc == null) initRevenueCalculator();

        // prepare and optionally activate revenue prediction
        prepareRevenuePrediction(activatePrediction);
        
        // Define ordering on vertexes by value
        NetworkVertex.initAllRailsVertices(vertexes, company, phase);
        Collections.sort(vertexes, new NetworkVertex.ValueOrder());
        
        for (int id=0; id < vertexes.size(); id++){ 
            NetworkVertex v = vertexes.get(id);
            if (v.isHQ()) {
                // HQ is not added to list, but used to assign startVertexes
                startVertexes.addAll(Graphs.neighborListOf(graph, v));
                continue;
            }
            // add to revenue calculator
            v.addToRevenueCalculator(rc, id);
            for (int trainId=0; trainId < trains.size(); trainId++) {
                NetworkTrain train = trains.get(trainId);
                rc.setVertexValue(id, trainId, getVertexValue(v, train, phase));
            }
            
            // set neighbors
            if (!v.isSink()) {
                List<NetworkVertex> neighbors = Graphs.neighborListOf(graph, v); 
                int j=0, neighborsArray[] = new int[neighbors.size()];
                for (NetworkVertex n:neighbors){
                    neighborsArray[j++] = vertexes.indexOf(n);
                }
                // sort by value order
                Arrays.sort(neighborsArray, 0, j);
                // define according edges
                int[] edgesArray = new int[j];
                for (int e=0; e < j; e++) {
                    NetworkVertex n = vertexes.get(neighborsArray[e]);
                    edgesArray[e] = edges.indexOf(graph.getEdge(v, n));
                }
                rc.setVertexNeighbors(id, neighborsArray, edgesArray);
            }
            // set blocks
            if (vertexVisitSets.containsKey(v)) {
                int b = 0, blocks[] = new int[vertexVisitSets.get(v).size()];
                for (NetworkVertex n: vertexVisitSets.get(v)) {
                    blocks[b++] = vertexes.indexOf(n);
                }
                rc.setVertexVisitSets(id, blocks);
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
            boolean greedy = e.isGreedy();
            int distance = e.getDistance();
            rc.setEdge(id, greedy, distance);
        }
        
        // set trains
        for (int id=0; id < trains.size(); id++) {
            NetworkTrain train = trains.get(id);
            train.addToRevenueCalculator(rc, id);
        }
        
        
    }

    public int getVertexValue(NetworkVertex vertex, NetworkTrain train, PhaseI phase) {
        
        // base value
        int value = vertex.getValueByTrain(train);
        
        // add potential revenueBonuses
        for (RevenueBonus bonus:revenueBonuses) {
            if (bonus.checkSimpleBonus(vertex, train.getRailsTrainType(), phase)) {
                value += bonus.getValue();
            }
        }
        
        return value;
    }
    
    public void addDefaultTrain(int cities) {
        String trainName = Integer.valueOf(cities).toString();
        NetworkTrain train =new NetworkTrain(cities, 0, false, 1, 1, trainName, null); 
        trains.add(train);
    }
    
    public void addTrain(TrainI railsTrain){
        NetworkTrain train = NetworkTrain.createFromRailsTrain(railsTrain);
        if (train != null) trains.add(train);
    }
    
    public void addTrainByString(String trainString) {
        String t = trainString.trim();
        TrainTypeI trainType = gameManager.getTrainManager().getTypeByName(trainString);
        if (trainType != null) { // string defines available trainType
            TrainI railsTrain = trainType.cloneTrain();
            addTrain(railsTrain);
            log.info("RA: found trainType" + trainType);
        } else { // otherwise interpret the train
            int cities = 0; int towns = 0; boolean ignoreTowns = false; int multiplyCities = 1; int multiplyTowns = 1;
            if (t.equals("D")) {
                cities = 99; // diesel
            } else if (t.contains("+")) {
                cities = Integer.parseInt(t.split("\\+")[0]); // + train
                towns = Integer.parseInt(t.split("\\+")[1]);
            } else if (t.contains("E")) {
                // express train
                cities = Integer.parseInt(t.replace("E", ""));
                ignoreTowns = true;
                multiplyTowns = 0;
            } else if (t.contains("D")) {
                // double (express) train
                cities = Integer.parseInt(t.replace("D", ""));
                ignoreTowns = true;
                multiplyCities = 2;
                multiplyTowns = 0;
            } else { 
                // default train
                cities = Integer.parseInt(t);
            }
            NetworkTrain networkTrain = new NetworkTrain(cities, towns, ignoreTowns, multiplyCities, multiplyTowns, t, null); 
            trains.add(networkTrain);
        }
    }
    
    public void addStartVertex(NetworkVertex v) {
        startVertexes.add(v);
    }
    
    public void addRevenueBonus(RevenueBonus bonus) {
        revenueBonuses.add(bonus);
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
        rc.initialPredictionRuns(startTrain, finalTrain);
        return rc.calculateRevenue(startTrain, finalTrain);
    }


    public void addRevenueListener(RevenueListener listener) {
        this.revenueListener = listener;
    }

    void notifyRevenueListener(final int revenue, final boolean finalResult) {
        if (revenueListener == null) return;
        
        EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        revenueListener.revenueUpdate(revenue, finalResult);
                    }
                });
    }
    
    public void run() {
        calculateRevenue(0, trains.size() -1);
    }
    
    public void removeRevenueListener() {
        // only removes revenueListener
        revenueListener = null;
    }
    

    public String getOptimalRunPrettyPrint() {
        StringBuffer runPrettyPrint = new StringBuffer();
        Map<NetworkTrain, List<NetworkVertex>> run = getOptimalRun();
        
        for (NetworkTrain train:run.keySet()) {
            runPrettyPrint.append("Train " + train + ": ");
            MapHex currentHex = null;
            NetworkVertex startVertex = null;
            for (NetworkVertex vertex:run.get(train)) {
                if (startVertex == null) {
                    currentHex = vertex.getHex();
                    startVertex = vertex;
                    runPrettyPrint.append(vertex.getHex().getName() + "(");
                } else if (startVertex == vertex) {
                    currentHex = vertex.getHex();
                    runPrettyPrint.append(") / " + vertex.getHex().getName() + "(0");
                    continue;
                } else if (vertex.getHex() != currentHex) {
                    currentHex = vertex.getHex();
                    runPrettyPrint.append("), " + vertex.getHex().getName() + "(");
                } else {
                    runPrettyPrint.append(",");
                }
                if (vertex.isStation()) {
                    runPrettyPrint.append(getVertexValue(vertex, train, phase));
                }  else {
                    runPrettyPrint.append(currentHex.getOrientationName(vertex.getSide()));
                }
            }
            if (currentHex != null) {
                runPrettyPrint.append(")");
            }
            runPrettyPrint.append("\n");
        }
        return runPrettyPrint.toString();
    }
    
    public void drawOptimalRunAsPath(HexMap map) {
        List<GeneralPath> pathList = new ArrayList<GeneralPath>();
        Map<NetworkTrain, List<NetworkVertex>> run = getOptimalRun();
        
        for (NetworkTrain train:run.keySet()) {
            GeneralPath path = new GeneralPath();
            NetworkVertex startVertex = null;
            NetworkVertex previousVertex = null;
            for (NetworkVertex vertex:run.get(train)) {
                log.debug("RA: Next vertex " + vertex);
                Point2D vertexPoint = NetworkVertex.getVertexPoint2D(map, vertex);
                if (startVertex == null) {
                    startVertex = vertex;
                    previousVertex = vertex;
                    path.moveTo((float)vertexPoint.getX(), (float)vertexPoint.getY());
                    continue;
                } else if (startVertex == vertex) {
                    path.moveTo((float)vertexPoint.getX(), (float)vertexPoint.getY());
                    previousVertex = vertex;
                    continue;
                } 
                // draw hidden vertexes
                NetworkEdge edge = graph.getEdge(previousVertex, vertex);
                if (edge != null) {
                    log.debug("RA: draw edge "+ edge.toFullInfoString());
                    List<NetworkVertex> hiddenVertexes = edge.getHiddenVertexes();
                    if (edge.getSource() == vertex) {
                        log.debug("RA: reverse hiddenVertexes");
                        for (int i = hiddenVertexes.size() - 1; i >= 0; i--) {
                            NetworkVertex v = hiddenVertexes.get(i);
                            Point2D vPoint = NetworkVertex.getVertexPoint2D(map, v);
                            if (vPoint != null) {
                                path.lineTo((float)vPoint.getX(), (float)vPoint.getY());
                            }
                        }
                    } else {
                        for (NetworkVertex v:hiddenVertexes) {
                            Point2D vPoint = NetworkVertex.getVertexPoint2D(map, v);
                            if (vPoint != null) {
                                path.lineTo((float)vPoint.getX(), (float)vPoint.getY());
                            }
                        }
                    }
                }
                if (vertexPoint != null) {
                    path.lineTo((float)vertexPoint.getX(), (float)vertexPoint.getY());
                }
                previousVertex = vertex;
            }
            pathList.add(path);
        }
        map.setTrainPaths(pathList);
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("RevenueCalculator: \n" + rc);
        buffer.append("\n Vertexes: \n" + vertexes);
        buffer.append("\n Edges: \n" + edges);
        return buffer.toString();
    }

    
}

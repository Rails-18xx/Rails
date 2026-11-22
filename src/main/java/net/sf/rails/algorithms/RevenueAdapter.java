package net.sf.rails.algorithms;

import java.awt.EventQueue;
import java.awt.geom.GeneralPath;
import java.lang.module.ModuleDescriptor.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.*;
import net.sf.rails.game.ai.AIEvaluatorService;
import net.sf.rails.ui.swing.hexmap.HexMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

/**
 * * RevenueAdapter links the revenue algorithm to Rails.
 */
public class RevenueAdapter implements Runnable {

    int specialRevenue;

    private static final Logger log = LoggerFactory.getLogger(RevenueAdapter.class);

    // define VertexVisitSet
    public static class VertexVisit {
        public Set<NetworkVertex> set;

        public VertexVisit() {
            set = new HashSet<>();
        }

        public VertexVisit(Collection<NetworkVertex> coll) {
            set = new HashSet<>(coll);
        }

        public String toString() {
            return "VertexVisit Set:" + set;
        }
    }

    // define EdgeTravelSet
    public static class EdgeTravel {
        public Set<NetworkEdge> set;

        public EdgeTravel() {
            set = new HashSet<>();
        }

        public EdgeTravel(Collection<NetworkEdge> coll) {
            set = new HashSet<>(coll);
        }

        public String toString() {
            return "EdgeTravel Set:" + set;
        }
    }

    // basic links, to be defined at creation
    private final RailsRoot root;
    private final GameManager gameManager;
    private final RevenueManager revenueManager;
    private final NetworkAdapter networkAdapter;
    private final PublicCompany company;
    private final Phase phase;

    // basic components, defined empty at creation
    private NetworkGraph graph;
    private Set<NetworkVertex> startVertices;
    private List<NetworkTrain> trains;
    private List<VertexVisit> vertexVisitSets;
    private List<RevenueBonus> revenueBonuses;
    private Set<NetworkVertex> protectedVertices;
    private Map<NetworkEdge, EdgeTravel> edgeTravelSets;

    // components related to the revenue calculator
    private RevenueCalculator rc;
    private boolean useMultiGraph;
    private Graph<NetworkVertex, NetworkEdge> rcGraph;
    private List<NetworkVertex> rcVertices;
    private List<NetworkEdge> rcEdges;
    private List<RevenueTrainRun> optimalRun;
    private boolean hasDynamicModifiers;

    // revenue listener to communicate results
    private RevenueListener revenueListener;

    public RevenueAdapter(RailsRoot root, NetworkAdapter networkAdapter,
            PublicCompany company, Phase phase) {

        // --- START NEW LOGIC ---

        // 1. Initialize all fields to their default non-null values first
        this.trains = new ArrayList<>();
        this.startVertices = new HashSet<>();
        this.vertexVisitSets = new ArrayList<>();
        this.edgeTravelSets = new HashMap<>();
        this.revenueBonuses = new ArrayList<>();
        this.protectedVertices = new HashSet<>();
        this.graph = null;

        // 2. Store the arguments
        this.root = root;
        this.networkAdapter = networkAdapter;
        this.company = company;
        this.phase = phase;

        // 3. ONLY wrap the lines that *use* root in a null check
        if (root != null) {
            this.gameManager = root.getGameManager();
            this.revenueManager = root.getRevenueManager();
        } else {
            // Explicitly set them to null if root is null
            this.gameManager = null;
            this.revenueManager = null;
        }
        // --- END NEW LOGIC ---
    }

    // *** ADD New Overloaded Factory Method ***
    /**
     * Creates a RevenueAdapter, optionally delaying the initial data population.
     * @param root The RailsRoot.
     * @param company The operating company.
     * @param phase The current phase.
     * @param populateNow If true, populateFromRails() is called immediately. If false, it must be called manually later.
     * @return A new RevenueAdapter instance.
     */
    public static RevenueAdapter createRevenueAdapter(RailsRoot root, PublicCompany company, Phase phase, boolean populateNow) {
        NetworkAdapter networkAdapter = NetworkAdapter.create(root);
        RevenueAdapter ra = new RevenueAdapter(root, networkAdapter, company, phase);
        if (populateNow) {
            ra.populateFromRails(); // Original behavior
            log.debug("RevenueAdapter (PopulateNow=true): Populated with {} trains for {}.", ra.getTrains().size(), company.getId());
        } else {
             log.debug("RevenueAdapter (PopulateNow=false): Created, population delayed.");
             // Ensure train list is initialized even if population is delayed
             if (ra.trains == null) {
                 ra.trains = new ArrayList<>();
             }
        }
        // Static modifiers might need careful handling if population is delayed
        // For now, assume they are handled within populateFromRails or initRevenueCalculator
        return ra;
    }

    

    public static RevenueAdapter createRevenueAdapter(RailsRoot root, PublicCompany company, Phase phase) {
        NetworkAdapter networkAdapter = NetworkAdapter.create(root);
        RevenueAdapter ra = new RevenueAdapter(root, networkAdapter, company, phase);
        ra.populateFromRails();

        /*
         * [FIX] The loop below was redundant (populateFromRails already populates
         * trains)
         * and contained compile errors (static access to non-static 'gameManager'
         * and 'addTrain').
         *
         * The log line was also broken (referencing out-of-scope 'train').
         *
         * Removing the broken loop and fixing the log message.
         */

        // --- FIXED LOG LINE ---
        log.debug("RevenueAdapter: Populated with {} trains for {}.", ra.getTrains().size(), company.getId());

        // add all static modifiers
        return ra;
    }

    public PublicCompany getCompany() {
        return company;
    }

    public Phase getPhase() {
        return phase;
    }

    public SimpleGraph<NetworkVertex, NetworkEdge> getGraph() {
        return graph.getGraph();
    }

    public Set<NetworkVertex> getVertices() {
        return graph.getGraph().vertexSet();
    }

    public Set<NetworkEdge> getEdges() {
        return graph.getGraph().edgeSet();
    }

    public Graph<NetworkVertex, NetworkEdge> getRCGraph() {
        return rcGraph;
    }

    public int getRCVertexId(NetworkVertex vertex) {
        return rcVertices.indexOf(vertex);
    }

    public int getRCEdgeId(NetworkEdge edge) {
        return rcEdges.indexOf(edge);
    }

    public Set<NetworkVertex> getStartVertices() {
        return startVertices;
    }

    public boolean addStartVertices(Collection<NetworkVertex> startVertices) {
        this.startVertices.addAll(startVertices);
        protectedVertices.addAll(startVertices);
        return true;
    }

    public List<NetworkTrain> getTrains() {
        return trains;
    }

    public boolean addTrain(Train railsTrain) {
        NetworkTrain train = NetworkTrain.createFromRailsTrain(railsTrain);
        if (train == null) {
            return false;
        } else {
            trains.add(train);
            return true;
        }
    }

    public void addTrain(NetworkTrain train) {
        trains.add(train);
    }

    public void removeTrain(NetworkTrain train) {
        trains.remove(train);
    }

    public boolean addTrainByString(String trainString) {
        NetworkTrain train = NetworkTrain.createFromString(trainString);
        if (train == null)
            return false;
        addTrain(train);
        return true;
    }

    public List<VertexVisit> getVertexVisitSets() {
        return vertexVisitSets;
    }

    public void addVertexVisitSet(VertexVisit visit) {
        vertexVisitSets.add(visit);
        protectedVertices.addAll(visit.set);
    }

    public List<RevenueBonus> getRevenueBonuses() {
        return revenueBonuses;
    }

    public void addRevenueBonus(RevenueBonus bonus) {
        revenueBonuses.add(bonus);
        protectedVertices.addAll(bonus.getVertices());
    }

    public void removeRevenueBonus(RevenueBonus bonus) {
        revenueBonuses.remove(bonus);
        // TODO: Change protectedVertices to multiSet then you can unprotect vertices
    }

    public void populateFromRails() {
        // define graph, without HQ
        graph = networkAdapter.getRouteGraphCached(company, false);

        // initialize vertices
        NetworkVertex.initAllRailsVertices(graph, company, phase, true);

        // define startVertexes
        addStartVertices(graph.getCompanyBaseTokenVertexes(company));

        // define visit sets
        defineVertexVisitSets();

        // define revenueBonuses
        defineRevenueBonuses();

        // define Trains
        for (Train train : company.getPortfolioModel().getTrainList()) {
            if (!gameManager.isTrainBlocked(train)) {
                addTrain(train);
            }
        }

        // add all static modifiers
        if (revenueManager != null) {
            revenueManager.initStaticModifiers(this);
        }

    }

    private void defineVertexVisitSets() {
        // define map of all locationNames
        Map<String, VertexVisit> locations = new HashMap<>();
        for (NetworkVertex vertex : getVertices()) {
            // String ln = vertex.getStopName();
            String ln = vertex.getMutexId();
            if (ln == null)
                continue;
            if (locations.containsKey(ln)) {
                locations.get(ln).set.add(vertex);
            } else {
                VertexVisit v = new VertexVisit();
                v.set.add(vertex);
                locations.put(ln, v);
            }
        }
        log.debug("Locations = {}", locations);
        // convert the location map to the vertex sets
        for (VertexVisit location : locations.values()) {
            if (location.set.size() >= 2) {
                addVertexVisitSet(location);
            }
        }
    }

    private void defineRevenueBonuses() {
        // create set of all hexes
        Set<MapHex> hexes = new HashSet<>();
        for (NetworkVertex vertex : getVertices()) {
            MapHex hex = vertex.getHex();
            if (hex != null)
                hexes.add(hex);
        }

        // check each vertex hex for a potential revenue bonus
        for (MapHex hex : hexes) {
            List<RevenueBonusTemplate> bonuses = new ArrayList<>();
            List<RevenueBonusTemplate> hexBonuses = hex.getRevenueBonuses();
            if (hexBonuses != null)
                bonuses.addAll(hexBonuses);
            List<RevenueBonusTemplate> tileBonuses = hex.getCurrentTile().getRevenueBonuses();
            if (tileBonuses != null)
                bonuses.addAll(tileBonuses);

            for (RevenueBonusTemplate bonus : bonuses) {
                RevenueBonus bonusConverted = bonus.toRevenueBonus(hex, root, graph);
                if (bonusConverted != null) {
                    addRevenueBonus(bonusConverted);
                }
            }
        }
        log.debug("RA: RevenueBonuses = {}", revenueBonuses);
    }

    /**
     * checks the set of trains for H-trains
     * 
     * @return true if H-trains are used
     */
    private boolean useHTrains() {
        for (NetworkTrain train : trains) {
            if (train.isHTrain()) {
                return true;
            }
        }
        return false;
    }

    public void initRevenueCalculator(boolean useMultiGraph) {

        this.useMultiGraph = useMultiGraph;

        // check for dynamic modifiers (including an own calculator
        if (revenueManager != null) {
            hasDynamicModifiers = revenueManager.initDynamicModifiers(this);
        }

        // define optimized graph

        if (useMultiGraph) {
            // generate phase 2 graph
            NetworkMultigraph multiGraph = networkAdapter.getMultigraph(company, protectedVertices);
            rcGraph = multiGraph.getGraph();
            // retrieve edge sets
            edgeTravelSets.putAll(multiGraph.getPhaseTwoEdgeSets(this));
        } else {
            // generate standard graph
            rcGraph = networkAdapter.getRevenueGraph(company, protectedVertices).getGraph();
        }

        // define the vertices and edges lists
        rcVertices = new ArrayList<>(rcGraph.vertexSet());
        // define ordering on vertexes by value
        rcVertices.sort(new NetworkVertex.ValueOrder());
        rcEdges = new ArrayList<>(rcGraph.edgeSet());
        rcEdges.sort(new NetworkEdge.CostOrder());
        log.debug("rcVertices={} rcEdges={}", rcVertices, rcEdges);
        for (NetworkVertex vertex : rcVertices) {
            log.debug("Stop={} value={}", vertex.getStop(), vertex.getValue());
        }

        // prepare train length
        prepareTrainLengths(rcVertices);

        // check dimensions
        int maxVisitVertices = maxVisitVertices();
        int maxBonusVertices = maxRevenueBonusVertices();
        int maxNeighbors = maxVertexNeighbors(rcVertices);
        int maxTravelEdges = maxTravelEdges();

        if (useMultiGraph) {
            if (useHTrains()) {
                rc = new RevenueCalculatorMultiHex(this, rcVertices.size(), rcEdges.size(),
                        maxNeighbors, maxVisitVertices, maxTravelEdges, trains.size(), maxBonusVertices);
            } else {
                rc = new RevenueCalculatorMulti(this, rcVertices.size(), rcEdges.size(),
                        maxNeighbors, maxVisitVertices, maxTravelEdges, trains.size(), maxBonusVertices);
            }
        } else {
            rc = new RevenueCalculatorSimple(this, rcVertices.size(), rcEdges.size(),
                    maxNeighbors, maxVisitVertices, trains.size(), maxBonusVertices);
        }

        populateRevenueCalculator();
        buildDynamicHexBonusCache();
    }

    private int maxVisitVertices() {
        int maxNbVertices = 0;
        for (VertexVisit vertexVisit : vertexVisitSets) {
            maxNbVertices = Math.max(maxNbVertices, vertexVisit.set.size());
        }
        log.debug("RA: Block of {}, maximum vertices in a set = {}", vertexVisitSets, maxNbVertices);
        return maxNbVertices;
    }

    private int maxVertexNeighbors(Collection<NetworkVertex> vertices) {
        int maxNeighbors = 0;
        for (NetworkVertex vertex : vertices) {
            maxNeighbors = Math.max(maxNeighbors, rcGraph.edgesOf(vertex).size());
        }
        log.debug("RA: Maximum neighbors in graph = {}", maxNeighbors);
        return maxNeighbors;
    }

    private int maxRevenueBonusVertices() {
        // get the number of non-simple bonuses
        int nbBonuses = RevenueBonus.getNumberNonSimpleBonuses(revenueBonuses);
        log.debug("Number of non simple bonuses = {}", nbBonuses);
        return nbBonuses;
    }

    private int maxTravelEdges() {
        int maxNbEdges = 0;
        for (EdgeTravel edgeTravel : edgeTravelSets.values()) {
            maxNbEdges = Math.max(maxNbEdges, edgeTravel.set.size());
        }
        for (Map.Entry<NetworkEdge, EdgeTravel> entry : edgeTravelSets.entrySet()) {
            StringBuilder edgeString = new StringBuilder("RA: EdgeSet for ").append(entry.getKey().toFullInfoString())
                    .append(" size = ").append(entry.getValue().set.size()).append("\n");
            for (NetworkEdge edgeInSet : entry.getValue().set) {
                edgeString.append(edgeInSet.toFullInfoString()).append("\n");
            }
            log.debug(edgeString.toString());
        }
        log.debug("RA: maximum edges in a set = {}", maxNbEdges);
        return maxNbEdges;
    }

    private void prepareTrainLengths(Collection<NetworkVertex> vertices) {

        // separate vertexes
        List<NetworkVertex> cities = new ArrayList<>();
        List<NetworkVertex> towns = new ArrayList<>();
        for (NetworkVertex vertex : vertices) {
            if (vertex.isMajor())
                cities.add(vertex);
            if (vertex.isMinor())
                towns.add(vertex);
        }

        int maxCities = cities.size();
        int maxTowns = towns.size();

        // check train lengths
        int maxCityLength = 0, maxTownLength = 0;
        for (NetworkTrain train : trains) {
            if (train.isHTrain()) {
                // This dirty trick fixes some of the problems with H-trains,
                // but may also have created new ones. (EV 02/2023)
                // maxCityLength = train.getMajors();
                // maxTownLength = 0;
                /* FIXME: H-trains still are not always routed correctly */
                maxCityLength = Math.min(maxCities + maxTowns, train.getMajors());
                maxTownLength = 0;
                // train.setMajors(maxCities+maxTowns);
                // train.setMinors(maxCities+maxTowns);
            } else {
                int trainTowns = train.getMinors();
                if (train.getMajors() > maxCities) {
                    trainTowns = trainTowns + train.getMajors() - maxCities;
                    train.setMajors(maxCities);
                }
                train.setMinors(Math.min(trainTowns, maxTowns));
                maxCityLength = Math.max(maxCityLength, train.getMajors());
                maxTownLength = Math.max(maxTownLength, train.getMinors());
            }

            log.debug("Train={} maxCities={} maxTowns={}", train, maxCityLength, maxTownLength);
        }

    }

// --- Method with Corrected Logging ---
    private void populateRevenueCalculator(){
        // Check if we are in M1 or M3 context (optional optimization)
        // Use this.company safely, it might be null if called outside normal flow
        boolean logDetailed = (this.company != null && ("M1".equals(this.company.getId()) || "M3".equals(this.company.getId())) );

        // [FIX] Use the existing 'log' field, not 'aiLog'
        log.debug("Populating RevenueCalculator for {} (Detail Log: {})",
                  (this.company != null ? this.company.getId() : "N/A"), logDetailed);

        for (int id=0; id < rcVertices.size(); id++){
            NetworkVertex v = rcVertices.get(id);

            // --- Detailed Logging for Relevant Vertices ---
            String hexId = (v.getHex() != null) ? v.getHex().getId() : "N/A";
            boolean isRelevantHex = "H2".equals(hexId) || "I1".equals(hexId) || "F14".equals(hexId) || "G15".equals(hexId);

            if (logDetailed && isRelevantHex) {
                 // [FIX] Use 'log.error' for visibility and log available boolean flags
                 // Removed the non-existent getVertexType()
           //      log.error(">>> SIM_POPULATE_VERTEX [{}]: ID={}, Hex={}, Value={}, isSink={}, isMajor={}, isMinor={}, isStation={}, isStop={}, isSide={}",
            //             (this.company != null ? this.company.getId() : "N/A"), // Add company ID safely
            //             id, hexId, v.getValue(),
            //             v.isSink(), v.isMajor(), v.isMinor(),
            //             v.isStation(), v.isStation(), v.isSide()); // Log available boolean flags
            }
            // --- End Logging ---

            // Add vertex to the calculator
            v.addToRevenueCalculator(rc, id);
            for (int trainId=0; trainId < trains.size(); trainId++) {
                NetworkTrain train = trains.get(trainId);
                // Log vertex value specifically for the train being used
                 if (logDetailed && isRelevantHex) {
                    // [FIX] Use 'log.error' for visibility
               //     log.error(">>> SIM_POPULATE_VERTEX_TRAINVAL [{}]: ID={}, Train={}, ValueForTrain={}",
                 //               (this.company != null ? this.company.getId() : "N/A"), // Add company ID safely
                //                id, train.getTrainName(), getVertexValue(v, train, phase));
                 }
                rc.setVertexValue(id, trainId, getVertexValue(v, train, phase));
            }

            // --- Vertex Neighbor Logic (Unchanged from original code) ---
             if (useMultiGraph) {
                Set<NetworkEdge> edges = rcGraph.edgesOf(v);
                int e=0; int[] edgesArray = new int[edges.size()];
                for (NetworkEdge edge:edges) {
                    edgesArray[e++] = rcEdges.indexOf(edge);
                }
                Arrays.sort(edgesArray, 0, e);
                int[] neighborsArray = new int[e];
                for (int j=0; j < e; j++) {
                    NetworkVertex toVertex = Graphs.getOppositeVertex(rcGraph, rcEdges.get(edgesArray[j]), v);
                    neighborsArray[j] = rcVertices.indexOf(toVertex);
                }
                rc.setVertexNeighbors(id, neighborsArray, edgesArray);
            } else {
                List<NetworkVertex> neighbors = Graphs.neighborListOf(rcGraph, v);
                int j=0;
                int[] neighborsArray = new int[neighbors.size()];
                for (NetworkVertex n:neighbors){
                    neighborsArray[j++] = rcVertices.indexOf(n);
                }
                Arrays.sort(neighborsArray, 0, j);
                int[] edgesArray = new int[j];
                for (int e=0; e < j; e++) {
                    NetworkVertex toVertex = rcVertices.get(neighborsArray[e]);
                    edgesArray[e] = rcEdges.indexOf(rcGraph.getEdge(v, toVertex));
                }
                rc.setVertexNeighbors(id, neighborsArray, edgesArray);
            }
            // --- End Vertex Neighbor Logic ---
        }

        // --- Rest of the method (unchanged from original code) ---
        // set startVertexes
        int startVertexId =0;
        int[] sv = new int[startVertices.size()];
        for (NetworkVertex startVertex:startVertices) {
            sv[startVertexId++] = rcVertices.indexOf(startVertex);
        }
        Arrays.sort(sv); // sort by value order
        rc.setStartVertexes(sv);

        // set edges
        for (int id=0; id < rcEdges.size(); id++) {
            NetworkEdge e = rcEdges.get(id);
            boolean greedy = e.isGreedy();
            int distance = e.getDistance();
            rc.setEdge(id, greedy, distance);
        }

        // set trains, check for H-trains
        for (int id=0; id < trains.size(); id++) {
            NetworkTrain train = trains.get(id);
            train.addToRevenueCalculator(rc, id);
        }

// set vertex sets
        for (VertexVisit visit:vertexVisitSets) {
            // Use an ArrayList first, since we don't know the final size
            ArrayList<Integer> idList = new ArrayList<>();
            for (NetworkVertex n:visit.set){
                int vertexId = rcVertices.indexOf(n); // Get the ID
                if (vertexId != -1) {
                    // Only add valid IDs
                    idList.add(vertexId);
                } else {
                    // Log a warning: this vertex was in a visit set but not in the revenue graph
                    // log.warn("Vertex {} (part of a visit set) was not found in the rcGraph. It was likely optimized out. Skipping.", n.getStopName());
                }
            }
            
            // Convert the ArrayList to the int[] array the calculator expects
            int[] setArray = new int[idList.size()];
            for (int j=0; j<idList.size(); j++) {
                setArray[j] = idList.get(j);
            }

            // Only call setVisitSet if there are still at least 2 vertices
            // (A visit set of 1 is meaningless)
            if (setArray.length >= 2) {
                rc.setVisitSet(setArray);
            }
        }
        log.debug("RA: rcVertices:{}", rcVertices);
        log.debug("RA: rcEdges:{}", rcEdges);
        for (NetworkVertex vertex : rcVertices) {
            log.debug ("Stop={} value={}", vertex.getStop(), vertex.getValue());
        }

        // set revenue bonuses
        int id = 0;
        for (RevenueBonus bonus:revenueBonuses) {
            if (bonus.addToRevenueCalculator(rc, id, rcVertices, trains, phase)) id ++;
        }

        log.debug("RA: edgeTravelSets:{}", edgeTravelSets);

        // set edge sets
        if (useMultiGraph) {
            for ( Map.Entry<NetworkEdge, EdgeTravel> entry:edgeTravelSets.entrySet()) {
                int j=0;
                int[] setArray = new int[entry.getValue().set.size()];
                for (NetworkEdge n:entry.getValue().set){
                    setArray[j++] = rcEdges.indexOf(n);
                }
                // Check if rc is actually a RevenueCalculatorMulti before casting
                if (rc instanceof RevenueCalculatorMulti) {
                    ((RevenueCalculatorMulti)rc).setTravelSet(rcEdges.indexOf(entry.getKey()), setArray);
                } else {
                     log.warn("Attempted to set TravelSet, but RevenueCalculator is not RevenueCalculatorMulti. Type: {}", rc.getClass().getName());
                }
            }
        }

        // activate dynamic modifiers
        rc.setDynamicModifiers(hasDynamicModifiers);
         // --- End Rest of Method ---
    }
    
    public int getVertexValue(NetworkVertex vertex, NetworkTrain train, Phase phase) {

        // base value
        int value = vertex.getValueByTrain(train);

        // add potential revenueBonuses
        for (RevenueBonus bonus : revenueBonuses) {
            if (bonus.checkSimpleBonus(vertex, train.getRailsTrain(), phase)) {
                value += bonus.getValue();
            }
        }

        return value;
    }

    public String getVertexValueAsString(NetworkVertex vertex, NetworkTrain train, Phase phase) {
        StringBuilder s = new StringBuilder();

        // base value
        s.append(vertex.getValueByTrain(train));

        // add potential revenueBonuses
        for (RevenueBonus bonus : revenueBonuses) {
            if (bonus.checkSimpleBonus(vertex, train.getRailsTrain(), phase)) {
                s.append("+").append(bonus.getValue());
            }
        }
        return s.toString();
    }

    private List<RevenueTrainRun> convertRcRun(int[][] rcRun) {

        // Just for logging
        int i = 0;
        for (int[] j : rcRun) {
            log.debug("rcRun {}={}", i++, j);
        }
        log.debug("rcEdges={}", rcEdges);

        List<RevenueTrainRun> convertRun = new ArrayList<>();

        for (int j = 0; j < rcRun.length; j++) {
            RevenueTrainRun trainRun = new RevenueTrainRun(this, trains.get(j));
            convertRun.add(trainRun);

            List<Integer> uniques = new ArrayList<>();

            if (rcEdges.size() == 0)
                continue;
            for (int v = 0; v < rcRun[j].length; v++) {
                int id = rcRun[j][v];
                if (id == -1)
                    break;

                // Avoid duplicates
                // This only fixes the per-train values in the message panel,
                // but NOT the calculated total income used by the main code.
                // See GitHub issue #483 and RevenueTrainRun.convertEdgesToVertices().
                // (I know this is a quick, dirty fix, but that's all I can do for now (EV))
                if (uniques.contains(id))
                    continue;
                uniques.add(id);

                if (useMultiGraph) {
                    log.debug("Adding edge {}={}", id, rcEdges.get(id));
                    trainRun.addEdge(rcEdges.get(id));
                } else {
                    trainRun.addVertex(rcVertices.get(id));
                }
            }
            if (useMultiGraph) {
                trainRun.convertEdgesToVertices();
            } else {
                trainRun.convertVerticesToEdges();
            }
        }
        return convertRun;
    }

    public int calculateRevenue() {
        // allows (one) dynamic modifiers to have their own revenue calculation method
        // TODO: Still to be added - beware: it is used differently in 1837
        // (see RunToCoalMineModifier).
        // if (hasDynamicCalculator) {
        // return revenueManager.revenueFromDynamicCalculator(this);
        // For 1837 we need to do both!
        // specialRevenue = revenueManager.revenueFromDynamicCalculator(this); //??
        specialRevenue = revenueManager.getSpecialRevenue();
        // } else { // otherwise standard calculation
        return calculateRevenue(0, trains.size() - 1);
        // }
    }

    // Another way to get the special revenue
    public void setSpecialRevenue(int value) {
        specialRevenue = value;
    }

    public int calculateRevenue(int startTrain, int finalTrain) {

     //   log.error(">>> RevenueAdapter.calculateRevenue: USING trains: {}", this.trains); // Use ERROR to ensure
                                                                                         // visibility

        if (startTrain < 0 || finalTrain >= trains.size() || startTrain > finalTrain) {
            return 0;
        }
        // the optimal run might change
        optimalRun = null;
        rc.initRuns(startTrain, finalTrain);
        rc.executePredictions(startTrain, finalTrain);
        int value = rc.calculateRevenue(startTrain, finalTrain);

        return value;
    }

    public int getSpecialRevenue() {
        return specialRevenue;
    }

    public List<RevenueTrainRun> getOptimalRun() {
        if (optimalRun == null) {
            optimalRun = convertRcRun(rc.getOptimalRun());
            if (hasDynamicModifiers) {
                revenueManager.adjustOptimalRun(optimalRun);
            }
        }
        return optimalRun;
    }

    public List<RevenueTrainRun> getCurrentRun() {
        return convertRcRun(rc.getCurrentRun());
    }

    /**
     * is called by rc for dynamic evaluations
     */
    int dynamicEvaluation() {
        int value = 0;
        if (hasDynamicModifiers) {
            value = revenueManager.evaluationValue(this.getCurrentRun(), false);
            specialRevenue = revenueManager.getSpecialRevenue();
        }
        return value;
    }

    /**
     * is called by rc for dynamic predictions
     */
    int dynamicPrediction() {
        int value = 0;
        if (hasDynamicModifiers) {
            value = revenueManager.predictionValue(this.getCurrentRun());
        }
        return value;
    }

    public void addRevenueListener(RevenueListener listener) {
        this.revenueListener = listener;
    }

    void notifyRevenueListener(final int revenue, final int specialRevenue, final boolean finalResult) {
        if (revenueListener == null)
            return;

        EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        // listener could have deregistered himself in the meantime
                        if (revenueListener != null) {
                            revenueListener.revenueUpdate(revenue, specialRevenue, finalResult);
                        }
                    }
                });
    }

    public void run() {
        calculateRevenue(0, trains.size() - 1);
    }

    public void removeRevenueListener() {
        // only removes revenueListener
        revenueListener = null;
    }

    public String getOptimalRunPrettyPrint(boolean includeDetails) {
        List<RevenueTrainRun> listRuns = getOptimalRun();
        if (listRuns == null)
            return LocalText.getText("RevenueNoRun");

        StringBuilder runPrettyPrint = new StringBuilder();
        for (RevenueTrainRun run : listRuns) {
            runPrettyPrint.append(run.prettyPrint(includeDetails));
            if (!includeDetails && run != listRuns.get(listRuns.size() - 1)) {
                runPrettyPrint.append("; ");
            }
        }
        if (includeDetails) {
            if (revenueManager != null) {
                runPrettyPrint.append(revenueManager.prettyPrint(this));
            }
        } else {
            int dynamicBonuses = 0;
            if (hasDynamicModifiers) {
                dynamicBonuses = revenueManager.evaluationValue(this.getOptimalRun(), true);
                specialRevenue = revenueManager.getSpecialRevenue();
            }
            if (dynamicBonuses != 0) {
                runPrettyPrint.append("; ").append(LocalText.getText("RevenueBonus", dynamicBonuses));
            }
        }
        return runPrettyPrint.toString();
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RevenueCalculator:\n").append(rc).append("\n");
        buffer.append("rcVertices:\n").append(rcVertices).append("\n");
        buffer.append("rcEdges:\n").append(rcEdges).append("\n");
        buffer.append("startVertices:").append(startVertices);
        return buffer.toString();
    }

    public RailsRoot getRoot() {
        return this.root;
    }

    /*
     * Provides access to the internal NetworkAdapter instance.
     * Used by AIEvaluatorService to force cache clearing during simulation.
     */
    public NetworkAdapter getNetworkAdapterInternal() {
        return this.networkAdapter;
    }

    public void drawOptimalRunAsPath(HexMap map) {
        List<RevenueTrainRun> listRuns = getOptimalRun();

        List<GeneralPath> pathList = new ArrayList<>();
        Set<MapHex> routeHexes = new HashSet<>(); // --- FIX: Initialize logical hex collection

        if (listRuns != null) {
            for (RevenueTrainRun run : listRuns) {
                pathList.add(run.getAsPath(map));
                
                // Extract the logical hexes the train actually visits
                if (run.getRunVertices() != null) {
                    for (NetworkVertex v : run.getRunVertices()) {
                        if (v.getHex() != null) {
                            routeHexes.add(v.getHex());
                        }
                    }
                }
            }
        }
        
        // --- FIX: Pass BOTH the graphical lines and logical hexes to the map ---
        map.setTrainPaths(pathList, routeHexes);
    }

    private Map<MapHex, Integer> dynamicHexBonusCache = new HashMap<>();

    public Map<MapHex, Integer> getDynamicHexBonusCache() {
        return dynamicHexBonusCache;
    }

    private void buildDynamicHexBonusCache() {
        dynamicHexBonusCache.clear();
        if (revenueManager == null || !hasDynamicModifiers || trains == null || trains.isEmpty()) return;

        NetworkTrain dummyTrain = trains.get(0);
        
        for (NetworkVertex v : rcVertices) {
            MapHex hex = v.getHex();
            if (hex == null) continue;
            
            RevenueTrainRun fakeRun = new RevenueTrainRun(this, dummyTrain);

            
            List<RevenueTrainRun> fakeRuns = new ArrayList<>();
            fakeRuns.add(fakeRun);
            
            int bonus = revenueManager.predictionValue(fakeRuns);
            if (bonus > 0) {
                dynamicHexBonusCache.put(hex, Math.max(dynamicHexBonusCache.getOrDefault(hex, 0), bonus));
            }
        }
    }
}
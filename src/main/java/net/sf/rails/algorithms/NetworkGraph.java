package net.sf.rails.algorithms;

import com.google.common.collect.*;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import net.sf.rails.game.*;
import net.sf.rails.game.state.Owner;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * NetworkGraph mirrors the structure of a 18xx track
 * <p>
 * TODO: Rewrite this by separating the creation code from the data code
 * TODO: Rails 2.0 add a NetworkManager
 */
public class NetworkGraph {

    private static final Logger log = LoggerFactory.getLogger(NetworkGraph.class);

    private final SimpleGraph<NetworkVertex, NetworkEdge> graph;

    private final Map<String, NetworkVertex> vertices;

    private NetworkIterator iterator;

    private NetworkGraph() {
        graph = new SimpleGraph<>(NetworkEdge.class);
        vertices = Maps.newHashMap();
    }

    private NetworkGraph(NetworkGraph inGraph) {
        graph = new SimpleGraph<>(NetworkEdge.class);
        Graphs.addGraph(graph, inGraph.graph);
        vertices = Maps.newHashMap(inGraph.vertices);
    }

    public static NetworkGraph createMapGraph(RailsRoot root) {
        NetworkGraph graph = new NetworkGraph();
        graph.generateMapGraph(root);
        return graph;
    }

    /**
     *
     * @param mapGraph The current map graph
     * @param company The company to create a route graph for
     * @param addHQ ??
     * @param running true for train runs, false for tile or token lay allowances
     * @return The updated graph
     */
    public static NetworkGraph createRouteGraph(NetworkGraph mapGraph, PublicCompany company, boolean addHQ, boolean running) {
        NetworkGraph newGraph = new NetworkGraph();
        newGraph.initRouteGraph(mapGraph, company, addHQ, running);
        newGraph.rebuildVertices();
        return newGraph;
    }

    public static NetworkGraph createOptimizedGraph(NetworkGraph inGraph,
                                                    Collection<NetworkVertex> protectedVertices) {
        NetworkGraph newGraph = new NetworkGraph(inGraph);
        newGraph.optimizeGraph(protectedVertices);
        newGraph.rebuildVertices();
        return newGraph;
    }

    public NetworkGraph cloneGraph() {
        return new NetworkGraph(this);
    }

    public SimpleGraph<NetworkVertex, NetworkEdge> getGraph() {
        return graph;
    }

    public void setIteratorStart(MapHex hex, Station station) {
        iterator = new NetworkIterator(graph, getVertex(hex, station));
    }

    public Iterator<NetworkVertex> iterator() {
        return iterator;
    }

    public NetworkVertex getVertexByIdentifier(String identVertex) {
        return vertices.get(identVertex);
    }

    public NetworkVertex getVertex(BaseToken token) {
        Owner owner = token.getOwner();
        // TODO: Check if this still works
        if (!(owner instanceof Stop)) return null;
        Stop city = (Stop) owner;
        MapHex hex = city.getParent();
        Station station = city.getRelatedStation();
        return getVertex(hex, station);
    }

    public NetworkVertex getVertex(MapHex hex, TrackPoint point) {
        return vertices.get(hex.getId() + "." + point.getTrackPointNumber());
    }

    public NetworkVertex getVertex(MapHex hex, int trackPointNr) {
        return vertices.get(hex.getId() + "." + trackPointNr);
    }

    public NetworkVertex getVertexRotated(MapHex hex, TrackPoint point) {
        if (point.getTrackPointType() == TrackPoint.Type.SIDE)
            point = point.rotate(hex.getCurrentTileRotation());
        return vertices.get(hex.getId() + "." + point.getTrackPointNumber());
    }

    public ImmutableMap<MapHex, HexSidesSet> getReachableSides() {
        // first create builders for all HexSides
        Map<MapHex, HexSidesSet.Builder> hexSides = Maps.newHashMap();
        for (NetworkVertex vertex : graph.vertexSet()) {
            if (vertex.isSide() && iterator.getSeenData().get(vertex)
                    != NetworkIterator.greedyState.GREEDY ) {
                MapHex hex = vertex.getHex();
                if (!hexSides.containsKey(hex)) {
                    hexSides.put(hex, HexSidesSet.builder());
                }
                hexSides.get(hex).set(vertex.getSide());
            }
        }
        // second build the map of mapHex to HexSides
        ImmutableMap.Builder<MapHex, HexSidesSet> hexBuilder = ImmutableMap.builder();
        hexSides.keySet().forEach(hex -> hexBuilder.put(hex, hexSides.get(hex).build()));
        return hexBuilder.build();
    }

    /**
     * @return a map of all hexes and stations that can be run through
     */
    public Multimap<MapHex, Station> getPassableStations() {

        ImmutableMultimap.Builder<MapHex, Station> hexStations =
                ImmutableMultimap.builder();

        for (NetworkVertex vertex : graph.vertexSet()) {
            if (vertex.isStation() && !vertex.isSink()) {
                hexStations.put(vertex.getHex(), vertex.getStation());
            }
        }

        return hexStations.build();
    }


    private void rebuildVertices() {
        // rebuild mapVertices
        vertices.clear();
        for (NetworkVertex v : graph.vertexSet()) {
            vertices.put(v.getIdentifier(), v);
        }
    }

    private void generateMapGraph(RailsRoot root) {
        MapManager mapManager = root.getMapManager();
        RevenueManager revenueManager = root.getRevenueManager();
        for (MapHex hex : mapManager.getHexes()) {
            // Don't add any inaccessible hexes to the graph
            if (!hex.isOpen()) continue;

            // get Tile
            Tile tile = hex.getCurrentTile();

            // then get stations
            Collection<Station> stations = tile.getStations();
            // and add those to the mapGraph
            for (Station station : stations) {
                NetworkVertex stationVertex = new NetworkVertex(hex, station);
                graph.addVertex(stationVertex);
                vertices.put(stationVertex.getIdentifier(), stationVertex);
                log.debug("Added {}", stationVertex);
            }

            // get tracks per side to add that vertex
            for (HexSide side : HexSide.all())
                if (tile.hasTracks(side)) {
                    HexSide rotated = side.rotate(hex.getCurrentTileRotation());
                    NetworkVertex sideVertex = new NetworkVertex(hex, rotated);
                    graph.addVertex(sideVertex);
                    vertices.put(sideVertex.getIdentifier(), sideVertex);
                    log.debug("Added {}", sideVertex);
                }
        }

        // loop over all hex and add tracks
        for (MapHex hex : mapManager.getHexes()) {
            if (!hex.isOpen()) continue;

            // get Tile
            Tile tile = hex.getCurrentTile();
            // get Tracks
            Set<Track> tracks = tile.getTracks();

            for (Track track : tracks) {
                NetworkVertex startVertex = getVertexRotated(hex, track.getStart());
                NetworkVertex endVertex = getVertexRotated(hex, track.getEnd());
                log.debug("Track: {}", track);
                NetworkEdge edge = new NetworkEdge(startVertex, endVertex, false);
                if (startVertex == endVertex) {
                    log.error("Track {} on hex {}has identical start/end", track, hex);
                } else {
                    graph.addEdge(startVertex, endVertex, edge);
                    log.debug("Added non-greedy edge {}", edge.getConnection());
                }
            }

            // TODO: Rewrite this by employing the features of Trackpoint
            // and connect to neighbouring hexes (for sides 0-2)
            for (HexSide side : HexSide.head()) {
                MapHex neighborHex = mapManager.getNeighbour(hex, side);
                if (neighborHex == null) {
                    log.debug("No connection for Hex {} at {}, No Neighbor", hex.getId(), hex.getOrientationName(side));
                    continue;
                }
                NetworkVertex vertex = getVertex(hex, side);
                HexSide rotated = side.opposite();
                NetworkVertex otherVertex = getVertex(neighborHex, rotated);
                if (vertex == null && otherVertex == null) {
                    log.debug("Hex {} has no track at {}", hex.getId(), hex.getOrientationName(side));
                    log.debug("And Hex {} has no track at {}", neighborHex.getId(), neighborHex.getOrientationName(rotated));
                    continue;
                } else if (vertex == null && otherVertex != null) {
                    log.debug("Deadend connection for Hex {} at {}, NeighborHex {} has no track at side {}", neighborHex.getId(), neighborHex.getOrientationName(rotated), hex.getId(), hex.getOrientationName(side));
                    vertex = new NetworkVertex(hex, side);
                    graph.addVertex(vertex);
                    vertices.put(vertex.getIdentifier(), vertex);
                    log.debug("Added deadend vertex {}", vertex);
                } else if (otherVertex == null) {
                    log.debug("Deadend connection for Hex {} at {}, NeighborHex {} has no track at side {}", hex.getId(), hex.getOrientationName(side), neighborHex.getId(), neighborHex.getOrientationName(rotated));
                    otherVertex = new NetworkVertex(neighborHex, rotated);
                    graph.addVertex(otherVertex);
                    vertices.put(otherVertex.getIdentifier(), otherVertex);
                    log.debug("Added deadend vertex {}", otherVertex);
                }
                NetworkEdge edge = new NetworkEdge(vertex, otherVertex, true);
                graph.addEdge(vertex, otherVertex,
                        edge);
                log.debug("Added greedy edge {}", edge.getConnection());
            }
        }

        // add graph modifiers
        if (revenueManager != null) {
            revenueManager.activateMapGraphModifiers(this);
        }

    }

    public void optimizeGraph() {
        optimizeGraph(new ArrayList<>(0));
    }

    private void optimizeGraph(Collection<NetworkVertex> protectedVertices) {

        // remove vertices until convergence
        boolean notDone = true;
        while (notDone) {
            increaseGreedness();
            notDone = removeVertexes(protectedVertices);
            // removedVertices can change Greedness, but not vice-versa
        }
    }

    // Increase Greedness implies that an edge that
    // connects stations and/or sides with only one track in/out
    // can be set to greedy (as one has to follow the exit anyway)
    private void increaseGreedness() {
        for (NetworkEdge edge : graph.edgeSet()) {
            if (edge.isGreedy()) continue;
            NetworkVertex source = edge.getSource();
            NetworkVertex target = edge.getTarget();
            if ((source.isSide() && graph.edgesOf(source).size() == 2 || source.isStation()) &&
                    (target.isSide() && graph.edgesOf(target).size() == 2 || target.isStation())) {
                edge.setGreedy(true);
                log.debug("Increased greedness for {}", edge.getConnection());
            }
        }
    }

    /**
     * remove deadend and vertex with only two edges
     */
    private boolean removeVertexes(Collection<NetworkVertex> protectedVertices) {

        boolean removed = false;

        for (NetworkVertex vertex : ImmutableSet.copyOf(graph.vertexSet())) {
            Set<NetworkEdge> vertexEdges = graph.edgesOf(vertex);

            // always keep protected vertices
            if (protectedVertices.contains(vertex)) {
                continue;
            }

            // remove hermit
            if (vertexEdges.size() == 0) {
                log.debug("Remove hermit (no connection) = {}", vertex);
                graph.removeVertex(vertex);
                removed = true;
            }

            // the following only for side vertexes
            if (!vertex.isSide()) continue;

            if (vertexEdges.size() == 1) {
                log.debug("Remove deadend side (single connection) = {}", vertex);
                graph.removeVertex(vertex);
                removed = true;
            } else if (vertexEdges.size() == 2) { // not necessary vertices
                NetworkEdge[] edges = vertexEdges.toArray(new NetworkEdge[2]);
                if (edges[0].isGreedy() == edges[1].isGreedy()) {
                    if (!edges[0].isGreedy()) {
                        log.debug("Remove deadend side (no greedy connection) = {}", vertex);
                        // two non greedy edges indicate a deadend
                        graph.removeVertex(vertex);
                        removed = true;
                    } else {
                        // greedy case:
                        // merge greedy edges if the vertexes are not already connected
                        if (NetworkEdge.mergeEdgesInGraph(graph, edges[0], edges[1])) {
                            removed = true;
                        }
                    }
                }
            }
        }
        return removed;
    }

    /**
     *
     * @param mapGraph The map graph
     * @param company The company to create a route graph for
     * @param addHQ ??
     * @param running true for train runs, false for tile or token lay allowances
     */
    private void initRouteGraph(NetworkGraph mapGraph, PublicCompany company, boolean addHQ, boolean running) {

        // add graph modifiers
        RevenueManager revenueManager = company.getRoot().getRevenueManager();
        if (revenueManager != null) {
            revenueManager.activateRouteGraphModifiers(mapGraph, company);
        }

        // set sinks on mapgraph
        NetworkVertex.initAllRailsVertices(mapGraph, company, null, running);

        // add Company HQ
        NetworkVertex hqVertex = new NetworkVertex(company);
        graph.addVertex(hqVertex);

        // create vertex set for subgraph
        List<NetworkVertex> tokenVertexes = mapGraph.getCompanyBaseTokenVertexes(company);
        Set<NetworkVertex> vertexes = new HashSet<>();

        for (NetworkVertex vertex : tokenVertexes) {
            // allow to leave tokenVertices even if those are sinks
            // Examples are tokens in offBoard hexes
            boolean storeSink = vertex.isSink();
            vertex.setSink(false);
            vertexes.add(vertex);
            // add connection to graph
            graph.addVertex(vertex);
            graph.addEdge(vertex, hqVertex, new NetworkEdge(vertex, hqVertex, false));
            iterator = new NetworkIterator(mapGraph.getGraph(), vertex, company);
            while (iterator.hasNext()) {
                vertexes.add(iterator.next());
            }
            // restore sink property
            vertex.setSink(storeSink);
        }

        AsSubgraph<NetworkVertex, NetworkEdge> subGraph = new AsSubgraph<>(mapGraph.getGraph(), vertexes);
        // now add all vertexes and edges to the graph
        Graphs.addGraph(graph, subGraph);

        // if addHQ is not set remove HQ vertex
        if (!addHQ) graph.removeVertex(hqVertex);
    }

    public List<NetworkVertex> getCompanyBaseTokenVertexes(PublicCompany company) {
        List<NetworkVertex> vertexes = new ArrayList<>();
        for (BaseToken token : company.getLaidBaseTokens()) {
            NetworkVertex vertex = getVertex(token);
            if (vertex == null) continue;
            vertexes.add(vertex);
        }
        return vertexes;
    }

    public JFrame visualize(String title) {
        // show network mapGraph
        if (graph.vertexSet().size() > 0) {
            JGraphXAdapter<NetworkVertex, NetworkEdge> jGraphXAdapter = new JGraphXAdapter<>(graph);

            jGraphXAdapter.getModel().beginUpdate();

            mxIGraphLayout layout = new mxFastOrganicLayout(jGraphXAdapter);
            layout.execute(jGraphXAdapter.getDefaultParent());

            jGraphXAdapter.getModel().endUpdate();

            mxGraphComponent graphComponent = new mxGraphComponent(jGraphXAdapter);

            JFrame frame = new JFrame();
            frame.setTitle(String.format("%s(V=%d,E=%d)", title, graph.vertexSet().size(), graph.edgeSet().size()));
            frame.setSize(new Dimension(800, 600));
            frame.getContentPane().add(new JScrollPane(graphComponent));
            frame.pack();
            frame.setVisible(true);
            return frame;
        }
        return null;
    }

    /**
     * @return a map of all hexes and stations that cannot be run through
     */
    public Multimap<MapHex, Station> getNonPassableStations() {

        ImmutableMultimap.Builder<MapHex, Station> hexStations =
                ImmutableMultimap.builder();

        for(NetworkVertex vertex:graph.vertexSet()) {
            if (vertex.isStation() && vertex.isSink()) {
                hexStations.put(vertex.getHex(), vertex.getStation());
            }
        }
        return hexStations.build();
    }


}

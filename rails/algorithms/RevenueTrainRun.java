package rails.algorithms;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rails.algorithms.NetworkVertex.StationType;
import rails.algorithms.NetworkVertex.VertexType;
import rails.ui.swing.hexmap.HexMap;
import rails.util.LocalText;

/**
 * Links the results from the revenue calculator to the rails program
 * Each object defines the run of one train
 * @author freystef
 *
 */
public class RevenueTrainRun {

    private static final int PRETTY_PRINT_LENGTH = 100;
    private static final int PRETTY_PRINT_INDENT = 10;
    
    protected static Logger log =
        Logger.getLogger(RevenueTrainRun.class.getPackage().getName());

    // definitions
    private RevenueAdapter revenueAdapter;
    private NetworkTrain train;
    
    // converted data
    private List<NetworkVertex> vertices;
    
    RevenueTrainRun(RevenueAdapter revenueAdapter, NetworkTrain train) {
        this.revenueAdapter = revenueAdapter;
        this.train = train;
        vertices = new ArrayList<NetworkVertex>();
    }
    
    public List<NetworkVertex> getRunVertices() {
        return vertices;
    }
    
    public Set<NetworkVertex> getUniqueVertices() {
        return new HashSet<NetworkVertex>(vertices);
    }
    
    public NetworkTrain getTrain() {
        return train;
    }
    
    int getRunValue() {
        int value = 0;
        NetworkVertex startVertex = null; 
        for (NetworkVertex vertex:vertices) {
            if (startVertex == vertex) continue;
            if (startVertex == null) startVertex = vertex;
            value += revenueAdapter.getVertexValue(vertex, train, revenueAdapter.getPhase());
        }
        // check revenueBonuses (complex)
        for (RevenueBonus bonus:revenueAdapter.getRevenueBonuses()) {
            if (bonus.checkComplexBonus(vertices, train.getRailsTrain(), revenueAdapter.getPhase())) {
                value += bonus.getValue();
            }
        }
        return value;
    }
    
    boolean hasButtomRun() {
        boolean buttomRun = false;
        NetworkVertex startVertex = null; 
        for (NetworkVertex vertex:vertices) {
            if (startVertex == vertex) buttomRun = true;
            if (startVertex == null) startVertex = vertex;
        }
        return buttomRun;
    }

    void addVertex(NetworkVertex vertex)  {
        vertices.add(vertex);
    }
    
    private String prettyPrintHexName(NetworkVertex vertex) {
        if (vertex.isVirtual()) {
            return vertex.getIdentifier();
        } else {
            return vertex.getHex().getName();
        }
    }
    
    private int prettyPrintNewLine(StringBuffer runPrettyPrint, int multiple, int initLength) {
        int length = runPrettyPrint.length() - initLength;
        if (length / PRETTY_PRINT_LENGTH != multiple) {
            multiple = length / PRETTY_PRINT_LENGTH;
            runPrettyPrint.append("\n");
            for (int i=0; i < PRETTY_PRINT_INDENT; i++)
                runPrettyPrint.append(" ") ;
        }
        return multiple;
    }
    
    String prettyPrint() {
        StringBuffer runPrettyPrint = new StringBuffer();
        runPrettyPrint.append(LocalText.getText("N_Train", train.toString()));
        runPrettyPrint.append(": " + getRunValue());
        
        Set<NetworkVertex> uniqueVertices = getUniqueVertices();
        int majors = NetworkVertex.numberOfVertexType(uniqueVertices, VertexType.STATION, StationType.MAJOR);
        int minors = NetworkVertex.numberOfVertexType(uniqueVertices, VertexType.STATION, StationType.MINOR);
        if (train.ignoresMinors() || minors == 0) {
            runPrettyPrint.append(LocalText.getText("RevenueStationsIgnoreMinors", majors));
        } else {
            runPrettyPrint.append(LocalText.getText("RevenueStations", majors, minors));
        }

        int initLength = runPrettyPrint.length();
        int multiple = prettyPrintNewLine(runPrettyPrint, -1, initLength);
        String currentHexName = null;
        NetworkVertex startVertex = null;
        for (NetworkVertex vertex:vertices) {
            if (startVertex == null) {
                currentHexName = prettyPrintHexName(vertex);
                startVertex = vertex;
                runPrettyPrint.append(prettyPrintHexName(vertex) + "(");
            } else if (startVertex == vertex) {
                currentHexName = prettyPrintHexName(vertex);
                runPrettyPrint.append(") / ");
                multiple = prettyPrintNewLine(runPrettyPrint, multiple, initLength);
                runPrettyPrint.append(prettyPrintHexName(vertex) + "(0");
                continue;
            } else if (!currentHexName.equals(prettyPrintHexName(vertex))) {
                currentHexName = prettyPrintHexName(vertex);
                runPrettyPrint.append("), ");
                multiple = prettyPrintNewLine(runPrettyPrint, multiple, initLength);
                runPrettyPrint.append(prettyPrintHexName(vertex) + "(");
            } else {
                runPrettyPrint.append(",");
            }
            if (vertex.isStation()) {
                runPrettyPrint.append(revenueAdapter.getVertexValueAsString(vertex, train, revenueAdapter.getPhase()));
            }  else {
                runPrettyPrint.append(vertex.getHex().getOrientationName(vertex.getSide()));
            }
        }
        
        if (currentHexName != null) {
            runPrettyPrint.append(")");
        }
        
        // check revenueBonuses (complex)
        List<RevenueBonus> activeBonuses = new ArrayList<RevenueBonus>();
        for (RevenueBonus bonus:revenueAdapter.getRevenueBonuses()) {
            if (bonus.checkComplexBonus(vertices, train.getRailsTrain(), revenueAdapter.getPhase())) {
                activeBonuses.add(bonus);
            }
        }
        Map<String,Integer> printBonuses = RevenueBonus.combineBonuses(activeBonuses);
        for (String bonusName:printBonuses.keySet()) {
            runPrettyPrint.append(" + ");
            runPrettyPrint.append(bonusName + "(" + printBonuses.get(bonusName) + ")");
            multiple = prettyPrintNewLine(runPrettyPrint, multiple, initLength);
        }
        runPrettyPrint.append("\n");

        return runPrettyPrint.toString(); 
    }

    GeneralPath getAsPath(HexMap map) {

        GeneralPath path = new GeneralPath();
        NetworkVertex startVertex = null;
        NetworkVertex previousVertex = null;
        for (NetworkVertex vertex:vertices) {
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
            NetworkEdge edge = revenueAdapter.getRCGraph().getEdge(previousVertex, vertex);
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
        return path;
    }
}
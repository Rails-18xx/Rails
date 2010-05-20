package rails.algorithms;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.ui.swing.hexmap.HexMap;

/**
 * Links the results from the revenue calculator to the rails program
 * Each object defines the run of one train
 * @author freystef
 *
 */
public class RevenueTrainRun {

    private static final int PRETTY_PRINT_LENGTH = 100;
    
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
    
    void addVertex(NetworkVertex vertex)  {
        vertices.add(vertex);
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

    private String prettyPrintHexName(NetworkVertex vertex) {
        if (vertex.isVirtual()) {
            return vertex.getIdentifier();
        } else {
            return vertex.getHex().getName();
        }
    }
    
    private int prettyPrintNewLine(StringBuffer runPrettyPrint, int multiple, int initLength) {
        if (runPrettyPrint.length() / PRETTY_PRINT_LENGTH != multiple) {
            multiple = runPrettyPrint.length() / PRETTY_PRINT_LENGTH;
            runPrettyPrint.append("\n");
            for (int i=0; i < initLength; i++)
                runPrettyPrint.append(" ") ;
        }
        return multiple;
    }
    
    String prettyPrint() {
        StringBuffer runPrettyPrint = new StringBuffer();
        runPrettyPrint.append("Train " + train + ": " + getRunValue() + " -> ");
        int initLength = runPrettyPrint.length();
        int multiple = runPrettyPrint.length() / PRETTY_PRINT_LENGTH;
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
        for (RevenueBonus bonus:revenueAdapter.getRevenueBonuses()) {
            if (bonus.checkComplexBonus(vertices, train.getRailsTrain(), revenueAdapter.getPhase())) {
                runPrettyPrint.append(" + ");
                runPrettyPrint.append(bonus.getName() + "(" + bonus.getValue() + ")");
                multiple = prettyPrintNewLine(runPrettyPrint, multiple, initLength);
            }
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
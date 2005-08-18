/* $Header: /Users/blentz/rails_rcs/cvs/18xx/util/Attic/ConvertTilesXML.java,v 1.5 2005/08/18 22:07:36 evos Exp $
 * 
 * Created on 14-Aug-2005
 * Change Log:
 */
package util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamResult;

import game.ConfigurationException;

import org.w3c.dom.*;

/**
 * Convert an XML tile dictionary, as created by Marco Rocci's Tile Designer, to
 * an XML file for use in Rails 18xx.
 * <p>
 * The default names are:
 * 
 * @author Erik Vos
 */
public class ConvertTilesXML {

    private static String inputFilePath = "tiles/TileDictionary.xml";

    private static String outputFilePath = "tiles/Tiles.xml";
    
    private static Map colourMap, stationMap, gaugeMap, sidesMap;
    private static Map junctionPosition;
    
    /** Maps non-edge non-station junctions to tracks ending there.*/
    private static Map unresolvedTrack;
    /** Maps tracks to edge/station junctions*/
    private static Map resolvedTrack;
    
    private static Pattern namePattern = Pattern.compile("^(\\d+)(/.*)?");
    
    Document outputDoc;
    Element outputJunction;
    String tileNo;
    String colour;

    static {
        colourMap = new HashMap();
        colourMap.put("tlYellow", "yellow");
        colourMap.put("tlGreen", "green");
        colourMap.put("tlBrown", "brown");
        colourMap.put("tlGray", "gray");
        colourMap.put("tlOffMap", "red");
        colourMap.put("tlMapFixed", "fixed");
        colourMap.put("tlMapUpgradableToYellow", "white");
        colourMap.put("tlMapUpgradableToGreen", "yellow");
        colourMap.put("tlMapUpgradableToBrown", "green");
        
        stationMap = new HashMap();
        stationMap.put("jtWhistlestop", new String[] {"Town", "0"});
        stationMap.put("jtCity", new String[] {"City", "1"});
        stationMap.put("jtDoubleCity", new String[] {"City", "2"});
        stationMap.put("jtTripleCity", new String[] {"City", "3"});
        stationMap.put("jtQuadrupleCity", new String[] {"City", "4"});
        // Note: an additional station type is "Pass".
        
        gaugeMap = new HashMap();
        gaugeMap.put ("ctNormal", "normal");
        gaugeMap.put ("ctSmall", "narrow");
        gaugeMap.put ("ctUniversal", "dual");
        gaugeMap.put ("ctTunnel", "normal");
        gaugeMap.put ("ctMountain", "normal");
        // 1841 Pass: Station type is changed to Pass.
        
        sidesMap = new HashMap();
        sidesMap.put("tp4SideA", "side0");
        sidesMap.put("tp4SideB", "side1");
        sidesMap.put("tp4SideC", "side2");
        sidesMap.put("tp4SideD", "side3");
        sidesMap.put("tp4SideE", "side4");
        sidesMap.put("tp4SideF", "side5");
            
    }

    public static void main(String[] args) {

        if (args != null) {
            if (args.length > 0)
                inputFilePath = args[0];
            if (args.length > 1)
                outputFilePath = args[1];
        }

        try {
            new ConvertTilesXML();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    private ConvertTilesXML() throws ConfigurationException {
        
        Element inputTopElement = XmlUtils.findElementInFile(inputFilePath,
                "tiles");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            outputDoc = impl.createDocument(null, "Tiles", null);
            
            convertXML (inputTopElement, outputDoc);
            
            TransformerFactory.newInstance().newTransformer().transform (
                    new DOMSource (outputDoc), 
                    new StreamResult(new FileOutputStream(new File(outputFilePath))));
            
        } catch (Exception e) {
            throw new ConfigurationException("Document build error", e);
        }

    }
    
    private void convertXML (Element inputElement, Document outputDoc) 
    throws ConfigurationException {
        
        NodeList children = inputElement.getElementsByTagName("tile");
        for (int i = 0; i<children.getLength(); i++){
            Element inputTile = (Element) children.item(i);
            Element outputTile = outputDoc.createElement("Tile");
            outputDoc.getDocumentElement().appendChild(outputTile);
            convertTile (inputTile, outputTile);
        }

    }
        
    private void convertTile (Element inputTile, Element outputTile) 
    throws ConfigurationException {
        
        String id = inputTile.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue();
        tileNo = id;
        outputTile.setAttribute("id", id);
        int intId;
        try {
            intId = Integer.parseInt (id);
        } catch (NumberFormatException e) {
            throw new ConfigurationException ("Non-numeric ID: "+id, e);
        }
        
        String level = inputTile.getElementsByTagName("level").item(0).getFirstChild().getNodeValue();
        colour = (String) colourMap.get(level);
        if (colour == null) {
            throw new ConfigurationException ("Unknown level: "+level);
        } else {
            outputTile.setAttribute("colour", colour);
        }
                
        String name = inputTile.getElementsByTagName("name").item(0).getFirstChild().getNodeValue();
        Matcher m = namePattern.matcher(name);
        if (m.matches()) {
            outputTile.setAttribute("name", m.group(1));
        } else if (intId > 0) {
            throw new ConfigurationException ("Tile with ID="+id
                    + " has a name not starting with a number: "+name);
        }
        
        /* Create map to hold the station 'identifiers', which are
         * referred to in the track definitions.
         */
        junctionPosition = new HashMap();
        outputJunction = null;
        
        Element junctions = (Element) inputTile.getElementsByTagName("junctions").item(0);
        NodeList children = junctions.getElementsByTagName("junction");
        for (int i = 0; i<children.getLength(); i++){
            Element inputJunction = (Element) children.item(i);
            outputJunction = outputDoc.createElement("Station");
            outputTile.appendChild(outputJunction);
            
            convertJunction (i, inputJunction, outputJunction);
        }
        
        unresolvedTrack = new HashMap();
        resolvedTrack = new HashMap();
        
        Element connections = (Element) inputTile.getElementsByTagName("connections").item(0);
        children = connections.getElementsByTagName("connection");
        for (int i = 0; i<children.getLength(); i++){
            Element inputConnection = (Element) children.item(i);
            convertConnection (inputConnection, outputTile);
        }
        
        Iterator it = unresolvedTrack.keySet().iterator();
        String end1, end2;
        while (it.hasNext()) {
            String key = (String) it.next();
            //System.out.println("Resolving "+key);
            List list = (List)unresolvedTrack.get(key);
            Element[] ends = (Element[]) list.toArray(new Element[0]);
            if (ends.length <= 1) {
                throw new ConfigurationException("Loose end "+ends[0]+" in tile "+tileNo);
            }
            for (int i=1; i<ends.length; i++) {
                //System.out.println("From "+ends[i]);
                end1 = (String) resolvedTrack.get(ends[i]);
                if (end1 == null) {
                    throw new ConfigurationException("Loose end "+ends[i]+" in tile "+tileNo);
                }
                for (int j=0; j<i; j++) {
                    //System.out.println("To "+ends[j]);
                    end2 = (String) resolvedTrack.get(ends[j]);
                    if (end2 == null) {
                        throw new ConfigurationException("Loose end "+ends[j]+" in tile "+tileNo);
                    }
                    Element outputConnection = outputDoc.createElement("Track");
                    outputConnection.setAttribute("gauge", ends[i].getAttribute("gauge"));
                    outputConnection.setAttribute("from", end1);
                    outputConnection.setAttribute("to", end2);
                    outputTile.appendChild(outputConnection);
                    
                }
            }
        }
    }
    
    private void convertJunction (int i, Element inputJunction, Element outputJunction)
    throws ConfigurationException {
        
        String cityId = "city"+(i+1);
        outputJunction.setAttribute("id", cityId);
        
        String type = inputJunction.getElementsByTagName("junType").item(0).getFirstChild().getNodeValue();
        
        String[] station = (String[])((String[]) stationMap.get(type)).clone();
        if (station == null) {
            throw new ConfigurationException ("Unknown junction type: "+type);
        }
        
        /* Off-map cities have the special type "OffMapCity"
         * which does not allow driving through.
         * Original type "town" indicates that no token can be placed.
         */ 
        if (colour.equals("red")) {
        	if (station[0].equals ("Town")) station[1] = "0";
        	station[0] = "OffMapCity"; 
        }
        
        outputJunction.setAttribute("type", station[0]);
        if (!station[1].equals("0")) {
            outputJunction.setAttribute("slots", station[1]);
        }
        
        Element revenue = (Element) inputJunction.getElementsByTagName("revenue").item(0);
        if (revenue != null) {
            String value = revenue.getElementsByTagName("value").item(0).getFirstChild().getNodeValue();
            outputJunction.setAttribute("value", value);
        }
        
        String junctionPos = inputJunction.getElementsByTagName("position").item(0).getFirstChild().getNodeValue();
        junctionPosition.put(junctionPos, cityId);
    }
    
    private void convertConnection (Element inputConnection, Element outputTile) 
    throws ConfigurationException {
        
        String type = inputConnection.getElementsByTagName("conType").item(0).getFirstChild().getNodeValue();
        String gauge = (String) gaugeMap.get(type);
        Element outputConnection;
        if (gauge == null) {
            throw new ConfigurationException ("Unknown gauge type: "+type);
        } else {
            outputConnection = outputDoc.createElement("Track");
            outputConnection.setAttribute("gauge", gauge);
        }
        
        // 1841 special: A pass is not a track type but a station type.
        if (type.equals("ctMountain")) outputJunction.setAttribute("type", "pass");
        
        boolean fromOK = 
            convertTrackEnd (inputConnection, outputConnection, "position1", "from");
        boolean toOK = 
            convertTrackEnd (inputConnection, outputConnection, "position2", "to");
            
        if (fromOK && toOK) outputTile.appendChild(outputConnection);
        
        
        
    }
    
    private boolean convertTrackEnd (Element inputConnection, Element outputConnection,
            String inputName, String outputName) 
    throws ConfigurationException {
        
        String position = inputConnection.getElementsByTagName(inputName).item(0).getFirstChild().getNodeValue();
        
        String end = (String) sidesMap.get(position);
        if (end == null) end = (String) junctionPosition.get(position);
        if (end != null) {
            outputConnection.setAttribute(outputName, end);
            resolvedTrack.put(outputConnection, end);
            return true;
        } else {
            //System.out.println("Tile "+tileNo+" unresolved: "+position);
            if (!unresolvedTrack.containsKey(position)) {
                unresolvedTrack.put(position, new ArrayList());
            }
            ((List)unresolvedTrack.get(position)).add(outputConnection);
            return false;
        }
    }
    
 
}

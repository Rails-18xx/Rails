package net.sf.rails.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;


/**
 * Convert an XML tile dictionary, as created by Marco Rocci's Tile Designer, to
 * an XML file for use in Rails 18xx. <p> The default names are:
 */
public class ConvertTilesXML {
    private static final Logger log = LoggerFactory.getLogger(ConvertTilesXML.class);

    private static String inputFilePath = "src/main/resources/tiles/TileDictionary.xml";

    private static String outputFilePath = "src/main/resources/tiles/Tiles.xml";

    private static final Map<String, String> colourMap;
    private static final Map<String, String> gaugeMap;
    private static final Map<String, String> sidesMap;
    private static final Map<String, String> cityMap;
    private static final Map<String, String[]> stationMap;
    private Map<String, String> junctionPosition;

    /** Maps non-edge non-station junctions to tracks ending there. */
    private Map<String, List<Element>> unresolvedTrack;
    /** Maps tracks to edge/station junctions */
    private Map<Element, String> resolvedTrack;

    private static final Pattern namePattern = Pattern.compile("^(\\d+)(/.*)?");

    private final Document outputDoc;
    private Element outputJunction;
    private String colour;

    static {
        Map<String, String> tempMap = new HashMap<>();
        tempMap.put("tlYellow", "yellow");
        tempMap.put("tlGreen", "green");
        tempMap.put("tlBrown", "brown");
        tempMap.put("tlGray", "gray");
        tempMap.put("tlOffMap", "red");
        tempMap.put("tlMapFixed", "fixed");
        tempMap.put("tlMapUpgradableToYellow", "white");
        tempMap.put("tlMapUpgradableToGreen", "yellow");
        tempMap.put("tlMapUpgradableToBrown", "green");
        tempMap.put("tlMapUpgradableToGray", "brown");
        colourMap = Collections.unmodifiableMap(tempMap);

        Map<String, String[]> tempAMap = new HashMap<>();
        tempAMap.put("jtWhistlestop", new String[] { "Town", "0" });
        tempAMap.put("jtCity", new String[] { "Location", "1" });
        tempAMap.put("jtDoubleCity", new String[] { "Location", "2" });
        tempAMap.put("jtTripleCity", new String[] { "Location", "3" });
        tempAMap.put("jtQuadrupleCity", new String[] { "Location", "4" });
        tempAMap.put("jtHextupleCity", new String[] { "Location", "6" });
        tempAMap.put("jtNone", new String[] { "", "0" });
        // Note: an additional station type is "Pass".
        stationMap = Collections.unmodifiableMap(tempAMap);

        tempMap = new HashMap<>();
        tempMap.put("ctNormal", "normal");
        tempMap.put("ctSmall", "narrow");
        tempMap.put("ctUniversal", "dual");
        tempMap.put("ctTunnel", "normal");
        tempMap.put("ctMountain", "normal");
        // 1841 Pass: Station type is changed to Pass.
        gaugeMap = Collections.unmodifiableMap(tempMap);

        tempMap = new HashMap<>();
        tempMap.put("tp4SideA", "side0");
        tempMap.put("tp4SideB", "side1");
        tempMap.put("tp4SideC", "side2");
        tempMap.put("tp4SideD", "side3");
        tempMap.put("tp4SideE", "side4");
        tempMap.put("tp4SideF", "side5");
        sidesMap = Collections.unmodifiableMap(tempMap);

        tempMap = new HashMap<>();
        tempMap.put("tpCenter", "0");
        tempMap.put("tp1SideA", "001");
        tempMap.put("tp1CornerB", "051");
        tempMap.put("tp1SideB", "101");
        tempMap.put("tp1CornerC", "151");
        tempMap.put("tp1SideC", "201");
        tempMap.put("tp1CornerD", "251");
        tempMap.put("tp1SideD", "301");
        tempMap.put("tp1CornerE", "351");
        tempMap.put("tp1SideE", "401");
        tempMap.put("tp1CornerF", "451");
        tempMap.put("tp1SideF", "501");
        tempMap.put("tp1CornerA", "551");
        tempMap.put("tp2SideA", "002");
        tempMap.put("tp2CornerB", "052");
        tempMap.put("tp2SideB", "102");
        tempMap.put("tp2CornerC", "152");
        tempMap.put("tp2SideC", "202");
        tempMap.put("tp2CornerD", "252");
        tempMap.put("tp2SideD", "302");
        tempMap.put("tp2CornerE", "352");
        tempMap.put("tp2SideE", "402");
        tempMap.put("tp2CornerF", "452");
        tempMap.put("tp2SideF", "502");
        tempMap.put("tp2CornerA", "552");
        tempMap.put("tp3SideA", "003");
        tempMap.put("tp3CornerB", "053");
        tempMap.put("tp3SideB", "103");
        tempMap.put("tp3CornerC", "153");
        tempMap.put("tp3SideC", "203");
        tempMap.put("tp3CornerD", "253");
        tempMap.put("tp3SideD", "303");
        tempMap.put("tp3CornerE", "353");
        tempMap.put("tp3SideE", "403");
        tempMap.put("tp3CornerF", "453");
        tempMap.put("tp3SideF", "503");
        tempMap.put("tp3CornerA", "553");
        tempMap.put("tpCurve1RightA", "006");
        tempMap.put("tpCurve2RightA", "007");
        tempMap.put("tpCurve2LeftA", "008");
        tempMap.put("tpCurve1LeftA", "009");
        tempMap.put("tpCurve1RightB", "106");
        tempMap.put("tpCurve2RightB", "107");
        tempMap.put("tpCurve2LeftB", "108");
        tempMap.put("tpCurve1LeftB", "109");
        tempMap.put("tpCurve1RightC", "206");
        tempMap.put("tpCurve2RightC", "207");
        tempMap.put("tpCurve2LeftC", "208");
        tempMap.put("tpCurve1LeftC", "209");
        tempMap.put("tpCurve1RightD", "306");
        tempMap.put("tpCurve2RightD", "307");
        tempMap.put("tpCurve2LeftD", "308");
        tempMap.put("tpCurve1LeftD", "309");
        tempMap.put("tpCurve1RightE", "406");
        tempMap.put("tpCurve2RightE", "407");
        tempMap.put("tpCurve2LeftE", "408");
        tempMap.put("tpCurve1LeftE", "409");
        tempMap.put("tpCurve1RightF", "506");
        tempMap.put("tpCurve2RightF", "507");
        tempMap.put("tpCurve2LeftF", "508");
        tempMap.put("tpCurve1LeftF", "509");
        cityMap = Collections.unmodifiableMap(tempMap);
    }

    public static void main(String[] args) {
        if (args != null) {
            if (args.length > 0) inputFilePath = args[0];
            if (args.length > 1) outputFilePath = args[1];
        }

        try {
            new ConvertTilesXML();
        } catch (ConfigurationException e) {
            log.warn("caught exception", e);
        }
    }

    private ConvertTilesXML() throws ConfigurationException {
        log.warn("Input file path: {}", new File(inputFilePath).getAbsolutePath());
        log.warn("Output file path: {}", new File(outputFilePath).getAbsolutePath());
        Element inputTopElement = XmlUtils.findElementInFile(inputFilePath, "tiles");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            outputDoc = impl.createDocument(null, "Tiles", null);

            convertXML(inputTopElement, outputDoc);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            //transformerFactory.setAttribute("indent-number", 5);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(outputDoc),
                    new StreamResult(Files.newOutputStream(Paths.get(outputFilePath))));

        } catch (Exception e) {
            throw new ConfigurationException("Document build error", e);
        }

    }

    private void convertXML(Element inputElement, Document outputDoc) throws ConfigurationException {

        NodeList children = inputElement.getElementsByTagName("tile");
            for (int i = 0; i < children.getLength(); i++) {
                try {
                    Element inputTile = (Element) children.item(i);
                    Element outputTile = outputDoc.createElement("Tile");
                    outputDoc.getDocumentElement().appendChild(outputTile);
                    convertTile(inputTile, outputTile);

                } catch (Exception e) {
                    log.error("Exception at i={}: {}", i, e);
                }
            }


    }

    private void convertTile(Element inputTile, Element outputTile) throws ConfigurationException {

        String id = inputTile.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue();
        log.debug("id: {}", id);
        outputTile.setAttribute("id", id);
        // int intId;
        try {
            Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Non-numeric ID: " + id, e);
        }

        String level = inputTile.getElementsByTagName("level").item(0).getFirstChild().getNodeValue();
        colour = colourMap.get(level);
        if (colour == null) {
            throw new ConfigurationException("Unknown level: " + level);
        } else {
            outputTile.setAttribute("colour", colour);
        }

        Node nameNode = inputTile.getElementsByTagName("name").item(0);
        if (nameNode != null) {
            String name = inputTile.getElementsByTagName("name").item(0).getFirstChild().getNodeValue();
            Matcher m = namePattern.matcher(name);
            if (m.matches()) {
                outputTile.setAttribute("name", m.group(1));
            } else
                outputTile.setAttribute("name", name);
            // The below does not work for "B+"
            /*
             * if (intId > 0) { throw new ConfigurationException("Tile with ID=" +
             * id + " has a name not starting with a number: " + name); }
             */
        }

        /*
         * Create map to hold the station 'identifiers', which are referred to
         * in the track definitions.
         */
        junctionPosition = new HashMap<>();
        outputJunction = null;

        Element junctions = (Element) inputTile.getElementsByTagName("junctions").item(0);
        NodeList children = junctions.getElementsByTagName("junction");
        for (int i = 0; i < children.getLength(); i++) {
            Element inputJunction = (Element) children.item(i);
            outputJunction = outputDoc.createElement("Station");
            outputTile.appendChild(outputJunction);

            convertJunction(i, inputJunction, outputJunction);
        }

        unresolvedTrack = new HashMap<>();
        resolvedTrack = new HashMap<>();

        Element connections = (Element) inputTile.getElementsByTagName("connections").item(0);
        children = connections.getElementsByTagName("connection");
        for (int i = 0; i < children.getLength(); i++) {
            Element inputConnection = (Element) children.item(i);
            convertConnection(inputConnection, outputTile);
        }

        // Iterator it = unresolvedTrack.keySet().iterator();
        String end1, end2;
        // while (it.hasNext())
        for (String key : unresolvedTrack.keySet()) {
            // String key = (String) it.next();
            List<Element> list = unresolvedTrack.get(key);
            Element[] ends = list.toArray(new Element[0]);
            if (ends.length <= 1) {
                throw new ConfigurationException("Loose end " + ends[0]
                        + " in tile " + id);
            }
            for (int i = 1; i < ends.length; i++) {
                end1 = resolvedTrack.get(ends[i]);
                if (end1 == null) {
                    throw new ConfigurationException("Loose end " + ends[i]
                                                                         + " in tile " + id);
                }
                for (int j = 0; j < i; j++) {
                    end2 = resolvedTrack.get(ends[j]);
                    if (end2 == null) {
                        throw new ConfigurationException("Loose end " + ends[j]
                                                                             + " in tile " + id);
                    }
                    Element outputConnection = outputDoc.createElement("Track");
                    outputConnection.setAttribute("gauge",
                            ends[i].getAttribute("gauge"));
                    outputConnection.setAttribute("from", end1);
                    outputConnection.setAttribute("to", end2);
                    outputTile.appendChild(outputConnection);

                }
            }
        }
    }

    private void convertJunction(int i, Element inputJunction, Element outputJunction) throws ConfigurationException {

        String cityId = "city" + (i + 1);
        outputJunction.setAttribute("id", cityId);

        String type = inputJunction.getElementsByTagName("junType").item(0).getFirstChild().getNodeValue();

        String[] station = stationMap.get(type);
        if (station == null) {
            throw new ConfigurationException("Unknown junction type: " + type);
        } else {
            station = station.clone();
        }

        /*
         * Off-map cities have the special type "OffMapCity" which does not
         * allow driving through. Original type "town" indicates that no token
         * can be placed.
         */
        if (colour.equals("red")) {
            if (station[0].equals("Town")) station[1] = "0";
            station[0] = "OffMapCity";
        }

        outputJunction.setAttribute("type", station[0]);
        if (!station[1].equals("0")) {
            outputJunction.setAttribute("slots", station[1]);
        }

        // Junction revenue
        Element revenue = (Element) inputJunction.getElementsByTagName("revenue").item(0);
        if (revenue != null) {
            String value =
                revenue.getElementsByTagName("value").item(0).getFirstChild().getNodeValue();
            outputJunction.setAttribute("value", value);
        }

        // Junction position
        String junctionPos = inputJunction.getElementsByTagName("position").item(0).getFirstChild().getNodeValue();
        junctionPosition.put(junctionPos, cityId);
        String jName = cityMap.get(junctionPos);
        if (Util.hasValue(jName)) {
            outputJunction.setAttribute("position", jName);
        } else {
            throw new ConfigurationException("Unknown position: " + junctionPos);
        }
    }

    private void convertConnection(Element inputConnection, Element outputTile) throws ConfigurationException {

        String type = inputConnection.getElementsByTagName("conType").item(0).getFirstChild().getNodeValue();
        String gauge = gaugeMap.get(type);
        Element outputConnection;
        if (gauge == null) {
            throw new ConfigurationException("Unknown gauge type: " + type);
        } else {
            outputConnection = outputDoc.createElement("Track");
            outputConnection.setAttribute("gauge", gauge);
        }

        // 1841 special: A pass is not a track type but a station type.
        if (type.equals("ctMountain"))
            outputJunction.setAttribute("type", "pass");

        boolean fromOK =
            convertTrackEnd(inputConnection, outputConnection, "position1",
            "from");
        boolean toOK =
            convertTrackEnd(inputConnection, outputConnection, "position2",
            "to");

        if (fromOK && toOK) outputTile.appendChild(outputConnection);

    }

    private boolean convertTrackEnd(Element inputConnection, Element outputConnection, String inputName, String outputName) {

        String position = inputConnection.getElementsByTagName(inputName).item(0).getFirstChild().getNodeValue();

        String end = sidesMap.get(position);
        if (end == null) end = junctionPosition.get(position);
        if (end != null) {
            outputConnection.setAttribute(outputName, end);
            resolvedTrack.put(outputConnection, end);
            return true;
        } else {
            if (!unresolvedTrack.containsKey(position)) {
                unresolvedTrack.put(position, new ArrayList<Element>());
            }
            unresolvedTrack.get(position).add(outputConnection);
            return false;
        }
    }

}

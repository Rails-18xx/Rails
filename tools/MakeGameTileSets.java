/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/MakeGameTileSets.java,v 1.9 2010/01/31 22:22:37 macfreek Exp $*/
package tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import rails.game.ConfigurationException;
import rails.util.Tag;

/**
 * Convert an XML tile dictionary, as created by Marco Rocci's Tile Designer, to
 * an XML file for use in Rails 18xx.
 */
public class MakeGameTileSets {

    private static List<String> directories = new ArrayList<String>();
    private static String tilesFilePath = "Tiles.xml";

    public static void main(String[] args) {

        try {
            if (args.length == 0) {

                System.out.println("Provide rails.game name(s) for which to create"
                                   + " tile sets as argument(s).\nALL implies all games below the data directory.");

            } else if (args[0].equalsIgnoreCase("ALL")) {

                List<String> games = new ArrayList<String>();

                File gamesDir = new File("../data");
                if (gamesDir.exists() && gamesDir.isDirectory()) {
                    File[] files = gamesDir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()
                            && !files[i].getName().equalsIgnoreCase("CVS")) {
                            games.add(files[i].getName());
                        }
                    }
                }

                new MakeGameTileSets(games.toArray(new String[0]));

            } else {

                new MakeGameTileSets(args);

            }

        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    private MakeGameTileSets(String[] games) throws ConfigurationException {

        directories.add("tiles");
        Element inputTopElement =
                Tag.findTopTagInFile(tilesFilePath, directories, "Tiles").getElement();

        Map<String, Element> tileMap = new HashMap<String, Element>();
        Element tileSpec;
        String tileName;
        NodeList tList = inputTopElement.getElementsByTagName("Tile");
        for (int i = 0; i < tList.getLength(); i++) {
            tileSpec = (Element) tList.item(i);
            tileName = tileSpec.getAttribute("id");
            tileMap.put(tileName, tileSpec);
        }

        for (int i = 0; i < games.length; i++) {
            System.out.println("Preparing "+games[i]);
            makeTileSet(games[i], tileMap);

        }
        System.out.println("Done");
    }

    private void makeTileSet(String gameName, Map tileMap)
            throws ConfigurationException {

        directories.clear();
        directories.add("../data/" + gameName);

        // Open and read the tile set for this rails.game
        String tileSetPath = "TileSet.xml";
        Element tileSet =
                Tag.findTopTagInFile(tileSetPath, directories, "TileManager").getElement();
        if (tileSet == null) return;
        NodeList tiles = tileSet.getElementsByTagName("Tile");
        Map<String, Object> tilesInSet = new HashMap<String, Object>();

        // Also open and read the map tiles.
        String mapPath = "Map.xml";
        Element mapHexes =
                Tag.findTopTagInFile(mapPath, directories, "Map").getElement();
        NodeList hexes = mapHexes.getElementsByTagName("Hex");

        String tilesPath = "../data/" + gameName + "/Tiles.xml";
        Document outputDoc;
        String tileName;

        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            outputDoc = impl.createDocument(null, "Tiles", null);

            // Scan the TileSet
            for (int i = 0; i < tiles.getLength(); i++) {

                tileName = ((Element) tiles.item(i)).getAttribute("id");
                // Save the tile in a Map so that we can check completeness
                // later.
                // If we already have it, skip
                if (tilesInSet.containsKey(tileName)) continue;
                tilesInSet.put(tileName, null);

                System.out.println("Tile "+tileName);

                // Get the Tile specification
                Element tileSpec = (Element) tileMap.get(tileName);
                if (tileSpec != null) {
                    // Copy it to the subset document
                    Element copy =
                            (Element) outputDoc.importNode(
                                    ((Element) tileMap.get(tileName)), true);
                    outputDoc.getDocumentElement().appendChild(copy);
                } else {
                    System.out.println("ERROR: specified " + gameName
                                       + " tile " + tileName
                                       + " not found in Tiles.xml.");
                }
            }

            // Scan the map, and add any missing tiles, with a warning.
            for (int i = 0; i < hexes.getLength(); i++) {

                tileName = ((Element) hexes.item(i)).getAttribute("tile");
                // Does the preprinted tile occur in TileSet?
                if (tilesInSet.containsKey(tileName)) continue;

                // No, warn and add it to the tiles document.
                System.out.println("WARNING: " + gameName
                                   + " preprinted map tile " + tileName
                                   + " does not occur in TileSet!");

                // Copy it to the subset document
                Element copy =
                        (Element) outputDoc.importNode(
                                ((Element) tileMap.get(tileName)), true);
                outputDoc.getDocumentElement().appendChild(copy);

            }

            TransformerFactory.newInstance().newTransformer().transform(
                    new DOMSource(outputDoc),
                    new StreamResult(new FileOutputStream(new File(tilesPath))));

        } catch (Exception e) {
            throw new ConfigurationException("Document build error", e);
        }

    }

}

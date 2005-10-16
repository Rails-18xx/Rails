/* $Header: /Users/blentz/rails_rcs/cvs/18xx/util/Attic/MakeGameTileSets.java,v 1.3 2005/10/16 15:06:08 evos Exp $
 * 
 * Created on 14-Aug-2005
 * Change Log:
 */
package util;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamResult;

import game.ConfigurationException;

import org.w3c.dom.*;

/**
 * Convert an XML tile dictionary, as created by Marco Rocci's Tile Designer, to
 * an XML file for use in Rails 18xx.
 *
 * 
 * @author Erik Vos
 */
public class MakeGameTileSets {

    private static String tilesFilePath = "tiles/Tiles.xml";
    

    public static void main(String[] args) {
        
        try {
	        if (args.length == 0) {
	        
	            System.out.println ("Provide game name(s) for which to create"
	                    +" tile sets as argument(s).\nALL implies all games below the data directory.");
	        
	        } else if (args[0].equalsIgnoreCase("ALL")) {
	            
	            List games = new ArrayList();
	            
	            File gamesDir = new File ("data");
	            if (gamesDir.exists() && gamesDir.isDirectory()) {
	                File[] files =  gamesDir.listFiles();
	                for (int i=0; i<files.length; i++) {
	                    if (files[i].isDirectory()) {
	                        games.add(files[i].getName());
	                    }
	                }
	            }
	            
	            
	            new MakeGameTileSets((String[])games.toArray());
	            
	        } else {
	            
	            new MakeGameTileSets (args);
	            
	        }
            
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    private MakeGameTileSets(String[] games) throws ConfigurationException {
        
        Element inputTopElement = XmlUtils.findElementInFile(tilesFilePath,
                "Tiles");
        
        Map tileMap = new HashMap();
        Element tileSpec;
        String tileName;
        NodeList tList = inputTopElement.getElementsByTagName("Tile");
        for (int i=0; i<tList.getLength(); i++) {
            tileSpec = (Element) tList.item(i);
            tileName = tileSpec.getAttribute("id");
            tileMap.put(tileName, tileSpec);
        }
        
        for (int i=0; i<games.length; i++) {
            
            makeTileSet (games[i], tileMap);
            
        }
        
    }
    
    private void makeTileSet (String gameName, Map tileMap) 
    		throws ConfigurationException {
        
     	// Open and read the tile set for this game
        String tileSetPath = "data/" + gameName + "/TileSet.xml";
        Element tileSet = XmlUtils.findElementInFile(tileSetPath,
        		"TileSet");
        if (tileSet == null) return;
        NodeList tiles = tileSet.getElementsByTagName("Tile");
        Map tilesInSet = new HashMap();

        // Also open and read the map tiles.
       	String mapPath = "data/" + gameName + "/Map.xml";
       	Element mapHexes = XmlUtils.findElementInFile(mapPath,
		"Map");
       	NodeList hexes = mapHexes.getElementsByTagName("Hex");

       	String tilesPath = "data/" + gameName + "/Tiles.xml";
        Document outputDoc;
        String tileName;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            outputDoc = impl.createDocument(null, "Tiles", null);
            
            // Scan the TileSet
            for (int i=0; i<tiles.getLength(); i++) {
            	
                tileName = ((Element)tiles.item(i)).getAttribute("id");
                // Save the tile in a Map so that we can check completeness later.
                tilesInSet.put(tileName, null);
                
                // Get the Tile specification
                Element tileSpec = (Element)tileMap.get(tileName);
                if (tileSpec != null) {
                    // Copy it to the subset document
                    Element copy = (Element) outputDoc.importNode(((Element)tileMap.get(tileName)), true);
                    outputDoc.getDocumentElement().appendChild(copy);
                } else {
                    System.out.println("ERROR: "+gameName+" tile "+tileName+" not found.");
                }
            }
            
            // Scan the map, and add any missing tiles, with a warning.
            for (int i=0; i<hexes.getLength(); i++) {
            	
                tileName = ((Element)hexes.item(i)).getAttribute("tile");
                // Does the preprinted tile occur in TileSet? 
                if (tilesInSet.containsKey(tileName)) continue;
                
                // No, warn and add it to the tiles document.
                System.out.println("WARNING: " + gameName 
                		+ " preprinted tile "+tileName+" does not occur in TileSet!");
                
                // Get the Tile specification
                Element tileSpec = (Element)tileMap.get(tileName);
                // Copy it to the subset document
                Element copy = (Element) outputDoc.importNode(((Element)tileMap.get(tileName)), true);
                outputDoc.getDocumentElement().appendChild(copy);
                
            }
            
            TransformerFactory.newInstance().newTransformer().transform (
                    new DOMSource (outputDoc), 
                    new StreamResult(new FileOutputStream(new File(tilesPath))));
            
        } catch (Exception e) {
            throw new ConfigurationException("Document build error", e);
        }

    }
    
 
}

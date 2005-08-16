/* $Header: /Users/blentz/rails_rcs/cvs/18xx/util/Attic/MakeGameTileSets.java,v 1.1 2005/08/16 20:24:18 evos Exp $
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
            
            makeTileSet (games[0], tileMap);
            
        }
        
    }
    
    private void makeTileSet (String gameName, Map tileMap) 
    		throws ConfigurationException {
        
        String tileSetPath = "data/" + gameName + "/TileSet.xml";
        String tilesPath = "data/" + gameName + "/Tiles.xml";
        Element tileSet = XmlUtils.findElementInFile(tileSetPath,
        		"TileSet");
        if (tileSet == null) return;
        NodeList tiles = tileSet.getElementsByTagName("Tile");
        
        Document outputDoc;
        String tileName;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            outputDoc = impl.createDocument(null, "Tiles", null);
            
            for (int i=0; i<tiles.getLength(); i++) {
                tileName = ((Element)tiles.item(i)).getAttribute("number");
                Element tileSpec = (Element)tileMap.get(tileName);
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

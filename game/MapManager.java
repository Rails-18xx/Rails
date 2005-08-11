/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/MapManager.java,v 1.2 2005/08/11 20:46:29 evos Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package game;

import java.util.*;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class MapManager implements ConfigurableComponentI {
    
    private String mapUIClassName = null;
    private static MapManager instance = null; 
 
    // The next attributes are duplicates in MapHex. We'll see what we really need.
    protected static int tileOrientation;
    protected static boolean lettersGoHorizontal; 
    protected static boolean letterAHasEvenNumbers;
    
    protected MapHex[][] hexes;
    protected Map mHexes = new HashMap();

    public MapManager () {
        instance = this;
    }
    
    /**
     * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Element el) throws ConfigurationException
    {
        NamedNodeMap nnp = el.getAttributes();
        mapUIClassName = XmlUtils.extractStringAttribute(nnp, "mapClass");
        if (mapUIClassName == null) {
            throw new ConfigurationException ("Map class name missing");
        }
        
        String attr = XmlUtils.extractStringAttribute(nnp, "tileOrientation");
        if (attr.equals("EW")) {
            tileOrientation = MapHex.EW;
            MapHex.setTileOrientation (MapHex.EW);
        } else if (attr.equals("NS")) {
            tileOrientation = MapHex.NS;
            MapHex.setTileOrientation(MapHex.NS);
        } else {
            throw new ConfigurationException ("Invalid tile orientation: "+attr);
        }
        
        attr = XmlUtils.extractStringAttribute(nnp, "letterOrientation");
        if (attr.equals("horizontal")) {
            lettersGoHorizontal = true;
        } else if (attr.equals("vertical")) {
            lettersGoHorizontal = false;
        } else {
            throw new ConfigurationException ("Invalid letter orientation: "+attr);
        }
        MapHex.setLettersGoHorizontal (lettersGoHorizontal);
        
        attr = XmlUtils.extractStringAttribute(nnp, "even");
        letterAHasEvenNumbers = ((int) (attr.toUpperCase().charAt(0) - 'A')) % 2 == 0;
        MapHex.setLetterAHasEvenNumbers(letterAHasEvenNumbers);
        
        NodeList children = el.getElementsByTagName("Hex");
        Element mapElement;
        MapHex hex;
        int maxX = 0;
        int maxY = 0;
        for (int i = 0; i < children.getLength(); i++)
        {
           mapElement = (Element) children.item(i);
           hex = new MapHex();
           hex.configureFromXML(mapElement);
           mHexes.put(hex.getName(), hex);
           maxX = Math.max(maxX, hex.getX());
           maxY = Math.max(maxY, hex.getY());
        }
           
        hexes = new MapHex [1+maxX][1+maxY];
        Iterator it = mHexes.keySet().iterator();
        while (it.hasNext()) {
            hex = (MapHex) mHexes.get((String) it.next());
            hexes [hex.getX()][hex.getY()] = hex;
        }
           
    }
     
    public static MapManager getInstance() {
        return instance;
    }

    
    /**
     * @return Returns the letterAHasEvenNumbers.
     */
    public static boolean letterAHasEvenNumbers() {
        return letterAHasEvenNumbers;
    }
    /**
     * @return Returns the lettersGoHorizontal.
     */
    public static boolean lettersGoHorizontal() {
        return lettersGoHorizontal;
    }
    /**
     * @return Returns the tileOrientation.
     */
    public static int getTileOrientation() {
        return tileOrientation;
    }
    /**
     * @return Returns the hexes.
     */
    public MapHex[][] getHexes() {
        return hexes;
    }
    /**
     * @return Returns the mapUIClassName.
     */
    public String getMapUIClassName() {
        return mapUIClassName;
    }
}

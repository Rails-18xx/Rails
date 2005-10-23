/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Tile.java,v 1.1 2005/10/23 18:02:00 evos Exp $
 * 
 * Created on 23-Oct-2005
 * Change Log:
 */
package game;

import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;
import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class Tile implements TileI {
    
    int id;
    String name;
    String colour; // May become a separate class TileType
    boolean upgradeable;
    List[] tracksPerSide = new ArrayList[6];
    List tracks = new ArrayList();
    
    public Tile (Integer id) {
        this.id = id.intValue();
        
        for (int i=0; i<6; i++) tracksPerSide[i] = new ArrayList();
            
    }
    
    public void configureFromXML (Element se, Element te) throws ConfigurationException {
        
        /* EV 23oct05: There is a lot to read and configure here,
         * for now we only read the tracks to determine
         * the impassable hexsides of offmap and fixed preprinted track.
         */
        
        NamedNodeMap teAttr = te.getAttributes();
        colour = XmlUtils.extractStringAttribute(teAttr, "colour");
        if (colour == null) throw new ConfigurationException("Missing colour in tile "+id);

        upgradeable = !colour.equals("red") && !colour.equals("fixed");
        
        NodeList trackElements = te.getElementsByTagName("Track");
        Element trackEl;
        Track track;
        int from, to;
        String fromStr, toStr;
        NamedNodeMap nnp;
        for (int i=0; i<trackElements.getLength(); i++) {
            trackEl = (Element) trackElements.item(i);
            nnp = trackEl.getAttributes();
            fromStr = XmlUtils.extractStringAttribute(nnp, "from");
            toStr = XmlUtils.extractStringAttribute(nnp, "to");
            if (fromStr == null || toStr == null) {
                throw new ConfigurationException ("Missing from or to in tile "+id);
            }

            from = getPointNumber (fromStr);
            to = getPointNumber (toStr);
            track = new Track (from, to);
            tracks.add (track);
            if (from >= 0) tracksPerSide[from].add(track);
            if (to >= 0) tracksPerSide[to].add(track);
        }
        
    }
    
    
    
    /**
     * @return Returns the colour.
     */
    public String getColour() {
        return colour;
    }
    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    private static final Pattern sidePattern = Pattern.compile("side(\\d+)");
    private static final Pattern cityPattern = Pattern.compile("city(\\d+)");
    
    private int getPointNumber (String trackEnd) throws ConfigurationException {
        
        Matcher m;
        if ((m = sidePattern.matcher(trackEnd)).matches()) {
            return Integer.parseInt(m.group(1))%6;
        } else if ((m = cityPattern.matcher(trackEnd)).matches()) {
            return -Integer.parseInt(m.group(1));
        }
        // Should add some validation!
        throw new ConfigurationException ("Invalid track end: "+trackEnd);
    }
    
    public boolean hasTracks(int sideNumber) {
        return (tracksPerSide[sideNumber%6].size() > 0);
    }
    
    public boolean isUpgradeable () {
        return upgradeable;
    }

}

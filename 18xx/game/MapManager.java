/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/MapManager.java,v 1.1 2005/08/08 20:08:27 evos Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package game;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class MapManager implements ConfigurableComponentI {
    
    private String mapClassName = null;
    private static MapManager instance = null; 
    
    public MapManager () {
        instance = this;
    }
    
    /**
     * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Element el) throws ConfigurationException
    {
        NamedNodeMap nnp = el.getAttributes();
        mapClassName = XmlUtils.extractStringAttribute(nnp, "mapClass");
        if (mapClassName == null) {
            throw new ConfigurationException ("Map class name missing");
        }
    }
     
    public String getMapClassName () {
        return mapClassName;
    }
    
    public static MapManager get() {
        return instance;
    }

}

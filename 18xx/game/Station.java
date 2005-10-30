/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Station.java,v 1.1 2005/10/30 16:29:56 evos Exp $
 * 
 * Created on 30-Oct-2005
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public class Station {
    
    String id;
    String type;
    int value;
    int baseSlots;
    
    Track[] tracks;
    
    public Station (String id, String type, int value) {
        this (id, type, value, 0);
    }
    
    public Station (String id, String type, int value, int slots) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.baseSlots = slots;
    }
    
    

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }
    
    
    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }
    /**
     * @return Returns the baseSlots.
     */
    public int getBaseSlots() {
        return baseSlots;
    }
    /**
     * @return Returns the tracks.
     */
    public Track[] getTracks() {
        return tracks;
    }
    /**
     * @return Returns the value.
     */
    public int getValue() {
        return value;
    }
    
    
}

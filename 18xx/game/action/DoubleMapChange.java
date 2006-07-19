/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/DoubleMapChange.java,v 1.1 2006/07/19 22:08:50 evos Exp $
 * 
 * Created on 19-Jul-2006
 * Change Log:
 */
package game.action;

import java.util.HashMap;
import java.util.Map;

/**
 * This Move class handles adding an entry to a 2-D Map
 * (a Map of Maps, or a matrix).
 * An Undo will remove the second key, but not the first key.
 * @author Erik Vos
 */
public class DoubleMapChange extends Move {
    
    protected Map map;
    protected Object firstKey;
    protected Object secondKey;
    protected Object value;
    
    public DoubleMapChange (Map map, Object firstKey, Object secondKey, Object value) {
        
        this.map = map;
        this.firstKey = firstKey;
        this.secondKey = secondKey;
        this.value = value;
    }

    public boolean execute() {
        
        if (map == null) map = new HashMap();
        if (!map.containsKey(firstKey)) map.put(firstKey, new HashMap());
        ((Map)map.get(firstKey)).put(secondKey, value);
        
        return true;
    }

    public boolean undo() {
        
        if (map.containsKey(firstKey)) {
            ((Map)map.get(firstKey)).remove(secondKey);
        }

        return true;
    }

}

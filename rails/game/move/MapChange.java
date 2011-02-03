/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MapChange.java,v 1.4 2010/05/05 21:36:59 evos Exp $
 *
 * Created on 19-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.Map;

/**
 * This Move class handles adding an entry to a Map.
 *
 * @author Erik Vos
 */
public class MapChange<K, V> extends Move {

    protected Map<K, V> map;
    protected K key;
    protected V newValue;
    protected V oldValue;
    protected boolean keyExisted;

    /**
     * Creates a move that changes a map <key,value> pair
     */
    
    public MapChange (Map<K, V> map, K key, V newValue) {

        this.map = map;
        this.key = key;
        this.newValue = newValue;
        this.oldValue = map.get(key);
        this.keyExisted = map.containsKey(key);

        MoveSet.add(this);
    }
    
    @Override
    public boolean execute() {
        map.put(key, newValue);
        return true;
    }

    @Override
    public boolean undo() {
        if (keyExisted) {
            map.put (key, oldValue);
        } else {
            map.remove(key);
        }
        return true;
    }
    
    public String toString() {
        return "MapChange: key="+key+" from "+oldValue+" to "+newValue;
    }
}

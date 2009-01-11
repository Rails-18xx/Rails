/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MapChange.java,v 1.1 2009/01/11 17:24:46 evos Exp $
 * 
 * Created on 19-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.HashMap;
import java.util.Map;

/**
 * This Move class handles adding an entry to a 2-D Map (a Map of Maps, or a
 * matrix). An Undo will remove the second key, but not the first key.
 * 
 * @author Erik Vos
 */
public class MapChange<K, V> extends Move {

    protected Map<K, V> map;
    protected K key;
    protected V newValue;
    protected V oldValue;
    protected boolean keyExisted;

    public MapChange (Map<K, V> map, K key, V newValue) {

        this.map = map;
        this.key = key;
        this.newValue = newValue;
        this.oldValue = map.get(key);
        this.keyExisted = map.containsKey(key);

        MoveSet.add(this);
    }

    public boolean execute() {

        map.put(key, newValue);

        return true;
    }

    public boolean undo() {

        if (keyExisted) {
            map.put (key, oldValue);
        } else {
            map.remove(key);
        }

        return true;
    }

}

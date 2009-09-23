/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/DoubleMapChange.java,v 1.7 2009/09/23 21:38:57 evos Exp $
 * 
 * Created on 19-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.HashMap;

/**
 * This Move class handles adding an entry to a 2-D Map (a Map of Maps, or a
 * matrix). An Undo will remove the second key, but not the first key.
 * 
 * @author Erik Vos
 */
public class DoubleMapChange<K1, K2, V> extends Move {

    protected HashMap<K1, HashMap<K2, V>> map;
    protected K1 firstKey;
    protected K2 secondKey;
    protected V value;

    public DoubleMapChange(HashMap<K1, HashMap<K2, V>> map, K1 firstKey,
            K2 secondKey, V value) {

        this.map = map;
        this.firstKey = firstKey;
        this.secondKey = secondKey;
        this.value = value;

        MoveSet.add(this);
    }

    public boolean execute() {

        if (map == null) map = new HashMap<K1, HashMap<K2, V>>();
        if (!map.containsKey(firstKey))
            map.put(firstKey, new HashMap<K2, V>());
        map.get(firstKey).put(secondKey, value);

        return true;
    }

    public boolean undo() {

        if (map.containsKey(firstKey)) {
            (map.get(firstKey)).remove(secondKey);
        }

        return true;
    }

    public String toString() {
        return "DoubleMapChange: key1="+firstKey+"key2="+secondKey+" value="+value;
    }
}

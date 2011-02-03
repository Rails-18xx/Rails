/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/MapChange.java,v 1.4 2010/05/05 21:36:59 evos Exp $
 *
 * Created on 19-Jul-2006
 * Change Log:
 */
package rails.game.move;

import java.util.Map;

/**
 * This Move class handles removable from a stateful map (collection)
 *
 * @author Erik Vos
 */
public class RemoveFromMap<K, V> extends Move {

    protected Map<K, V> map;
    protected K key;
    protected V oldValue;
    protected boolean keyExisted;

    /**
     * Creates a move that removes key from map
     */
    
    public RemoveFromMap (Map<K, V> map, K key) {

        keyExisted = map.containsKey(key);
        if (!keyExisted) return;  // Nothing to do
        this.map = map;
        this.key = key;
        this.oldValue = map.get(key);

        MoveSet.add(this);
    }
    
    @Override
    public boolean execute() {
        if (keyExisted) {
            map.remove(key);
        }
        return true;
    }

    @Override
    public boolean undo() {
        if (keyExisted) {
            map.put (key, oldValue);
        }
        return true;
    }
    
    public String toString() {
        return "RemoveFromMap: remove key="+key+" value="+oldValue+" from map";
    }

}

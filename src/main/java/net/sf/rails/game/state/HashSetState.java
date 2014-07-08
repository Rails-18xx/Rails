package net.sf.rails.game.state;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * State class that wraps a HashSet
 */
public final class HashSetState<E> extends SetState<E> {

    private final HashSet<E> set;

    private HashSetState(Item parent, String id, Collection<E> collection) {
        super(parent, id);
        if (collection == null) {
            set = new HashSet<E>();
        } else {
            set = new HashSet<E>(collection);
        }
    }

    /**
     * @return empty HashSetState
     */
    public static <E> HashSetState<E> create(Item parent, String id){
        return new HashSetState<E>(parent, id, null);
    }
    
    /**
     * @return prefilled HashSetState
     */
    public static <E> HashSetState<E> create(Item parent, String id, Collection<E> collection){
        return new HashSetState<E>(parent, id, collection);
    }
    
    @Override
    protected Set<E> getSet() {
        return set;
    }
}

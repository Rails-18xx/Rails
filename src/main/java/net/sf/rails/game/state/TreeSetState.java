package net.sf.rails.game.state;

import java.util.Collection;
import java.util.TreeSet;
import java.util.Set;

public final class TreeSetState<E extends Comparable<?>> extends SetState<E> {

    private final TreeSet<E> set;

    private TreeSetState(Item parent, String id, Collection<E> collection) {
        super(parent, id);
        if (collection == null) {
            set = new TreeSet<E>();
        } else {
            set = new TreeSet<E>(collection);
        }
    }

    /**
     * @return empty TreeSetState
     */
    public static <E extends Comparable<?>> TreeSetState<E> create(Item parent, String id){
        return new TreeSetState<E>(parent, id, null);
    }
    
    /**
     * @return prefilled TreeSetState
     */
    public static <E extends Comparable<?>> TreeSetState<E> create(Item parent, String id, Collection<E> collection){
        return new TreeSetState<E>(parent, id, collection);
    }

    @Override
    protected Set<E> getSet() {
        return set;
    }

}

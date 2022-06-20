package net.sf.rails.game.state;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;

public final class TreeSetState<E extends Comparable<?>> extends SetState<E> {

    private final TreeSet<E> set;

    private TreeSetState(Item parent, String id, Collection<E> collection) {
        super(parent, id);
        if (collection == null) {
            set = new TreeSet<>();
        } else {
            set = new TreeSet<>(collection);
        }
    }

    /**
     * @return empty TreeSetState
     */
    public static <E extends Comparable<?>> TreeSetState<E> create(Item parent, String id){
        return new TreeSetState<>(parent, id, null);
    }
    
    /**
     * @return prefilled TreeSetState
     */
    public static <E extends Comparable<?>> TreeSetState<E> create(Item parent, String id, Collection<E> collection){
        return new TreeSetState<>(parent, id, collection);
    }

    @Override
    public SortedSet<E> getSet() {
        return set;
    }

}

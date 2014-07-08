package net.sf.rails.game.state;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * A stateful version of a TreeMultimap
 */
public final class TreeMultimapState<K extends Comparable<?>, V extends Comparable<?>> 
    extends MultimapState<K,V> {
    
    private final TreeMultimap<K,V> map = TreeMultimap.create();

    private TreeMultimapState(Item parent, String id) {
        super(parent, id);
    }

    /** 
     * Creates an empty TreeMultimapState 
     */
    public static <K extends Comparable<?>,V extends Comparable<?>> TreeMultimapState<K,V> 
        create(Item parent, String id) {
        return new TreeMultimapState<K,V>(parent ,id);
    }
    
    @Override
    protected Multimap<K,V> getMap() {
        return map;
    }
    
    @Override
    public ImmutableSortedSet<V> values() {
        return ImmutableSortedSet.copyOf(map.values());
    }
    
    @Override
    public ImmutableSetMultimap<K,V> view() {
        return ImmutableSetMultimap.copyOf(map);
    }

}

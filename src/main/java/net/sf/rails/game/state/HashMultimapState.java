package net.sf.rails.game.state;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A stateful version of a HashMultimap
 */

public final class HashMultimapState<K,V> extends MultimapState<K,V> {
    
    private final HashMultimap<K,V> map  = HashMultimap.create();
    
    private HashMultimapState(Item parent, String id) {
        super(parent, id);
    }
    
    /** 
     * Creates an empty HashMultimapState 
     */
    public static <K,V> HashMultimapState<K,V> create(Item parent, String id){
        return new HashMultimapState<K,V>(parent ,id);
    }
    
    @Override
    protected Multimap<K,V> getMap() {
        return map;
    }
    
}

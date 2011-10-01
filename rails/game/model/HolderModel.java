package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;

import rails.game.state.ArrayListState;

/**
 * GenericHolder is an implementation that allows to store Ownable objects
 * @author freystef
 *
 * @param <T> The type of objects to store inside the HolderModel
 */

public class HolderModel<T extends Ownable> extends AbstractModel<String> implements Holder<T> {

    private final ArrayListState<T> holdObjects;
    private final Owner owner;
    
    public static <T extends Ownable> HolderModel<T> create(Owner owner, Class<T> clazz) {
        HolderModel<T> holderModel = new HolderModel<T>(owner, clazz);
        owner.addHolder(holderModel, clazz);
        return holderModel;
    }
    
    protected HolderModel(Owner owner, Class<T> clazz) {
        this(owner, clazz, "");
    }
    
    protected HolderModel(Owner owner, Class<T> clazz, String postfix_id) {
        super(owner, clazz.getName() + postfix_id);
        this.owner = owner;
        
        // create the state variable
        holdObjects = new ArrayListState<T>(owner, clazz.getName());
        holdObjects.addObserver(this);
    }
        
    public String getData() {
        return holdObjects.getData();
    }

    public final boolean addObject(T object){
        holdObjects.add(object);
        return true;
    }
    
    public final boolean addObject(T object, int position) {
        holdObjects.add(position, object);
        return true;
    }

    public final boolean removeObject(T object) {
        return holdObjects.remove(object);
    }
    
    public final Owner getOwner() {
        return owner;
    }

    public final T get(int index) {
        return holdObjects.get(index);
    }
    
    public final int getListIndex(T object) {
        return holdObjects.indexOf(object);
    }
    
    public final int size() {
        return holdObjects.size();
    }

    public final boolean isEmpty() {
        return holdObjects.isEmpty();
    }

    public ImmutableList<T> view() {
        return holdObjects.view();
    }
    
    public final Iterator<T> iterator() {
        return holdObjects.iterator();
    }

}

package rails.game.model;

import com.google.common.collect.ImmutableList;

import rails.game.state.ArrayListState;
import rails.game.state.Holder;
import rails.game.state.Item;
import rails.game.state.Moveable;

/**
 * The HolderModel holds a list of Moveable objects
 * @author freystef
 *
 * @param <E> The type of objects to store inside the HolderModel
 */

public class HolderModel<E extends Moveable> extends AbstractModel<String> implements Holder<E> {

    private final ArrayListState<E> holdObjects;
    
    public HolderModel(Item owner, String id) {
        super(owner, id);
        holdObjects = new ArrayListState<E>(owner, id);
        holdObjects.addModel(this);
    }

    public String getData() {
        return holdObjects.getData();
    }

    public boolean addObject(E object, int position) {
        holdObjects.add(position, object);
        return true;
    }

    public boolean removeObject(E object) {
        return holdObjects.remove(object);
    }

    public int getListIndex(E object) {
        return holdObjects.indexOf(object);
    }
    
    public ImmutableList<E> viewList() {
        return holdObjects.view();
    }
    
}

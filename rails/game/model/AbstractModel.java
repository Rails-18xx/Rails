package rails.game.model;

import java.util.ArrayList;
import java.util.List;

import rails.game.state.AbstractItem;
import rails.game.state.Item;

/**
 * A generic superclass for all Model values that need be displayed in some form
 * in the View (UI). 
 * It is an abstract (base) implementation for the Presenter interface.
 * It replaces the ModelObject class in Rails 1.0 
 * 
 * @author freystef
 */
public abstract class AbstractModel<E> extends AbstractItem implements Model<E> {

    private final List<View<E>> views;
    
    public AbstractModel(Item owner, String id) {
        super(owner, id);
        views = new ArrayList<View<E>>();
    }
    
    public void addView(View<E> view) {
        views.add(view);
    }
    
    public void removeView(View<E> view) {
        views.remove(view);
    }
    
    public void notifyModel() {
        // update data
        E data = this.getData();
        // and inform views
        for (View<E> view:views) {
            view.update(data);
        }
    }
}

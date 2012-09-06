package rails.game.state;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
/**
 * Requirement:
 * The observable object has to call each observer per update() if the object has changed.
 */
public abstract class Observable implements Item {

    // fields for Item implementation
    private final String id;
    private final Item parent;
    private final Context context;
    
    /**
     * @param parent parent node in item hierarchy (cannot be null)
     * @param id id of the observable
     * If id is null it creates an "unobservable" observable
     * This is required for the creation of states that are themselves stateless
     */

    protected Observable(Item parent, String id) {
        checkNotNull(parent, "Parent cannot be null");
        checkNotNull(id, "Id cannot be null");
        
        // defined standard fields
        this.parent = parent;
        this.id = id;

        if (parent instanceof Context) {
            context = (Context)parent;
        } else { 
            // recursive definition
            context = parent.getContext();
        }

        context.addItem(this);
    }

    // has to be delayed as at the time of initialization the complete link is not yet defined
    protected StateManager getStateManager() {
        return context.getRoot().getStateManager();
    }

    public void addObserver(Observer o) {
        getStateManager().addObserver(o, this);
    }
    
    public boolean removeObserver(Observer o) {
        return getStateManager().removeObserver(o, this);
    }
    
    public ImmutableSet<Observer> getObservers() {
        return getStateManager().getObservers(this);
    }

    public void addModel(Model m) {
        getStateManager().addModel(m, this);
    }
    
    public boolean removeModel(Model m) {
        return getStateManager().removeModel(m, this);
    }
    
    public ImmutableSet<Model> getModels() {
        return getStateManager().getModels(this);
    }

    /**
     * Text to delivered to Observers
     * Default is defined to be identical with toString()
     * @return text for observers
     */
    public String toText() {
        return this.toString();
    }
    
    // Item methods
    
    public String getId() {
        return id;
    }

    public Item getParent() {
        return parent;
    }

    public Context getContext() {
        return context;
    }
    
    public Root getRoot() {
        // forward it to the context
        return context.getRoot();
    }

    public String getURI() {
        if (parent instanceof Context) {
            return id;
        } else {
            // recursive definition
            return parent.getURI() + Item.SEP + id;
        }
    }
    
    public String getFullURI() {
        // recursive definition
        return parent.getFullURI() + Item.SEP + id;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("uri", getFullURI()).toString();
    }
    
}

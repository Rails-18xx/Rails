package rails.game.state;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;

import rails.game.model.Model;
import rails.game.model.Observer;

/**
 * State is an abstract generic class
 * that defines the base layer of objects that contain game state.
 * 
 * It implements the following interfaces
 * Observable<String>: Allows update Model(s) or other Observer(s)
 * 
 * All State(s) are Item(s) themselves.
 * 
 * It allows to add a Formatter to change the String output dynamically.
 * 
 * @author freystef
 *
 */
public abstract class State extends AbstractItem implements Observable<String> {
    
    // cached version of the String data (similar to AbstractModel)
    private String cached = null;
    private boolean calculated = false;
    
    // stores observers and models
    private Set<Observer<String>> observers = null; // lazy initialization
    private Set<Model<?>> models = null; // lazy initialization
    
    // optional formatter
    private Formatter<State> formatter = null;

    /**
     * Creates a State object
     * @param id identifier for the item (cannot be null)
     */
    public State(String id){
        super(id);
    }
    
    // methods for Item
    /**
     * Remark: If the parent of the state is a model, the model is registered automatically
     */
    @Override
    public void init(Item parent) {
        // set parent using AbstractItem.init() method
        super.init(parent);
        
        // check if the parent's context is a GameContext
        if (parent.getContext() instanceof GameContext) {
            // register state
            ((GameContext)parent.getContext()).getStateManager().registerState(this); 
        } else {
            throw new InvalidParameterException("Invalid parent: States can only be created inside a GameContext hierachy");
        }
        
        // check if parent is a model
        if (parent instanceof Model) {
            addModel((Model<?>)parent);
        }
    }

    // methods for State
    /**
     * Replaces the standard getContext() method:
     * @return GameContext object where the State object exists in
     */
    public GameContext getContext() {
        return (GameContext)super.getContext();
    }
    
    /**
     * Adds a Formatter 
     * @param formatter
     */
    public void setFormatter(Formatter<State> formatter) {
        this.formatter = formatter;
    }
    
    // observable interface
    public String getData() {
        if (formatter == null) {
            return toString();
        } else {
            if (!calculated) {
                cached =  formatter.formatData(this);
                calculated = true;
            }
            return cached;
        }
    }

    public void addObserver(Observer<String> observer) {
        observers.add(observer);
    }
    
    public boolean removeObserver(Observer<String> observer) {
        return observers.remove(observer);
    }
    
    public Set<Observer<String>> getObservers() {
        return observers;
    }

    public void addModel(Model<?> m) {
        if (models == null) {
            models = new HashSet<Model<?>>();
        }
        models.add(m);
    }
    
    public boolean removeModel(Model<?> m) {
        return models.remove(m);
    }
    
    public Set<Model<?>> getModels() {
        return models;
    }
    
    public void updateModels() {
        for (Model<?> m:models) {
            m.update();
        }
    }
    
}

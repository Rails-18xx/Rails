package rails.game.state;

import java.security.InvalidParameterException;

import rails.game.model.Observer;

/**
 * 
 * @author freystef
 *
 */

abstract class AbstractState implements State, Item {
    // set at construction
    private final String id;
    private Item parent;
    
    // set once at initialization
    private GameContext context;

    // can be added later
    private final Formatter<AbstractState> formatter;
    
    // cached version of the String data (similar to AbstractModel)
    private String cached = null;
    private boolean calculated = false;
    
    /**
     * Creates an AbstractState
     * @param parent the parent of the state (cannot be null)
     * @param id identifier for the item (cannot be null)
     */
    public AbstractState(Item parent, String id){
        if (id == null) {
            throw new NullPointerException("Missing id for a state object");
        }
        if (parent == null) {
            throw new IllegalArgumentException("Missing parent for a state object");
        }

        this.id = id;
        init(parent);
    }
    
    
    

    public void setFormatter(Formatter<AbstractState> formatter) {
        this.formatter = formatter;
        
    
    

        // pass along the root
        if (parent.getContext() instanceof GameContext) {
            this.context = (GameContext) parent.getContext();
        } else {
            throw new InvalidParameterException("Invalid parent: States can only be created inside a GameContext hierachy");
        }
        
        // add to StateManager
        context.getStateManager().registerState(this);
    }
    
    // methods for item interface
    public String getId() {
        return id;
    }
    
    public String getURI() {
        if (parent != null && parent.getId() != null ) {
            return parent.getId() + id;
        } else {
            return id;
        }
    }

    public Item getParent() {
        return parent;
    }
    
    public Context getContext() {
        return context;
    }
    
    // methods for model interface
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
    
    public void getLazyData() {
        throw new AssertionError("You have to overwrite either getData() or getLazyData() for classes that extend AbstractState.");
    }
    
    // methods for observer
    public void update() {
        calculated = false;
    }
    
    // methods for observable
    public void addObserver(Observer observer) {
        context.getStateManager().registerObserver(observer, this);
    }
    
    public void removeObserver(Observer observer) {
        
    }
    
    // methods for state
    public void addReceiver(Triggerable receiver) {
        context.getStateManager().registerReceiver(receiver, this);
    }
    
}

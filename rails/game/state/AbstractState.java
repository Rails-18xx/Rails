package rails.game.state;

import java.security.InvalidParameterException;

import rails.game.model.Observer;

abstract class AbstractState implements State {
    
    private final String id;
    private final GameContext root;
    
    private final Formatter<AbstractState> formatter;

    // cached version of the String data (similar to Abstract Model
    private String cached = null;
    private boolean calculated = false;
    
    public AbstractState(Item parent, String id) {
        this(parent, id, null);
    }
    
    public AbstractState(Item parent, String id, Formatter<AbstractState> formatter) {
        
        if (parent.getId() != null) {
           this.id = parent.getId() + "." + id;
        } else {
           this.id = id;
        }
        
        // define formatter 
        this.formatter = formatter;

        // pass along the root
        if (parent.getRoot() instanceof GameContext) {
            this.root = (GameContext) parent.getRoot();
        } else {
            throw new InvalidParameterException("Invalid parent: States can only be created inside a GameContext hierachy");
        }
        
        // add to StateManager
        root.getStateManager().registerState(this);
    }
    
    // methods for item
    public String getId() {
        return id;
    }

    public Context getRoot() {
        return root;
    }
    
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
        root.getStateManager().registerObserver(observer, this);
    }
    
    public void removeObserver(Observer observer) {
        
    }
    
    // methods for state
    public void addReceiver(Triggerable receiver) {
        root.getStateManager().registerReceiver(receiver, this);
    }
    
}

package rails.game.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Contexts allows the location of items
 * Contexts take care of the registration of the state variables
 * 
 * All contexts have to be part of the hierachy with the root context at the top
 * @author freystef
 */
public class Context extends GameItem {
    public static final String ROOT = "game"; 
    public static final char SEP = '.';
    
    final Map<String, GameItem> items = new HashMap<String, GameItem> ();
    private StateManager stateManager;
    
    /** 
     * Creates a game context
     */
    public Context(String id) {
        super(id);
        if (id.equals(ROOT)) {
            throw new IllegalArgumentException("Context id cannot equal ROOT id");
        }
    }

    private Context() {
        super(ROOT);
    }
    
    /**
     * Creates an initialized Context
     */
    public static Context create(Item parent, String id) {
        return new Context(id).init(parent);
    }
    
    /**
     * Creates the top-level Context named with Context.ROOT
     */
    public static Context createRootContext() {
        return new Context().initRoot();
    }
    
    /**
     * Intializes the game context
     * The parent has to be in a hierarchy with the root GameContext
     **/
    @Override
    public Context init(Item parent) {
        super.init(parent);
        
        // defines stateManager for root
        if (getId().equals(ROOT)) {
        } else {
            if (getContext() instanceof Context) {
                this.stateManager = ((Context)getContext()).getStateManager();
            } else {
                throw new IllegalArgumentException("GameContext can only be created in a hierachy with GameContexts at the top");
            }
        }
        
        return this;
    }
    
    private Context initRoot() {
        super.init(this); // sets parent identical to ROOT
        stateManager = StateManager.create();
        return this;
    }

    @Override
    public Context getContext() {
        return this;
    }
    
    public GameItem localize(String uri) {
        if (items.containsKey(uri)) {
            return items.get(uri);
        } else if (getParent() != null) {
            return getContext().localize(uri);
        } else { 
            return null;
        }
    }

    public void addItem(GameItem item) {
        // first check if this context is the containing one
        String uri;
        if (item.getContext() == this) {
            uri = item.getURI();
        } else {
            uri = item.getContext().getURI() + Context.SEP + item.getURI();
        }
        
        // check if it exists
        if (items.containsKey(uri)) {
            throw new RuntimeException("Context already contains item with identical URI = " + item.getURI());
        }
        
        // otherwise put it to the items list
        items.put(uri, item);
        
        // forward to parent context if that is defined
        if (getContext() != null) {
            getContext().addItem(item);
        }
        
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    
}

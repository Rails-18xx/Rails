package rails.game.state;

import java.util.ArrayList;
import java.util.List;

/**
 * GameItems are Items that allow the storage of State variables 
 * @author freystef
 *
 */
public abstract class GameItem implements Item {
    
    // list of contained states
    private final List<State> states = new ArrayList<State>();
    private final String id;

    // parent can be initialized later
    private GameItem parent;
    
    @Deprecated
    // TODO: Remove that default constructor here
    public GameItem() {
        this.id = null;
    }

    /**
     * Creates an GameItem
     * @param id identifier for the item (cannot be null)
     */
    public GameItem(String id){
        if (id == null) {
            throw new IllegalArgumentException("Missing id for an item in hierarchy");
        }
        this.id = id;
    }

    /**
     * Initializing of GameItem
     * @param parent has to be of type GameItem otherwise an Exception is raised
     */
    public void init(Item parent){
        if (this.parent != null) {
            throw new IllegalStateException("Item already intialized");
        }
        if (parent instanceof GameItem) {
            throw new IllegalArgumentException("Parents of GameItems have to be GameItems themselves");
        } else {
            this.parent = (GameItem)parent;
            // Register states
            for (State state:states) {
                getContext().getStateManager().registerState(state);
            }
        }
    }
    
    /**
     * Adding a new state to a GameItem
     * State is not usable until GameItem is initialized
     * Adding further states later is possible
     */
    public void addState(State state){
        if (parent == null) {
            states.add(state);
        } else {
            getContext().getStateManager().registerState(state);
        }
    }
    
    public String getId() {
        return id;
    }

    public GameItem getParent() {
        if (parent == null) {
            throw new IllegalStateException("Item not yet intialized");
        }
        return parent;
    }
    
    public GameContext getContext() {
        if (parent == null) {
            throw new IllegalStateException("Item not yet intialized");
        }
        return parent.getContext();
    }

    public String getURI() {
        if (parent != null && parent.getURI() != null ) {
            if (parent instanceof Context) {
                return id;
            } else {
                return parent.getURI() + Context.SEP + id;
            }
        } else {
            return id;
        }
    }
    
}

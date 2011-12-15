package rails.game.state;

/**
 * Game context are that type of context that allows to add state objects
 * Those state objects are registered with the StateManager
 * @author freystef
 */
public class GameContext extends GameItem implements Context {
    public static final String ROOT = "game"; 
    
    private StateManager stateManager;

    /** 
     * Creates a game context
     * @param id 
     * Remark: For the root GameContext use id identical to GameContext.ROOT
     */
    public GameContext(String id) {
        super(id);
    }
    
    /**
     * Intializes the game context
     * The parent has to be in a hierarchy with the root GameContext
     **/
    public void init(Item parent) {
        super.init(parent);
        
        // defines stateManager for root
        if (getId().equals(ROOT)) {
            stateManager = new StateManager(this);
        } else {
            if (getContext() instanceof GameContext) {
                this.stateManager = ((GameContext)getContext()).getStateManager();
            } else {
                throw new IllegalArgumentException("GameContext can only be created in a hierachy with GameContexts at the top");
            }
        }
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public Item localize(String uri) {
        // TODO Auto-generated method stub
        return null;
    }

    public void addItem(Item item) {
        // TODO Auto-generated method stub
        
    }
    
}

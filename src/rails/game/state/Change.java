package rails.game.state;
/**
 * Base Class for all Change Objects
 * 
 * Replaces previous move interface
 */
abstract class Change {
    
    protected void init(State state){
        state.getStateManager().getChangeStack().addChange(this);
    }

    abstract void execute();
    abstract void undo();  
    abstract State getState();

}

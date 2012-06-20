package rails.game.state;
/**
 * Base Class for all Change Objects
 * 
 * Replaces previous move interface
 * 
 * @author freystef
 */
abstract class Change {
    
    Change(State state){
        state.getStateManager().addChangeToStack(this);
    }

    abstract void execute();
    abstract void undo();  
    abstract State getState();

}

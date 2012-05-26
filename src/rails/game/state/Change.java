package rails.game.state;
/**
 * Is implemented by classes that change state objects
 * 
 * Replaces previous move interface
 * 
 * @author freystef
 */
interface Change {
    
       public void execute();

       public void undo();
    
       public State getState();
       
}

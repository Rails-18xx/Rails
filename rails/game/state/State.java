package rails.game.state;

import rails.game.model.Model;

/**
 * Interface to be implemented by all state variables.
 * 
 * States have to be linked to a GameContext to be working.
 * To achieve parent of States can only be GameItems or GameContexts.
 * 
 * All states can be used as a Model<String>.
 *  
 * @author freystef
 *
 */
public interface State extends Model<State> {
    
   /**
    * Adds a triggerable object (receiver) 
    * @param receiver
    */
    public void addReceiver(Triggerable receiver);
    
    /**
     
     */
    public GameContext getGameContext();
    
}

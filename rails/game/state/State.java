package rails.game.state;

import rails.game.model.Model;

/**
 * Interface to be implemented by all state variables
 * @author freystef
 *
 */
public interface State extends Item, Model<String> {
    
    public void addModel(Notifiable toUpdate);
    
    public void addReceiver(Triggerable receiver);
    
    
}

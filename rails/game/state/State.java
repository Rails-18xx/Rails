package rails.game.state;

import rails.game.model.Model;

/**
 * Interface to be implemented by all state variables
 * @author freystef
 *
 */
public interface State extends Model<String> {
    
    public void addReceiver(Triggerable receiver);
    
}

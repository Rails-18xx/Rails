package rails.game.model;

import rails.game.state.Item;
import rails.game.state.GenericState;

/**
 * A convencience class to use instead the verbose GenericState<Owner>
 * @author freystef
 */
public class OwnerState extends GenericState<Owner> {

    public OwnerState(Item parent) {
        super(parent, "owner");
    }
    
}

package rails.game.model;

import rails.game.state.GenericState;

/**
 * A convencience class to use instead the verbose GenericState<Owner>
 * @author freystef
 */
public class OwnerState extends GenericState<Owner> {

    /**
     * OwnerState uses specific id "owner"
     */
    public OwnerState() {
        super("owner");
    }
    
}

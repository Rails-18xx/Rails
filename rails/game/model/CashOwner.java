package rails.game.model;

import rails.game.state.Item;

@Deprecated
public interface CashOwner extends Item {

    public CashModel getCashModel();
    
    public int getCash();

}

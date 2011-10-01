package rails.game.model;

import rails.game.state.Item;

public interface CashOwner extends Item {

    public CashModel getCashModel();
    
    public int getCash();

}

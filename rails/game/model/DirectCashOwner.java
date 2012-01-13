package rails.game.model;

import rails.game.state.Item;

public abstract class DirectCashOwner extends DirectOwner implements CashOwner {

    private final CashModel cashModel = new CashModel();
    
    @Deprecated
    public DirectCashOwner() {
        super();
    }

    public DirectCashOwner(String id) {
        super(id);
    }
    
    public final CashModel getCashModel() {
       return cashModel;
    }
    
    @Override
    public void init(Item parent){
        super.init(parent);
        cashModel.init(this);
    }

    public final int getCash() {
        return cashModel.value();
    }

}

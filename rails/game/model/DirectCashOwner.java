package rails.game.model;

public abstract class DirectCashOwner extends DirectOwner implements CashOwner {

    private final CashModel cashModel = new CashModel(this);
    
    public final CashModel getCashModel() {
       return cashModel;
    }

    public final int getCash() {
        return cashModel.value();
    }

}

package rails.game.model;

import rails.game.Currency;
import rails.game.MoneyOwner;
import rails.game.state.BooleanState;
import rails.game.state.WalletBag;

/**
 * A MoneyModel with a wallet inside
 */
public class WalletMoneyModel extends MoneyModel {

    private final WalletBag<Currency> wallet;
    
    private final BooleanState initialised;

    private WalletMoneyModel(MoneyOwner parent, String id, Boolean init, Currency currency) {
        super(parent, id, currency);
        wallet = WalletBag.create(parent, "wallet", Currency.class, currency);
        wallet.addModel(this);
        initialised = BooleanState.create(this, "initialised", init);
    }
    
    public static WalletMoneyModel create(MoneyOwner parent, String id, Boolean init){
        Currency currency = parent.getRoot().getCurrency();
        return new WalletMoneyModel(parent, id, init, currency);
    }
    
    public static WalletMoneyModel create(MoneyOwner parent, String id, Boolean init, Currency currency) {
        return new WalletMoneyModel(parent, id, init, currency);
    }

    @Override
    public MoneyOwner getParent() {
        return (MoneyOwner)super.getParent();
    }
    
    // MoneyModel abstracts
    @Override
    public int value() {
        return wallet.value();
    }

    @Override
    public boolean initialised() {
        return initialised.value();
    }

}

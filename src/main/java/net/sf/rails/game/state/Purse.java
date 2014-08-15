package net.sf.rails.game.state;

import com.google.common.base.Preconditions;

/**
 * Purse is a wallet that allows to store money of only one currency
 * This currency is set at time of creation
 */
public class Purse extends Wallet<Currency> {

    private final Currency currency;
    private int amount = 0;
    
    private Purse(Owner parent, String id, Currency currency) {
        super(parent, id, Currency.class);
        this.currency = currency;
    }
    
    /**
     * Creates an empty WalletBag
     */
    public static Purse create(Owner parent, String id, Currency currency){
        return new Purse(parent, id, currency);
    }
    
    /**
     * @return currency of the purse
     */
    public Currency getCurrency() {
        return currency;
    }
    
    @Override
    public int value(Currency currency) {
        Preconditions.checkArgument(currency == this.currency, "Purse only accepts " + this.currency);
        return amount;
    }

    @Override
    public int value() {
        return amount;
    }
    
    @Override
    void change (Currency item, int value) {
        amount += value;
    }

    @Override
    public String toText() {
        return currency.format(amount);
    }
    
}

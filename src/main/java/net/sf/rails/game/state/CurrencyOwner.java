package net.sf.rails.game.state;

/**
 * The owner of a currency (typically something like a bank)
 */
public interface CurrencyOwner extends MoneyOwner {
    
    public Currency getCurrency();
    
}

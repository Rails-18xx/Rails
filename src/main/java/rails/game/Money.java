package rails.game;

import com.google.common.base.Preconditions;

/**
 * Money encapsulates an integer and a currency
 */
public class Money {

    private final Currency currency;
    private final int value;
    
    public Money(Currency currency, int value) {
        this.currency = currency;
        this.value = value;
    }
    
    public Currency currency() {
        return currency;
    }

    public int value() {
        return value;
    }
    
    public Money add(int amount) {
        return new Money(currency, value + amount);
    }
    
    public Money add(Money money) {
        Preconditions.checkArgument(money.currency() == currency, "Different currencies cannot be added");
        return new Money(currency, value + money.value());
    }
    
    public Money multiply(int multiple) {
        return new Money(currency, value * multiple);
    }
    
    public void wire(MoneyOwner from, MoneyOwner to) {
        currency.move(from, value, to);
    }
    
    public void toBank(MoneyOwner from) {
        wire(from, currency.getParent());
    }

    public void fromBank(MoneyOwner to) {
        wire(currency.getParent(), to);
    }
    
    public String toText() {
        return currency.format(value);
    }
}

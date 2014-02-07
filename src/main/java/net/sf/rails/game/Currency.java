package net.sf.rails.game;

import net.sf.rails.game.state.CountableItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The class that represents a Currency
 */
public class Currency extends CountableItem<Currency> implements RailsItem {

    protected static Logger log =
            LoggerFactory.getLogger(Currency.class);
    
    /**
     * The money format template. '@' is replaced by the numeric amount, the
     * rest is copied.
     */
    private String format;
    
    private Currency(Bank parent, String id) {
        super(parent, id, Currency.class);
    }

    static Currency create(Bank parent, String id) {
        return new Currency(parent, id);
    }
    
    @Override
    public Bank getParent() {
        return (Bank)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }

    public String format(int amount) {
        // Replace @ with the amount
        String result = format.replaceFirst("@", String.valueOf(amount));
        // Move any minus to the front
        if (amount < 0) result = result.replaceFirst("(.+)-", "-$1");
        return result;
    }

    public String format(Iterable<Integer> amountList) {
        StringBuilder result = new StringBuilder();
        boolean init = true;
        for (int amount:amountList) {
            if (init) {
                init = false; 
            } else {
                result.append(",");
            }
            result.append(format(amount));
        }
        return result.toString();
    }

    void setFormat(String format) {
        this.format = format;
    }
    
    public static String wire(MoneyOwner from, int amount, MoneyOwner to) {
        Currency currency = from.getRoot().getCurrency();
        currency.move(from, amount, to);
        return currency.format(amount);
    }

    public static String wireAll(MoneyOwner from, MoneyOwner to) {
        return wire(from, from.getCash(), to);
    }
    
    public static String toBank(MoneyOwner from, int amount) {
        Currency currency = from.getRoot().getCurrency();
        currency.move(from, amount, currency.getParent());
        return currency.format(amount);
    }
    
    public static String toBankAll(MoneyOwner from) {
        return toBank(from, from.getCash());
    }
    
    public static String fromBank(int amount, MoneyOwner to) {
        Currency currency = to.getRoot().getCurrency();
        currency.move(currency.getParent(), amount, to);
        return currency.format(amount);
    }

    public static String format(RailsItem item, int amount) {
        Currency currency = item.getRoot().getCurrency();
        return currency.format(amount);
    }

    public static String format(RailsItem item, Iterable<Integer> amount) {
        Currency currency = item.getRoot().getCurrency();
        return currency.format(amount);
    }
    
    public static Money newMoney(RailsItem item, int amount) {
        return new Money(item.getRoot().getCurrency(), amount);
    }

}

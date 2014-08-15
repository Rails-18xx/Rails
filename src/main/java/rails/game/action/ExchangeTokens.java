package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.rails.util.RailsObjects;

import com.google.common.base.Objects;

/**
 * Rails 2.0: Updated equals and toString methods
 */

// FIXME: This stores client settings (action results) in an external object (ExchangeableToken).
// This is bad practice in Rails. This action is only used in 1856, maybe it should be a specific action there 
public class ExchangeTokens extends PossibleORAction {

    // Server settings
    private List<ExchangeableToken> tokensToExchange = new ArrayList<ExchangeableToken>();
    private int minNumberToExchange = 0;
    private int maxNumberToExchange = 0;

    // Client settings

    public static final long serialVersionUID = 1L;

    public ExchangeTokens(List<ExchangeableToken> tokensToSelectFrom,
            int minNumberToExchange,
            int maxNumberToExchange) {

        super();
        this.tokensToExchange = tokensToSelectFrom;
        this.minNumberToExchange = minNumberToExchange;
        this.maxNumberToExchange = maxNumberToExchange;
    }

    public void setExchangedTokens(List<ExchangeableToken> exchangedTokens) {
        for (ExchangeableToken t : exchangedTokens) {
            t.setSelected(true);
        }
    }

    public int getMaxNumberToExchange() {
        return maxNumberToExchange;
    }

    public int getMinNumberToExchange() {
        return minNumberToExchange;
    }

    public List<ExchangeableToken> getTokensToExchange() {
        return tokensToExchange;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        ExchangeTokens action = (ExchangeTokens)pa; 
        return Objects.equal(this.tokensToExchange, action.tokensToExchange)
                && Objects.equal(this.minNumberToExchange, action.minNumberToExchange)
                && Objects.equal(this.maxNumberToExchange, action.maxNumberToExchange)
        ;
        // no asAction attributes to be checked
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("tokensToExchange", tokensToExchange)
                    .addToString("minNumberToExchange", minNumberToExchange)
                    .addToString("maxNumberToExchange", maxNumberToExchange)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

    }
}

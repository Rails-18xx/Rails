/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/ExchangeTokens.java,v 1.2 2009/08/03 21:26:20 evos Exp $
 *
 * Created on 20-May-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Erik Vos
 */
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
    public String toString() {

        StringBuffer b = new StringBuffer();
        b.append("ExchangeTokens for "+companyName+":");
        for (ExchangeableToken token : tokensToExchange) {
            b.append(" ").append(token.toString());
        }
        b.append(" min=").append (minNumberToExchange);
        b.append(" max=").append (maxNumberToExchange);
        return b.toString();
    }
    
    @Override
    public boolean equals(PossibleAction action) {
        if (!(action instanceof ExchangeTokens)) return false;
        ExchangeTokens a = (ExchangeTokens) action;
        return a.tokensToExchange == tokensToExchange && a.company == company;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

    }
}

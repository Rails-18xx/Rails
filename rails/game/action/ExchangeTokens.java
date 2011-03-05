/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/ExchangeTokens.java,v 1.3 2010/01/01 14:34:06 evos Exp $
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
        if (tokensToExchange != null) {
            for (ExchangeableToken token : tokensToExchange) {
                b.append(" ").append(token.toString());
            }
        } else {
            b.append ("-none?-");
            log.error("No exchangable tokens found when expected - error seen during Undo CGR formation");
        }
        b.append(" min=").append (minNumberToExchange);
        b.append(" max=").append (maxNumberToExchange);
        return b.toString();
    }
    
    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action instanceof ExchangeTokens)) return false;
        ExchangeTokens a = (ExchangeTokens) action;
        return a.tokensToExchange == tokensToExchange && a.company == company;
    }

    @Override
    public boolean equalsAsAction(PossibleAction action) {
        if (!action.equalsAsOption(this)) return false;
        ExchangeTokens a = (ExchangeTokens) action;
        for (int i=0; i<tokensToExchange.size(); i++) {
            if (a.tokensToExchange.get(i).isSelected() != tokensToExchange.get(i).isSelected()) return false;
        }
        return true;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

    }
}

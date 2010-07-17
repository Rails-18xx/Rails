/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Token.java,v 1.8 2010/03/23 18:44:55 stefanfrey Exp $
 *
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolder;
import rails.game.move.ObjectMove;

/**
 * @author Erik Vos
 */
public abstract class Token implements TokenI {

    protected TokenHolder holder = null;
    protected String description = "";
    protected String uniqueId;

    private static Map<String, TokenI> tokenMap = new HashMap<String, TokenI>();
    private static int index = 0;

    protected static Logger log =
        Logger.getLogger(Token.class.getPackage().getName());

    public Token() {

        uniqueId = "Token_" + (index++);
        tokenMap.put(uniqueId, this);
    }

    // initialize the special properties static variables
    public static void init() {
        tokenMap = new HashMap<String, TokenI>();
        index = 0;
        log.debug("Init token static variables");
    }

    public static TokenI getByUniqueId(String id) {
        return tokenMap.get(id);
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setHolder(TokenHolder holder) {
        this.holder = holder;
    }

    public TokenHolder getHolder() {
        return holder;
    }

    public void moveTo(MoveableHolder newHolder) {
        if (newHolder instanceof TokenHolder) {
            new ObjectMove(this, holder, newHolder);
        }
    }

    public boolean equals(TokenI otherToken) {
        return otherToken != null && uniqueId.equals(otherToken.getUniqueId());
    }

    /**
     * Transfer a token object from one TokenHolder (e.g. a Company) to another
     * (e.g. a Station in a MapHex).
     *
     * @param token
     * @param from
     * @param to
     */
    /*
    public static void transfer(TokenI token, TokenHolder from, TokenHolder to) {
        to.addToken(token, -1);
        from.removeToken(token);
        token.setHolder(to);
    }
     */

}

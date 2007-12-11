/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Token.java,v 1.3 2007/12/11 20:58:33 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Erik Vos
 */
public abstract class Token implements TokenI {

    TokenHolderI holder = null; 
    String description = "";
    String uniqueId;
    
    private static Map<String, TokenI> tokenMap 
        = new HashMap<String, TokenI> ();
    private static int index = 0;
    
    public Token() {
    
        uniqueId = "Token_"+ (index++);
        tokenMap.put (uniqueId, this);
    }
    
    public static TokenI getByUniqueId (String id) {
        return tokenMap.get (id);
    }
    
    public String getUniqueId () {
        return uniqueId;
    }
    
    public void setHolder (TokenHolderI holder) {
        this.holder = holder;
    }
    
    public TokenHolderI getHolder () {
        return holder;
    }
    
    /**
     * Transfer a token object from one TokenHolder (e.g. a Company)
     * to another (e.g. a Station in a MapHex).
     * @param token
     * @param from
     * @param to
     */
    public static void transfer (TokenI token, TokenHolderI from, TokenHolderI to) {
        to.addToken(token);
        from.removeToken(token);
        token.setHolder(to);
    }

}

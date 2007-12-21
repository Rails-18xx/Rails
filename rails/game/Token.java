/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Token.java,v 1.4 2007/12/21 21:18:12 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import rails.game.move.MoveableHolderI;
import rails.game.move.ObjectMove;

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
    
    public void moveTo (MoveableHolderI newHolder) {
        if (newHolder instanceof TokenHolderI) {
            new ObjectMove (this, holder, newHolder);
        }
    }
    
    public boolean equals (TokenI otherToken) {
        return otherToken != null
            &&  uniqueId.equals(otherToken.getUniqueId());
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

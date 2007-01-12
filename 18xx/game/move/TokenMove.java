/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/TokenMove.java,v 1.3 2007/01/12 22:51:29 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import game.*;

/**
 * @author Erik Vos
 */
public class TokenMove extends Move {
    
    TokenI token;
    TokenHolderI from;
    TokenHolderI to;
    
    /**
     * Create a generic TokenMove object.
     * Any specific side effects must be implemented in the addToken 
     * and removeToken methods of the 'from' and 'to' TokenHolders.
     * <p>The parameter descriptions cover the usual case of a Base Token lay,
     * which is physically removed from a PublicCompany and added to a Station 
     * on a MapHex.
     * 
     * @param token The token to be moved (e.g. a BaseToken).
     * @param from Where the token is removed from (e.g. a PublicCompany charter).
     * @param to Where the token is moved to (e.g. a MapHex).
     */
            
    public TokenMove (TokenI token, TokenHolderI from, TokenHolderI to) {
        
        this.token = token;
        this.from = from;
        this.to = to;
    }


    public boolean execute() {

        return from.removeToken(token) && to.addToken(token);
    }

    public boolean undo() {
        
        return to.removeToken(token) && from.addToken(token);
    }
    
    public String toString() {
        if (token == null) log.debug ("Token is null");
        if (from == null) log.debug ("From is null");
        if (to == null) log.debug ("To is null");        
        return "TokenMove: "+token.getName()
        	+ " from " + from.getName()
        	+ " to " + to.getName();
   }

}

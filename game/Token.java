/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Token.java,v 1.1 2007/01/03 22:34:16 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public abstract class Token implements TokenI {

    TokenHolderI holder = null; 
    /**
     * 
     */
    public Token() {
        super();
        // TODO Auto-generated constructor stub
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

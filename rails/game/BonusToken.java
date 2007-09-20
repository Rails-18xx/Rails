/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/BonusToken.java,v 1.1 2007/09/20 19:49:26 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

/**
 * A BaseToken object represents a token that a operating public company can
 * place on the map to act as a rail building and train running starting point.
 * <p>
 * The "Base" qualifier is used (more or less) consistently in this rails.game program
 * as it most closely the function of such a token: to act as a base from which a
 * company can operate.
 * Other names used in various games and discussions are "railhead", "station",
 * "garrison", or just "token". 
 * 
 * @author Erik Vos
 */
public class BonusToken extends Token {
    
    int value;
    String name;

    /**
     * Create a BonusToken.
     */
    public BonusToken() {
        super();
        setHolder (null);
    }
    
    public void setName (String name) {
    	this.name = name;
    }
    
    public void setValue(int value) {
    	this.value = value;
    }
    
    public boolean isPlaced () {
        return (holder instanceof Tile);
    }
    
    public String getName() {
        if (description.equals("")) {
        	description = name + " " + Bank.format(value)+" bonus token";
        }
        return description; 
    }
    
    public int getValue () {
        return value;
    }
    
}

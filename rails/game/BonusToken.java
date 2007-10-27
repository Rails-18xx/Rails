/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/BonusToken.java,v 1.2 2007/10/27 15:26:34 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import rails.util.Tag;
import rails.util.Util;

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
    
    public void configureFromXML(Tag tag) throws ConfigurationException
    {
        Tag bonusTokenTag = tag.getChild("BonusToken");
        if (bonusTokenTag == null)
        {
            throw new ConfigurationException("<BonusToken> tag missing");
        }
        value = bonusTokenTag.getAttributeAsInteger("value");
        if (value <= 0) {
            throw new ConfigurationException ("Missing or invalid value "+value);
        }

        name = bonusTokenTag.getAttributeAsString("name");
        if (!Util.hasValue(name)) {
            throw new ConfigurationException ("Bonus token must have a name");
        }
        description = name + " +" + Bank.format(value)+" bonus token";
    }
    
    public boolean isPlaced () {
        return (holder instanceof Tile);
    }
    
    public String getName() {
        return description; 
    }
    
    public int getValue () {
        return value;
    }
    
    public String toString() {
    	return description; 
    }
    
}

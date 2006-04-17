/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/special/Attic/SpecialORProperty.java,v 1.2 2006/04/17 14:17:05 evos Exp $
 * 
 * Created on 24-Nov-2005
 * Change Log:
 */
package game.special;

/**
 * @author Erik Vos
 */
public abstract class SpecialORProperty extends SpecialProperty {
    
    public SpecialORProperty () {
        super();
        isORProperty = true;
    }
    
    /** TODO not used yet */
    public boolean isExecutionable() {
        return true;
    }

}

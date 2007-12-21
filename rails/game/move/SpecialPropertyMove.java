/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/Attic/SpecialPropertyMove.java,v 1.1 2007/12/21 21:18:12 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.*;
import rails.game.special.SpecialPropertyI;

/**
 * @author Erik Vos
 */
public class SpecialPropertyMove extends Move {
    
    SpecialPropertyI specialProperty;
    SpecialPropertyHolderI from;
    SpecialPropertyHolderI to;
    
    /**
     * Create a generic SpecialPropertyMove object.
     * Any specific side effects must be implemented in the addToken 
     * and removeToken methods of the 'from' and 'to' TokenHolders.
     * <p>The parameter descriptions cover the usual case of a Base Token lay,
     * which is physically removed from a PublicCompany and added to a Station 
     * on a MapHex.
     * 
     * @param specialProperty The specialProperty to be moved (e.g. a BaseToken).
     * @param from Where the specialProperty is removed from (e.g. a PublicCompany charter).
     * @param to Where the specialProperty is moved to (e.g. a MapHex).
     */
            
    public SpecialPropertyMove (SpecialPropertyI specialProperty, SpecialPropertyHolderI from, SpecialPropertyHolderI to) {
        
        this.specialProperty = specialProperty;
        this.from = from;
        this.to = to;
        
        MoveSet.add (this);
    }


    public boolean execute() {

        return (from == null || from.removeSpecialProperty(specialProperty)) && to.addSpecialProperty(specialProperty);
    }

    public boolean undo() {
        
        return to.removeSpecialProperty(specialProperty) && (from == null ||from.addSpecialProperty(specialProperty));
    }
    
    public String toString() {
        if (specialProperty == null) log.error ("Token is null");
        if (from == null) log.warn ("From is null");
        if (to == null) log.error ("To is null");        
        return "SpecialPropertyMove: "+specialProperty.getName()
        	+ " from " + (from == null ? from : from.getName())
        	+ " to " + to.getName();
   }

}

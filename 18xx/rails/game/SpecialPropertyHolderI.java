/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Attic/SpecialPropertyHolderI.java,v 1.1 2007/12/21 21:18:12 evos Exp $ */
package rails.game;

import java.util.*;

import rails.game.special.SpecialPropertyI;

/**
 * Interface for implementing a SpecialPropertyHolder
 * 
 * A SpecialPropertyHolder is any object that can own
 * SpecialProperty objects.
 * Currently known cases: PrivateCompany and Portfolio.
 * Portfolios hold SpecialProperties that have been detached
 * from the private that originally held it, so that its
 * lifetime is no longer dependent on that of the private.
 */
public interface SpecialPropertyHolderI
{


	/** Add a special property. 
	 * Subclasses may override this method to implement side effects.
	 * @param property The SpecialProperty object to add.
	 * @return True if successful.
	 */
	public boolean addSpecialProperty (SpecialPropertyI property);
	
	/** Remove a token.
	 * Subclasses may override this method to implement side effects.
	 * @param token The token object to remove.
	 * @return True if successful.
	 */
	public boolean removeSpecialProperty (SpecialPropertyI property);

	/**
	 * @return ArrayList of all special properties we have.
	 */
	public List<SpecialPropertyI> getSpecialProperties();

    /**
	 * Do we have any special properties?
	 * 
	 * @return Boolean
	 */
	public boolean hasSpecialProperties();
    
    public String getName();
	
}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialProperty.java,v 1.10 2007/12/30 14:25:13 evos Exp $ */
package rails.game.special;

import java.util.HashMap;
import java.util.Map;

import rails.game.*;
import rails.game.move.MoveableHolderI;
import rails.game.state.BooleanState;
import rails.util.Tag;
import rails.util.Util;


public abstract class SpecialProperty implements SpecialPropertyI
{

	protected PrivateCompanyI privateCompany;
	protected int closingValue = 0;
	protected BooleanState exercised; 
	protected boolean usableIfOwnedByPlayer = false;
	protected boolean usableIfOwnedByCompany = false;
	protected boolean closeIfExercised = false;
	
	protected String conditionText = "";
	protected String whenText = "";
    protected String transferText = "";
	protected boolean isORProperty = false;
	protected boolean isSRProperty = false;
	
	protected int uniqueId;
	
	protected static Map<Integer, SpecialPropertyI> spMap
			= new HashMap<Integer, SpecialPropertyI> ();
	protected static int lastIndex = 0;
	
	public SpecialProperty () {
		uniqueId = ++lastIndex;
		spMap.put(uniqueId, this);
	}
	
	public void configureFromXML(Tag tag) throws ConfigurationException {
		
		closeIfExercised = tag.getAttributeAsBoolean(
				"closeIfExercised", closeIfExercised);
		
		conditionText = tag.getAttributeAsString("condition");
		if (!Util.hasValue(conditionText))
			throw new ConfigurationException("Missing condition in private special property");
		setUsableIfOwnedByPlayer(conditionText.matches("(?i).*ifOwnedByPlayer.*"));
		setUsableIfOwnedByCompany(conditionText.matches("(?i).*ifOwnedByCompany.*"));
		
		whenText = tag.getAttributeAsString("when");
		if (!Util.hasValue(whenText))
			throw new ConfigurationException("Missing condition in private special property");
		// to be interpreted...
        
        transferText = tag.getAttributeAsString("transfer", "");
	}
	
	public int getUniqueId () {
		return uniqueId;
	}
	
	public static SpecialPropertyI getByUniqueId (int i) {
		return spMap.get(i);
	}

	public void setCompany(PrivateCompanyI company)
	{
		this.privateCompany = company;
        exercised = new BooleanState(company.getName()+"_SP_"+uniqueId+"_Exercised", 
                false);
	}

	public PrivateCompanyI getCompany()
	{
		return privateCompany;
	}

    /**
     * @return Returns the usableIfOwnedByCompany.
     */
    public boolean isUsableIfOwnedByCompany() {
        return usableIfOwnedByCompany;
    }
    /**
     * @param usableIfOwnedByCompany The usableIfOwnedByCompany to set.
     */
    public void setUsableIfOwnedByCompany(boolean usableIfOwnedByCompany) {
        this.usableIfOwnedByCompany = usableIfOwnedByCompany;
    }
    /**
     * @return Returns the usableIfOwnedByPlayer.
     */
    public boolean isUsableIfOwnedByPlayer() {
        return usableIfOwnedByPlayer;
    }
    /**
     * @param usableIfOwnedByPlayer The usableIfOwnedByPlayer to set.
     */
    public void setUsableIfOwnedByPlayer(boolean usableIfOwnedByPlayer) {
        this.usableIfOwnedByPlayer = usableIfOwnedByPlayer;
    }
    
	public void setExercised()
	{
		exercised.set (true);
		if (this.closeIfExercised) {
			privateCompany.setClosed();
		}
	}

	public boolean isExercised()
	{
		return exercised.booleanValue();
	}

	public boolean closesIfExercised() {
		return closeIfExercised;
	}

	public int getClosingValue()
	{
		return closingValue;
	}

	public boolean isSRProperty()
	{
		return isSRProperty;
	}

	public boolean isORProperty()
	{
		return isORProperty;
	}
    
    public String getTransferText() {
        return transferText;
    }

    /** Stub for moving the special property to another holder.
     * Must be overridden by subsclasses that actually can be moved.
     */
    public void moveTo (MoveableHolderI newHolder) {
    }

	public String toString () {
	    return getClass().getSimpleName() + " of private " + privateCompany.getName();
	}
    
    /** Default menu item text, should be by all special properties
     * that can appear as a menu item */
    public String toMenu() {
        return toString();
    }
}

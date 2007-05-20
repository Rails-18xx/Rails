package rails.game.special;

import rails.game.*;
import rails.game.state.BooleanState;

public abstract class SpecialProperty implements SpecialPropertyI
{

	protected PrivateCompanyI privateCompany;
	protected int closingValue = 0;
	protected BooleanState exercised = new BooleanState("SpecialPropertyExercised", 
	        false);
	protected boolean isSRProperty = false;
	protected boolean isORProperty = false;
	protected boolean usableIfOwnedByPlayer = false;
	protected boolean usableIfOwnedByCompany = false;

	public void setCompany(PrivateCompanyI company)
	{
		this.privateCompany = company;
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
		privateCompany.getPortfolio().updateSpecialProperties();
	}

	public boolean isExercised()
	{
		return exercised.booleanValue();
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

	public String toString () {
	    return getClass().getName() + " of private " + privateCompany.getName();
	}
}

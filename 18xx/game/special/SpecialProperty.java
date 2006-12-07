package game.special;

import game.*;

public abstract class SpecialProperty implements SpecialPropertyI
{

	protected PrivateCompanyI privateCompany;
	protected int closingValue = 0;
	protected boolean exercised = false;
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
//        if (usableIfOwnedByCompany)
//System.out.println(privateCompany.getName()+" spec.prop. "+getClass().getName()+" is usable if owned by a Company");
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
//        if (usableIfOwnedByPlayer)
//System.out.println(privateCompany.getName()+" spec.prop. "+getClass().getName()+" is usable if owned by a Player");
    }
	public void setExercised()
	{
		exercised = true;
		privateCompany.getPortfolio().updateSpecialProperties();
	}

	public boolean isExercised()
	{
		return exercised;
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

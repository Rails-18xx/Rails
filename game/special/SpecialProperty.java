package game.special;

import game.*;

public abstract class SpecialProperty implements SpecialPropertyI
{

	String condition;
	PrivateCompanyI privateCompany;
	int closingValue = 0;
	boolean exercised = false;
	boolean isSRProperty = false;
	boolean isORProperty = false;

	public void setCondition(String condition)
	{
		this.condition = condition;
	}

	public String getCondition()
	{
		return condition;
	}

	public void setCompany(PrivateCompanyI company)
	{
		this.privateCompany = company;
	}

	public PrivateCompanyI getCompany()
	{
		return privateCompany;
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

}

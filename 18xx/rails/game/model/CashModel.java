package rails.game.model;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.PublicCompanyI;

public class CashModel extends ModelObject
{

	private int cash;
	private CashHolder owner;

	public CashModel(CashHolder owner)
	{
		cash = 0;
		this.owner = owner;
	}

	public void setCash(int newCash)
	{
		cash = newCash;
		notifyViewObjects();
	}

	public boolean addCash(int addedCash)
	{
		cash += addedCash;
		notifyViewObjects();
		
		if(cash <= 0)
			return false;
		else
			return true;
	}

	public int getCash()
	{
		return cash;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rails.rails.game.model.ModelObject#getValue()
	 */
	public String toString()
	{
		if (owner instanceof PublicCompanyI
				&& !((PublicCompanyI) owner).hasStarted())
		{
			return "";
		}
		return Bank.format(cash);
	}

}

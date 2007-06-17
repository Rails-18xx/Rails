package rails.game.model;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.PublicCompanyI;

public class CashModel extends ModelObject
{

	private int cash;
	private CashHolder owner;
	
	public static final int SUPPRESS_ZERO = 1;

	public CashModel(CashHolder owner)
	{
		cash = 0;
		this.owner = owner;
	}

	public void setCash(int newCash)
	{
		cash = newCash;
		update();
	}

	public boolean addCash(int addedCash)
	{
		cash += addedCash;
		update();
		
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
	public String getText()
	{
	    if (cash == 0 && (option & SUPPRESS_ZERO) > 0
	        || owner instanceof PublicCompanyI
				&& !((PublicCompanyI) owner).hasStarted())
		{
			return "";
		} else {
		    return Bank.format(cash);
		}
	}

}

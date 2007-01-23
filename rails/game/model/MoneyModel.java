package rails.game.model;

import rails.game.Bank;

public class MoneyModel extends ModelObject
{

	int amount = 0;
	boolean initialised = false;
	Object owner = null;

	public MoneyModel(Object owner)
	{
		this.owner = owner;
	}

	public void setAmount(int amount)
	{
		this.amount = amount;
		initialised = true;
		notifyViewObjects();
	}

	public int getAmount()
	{
		return amount;
	}

	public String toString()
	{

		return (initialised ? Bank.format(amount) : "");
	}

}

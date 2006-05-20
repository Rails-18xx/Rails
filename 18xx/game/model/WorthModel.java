package game.model;

import game.Bank;
import game.Player;

public class WorthModel extends ModelObject
{

	private Player owner;

	public WorthModel(Player owner)
	{
		this.owner = owner;
	}

	public String toString()
	{
		return Bank.format(owner.getWorth());
	}

}

package game.model;


import game.Player;

public class CertCountModel extends ModelObject
{

	private Player owner;

	public CertCountModel(Player owner)
	{
		this.owner = owner;
	}

	public String toString()
	{
		return ""+owner.getPortfolio().getNumberOfCountedCertificates();
	}

}

package rails.game.model;


import rails.game.Player;

public class CertCountModel extends ModelObject
{

	private Player owner;

	public CertCountModel(Player owner)
	{
		this.owner = owner;
	}

	public String getText()
	{
		return ""+owner.getPortfolio().getNumberOfCountedCertificates();
	}

}

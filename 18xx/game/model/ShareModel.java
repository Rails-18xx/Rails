package game.model;

import game.Player;
import game.Portfolio;
import game.PublicCompanyI;

public class ShareModel extends ModelObject
{

	private int share;
	private Portfolio portfolio;
	private PublicCompanyI company;
	private boolean markPresidency = false;

	public ShareModel(Portfolio portfolio, PublicCompanyI company)
	{
		this.portfolio = portfolio;
		this.company = company;
		this.share = 0;
		this.markPresidency = (portfolio.getOwner() instanceof Player);
	}

	public void setShare(int share)
	{
		this.share = share;
		notifyViewObjects();
	}

	public void setShare()
	{
		this.share = portfolio.ownsShare(company);
		notifyViewObjects();
	}

	public void addShare(int addedShare)
	{
		share += addedShare;
		notifyViewObjects();
	}

	public int getShare()
	{
		return share;
	}

	public String toString()
	{
		if (share == 0)
			return "";
		return share + "%"
			+ ((portfolio.getOwner() instanceof Player)
			        && company.getPresident() == portfolio.getOwner() ? "P" : "");
	}

}

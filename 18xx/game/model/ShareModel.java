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
		// System.out.println("SHAREMODEL.ShareModel share="+share);
	}

	public void setShare(int share)
	{
		this.share = share;
		// System.out.println("SHAREMODEL.setShare share="+share);
		notifyViewObjects();
	}

	public void setShare()
	{
		this.share = portfolio.ownsShare(company);
		// System.out.println("SHAREMODEL.setShare2 share="+share);
		notifyViewObjects();
	}

	public void addShare(int addedShare)
	{
		share += addedShare;
		// System.out.println("SHAREMODEL.addShare share="+share);
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
			+ (markPresidency && company.getPresident() == portfolio.getOwner() ? "P" : "");
	}

}

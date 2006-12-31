package game.model;

import game.PublicCompanyI;

public class BaseTokensModel extends ModelObject
{

	private PublicCompanyI company;

	public BaseTokensModel(PublicCompanyI company)
	{
		this.company = company;
	}

	public String toString()
	{
	    int maxTokens = company.getMaxCityTokens();
	    int freeTokens = company.getFreeBaseTokens();
	    if (maxTokens == 0) {
	        return "";
	    } else if (freeTokens == 0) {
	        return "-/" + maxTokens;
	    } else {
	        return freeTokens + "/" + maxTokens;
	    }
	}

}

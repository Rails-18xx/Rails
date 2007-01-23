package rails.game.model;

import rails.game.PublicCompanyI;

public class BaseTokensModel extends ModelObject
{

	private PublicCompanyI company;

	public BaseTokensModel(PublicCompanyI company)
	{
		this.company = company;
	}

	public String toString()
	{
	    int allTokens = company.getNumberOfBaseTokens();
	    int freeTokens = company.getNumberOfFreeBaseTokens();
	    if (allTokens == 0) {
	        return "";
	    } else if (freeTokens == 0) {
	        return "-/" + allTokens;
	    } else {
	        return freeTokens + "/" + allTokens;
	    }
	}

}

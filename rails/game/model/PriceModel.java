package rails.game.model;

import rails.game.Bank;
import rails.game.PublicCompanyI;
import rails.game.StockSpaceI;
import rails.game.move.PriceMove;
import rails.game.state.StateI;

public class PriceModel extends ModelObject implements StateI
{

	private StockSpaceI stockPrice = null;
	private PublicCompanyI company = null;
	private String name = null;

	public PriceModel(PublicCompanyI company, String name)
	{
		this.company = company;
		this.name = name;
	}

	public void setPrice(StockSpaceI price)
	{
	    new PriceMove (this, stockPrice, price);
	}

	public StockSpaceI getPrice()
	{
		return stockPrice;
	}
	
	public PublicCompanyI getCompany() {
	    return company;
	}
	
	public String getText()
	{
		if (stockPrice != null)
		{
			return Bank.format(stockPrice.getPrice()) + " ("
					+ stockPrice.getName() + ")";
		}
		return "";
	}
	
	// StateI required methods
	public Object getState() {
		return stockPrice;
	}

	public void setState(Object object) {
	    if (object == null) {
			stockPrice = null;
			notifyViewObjects();
		} else if (object instanceof StockSpaceI) {
		    stockPrice = (StockSpaceI) object;
			notifyViewObjects();
		} else {
			new Exception ("Incompatible object type "+object.getClass().getName()
					+ "passed to PriceModel "+name)
				.printStackTrace();
		}
	}

	public String getName() {
	    return name;
	}

}

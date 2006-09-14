package game.model;

import game.Bank;
import game.PublicCompanyI;
import game.StockSpaceI;
import game.move.MoveSet;
import game.move.PriceMove;
import game.state.StateI;

public class PriceModel extends ModelObject implements StateI
{

	private StockSpaceI stockPrice = null;
	private PublicCompanyI company = null;

	public PriceModel(PublicCompanyI company)
	{
		this.company = company;
	}

	public void setPrice(StockSpaceI price)
	{
	    MoveSet.add (new PriceMove (this, stockPrice, price));
	}

	public StockSpaceI getPrice()
	{
		return stockPrice;
	}
	
	public PublicCompanyI getCompany() {
	    return company;
	}

	public String toString()
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
					+ "passed to State wrapper for object type StockSpaceI")
				.printStackTrace();
		}
	}


}

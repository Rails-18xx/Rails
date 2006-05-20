package game.model;

import game.Bank;
import game.StockSpaceI;

public class PriceModel extends ModelObject
{

	private StockSpaceI stockPrice = null;

	public PriceModel(StockSpaceI price)
	{
		this.stockPrice = price;
	}

	public void setPrice(StockSpaceI price)
	{
		stockPrice = price;
		notifyViewObjects();
	}

	public StockSpaceI getPrice()
	{
		return stockPrice;
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

}

/*
 * Created on Feb 23, 2005
 *
 */
package game;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author a-blentz
 */
public class StockMarket {
	//int[][] stockChart; // Can possibly be replaced by:
	StockChart stockChart;
	ArrayList ipoPile;
	ArrayList companiesStarted;

	public StockMarket(String game) {

		stockChart = new StockChart(game);
	
	}

	/**
	 * @return Returns the companiesStarted.
	 */
	public ArrayList getCompaniesStarted() {
		return companiesStarted;
	}
	/**
	 * @param companiesStarted The companiesStarted to set.
	 */
	public void setCompaniesStarted(BigCompany companyStarted) {
		companiesStarted.add(companyStarted);
	}
	/**
	 * @return Returns the ipoPile.
	 */
	public ArrayList getIpoPile() {
		return ipoPile;
	}
	/**
	 * @param ipoPile The ipoPile to set.
	 */
	public void addShareToPile(Stock stock) {
		ipoPile.add(stock);
	}
	public Stock removeShareFromPile(Stock stock) {
		if (ipoPile.contains(stock)) {
			int index = ipoPile.lastIndexOf(stock);
			stock = (Stock) ipoPile.get(index);
			ipoPile.remove(index);
			return stock;
		} else {
			return null;
		}

	}
	
	/** @deprecated by EV: Have moved it to StockChart, and replaced by higher-level actions */
	public void moveChitUp(BigCompany company) {
		//if not at the top
		//move chit up
		//else
		//move chit forward
	}
	/** @deprecated by EV: Have moved it to StockChart, and replaced by higher-level actions */
	public void moveChitDown(BigCompany company) {
		//if not on a ledge
		//move chit down
		//
		//for games like 1870, we'll need to know if there's any values
		//below the ledge that can be moved down if the player sells >2
		//shares of a company's stock during his turn
		//or if the ledge is the end of the road.
	}
	/** @deprecated by EV: Have moved it to StockChart, and replaced by higher-level actions */
	public void moveChitForward(BigCompany company) {
		//if not on a ledge
		//move chit forward
		//else
		//move chit up
	}
	/** @deprecated by EV: Have moved it to StockChart, and replaced by higher-level actions */
	public void moveChitBackward(BigCompany company) {
		//if not on the edge (rare)
		//move chit back
		//else
		//move chit down
	}

	/** To be called if shares are sold. The number sold does not always matter! */
	public void sell (Company company, int number) {
		/* For now ignore the company. Will be dealt with in the next version. */
		stockChart.moveDown (company, number);
		
	}
	/** To be called if a company is sold out at the end of an SR */
	public void soldOut (Company company) {
		/* For now ignore the company. Will be dealt with in the next version. */
		stockChart.moveUp (company);
	}
	/** To be called on dividend payout. The amount may matter! */
	public void payout (Company company) {
		/* For now ignore the company. Will be dealt with in the next version. */
		stockChart.moveRightOrUp (company);
	}
	/** To be called if a complany withholds */
	public void withhold(Company company) {
		/* For now ignore the company. Will be dealt with in the next version. */
		stockChart.moveLeftOrDown (company);
	}
	
	/*--- Getters ---*/
	

	/**
	 * @return
	 */
	public StockChart getStockChart() {
		return stockChart;
	}

}

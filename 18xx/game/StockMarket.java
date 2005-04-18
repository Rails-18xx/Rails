/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.*;

import util.XmlUtils;

public class StockMarket implements StockMarketI, ConfigurableComponentI {
	
	protected HashMap stockSpaceTypes = new HashMap();
	
	protected StockSpace stockChart[][];
	protected HashMap stockChartSpaces = new HashMap();
	protected int numRows = 0;
	protected int numCols = 0;

	protected StockSpace currentSquare;
	protected ArrayList startSpaces = new ArrayList();

	/* Game-specific flags */
	protected boolean upOrDownRight = false; /* Sold out and at top: go down right (1870) */

	/* States */
	protected boolean gameOver = false; /* Some games have "game over" stockmarket squares */

	ArrayList ipoPile;

	ArrayList companiesStarted;

	public StockMarket() {
	}

	/**
		* @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
		*/
	public void configureFromXML(Element topElement) throws ConfigurationException {

		/* Read and configure the stock market space types */
		NodeList types = topElement.getElementsByTagName(StockSpaceTypeI.ELEMENT_ID);
		NodeList typeFlags;
		for (int i = 0; i < types.getLength(); i++) {
			Element typeElement = (Element) types.item(i);
			NamedNodeMap nnp = typeElement.getAttributes();
			
			/* Extract the attributes of the Stock space type */
			String name = XmlUtils.extractStringAttribute(nnp, StockSpaceTypeI.NAME_TAG);
			if (name == null) {
				throw new ConfigurationException("Unnamed stock space type found.");
			}
			String colour = XmlUtils.extractStringAttribute(nnp, StockSpaceTypeI.COLOUR_TAG);
			
			/* Check for duplicates */
			if (stockSpaceTypes.get(name) != null) {
				throw new ConfigurationException("Stock space type " + name + " configured twice");
			}
			
			/* Create the type */
			StockSpaceTypeI type = new StockSpaceType(name, colour);
			stockSpaceTypes.put(name, type);

			// Loop through the stock space type flags
			typeFlags = typeElement.getChildNodes();
			
			for (int j = 0; j < typeFlags.getLength(); j++) {

				String flagName = typeFlags.item(j).getLocalName();
				if (flagName == null)
					continue;

				if (flagName.equalsIgnoreCase(StockSpaceTypeI.NO_BUY_LIMIT_TAG)) {
					type.setNoBuyLimit(true);
				} else if (flagName.equalsIgnoreCase(StockSpaceTypeI.NO_CERT_LIMIT_TAG)) {
					type.setNoCertLimit(true);
				} else if (flagName.equalsIgnoreCase(StockSpaceTypeI.NO_HOLD_LIMIT_TAG)) {
					type.setNoHoldLimit(true);
				}
			}
		}
		
		/* Read and configure the stock market spaces */
		NodeList spaces = topElement.getElementsByTagName(StockSpaceI.ELEMENT_ID);
		NodeList spaceFlags;
		StockSpaceTypeI type;
		int row, col;
		for (int i = 0; i < spaces.getLength(); i++) {
			Element spaceElement = (Element) spaces.item(i);
			NamedNodeMap nnp = spaceElement.getAttributes();
			type = null;

			//Extract the attributes of the Stock space
			String name = XmlUtils.extractStringAttribute(nnp, StockSpaceI.NAME_TAG);
			if (name == null) {
				throw new ConfigurationException("Unnamed stock space found.");
			}
			String price = XmlUtils.extractStringAttribute(nnp, StockSpaceI.PRICE_TAG);
			if (price == null) {
				throw new ConfigurationException("Stock space " + name + " has no price defined.");
			}
			String typeName = XmlUtils.extractStringAttribute(nnp, StockSpaceI.TYPE_TAG);
			if (typeName != null && (type = (StockSpaceTypeI)stockSpaceTypes.get(typeName)) == null) {
				throw new ConfigurationException("Stock space type " + type + " is undefined.");
			}

			if (stockChartSpaces.get(name) != null) {
				throw new ConfigurationException("Stock space " + name + " configured twice");
			}

			StockSpaceI space = new StockSpace(name, Integer.parseInt(price), type);
			stockChartSpaces.put(name, space);

			row = Integer.parseInt(name.substring(1));
			col = (int) (name.toUpperCase().charAt(0) - '@');
			if (row > numRows)
				numRows = row;
			if (col > numCols)
				numCols = col;

			// Loop through the stock space flags
			spaceFlags = spaceElement.getChildNodes();
			
			for (int j = 0; j < spaceFlags.getLength(); j++) {

				String flagName = spaceFlags.item(j).getLocalName();
				if (flagName == null)
					continue;

				if (flagName.equalsIgnoreCase(StockSpaceI.START_SPACE_TAG)) {
					space.setStart(true);
					startSpaces.add(space);
				} else if (flagName.equalsIgnoreCase(StockSpaceI.CLOSES_COMPANY_TAG)) {
					space.setClosesCompany(true);
				} else if (flagName.equalsIgnoreCase(StockSpaceI.GAME_OVER_TAG)) {
					space.setEndsGame(true);
				} else if (flagName.equalsIgnoreCase(StockSpaceI.BELOW_LEDGE_TAG)) {
					space.setBelowLedge(true);
				} else if (flagName.equalsIgnoreCase(StockSpaceI.LEFT_OF_LEDGE_TAG)) {
					space.setLeftOfLedge(true);
				}
			}

		}

		stockChart = new StockSpace[numRows][numCols];
		Iterator it = stockChartSpaces.values().iterator();
		StockSpace space;
		while (it.hasNext()) {
			space = (StockSpace) it.next();
			stockChart[space.getRow()][space.getColumn()] = space;
		}
		
		if (topElement.getElementsByTagName("UpOrDownRight").getLength() > 0) {
			upOrDownRight = true;
		}

	}

	/**
		* @return
		*/
	public StockSpace[][] getStockChart() {
		return stockChart;
	}

	public StockSpace getStockSpace(int row, int col) {
		if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
			return stockChart[row][col];
		} else {
			return null;
		}
	}

	/*--- Actions ---*/

	public void payOut(PublicCompanyI company) {
		moveRightOrUp(company);
	}
	public void withhold(PublicCompanyI company) {
		moveLeftOrDown(company);
	}
	public void sell(PublicCompanyI company, int numberOfSpaces) {
		moveDown(company, numberOfSpaces);
	}
	public void soldOut(PublicCompanyI company) {
		moveUp(company);
	}

	protected void moveUp(PublicCompanyI company) {
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = null;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (row > 0) {
			newsquare = getStockSpace(row - 1, col);
		} else if (upOrDownRight && col < numCols - 1) {
			newsquare = getStockSpace(row + 1, col + 1);
		}
		processMove(company, oldsquare, newsquare);
	}

	protected void moveDown(PublicCompanyI company, int numberOfSpaces) {
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = null;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();

		/* Drop the indicated number of rows */
		int newrow = row + numberOfSpaces;

		/* Don't drop below the bottom of the chart */
		while (newrow >= numRows || getStockSpace(newrow, col) == null)
			newrow--;

		/* If marker landed just below a ledge, and NOT because it was bounced by the 
			* bottom of the chart, it will stay just above the ledge.
			*/
		if (getStockSpace(newrow, col).isBelowLedge() && newrow == row + numberOfSpaces)
			newrow--;

		if (newrow > row) {
			newsquare = getStockSpace(newrow, col);
		}
		if (newsquare != null && newsquare.closesCompany()) {
			company.setClosed(true);
			oldsquare.removeToken(company);
			Log.write(company.getName() + " closes at " + newsquare.getName());
		} else {
			processMove(company, oldsquare, newsquare);
		}
	}

	protected void moveRightOrUp(PublicCompanyI company) {
		/* Ignore the amount for now */
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = null;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (col < numCols - 1
			&& !oldsquare.isLeftOfLedge()
			&& (newsquare = getStockSpace(row, col + 1)) != null) {
		} else if (row > 0 && (newsquare = getStockSpace(row - 1, col)) != null) {
		}
		processMove(company, oldsquare, newsquare);
	}

	protected void moveLeftOrDown(PublicCompanyI company) {
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = null;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (col > 0 && (newsquare = getStockSpace(row, col - 1)) != null) {
		} else if (row < numRows - 1 && (newsquare = getStockSpace(row + 1, col)) != null) {
		}
		if (newsquare != null && newsquare.closesCompany()) {
			company.setClosed(true);
			oldsquare.removeToken(company);
			Log.write(company.getName() + " closes at " + newsquare.getName());
		} else {
			processMove(company, oldsquare, newsquare);
		}
	}

	protected void processMove(PublicCompanyI company, StockSpaceI from, StockSpaceI to) {
		// To be written to a log file in the future.
		if (to == null || from == to) {
			Log.write(company.getName() + " stays at " + from.getName());
		} else {
			from.removeToken(company);
			to.addToken(company);
			company.setCurrentPrice(to);
			Log.write(
				company.getName() + " moved from " + from.getName() + " to " + to.getName());

			/* Check for game closure */
			if (to.endsGame()) {
				Log.write("Game over!");
				gameOver = true;
			}

		}
	}

	/**
		* @return
		*/
	public List getStartSpaces() {
		return startSpaces;
	}
	
	public StockSpaceI getStartSpace (int price) {
		Iterator it = startSpaces.iterator();
		StockSpaceI square;
		while (it.hasNext()) {
			square = ((StockSpaceI)it.next());
			if (square.getPrice() == price) return square; 
		}
		return null;
	}

	/**
		* @return
		*/
	public boolean isGameOver() {
		return gameOver;
	}

	/* Brett's original code */

	/**
	 * @return Returns the companiesStarted.
	 */
	public ArrayList getCompaniesStarted() {
		return companiesStarted;
	}

	/**
	 * @param companiesStarted
	 *           The companiesStarted to set.
	 */
	public void setCompaniesStarted(PublicCompany companyStarted) {
		companiesStarted.add(companyStarted);
	}

	/**
	 * @return Returns the ipoPile.
	 */
	public ArrayList getIpoPile() {
		return ipoPile;
	}

	/**
	 * @param ipoPile
	 *           The ipoPile to set.
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

	/**
	 * @return
	 */
	public int getNumberOfColumns() {
		return numCols;
	}

	/**
	 * @return
	 */
	public int getNumberOfRows() {
		return numRows;
	}

}
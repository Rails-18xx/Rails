package game;

import game.move.MoveSet;
import game.move.PriceMove;
import game.move.PriceTokenMove;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.*;
import util.*;

public class StockMarket implements StockMarketI, ConfigurableComponentI
{

	protected HashMap stockSpaceTypes = new HashMap();
	protected HashMap stockChartSpaces = new HashMap();
	protected StockSpace stockChart[][];
	protected StockSpace currentSquare;
	protected int numRows = 0;
	protected int numCols = 0;
	protected ArrayList startSpaces = new ArrayList();
	protected int[] startPrices;

	protected static StockMarketI instance;

	/* Game-specific flags */
	protected boolean upOrDownRight = false; /*
												 * Sold out and at top: go down
												 * right (1870)
												 */

	/* States */
	protected boolean gameOver = false; /*
										 * Some games have "game over"
										 * stockmarket squares
										 */

	ArrayList ipoPile;

	ArrayList companiesStarted;

	public StockMarket()
	{
		instance = this;
	}

	public static StockMarketI getInstance()
	{
		return instance;
	}

	/**
	 * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Element topElement)
			throws ConfigurationException
	{

		/* Read and configure the stock market space types */
		NodeList types = topElement.getElementsByTagName(StockSpaceTypeI.ELEMENT_ID);
		NodeList typeFlags;

		for (int i = 0; i < types.getLength(); i++)
		{
			Element typeElement = (Element) types.item(i);
			NamedNodeMap nnp = typeElement.getAttributes();

			/* Extract the attributes of the Stock space type */
			String name = XmlUtils.extractStringAttribute(nnp,
					StockSpaceTypeI.NAME_TAG);
			if (name == null)
			{
				throw new ConfigurationException(LocalText.getText("UnnamedStockSpaceType"));
			}
			String colour = XmlUtils.extractStringAttribute(nnp,
					StockSpaceTypeI.COLOUR_TAG);

			/* Check for duplicates */
			if (stockSpaceTypes.get(name) != null)
			{
				throw new ConfigurationException(LocalText.getText("StockSpaceType1")
						+ name + LocalText.getText("ConfiguredTwice2"));
			}

			/* Create the type */
			StockSpaceTypeI type = new StockSpaceType(name, colour);
			stockSpaceTypes.put(name, type);

			// Loop through the stock space type flags
			typeFlags = typeElement.getChildNodes();

			for (int j = 0; j < typeFlags.getLength(); j++)
			{

				String flagName = typeFlags.item(j).getLocalName();
				if (flagName == null)
					continue;

				if (flagName.equalsIgnoreCase(StockSpaceTypeI.NO_BUY_LIMIT_TAG))
				{
					type.setNoBuyLimit(true);
				}
				else if (flagName.equalsIgnoreCase(StockSpaceTypeI.NO_CERT_LIMIT_TAG))
				{
					type.setNoCertLimit(true);
				}
				else if (flagName.equalsIgnoreCase(StockSpaceTypeI.NO_HOLD_LIMIT_TAG))
				{
					type.setNoHoldLimit(true);
				}
			}
		}

		/* Read and configure the stock market spaces */
		NodeList spaces = topElement.getElementsByTagName(StockSpaceI.ELEMENT_ID);
		NodeList spaceFlags;
		StockSpaceTypeI type;
		int row, col;
		for (int i = 0; i < spaces.getLength(); i++)
		{
			Element spaceElement = (Element) spaces.item(i);
			NamedNodeMap nnp = spaceElement.getAttributes();
			type = null;

			// Extract the attributes of the Stock space
			String name = XmlUtils.extractStringAttribute(nnp,
					StockSpaceI.NAME_TAG);
			if (name == null)
			{
				throw new ConfigurationException(LocalText.getText("UnnamedStockSpace"));
			}
			String price = XmlUtils.extractStringAttribute(nnp,
					StockSpaceI.PRICE_TAG);
			if (price == null)
			{
				throw new ConfigurationException(LocalText.getText("StockSpace1") + " " + name
						+ LocalText.getText("NoPrice2"));
			}
			String typeName = XmlUtils.extractStringAttribute(nnp,
					StockSpaceI.TYPE_TAG);
			if (typeName != null
					&& (type = (StockSpaceTypeI) stockSpaceTypes.get(typeName)) == null)
			{
				throw new ConfigurationException(LocalText.getText("StockSpaceType1") + " " + type
						+ LocalText.getText("IsUndefined2"));
			}

			if (stockChartSpaces.get(name) != null)
			{
				throw new ConfigurationException(LocalText.getText("StockSpace1") + name
						+ LocalText.getText("ConfiguredTwice2"));
			}

			StockSpaceI space = new StockSpace(name,
					Integer.parseInt(price),
					type);
			stockChartSpaces.put(name, space);

			row = Integer.parseInt(name.substring(1));
			col = (int) (name.toUpperCase().charAt(0) - '@');
			if (row > numRows)
				numRows = row;
			if (col > numCols)
				numCols = col;

			// Loop through the stock space flags
			spaceFlags = spaceElement.getChildNodes();

			for (int j = 0; j < spaceFlags.getLength(); j++)
			{

				String flagName = spaceFlags.item(j).getLocalName();
				if (flagName == null)
					continue;

				if (flagName.equalsIgnoreCase(StockSpaceI.START_SPACE_TAG))
				{
					space.setStart(true);
					startSpaces.add(space);
				}
				else if (flagName.equalsIgnoreCase(StockSpaceI.CLOSES_COMPANY_TAG))
				{
					space.setClosesCompany(true);
				}
				else if (flagName.equalsIgnoreCase(StockSpaceI.GAME_OVER_TAG))
				{
					space.setEndsGame(true);
				}
				else if (flagName.equalsIgnoreCase(StockSpaceI.BELOW_LEDGE_TAG))
				{
					space.setBelowLedge(true);
				}
				else if (flagName.equalsIgnoreCase(StockSpaceI.LEFT_OF_LEDGE_TAG))
				{
					space.setLeftOfLedge(true);
				}
			}

		}

		startPrices = new int[startSpaces.size()];
		for (int i = 0; i < startPrices.length; i++)
		{
			startPrices[i] = ((StockSpaceI) startSpaces.get(i)).getPrice();
		}

		stockChart = new StockSpace[numRows][numCols];
		Iterator it = stockChartSpaces.values().iterator();
		StockSpace space;
		while (it.hasNext())
		{
			space = (StockSpace) it.next();
			stockChart[space.getRow()][space.getColumn()] = space;
		}

		if (topElement.getElementsByTagName("UpOrDownRight").getLength() > 0)
		{
			upOrDownRight = true;
		}

	}

	/**
	 * Final initialisations, to be called after all XML processing is complete.
	 * The purpose is to register fixed company start prices.
	 */
	public void init()
	{

		Iterator it = Game.getCompanyManager()
				.getAllPublicCompanies()
				.iterator();
		PublicCompanyI comp;
		StockSpaceI space;
		while (it.hasNext())
		{
			comp = (PublicCompanyI) it.next();
			if (!comp.hasStarted() && comp.getParPrice() != null)
			{
				comp.getParPrice().addFixedStartPrice(comp);
			}
		}

	}

	/**
	 * @return
	 */
	public StockSpace[][] getStockChart()
	{
		return stockChart;
	}

	public StockSpace getStockSpace(int row, int col)
	{
		if (row >= 0 && row < numRows && col >= 0 && col < numCols)
		{
			return stockChart[row][col];
		}
		else
		{
			return null;
		}
	}

	public StockSpace getStockSpace(String name)
	{
		return (StockSpace) stockChartSpaces.get(name);
	}

	/*--- Actions ---*/
	
	public void start (PublicCompanyI company, StockSpaceI price) {
	    prepareMove (company, null, price);
	}

	public void payOut(PublicCompanyI company)
	{
		moveRightOrUp(company);
	}

	public void withhold(PublicCompanyI company)
	{
		moveLeftOrDown(company);
	}

	public void sell(PublicCompanyI company, int numberOfSpaces)
	{
		moveDown(company, numberOfSpaces);
	}

	public void soldOut(PublicCompanyI company)
	{
		moveUp(company);
	}

	protected void moveUp(PublicCompanyI company)
	{
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = oldsquare;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (row > 0)
		{
			newsquare = getStockSpace(row - 1, col);
		}
		else if (upOrDownRight && col < numCols - 1)
		{
			newsquare = getStockSpace(row + 1, col + 1);
		}
		prepareMove(company, oldsquare, newsquare);
	}

	protected void moveDown(PublicCompanyI company, int numberOfSpaces)
	{
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = oldsquare;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();

		/* Drop the indicated number of rows */
		int newrow = row + numberOfSpaces;

		/* Don't drop below the bottom of the chart */
		while (newrow >= numRows || getStockSpace(newrow, col) == null)
			newrow--;

		/*
		 * If marker landed just below a ledge, and NOT because it was bounced
		 * by the bottom of the chart, it will stay just above the ledge.
		 */
		if (getStockSpace(newrow, col).isBelowLedge()
				&& newrow == row + numberOfSpaces)
			newrow--;

		if (newrow > row)
		{
			newsquare = getStockSpace(newrow, col);
		}
		if (newsquare != oldsquare && newsquare.closesCompany())
		{
			company.setClosed();
			oldsquare.removeToken(company);
			Log.write(company.getName() + " closes at " + newsquare.getName());
		}
		else
		{
		    prepareMove(company, oldsquare, newsquare);
		}
	}

	protected void moveRightOrUp(PublicCompanyI company)
	{
		/* Ignore the amount for now */
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = oldsquare;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (col < numCols - 1 && !oldsquare.isLeftOfLedge()
				&& (newsquare = getStockSpace(row, col + 1)) != null)
		{
		}
		else if (row > 0 && (newsquare = getStockSpace(row - 1, col)) != null)
		{
		}
		prepareMove(company, oldsquare, newsquare);
	}

	protected void moveLeftOrDown(PublicCompanyI company)
	{
		StockSpaceI oldsquare = company.getCurrentPrice();
		StockSpaceI newsquare = oldsquare;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (col > 0 && (newsquare = getStockSpace(row, col - 1)) != null)
		{
		}
		else if (row < numRows - 1
				&& (newsquare = getStockSpace(row + 1, col)) != null)
		{
		}
		if (newsquare != oldsquare && newsquare.closesCompany())
		{
			company.setClosed();
			oldsquare.removeToken(company);
			Log.write(company.getName() + LocalText.getText("CLOSES_AT") + " " + newsquare.getName());
		}
		else
		{
			prepareMove(company, oldsquare, newsquare);
		}
	}
	
	protected void prepareMove (PublicCompanyI company,
	        StockSpaceI from, StockSpaceI to) {
		// To be written to a log file in the future.
		if (from != null && from == to)
		{
			Log.write(LocalText.getText("PRICE_STAYS_LOG", new String[] {
			        company.getName(),
			        Bank.format(from.getPrice()),
			        from.getName()
			}));
			return;
		}
		else if (from == null && to != null)
		{
		}
		else if (from != null && to != null)
		{
			Log.write (LocalText.getText("PRICE_MOVES_LOG", new String[] {
			        company.getName(),
			        Bank.format(from.getPrice()),
			        from.getName(),
			        Bank.format(to.getPrice()),
			        to.getName()
			}));

			/* Check for game closure */
			if (to.endsGame())
			{
				Log.write(LocalText.getText("GAME_OVER"));
				gameOver = true;
			}

		}
		company.setCurrentPrice(to);
		MoveSet.add (new PriceTokenMove (company, from, to));
	}

	public void processMove(PublicCompanyI company, StockSpaceI from,
			StockSpaceI to)
	{
		// To be written to a log file in the future.
		if (from != null) from.removeToken(company);
		if (to != null) to.addToken(company);
		//company.getCurrentPriceModel().setState(to);
	}

	/**
	 * @return
	 */
	public List getStartSpaces()
	{
		return startSpaces;
	}

	/**
	 * Return start prices as an int array. Note: this array is NOT sorted.
	 * 
	 * @return
	 */
	public int[] getStartPrices()
	{
		return startPrices;
	}

	public StockSpaceI getStartSpace(int price)
	{
		Iterator it = startSpaces.iterator();
		StockSpaceI square;
		while (it.hasNext())
		{
			square = ((StockSpaceI) it.next());
			if (square.getPrice() == price)
				return square;
		}
		return null;
	}

	/**
	 * @return
	 */
	public boolean isGameOver()
	{
		return gameOver;
	}

	/* Brett's original code */

	/**
	 * @return Returns the companiesStarted.
	 */
	public ArrayList getCompaniesStarted()
	{
		return companiesStarted;
	}

	/**
	 * @param companiesStarted
	 *            The companiesStarted to set.
	 */
	public void setCompaniesStarted(PublicCompany companyStarted)
	{
		companiesStarted.add(companyStarted);
	}

	/**
	 * @return Returns the ipoPile.
	 */
	public ArrayList getIpoPile()
	{
		return ipoPile;
	}

	/**
	 * @param ipoPile
	 *            The ipoPile to set.
	 */
	public void addShareToPile(PublicCertificate stock)
	{
		ipoPile.add(stock);
	}

	public PublicCertificate removeShareFromPile(PublicCertificate stock)
	{
		if (ipoPile.contains(stock))
		{
			int index = ipoPile.lastIndexOf(stock);
			stock = (PublicCertificate) ipoPile.get(index);
			ipoPile.remove(index);
			return stock;
		}
		else
		{
			return null;
		}

	}

	/**
	 * @return
	 */
	public int getNumberOfColumns()
	{
		return numCols;
	}

	/**
	 * @return
	 */
	public int getNumberOfRows()
	{
		return numRows;
	}

}

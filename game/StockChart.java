/*
 * Created on 25-Feb-2005
 */
package game;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.io.*;

/**
 * @author Erik
 */
public class StockChart extends DefaultHandler {
	
	private StockPrice stockChart[][];
	private HashMap stockChartSquares = new HashMap(); 
	private int numRows = 0;
	private int numCols = 0;
	
	private String lastName; /* Probably for testing only */

	/** Location on the Stock Chart per company */
	/* For now we will use the company name as a key, but that will become the Company object later on.*/
	HashMap stockPricePerCompany = new HashMap();

	private static ClassLoader classLoader =
		StockChart.class.getClassLoader();

	public StockChart(String game) {
		System.setProperty(
			"org.xml.sax.driver",
			"org.apache.crimson.parser.XMLReaderImpl");
		loadMarket("1830");
	}

	/**
	  * Load the XML file that defines the stock market.
	  * @param game Name of the game (e.g. "1830")
	  */
	public void loadMarket(String game) {

		String file = new String (game + "market.xml");
		
		try {
			XMLReader xr = XMLReaderFactory.createXMLReader();

			xr.setContentHandler(this);
			xr.setErrorHandler(this);
			InputStream i = classLoader.getResourceAsStream(file);
			if (i == null)
				throw new Exception("Stock market file "+file+ " not found");
			xr.parse(new InputSource(i));
			System.out.println(
				"Stock market file " + file + " read successfully");
		} catch (Exception e) {
			System.err.println(
				"Exception catched while parsing the stock market file");
			e.printStackTrace(System.err);
		}

	}
	
	/* SAX parser callback methods */
	/**
	 * DefaultHandler callback method.
	 */
	public void startDocument () {
		System.out.println("Start of reading the stock market file");
	}

	/**
	 * DefaultHandler callback method.
	 */
	public void endDocument () {
		System.out.println("End of reading the stock market file");
	}

	/**
	 * DefaultHandler callback method.
	 */
	public void startElement (String uri, String name,
				  String qName, Attributes atts) 
	{
		String qname;
		String location = null;
		int price = 0;
		int colour = 0;
		int index;
		int length;
		StockPrice square;
		int row, column;
		
		if ("".equals (uri)) {
			if (qName.equals("stockmarket")) {
				;
			} else if (qName.equals("square")) {
				length = atts.getLength();
				for (index=0; index < length; index++) {
					qname = atts.getQName(index);
					if (qname.equals("name")) {
						location = atts.getValue(index);
					} else if (qname.equals("price")) {
						price = Integer.parseInt(atts.getValue(index));
					} else if (qname.equals("colour")) {
						colour = Integer.parseInt(atts.getValue(index));
					} else {
						System.err.println("Unknown attribute: {" + uri + "}" + qname);
					}
				}
				if (stockChartSquares.containsKey(location)) {
					System.err.println ("STOCKMARKET ERROR: Duplicate lcoation definition ignored: "
						+ location);
				} else {
					square = new StockPrice (location, price, colour);
					row = Integer.parseInt(location.substring(1));
					column = (int)(location.toUpperCase().charAt(0) - '@');
					stockChartSquares.put(location, square);
					if (row > numRows) numRows = row;
					if (column > numCols) numCols = column;
				}
			} else {
				System.err.println("Unknown start element: " + name);
			}
		} else {
			System.err.println("Unknown start element: {" + uri + "}" + name);
		}
	}
	
	/**
	 * DefaultHandler callback method.
	 */
	public void endElement (String uri, String name, String qName) 
	{
		StockPrice square;
		if ("".equals (uri)) {
			if (qName.equals("stockmarket")) {
				/* Create the 2-dimensional array. 
				 * One extra row and column to facilitate border checking. May be unneeded. */
				stockChart = new StockPrice[numRows+1][numCols+1];
				Iterator it = stockChartSquares.values().iterator();
				while (it.hasNext()) {
					square = (StockPrice) it.next()	;
					stockChart[square.getRow()][square.getColumn()] = square;
				}
			} else if (qName.equals("square")) {
			} else {
				System.err.println("Unknown end element: " + name);
			}
		} else {
			System.out.println("Unknown end element:   {" + uri + "}" + name);
		}
	}

	/**
	 * DefaultHandler callback method.
	 */
	public void characters (char ch[], int start, int length) {
	}


	/*--- Getters ---*/
	/**
	 * @return
	 */
	public int getNumCols() {
		return numCols;
	}

	/**
	 * @return
	 */
	public int getNumRows() {
		return numRows;
	}

	/**
	 * @return
	 */
	public StockPrice[][] getStockChart() {
		return stockChart;
	}
	
	public StockPrice getStockPrice (int row, int col) {
		if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
			return stockChart[row][col];
		} else {
			return null;
		}
	}
	
	public StockPrice getStockPrice (String companyName) {
		return (StockPrice) stockPricePerCompany.get(companyName);
	}
	/*--- Actions ---*/
	
	public void startCompany (String name, int row, int col) {
		/* Name will probably be replaced by a Company object */
		stockPricePerCompany.put(name, getStockPrice(row, col));
	  	lastName = name; /* Just to fix the name for this test */
	}
	
	public void moveUp()
	 {
	 	StockPrice oldsquare = (StockPrice) stockPricePerCompany.get(lastName);
	 	int row = oldsquare.getRow();
	 	int col = oldsquare.getColumn();
	 	if (row > 0) {
	 		StockPrice newsquare = getStockPrice(row-1, col);
	 		if (newsquare != null) stockPricePerCompany.put(lastName, newsquare);
	 		System.out.println (lastName+" moved from "+row+","+col+" to "+(row-1)+","+col);
	 	}
	 }
	 public void moveDown(int number)
	 {
		StockPrice oldsquare = (StockPrice) stockPricePerCompany.get(lastName);
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		int newrow = row + number;
		while (newrow >= numRows || getStockPrice (newrow, col) == null) newrow--;
		if (newrow > row) {
			StockPrice newsquare = getStockPrice(newrow, col);
			stockPricePerCompany.put(lastName, newsquare);
			System.out.println (lastName+" moved from "+row+","+col+" to "+newrow+","+col);
		}
	 }
	 public void moveRightOrUp(int amount)
	 {
	 	/* Ignore the amount for now */
		StockPrice oldsquare = (StockPrice) stockPricePerCompany.get(lastName);
		StockPrice newsquare;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (col < numCols-1 && (newsquare = getStockPrice(row, col+1)) != null) {
			stockPricePerCompany.put(lastName, newsquare);
			System.out.println (lastName+" moved from "+row+","+col+" to "+row+","+(col+1));
		} else if (row > 0 && (newsquare = getStockPrice(row-1, col)) != null) {
			stockPricePerCompany.put(lastName, newsquare);
			System.out.println (lastName+" moved from "+row+","+col+" to "+(row-1)+","+col);
		}
	 	
	 }
	 public void moveLeftOrDown()
	 {
		StockPrice oldsquare = (StockPrice) stockPricePerCompany.get("PRR");
		StockPrice newsquare;
		int row = oldsquare.getRow();
		int col = oldsquare.getColumn();
		if (col > 0 && (newsquare = getStockPrice(row, col-1)) != null) {
			stockPricePerCompany.put(lastName, newsquare);
			System.out.println (lastName+" moved from "+row+","+col+" to "+row+","+(col-1));
		} else if (row < numRows - 1 && (newsquare = getStockPrice(row+1, col)) != null) {
			stockPricePerCompany.put(lastName, newsquare);
			System.out.println (lastName+" moved from "+row+","+col+" to "+(row+1)+","+col);
		}
	 }
 

}

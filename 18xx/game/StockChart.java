/*
 * Created on 25-Feb-2005
 * Changes: 
 * 04mar2005 EV: Implemented ledge.
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
public class StockChart extends DefaultHandler
{

   protected StockPrice stockChart[][];
   protected HashMap stockChartSquares = new HashMap();
   protected int numRows = 0;
   protected int numCols = 0;

   protected StockPrice currentSquare;
   protected ArrayList startSpaces = new ArrayList();

   /* Game-specific flags */
   protected boolean upOrDownRight = false; /*
                                             * Sold out and at top: go down
                                             * right (1870)
                                             */

   private static ClassLoader classLoader = StockChart.class.getClassLoader();

   /* Preferred Constructor */
   public StockChart(String game)
   {
      System.setProperty("org.xml.sax.driver",
            "org.apache.crimson.parser.XMLReaderImpl");
      loadMarket(game);
   }

   /* Default Constructor */
   public StockChart()
   {
      this("1830");
   }

   /**
    * Load the XML file that defines the stock market.
    * 
    * @param game
    *           Name of the game (e.g. "1830")
    */
   public void loadMarket(String game)
   {

      String file = new String("data/" + game + "/" + game + "market.xml");

      try
      {
         XMLReader xr = XMLReaderFactory.createXMLReader();

         xr.setContentHandler(this);
         xr.setErrorHandler(this);
         InputStream i = classLoader.getResourceAsStream(file);
         if (i == null)
            throw new Exception("Stock market file " + file + " not found");
         xr.parse(new InputSource(i));
         System.out.println("Stock market file " + file + " read successfully");
      } catch (Exception e)
      {
         System.err
               .println("Exception caught while parsing the stock market file");
         e.printStackTrace(System.err);
      }

   }

   /* SAX parser callback methods */
   /**
    * DefaultHandler callback method.
    */
   public void startDocument()
   {
      System.out.println("Start of reading the stock market file");
   }

   /**
    * DefaultHandler callback method.
    */
   public void endDocument()
   {
      System.out.println("End of reading the stock market file");
   }

   /**
    * DefaultHandler callback method.
    */
   public void startElement(String uri, String name, String qName,
         Attributes atts)
   {
      String qname;
      String location = null;
      String colour = null;
      int price = 0;
      boolean belowLedge = false;
      boolean closesCompany = false;
      boolean endsGame = false;

      int index, i;
      int length;
      StockPrice square = null;
      int row, column;

      if ("".equals(uri))
      {
         if (qName.equals("stockmarket"))
         {
            // Ignore type for now, we only consider rectangular stockmarkets
            // yet.
            ;
         } else if (qName.equals("upOrDownRight"))
         {
            upOrDownRight = true;
         } else if (qName.equals("square"))
         {
            length = atts.getLength();
            for (index = 0; index < length; index++)
            {
               qname = atts.getQName(index);
               if (qname.equals("name"))
               {
                  location = atts.getValue(index);
               } else if (qname.equals("price"))
               {
                  price = Integer.parseInt(atts.getValue(index));
               } else if (qname.equals("colour"))
               {
                        colour = atts.getValue(index);
               } else
               {
                  System.err
                        .println("Unknown attribute: {" + uri + "}" + qname);
               }
            }
            if (stockChartSquares.containsKey(location))
            {
               System.err
                     .println("STOCKMARKET ERROR: Duplicate location definition ignored: "
                           + location);
            } else
            {
               currentSquare = square = new StockPrice(location, price, colour);
               row = Integer.parseInt(location.substring(1));
               column = (int) (location.toUpperCase().charAt(0) - '@');
               stockChartSquares.put(location, square);
               if (row > numRows)
                  numRows = row;
               if (column > numCols)
                  numCols = column;
            }
         } else if (qName.equals("belowLedge"))
         {
            if (currentSquare != null)
               currentSquare.setBelowLedge(true);
         } else if (qName.equals("leftOfLedge"))
         {
            if (currentSquare != null)
               currentSquare.setLeftOfLedge(true);
         } else if (qName.equals("closesCompany"))
         {
            if (currentSquare != null)
               currentSquare.setClosesCompany(true);
         } else if (qName.equals("endsGame"))
         {
            if (currentSquare != null)
               currentSquare.setEndsGame(true);
         } else if (qName.equals("start"))
         {
            if (currentSquare != null)
            {
               currentSquare.setStart(true);
               startSpaces.add(currentSquare);
            }
         } else
         {
            System.err.println("Unknown start element: " + name);
         }
      } else
      {
         System.err.println("Unknown start element: {" + uri + "}" + name);
      }
   }

   /**
    * DefaultHandler callback method.
    */
   public void endElement(String uri, String name, String qName)
   {
      StockPrice square;
      if ("".equals(uri))
      {
         if (qName.equals("stockmarket"))
         {
            /*
             * Create the 2-dimensional array. One extra row and column to
             * facilitate border checking. May be unneeded.
             */
            stockChart = new StockPrice[numRows + 1][numCols + 1];
            Iterator it = stockChartSquares.values().iterator();
            while (it.hasNext())
            {
               square = (StockPrice) it.next();
               stockChart[square.getRow()][square.getColumn()] = square;
            }
         } else if (qName.equals("square"))
         {
            currentSquare = null;
         }
      } else
      {
         System.out.println("Unknown end element:   {" + uri + "}" + name);
      }
   }

   /**
    * DefaultHandler callback method.
    */
   public void characters(char ch[], int start, int length)
   {
   }

   /*--- Getters ---*/
   /**
    * @return
    */
   public int getNumCols()
   {
      return numCols;
   }

   /**
    * @return
    */
   public int getNumRows()
   {
      return numRows;
   }

   /**
    * @return
    */
   public StockPrice[][] getStockChart()
   {
      return stockChart;
   }

   public StockPrice getStockPrice(int row, int col)
   {
      if (row >= 0 && row < numRows && col >= 0 && col < numCols)
      {
         return stockChart[row][col];
      } else
      {
         return null;
      }
   }

   /*--- Actions ---*/

   public void moveUp(Company company)
   {
      StockPrice oldsquare = company.getCurrentPrice();
      StockPrice newsquare = null;
      int row = oldsquare.getRow();
      int col = oldsquare.getColumn();
      if (row > 0)
      {
         newsquare = getStockPrice(row - 1, col);
      } else if (upOrDownRight && col < numCols - 1)
      {
         newsquare = getStockPrice(row + 1, col + 1);
      }
      processMove(company, oldsquare, newsquare);
   }

   public void moveDown(Company company, int numberOfSpaces)
   {
      StockPrice oldsquare = company.getCurrentPrice();
      StockPrice newsquare = null;
      int row = oldsquare.getRow();
      int col = oldsquare.getColumn();

      /* Drop the indicated number of rows */
      int newrow = row + numberOfSpaces;

      /* Don't drop below the bottom of the chart */
      while (newrow >= numRows || getStockPrice(newrow, col) == null)
         newrow--;

      /*
       * If marker landed just below a ledge, and NOT because it was bounced by
       * the bottom of the chart, it will stay just above the ledge.
       */
      if (getStockPrice(newrow, col).isBelowLedge()
            && newrow == row + numberOfSpaces)
         newrow--;

      if (newrow > row)
      {
         newsquare = getStockPrice(newrow, col);
      }
      if (newsquare.closesCompany())
      {
         company.setClosed(true);
         oldsquare.removeToken(company);
         System.out.println(company.getName() + " closes at "
               + newsquare.getName());
      } else
      {
         processMove(company, oldsquare, newsquare);
      }
   }

   public void moveRightOrUp(Company company)
   {
      /* Ignore the amount for now */
      StockPrice oldsquare = company.getCurrentPrice();
      StockPrice newsquare = null;
      int row = oldsquare.getRow();
      int col = oldsquare.getColumn();
      if (col < numCols - 1 && !oldsquare.isLeftOfLedge()
            && (newsquare = getStockPrice(row, col + 1)) != null)
      {
      } else if (row > 0 && (newsquare = getStockPrice(row - 1, col)) != null)
      {
      }
      processMove(company, oldsquare, newsquare);
   }

   public void moveLeftOrDown(Company company)
   {
      StockPrice oldsquare = company.getCurrentPrice();
      StockPrice newsquare = null;
      int row = oldsquare.getRow();
      int col = oldsquare.getColumn();
      if (col > 0 && (newsquare = getStockPrice(row, col - 1)) != null)
      {
      } else if (row < numRows - 1
            && (newsquare = getStockPrice(row + 1, col)) != null)
      {
      }
      if (newsquare.closesCompany())
      {
         company.setClosed(true);
         oldsquare.removeToken(company);
         System.out.println(company.getName() + " closes at "
               + newsquare.getName());
      } else
      {
         processMove(company, oldsquare, newsquare);
      }
   }

   protected void processMove(Company company, StockPrice from, StockPrice to)
   {
      // To be written to a log file in the future.
      if (to == null || from == to)
      {
         System.out.println(company.getName() + " stays at " + from.getName());
      } else
      {
         from.removeToken(company);
         to.addToken(company);
         company.setCurrentPrice(to);
         System.out.println(company.getName() + " moved from " + from.getName()
               + " to " + to.getName());

      }
   }

   /**
    * @return
    */
   public List getStartSpaces()
   {
      return startSpaces;
   }

}